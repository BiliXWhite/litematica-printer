package me.aleksilassila.litematica.printer.handler;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import lombok.Getter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.*;
import me.aleksilassila.litematica.printer.printer.*;
import me.aleksilassila.litematica.printer.printer.ActionManager;
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
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 打印机客户端玩家Tick抽象处理器
 */
public abstract class ClientPlayerTickHandler extends ConfigUtils {
    // 交互盒引用：存储迭代范围，null表示不使用迭代功能
    @Getter
    @Nullable
    public final AtomicReference<PrinterBox> boxRef;

    @Getter
    private final String id;

    @Getter
    @Nullable
    private final PrintModeType printMode;

    @Getter
    @Nullable
    private final ConfigBoolean enableConfig;

    @Getter
    @Nullable
    private final ConfigOptionList selectionType;

    // 跳过迭代标志
    private final AtomicReference<Boolean> skipIteration = new AtomicReference<>(false);

    // GUI信息队列（用于渲染）
    private final Queue<GuiBlockInfo> guiQueue = new ConcurrentLinkedQueue<>();

    // 迭代状态缓存（性能优化关键）
    private Iterator<BlockPos> cachedIterator = null;
    private final BlockPos lastBasePos = null;
    private int expandRange = -1;

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
    @Nullable
    private PrinterBox lastBox;
    @Nullable
    private BlockPos lastPos;

    private long lastTickTime = -1L;

    @Getter
    private int renderIndex = 0;

    private int guiCacheTicks;

