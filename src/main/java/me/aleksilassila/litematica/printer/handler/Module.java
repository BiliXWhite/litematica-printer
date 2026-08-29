package me.aleksilassila.litematica.printer.handler;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import lombok.Getter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.*;
import me.aleksilassila.litematica.printer.printer.*;
import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.utils.BreakUtils;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.LitematicaUtils;
import me.aleksilassila.litematica.printer.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Module extends ConfigUtils {
    private static final ScheduledExecutorService TIMEOUT_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Printer-TimeoutGuard");
                t.setDaemon(true);
                return t;
            });
    @Getter
    @Nullable
    public final AtomicReference<PrinterBox> box;
    protected final IteratorManager iteratorManager = new IteratorManager();
    @Getter
    private final String id;
    @Getter
    @Nullable
    private final ConfigBoolean enableConfig;
    @Getter
    @Nullable
    private final ConfigOptionList selectionType;
    private final AtomicReference<Boolean> skipIteration = new AtomicReference<>(false);
    private final AtomicBoolean timeLimitExceeded = new AtomicBoolean(false);
    @Getter
    private final Queue<PendingHighlight> pendingHighlights = new ConcurrentLinkedQueue<>();
    protected Minecraft mc;
    protected ClientLevel level;
    protected LocalPlayer player;
    protected ClientPacketListener connection;
    protected MultiPlayerGameMode gameMode;
    protected GameType gameType;
    @Nullable
    protected HitResult hitResult;
    @Nullable
    protected BlockHitResult blockHitResult;
    protected boolean needSchematic = false;
    private long lastTickTime = -1L;
    @Getter
    private ScanState scanState = ScanState.RUNNING;

    @Nullable
    private BlockPos waitingPos = null;

    // 按方块分类：本轮扫描锁定的方块类型（null = 未锁定/普通迭代）
    @Nullable
    private Item currentCycleItem = null;

    private volatile GuiBlockInfo currentGuiInfo = null;

    protected Module(String id, @Nullable ConfigBoolean enableConfig, @Nullable ConfigOptionList selectionType, boolean useBox) {
        this.id = id;
        this.enableConfig = enableConfig;
        this.selectionType = selectionType;
        this.box = useBox ? new AtomicReference<>() : null;
        updateVariables();
    }

    protected void updateVariables() {
        mc = Minecraft.getInstance();
        level = mc.level;
        player = mc.player;
        connection = mc.getConnection();
        gameMode = mc.gameMode;
        gameType = gameMode == null ? null : gameMode.getPlayerMode();
        hitResult = mc.hitResult;
        blockHitResult = (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK)
                ? (BlockHitResult) hitResult : null;
    }

    public void tick() {
        int tickInterval = getTickInterval();
        if (tickInterval > 0) {
            long currentTickTime = ModuleManager.getCurrentHandlerTime();
            if (lastTickTime != -1L && currentTickTime - lastTickTime < tickInterval) {
                return;
            }
            lastTickTime = currentTickTime;
        }

        if (!isConfigAllowed()) {
            pendingHighlights.clear();
            return;
        }

        updateVariables();
        if (mc == null || level == null || player == null || connection == null || gameMode == null || gameType == null) {
            return;
        }

        if (box == null) return;
        if (iteratorManager.tryBuildBox(player, selectionType != null ? selectionType.getOptionListValue() : null)) {
            box.set(iteratorManager.getBox());
            scanState = ScanState.RUNNING;
            waitingPos = null;
            currentCycleItem = null;
            iteratorManager.reset();
        }

        preprocess();

        skipIteration.set(false);
        int remainingExecs = Math.max(getMaxExecutions(), 0);
        if (!canExecute() || !canIterate()) return;

        // 高亮渐隐
        long cutoff = System.currentTimeMillis() - Configs.Highlight.HIGHLIGHT_FADE_DURATION.getIntegerValue() * 100L;
        pendingHighlights.removeIf(ph -> ph.time() < cutoff);

        // 远离工作区时提前退出，避免空跑卡顿
        if (needsAreaCheck() && !isPlayerRangeInWorkArea()) return;

        iterateBlocks(remainingExecs);
    }

    private void iterateBlocks(int maxExecs) {
        int execCount = 0;
        int timeLimitMs = getIterationTimeLimit();

        skipIteration.set(false);
        timeLimitExceeded.set(false);

        // 超时保护
        ScheduledFuture<?> timeoutTask = null;
        if (timeLimitMs > 0) {
            timeoutTask = TIMEOUT_SCHEDULER.schedule(
                    () -> timeLimitExceeded.set(true),
                    timeLimitMs, TimeUnit.MILLISECONDS);
        }

        try {
            if (scanState == ScanState.WAITING) {
                BlockPos pos = waitingPos;
                waitingPos = null;
                scanState = ScanState.RUNNING;
                if (pos != null && needsWork(pos)) {
                    executeIteration(pos, skipIteration);
                    execCount++;
                    if (maxExecs > 0 && execCount >= maxExecs) return;
                }
                if (skipIteration.get() || ActionManager.INSTANCE.needWaitModifyLook) return;
            }

            while (true) {
                if (timeLimitExceeded.get()) return;
                if (skipIteration.get() || ActionManager.INSTANCE.needWaitModifyLook) return;

                BlockPos pos = iteratorManager.next();
                if (pos == null) {
                    currentCycleItem = null; // 一轮扫描耗尽，重置方块分类
                    return;
                }

                if (needsAreaCheck() && !isPosInWorkspace(pos)) continue;

                boolean executed = false;
                if (needsWork(pos)) {
                    // 按方块分类：一轮扫描仅处理一种方块类型（可选开关）
                    if (!Configs.Core.CLASSIFY_BY_BLOCK.getBooleanValue() || isCycleItemMatch(pos)) {
                        executeIteration(pos, skipIteration);
                        executed = true;
                        if (maxExecs > 0 && ++execCount >= maxExecs) return;
                    }
                }

                currentGuiInfo = new GuiBlockInfo(pos,
                        level.getBlockState(pos), LitematicaUtils.getBlockState(pos),
                        PlayerUtils.canInteracted(pos), executed,
                        isPosInWorkspace(pos) && PlayerUtils.canInteracted(pos));
            }
        } finally {
            if (timeoutTask != null) timeoutTask.cancel(false);
            timeLimitExceeded.set(false);
        }
    }

    private boolean isCycleItemMatch(BlockPos pos) {
        Item[] items = getRequiredItems(pos);
        Item item = items != null && items.length > 0 ? items[0] : null;
        if (item == null) return true; // 无物品需求的位置始终处理（对应旧 noItemPositions）
        if (currentCycleItem == null) {
            currentCycleItem = item; // 锁定本轮首个所需物品
            return true;
        }
        return item.equals(currentCycleItem);
    }

    /**
     * 粗筛：玩家可达范围是否与工作区有交集。
     * 投影模式 isSchematicBlock 已够快，无需提前退出；
     * 选区模式用选区边界盒做 O(1) 排空判断。
     */
    private boolean isPlayerRangeInWorkArea() {
        if (needSchematic) return true;
        if (player == null) return false;
        PrinterBox selectBounds = LitematicaUtils.getSelectionBounds();
        if (selectBounds == null) return false;
        double r = ConfigUtils.getEffectiveRange();
        double px = player.getX(), py = player.getEyeY(), pz = player.getZ();
        return Math.floor(px - r) <= selectBounds.maxX && Math.ceil(px + r) >= selectBounds.minX
            && Math.floor(py - r) <= selectBounds.maxY && Math.ceil(py + r) >= selectBounds.minY
            && Math.floor(pz - r) <= selectBounds.maxZ && Math.ceil(pz + r) >= selectBounds.minZ;
    }

    protected void enterWaiting(@Nullable BlockPos pos) {
        scanState = ScanState.WAITING;
        waitingPos = pos;
    }

    private boolean needsWork(BlockPos pos) {
        return !isOnCooldown(pos) && canProcessPos(pos) && !isCorrectBlock(pos);
    }

    private boolean isPosInWorkspace(BlockPos pos) {
        return needSchematic
                ? LitematicaUtils.isSchematicBlock(pos)
                : LitematicaUtils.inSelection(pos);
    }

    @Nullable
    protected Item[] getRequiredItems(BlockPos pos) {
        return null;
    }

    public void resetScanState() {
        scanState = ScanState.RUNNING;
        waitingPos = null;
        currentCycleItem = null;
        iteratorManager.reset();
    }

    @Nullable
    public GuiBlockInfo getGuiInfo() {
        return currentGuiInfo;
    }

    private boolean isConfigAllowed() {
        if (!ConfigUtils.isPrinterEnable()) return false;
        return enableConfig == null || enableConfig.getBooleanValue();
    }

    protected int getTickInterval() {
        return -1;
    }

    protected int getMaxExecutions() {
        return -1;
    }

    protected int getIterationTimeLimit() {
        return Configs.Core.ITERATION_TIME_LIMIT.getIntegerValue();
    }

    protected void preprocess() {
    }

    protected boolean canExecute() {
        return true;
    }

    protected boolean canIterate() {
        return true;
    }

    public abstract boolean canProcessPos(BlockPos pos);

    public abstract boolean isCorrectBlock(BlockPos pos);

    protected void addHighlight(BlockPos pos, HighlightType type) {
        BlockPos immutable = pos.immutable();
        pendingHighlights.removeIf(ph -> ph.pos().equals(immutable));
        pendingHighlights.add(new PendingHighlight(immutable, System.currentTimeMillis(), type));
    }

    protected void executeIteration(BlockPos pos, AtomicReference<Boolean> skipIteration) {
    }

    public boolean isOnCooldown(@Nullable BlockPos pos) {
        if (level == null || pos == null) return true;
        return BlockPosCooldownManager.INSTANCE.isOnCooldown(level, id, pos);
    }

    public void setCooldown(@Nullable BlockPos pos, int ticks) {
        if (level == null || pos == null || ticks < 1) return;
        BlockPosCooldownManager.INSTANCE.setCooldown(level, id, pos, ticks);
    }

    protected Direction getPlayerPlacementDirection() {
        return Direction.orderedByNearest(player)[0].getOpposite();
    }

    protected boolean needsAreaCheck() {
        return true;
    }

    public record PendingHighlight(BlockPos pos, long time, HighlightType type) {
    }
}