    protected ClientPlayerTickHandler(String id, @Nullable PrintModeType printMode, @Nullable ConfigBoolean enableConfig, @Nullable ConfigOptionList selectionType, boolean useBox) {
        this.id = id;
        this.printMode = printMode;
        this.enableConfig = enableConfig;
        this.selectionType = selectionType;
        this.boxRef = useBox ? new AtomicReference<>() : null;
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

    /**
     * 核心Tick方法：处理GUI缓存、间隔控制、迭代范围更新和方块迭代
     */
    public void tick() {
        // GUI缓存倒计时
        if (guiCacheTicks > 0) {
            guiCacheTicks--;
        } else {
            guiQueue.clear();
            renderIndex = 0;
        }

        // 执行间隔控制
        int tickInterval = getTickInterval();
        if (tickInterval > 0) {
            long currentTickTime = ClientPlayerTickManager.getCurrentHandlerTime();
            if (lastTickTime != -1L && currentTickTime - lastTickTime < tickInterval) {
                return;
            }
            lastTickTime = currentTickTime;
        }

        // 基础检查
        if (!isPrinterEnable()) {
            lastPos = null;
            return;
        }

        if (!isConfigAllowed()) {
            lastPos = null;
            return;
        }

        updateVariables();
        if (mc == null || level == null || player == null || connection == null || gameMode == null || gameType == null) {
            lastPos = null;
            return;
        }

        updateBox();
        // 例如填充和拍流体等需要额外方块的模式，需要提前处理好转换
        preprocess();

        // 执行方块迭代
        if (!iterateBlocks()) {
            lastPos = null;
        }
    }

    /**
     * 更新交互盒：根据玩家位置和配置动态调整迭代范围
     */
    private void updateBox() {
        if (boxRef == null) return;

        BlockPos eyePos = new BlockPos(new Vec3i((int) Math.round(player.getX()), (int) Math.round(player.getEyeY()), (int) Math.round(player.getZ())));
        PrinterBox box = boxRef.get();

        int currentRange = (int) Math.ceil(ConfigUtils.getEffectiveRange());

        boolean needRebuild = box == null
                || !box.equals(lastBox)
                || lastPos == null
                || !lastPos.closerThan(eyePos, ConfigUtils.getEffectiveRange() * 0.4)
                || expandRange != currentRange;

        if (needRebuild) {
            lastPos = eyePos;
            expandRange = currentRange;

            box = new PrinterBox(eyePos).expand(expandRange, expandRange, expandRange);
            lastBox = box;
            boxRef.set(box);

            box.iterationMode = (IterationOrderType) Configs.Core.ITERATION_ORDER.getOptionListValue();
            box.xIncrement = !Configs.Core.X_REVERSE.getBooleanValue();
            box.yIncrement = !Configs.Core.Y_REVERSE.getBooleanValue();
            box.zIncrement = !Configs.Core.Z_REVERSE.getBooleanValue();
            
            cachedIterator = null;
        }
    }

    /**
     * 执行方块迭代
     * @return 是否被中断
     */
    private boolean iterateBlocks() {
        if (boxRef == null || !canExecute()) return false;
    
        PrinterBox box = boxRef.get();
        if (box == null || !canIterate()) return false;
    
        if (cachedIterator == null) {
            cachedIterator = box.iterator();
        }
    
        int maxExecs = getMaxExecutions();
        int timeLimit = getIterationTimeLimit();
        int execCount = 0;
    
        boolean debugMode = Configs.Core.DEBUG_OUTPUT.getBooleanValue();
        boolean needRangeCheck = needsRangeCheck();
        boolean isSchematic = isSchematicHandler();
    
        long startTime = timeLimit > 0 ? System.nanoTime() : 0;
        long timeLimitNanos = timeLimit * 1_000_000L;
        int checkInterval = 10;
        int iterCount = 0;
    
        skipIteration.set(false);
        guiQueue.clear();
        renderIndex = 0;
    
        while (cachedIterator.hasNext()) {
            if (timeLimit > 0 && ++iterCount % checkInterval == 0) {
                if (System.nanoTime() - startTime >= timeLimitNanos) {
                    stopIteration(true);
                    return true;
                }
            }
    
            if (skipIteration.get() || ActionManager.INSTANCE.needWaitModifyLook) {
                stopIteration(true);
                return true;
            }
    
            BlockPos pos = cachedIterator.next();
            if (pos == null) continue;
    
            if (!PlayerUtils.canInteracted(pos)) continue;
    
            if (needRangeCheck) {
                if (isSchematic ? !LitematicaUtils.isSchematicBlock(pos)
                        : !LitematicaUtils.isWithinSelection1ModeRange(pos)) {
                    continue;
                }
    
                if (selectionType != null && !PlayerUtils.isPositionInSelectionRange(player, pos, selectionType)) {
                    continue;
                }
            }
    
            if (debugMode) {
                GuiBlockInfo gui = isSchematic
                        ? new GuiBlockInfo(level, SchematicWorldHandler.getSchematicWorld(), pos)
                        : new GuiBlockInfo(level, null, pos);
                gui.interacted = true;
                gui.posInSelectionRange = true;
                gui.execute = canProcessPos(pos) && !isOnCooldown(pos);
                addGuiInfo(gui);
            }
    
            if (canProcessPos(pos) && !isOnCooldown(pos)) {
                executeIteration(pos, skipIteration);
    
                if (skipIteration.get() || (maxExecs > 0 && ++execCount >= maxExecs)) {
                    stopIteration(true);
                    return true;
                }
            }
        }
    
        cachedIterator = null;
        stopIteration(false);
        return false;
    }

    protected void stopIteration(boolean interrupt) {
        // 如果被打断，保留迭代器状态以便下次继续
        // 如果完成迭代，cachedIterator 会在 iterateBlocks 中被置 null
    }

    protected boolean isSchematicHandler() {
        return false;
    }

    /**
     * 添加GUI信息到队列
     */
    private void addGuiInfo(GuiBlockInfo info) {
        if (info != null) {
            guiQueue.add(info);
            guiCacheTicks = 20;
        }
    }

    /**
     * 获取下一个GUI信息（渲染阶段调用）
     */
    @Nullable
    public GuiBlockInfo nextGuiInfo() {
        if (guiQueue.isEmpty()) return null;

        GuiBlockInfo[] arr = guiQueue.toArray(new GuiBlockInfo[0]);
        if (renderIndex >= arr.length) {
            renderIndex = 0;
            return arr[arr.length - 1];
        }
        return arr[renderIndex++];
    }

    /**
     * 获取最后一个GUI信息
     */
    @Nullable
    public GuiBlockInfo getLastGuiInfo() {
        if (guiQueue.isEmpty()) return null;
        GuiBlockInfo[] arr = guiQueue.toArray(new GuiBlockInfo[0]);
        return arr[arr.length - 1];
    }

    public void setGuiInfo(@Nullable GuiBlockInfo info) {
        addGuiInfo(info);
    }

    public int getGuiQueueSize() {
        return guiQueue.size();
    }

    /**
     * 配置层面的执行权限校验
     */
    private boolean isConfigAllowed() {
        if (!ConfigUtils.isPrinterEnable()) return false;

        if (printMode != null && enableConfig != null) {
            WorkingModeType mode = (WorkingModeType) Configs.Core.WORK_MODE.getOptionListValue();
            return switch (mode) {
                case SINGLE -> Configs.Core.WORK_MODE_TYPE.getOptionListValue().equals(printMode);
                case MULTI -> enableConfig.getBooleanValue();
            };
        }

        return enableConfig == null || enableConfig.getBooleanValue();
    }

    protected int getTickInterval() {
        return -1;
    }

    protected int getMaxExecutions() {
        return -1;
    }

    /**
     * 获取迭代时间限制（毫秒），0表示禁用
     */
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

    public boolean canProcessPos(BlockPos pos) {
        return true;
    }

    /**
     * 单次方块迭代的核心执行方法，子类重写实现具体逻辑
     */
    protected void executeIteration(BlockPos pos, AtomicReference<Boolean> skipIteration) {
    }

    /**
     * 判断方块是否处于冷却中
     */
    public boolean isOnCooldown(@Nullable BlockPos pos) {
        if (level == null || pos == null) return true;
        return BlockPosCooldownManager.INSTANCE.isOnCooldown(level, id, pos);
    }

    public boolean isOnCooldown(String name, @Nullable BlockPos pos) {
        if (level == null || pos == null) return true;
        return BlockPosCooldownManager.INSTANCE.isOnCooldown(level, id + "_" + name, pos);
    }

    /**
     * 设置方块冷却时间
     */
    public void setCooldown(@Nullable BlockPos pos, int ticks) {
        if (level == null || pos == null || ticks < 1) return;
        BlockPosCooldownManager.INSTANCE.setCooldown(level, id, pos, ticks);
    }

    public void setCooldown(String name, @Nullable BlockPos pos, int ticks) {
        if (level == null || pos == null || ticks < 1) return;
        BlockPosCooldownManager.INSTANCE.setCooldown(level, id + "_" + name, pos, ticks);
    }


    protected Direction[] getPlayerOrderedByNearest() {
        return Direction.orderedByNearest(player);
    }

    protected Direction getPlayerPlacementDirection() {
        return Direction.orderedByNearest(player)[0].getOpposite();
    }

    protected boolean needsRangeCheck() {
        return true;
    }
}