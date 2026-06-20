package me.aleksilassila.litematica.printer.handler;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.util.LayerMode;
import fi.dy.masa.malilib.util.LayerRange;
import lombok.Getter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.*;
import me.aleksilassila.litematica.printer.printer.*;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.printer.OperationQueue;
import me.aleksilassila.litematica.printer.printer.QueuedOperation;
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
import net.minecraft.world.phys.Vec3;
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
    // Cached layer state for rebuild detection
    private int lastLayerMin = Integer.MIN_VALUE;
    private int lastLayerMax = Integer.MIN_VALUE;
    @Nullable
    private Direction.Axis lastLayerAxis = null;

    private long lastTickTime = -1L;

    @Getter
    private int renderIndex = 0;

    // 高亮队列（记录正在处理或已完成但尚在渐隐中的方块）
    @Getter
    private final Queue<PendingHighlight> pendingHighlights = new ConcurrentLinkedQueue<>();

    public record PendingHighlight(BlockPos pos, long time, HighlightType type) {
        public PendingHighlight(BlockPos pos, long time) {
            this(pos, time, HighlightType.PLACE);
        }
    }

    // 扫描状态机
    @Getter
    private ScanState scanState = ScanState.FULL;
    private int idleTicks = 0;
    protected boolean didWorkThisTick = false;

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
            pendingHighlights.clear();
            return;
        }

        if (!isConfigAllowed()) {
            lastPos = null;
            pendingHighlights.clear();
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

        // Phase 1: 消费 BlockUpdate 脏坐标 → 生成修复操作
        OperationQueue.INSTANCE.processDirty();

        // --- Scan State Gate: 惰性 / 部分 / 全量 模式选择 ---
        if (scanState == ScanState.LAZY) {
            // shouldProcessQueue()=false 的 handler（如 GUI）不依赖队列判空
            boolean noQueueWork = !shouldProcessQueue() || OperationQueue.INSTANCE.isEmpty();
            if (noQueueWork && !RegionTracker.INSTANCE.hasDirtyRegions()) {
                return;  // 无待处理操作，跳过整个迭代阶段
            }
            // BlockUpdate 唤醒了我们 → 按脏区域数量决定重扫粒度
            int dirtyRegions = RegionTracker.INSTANCE.getDirtyCount();
            int fullThreshold = Configs.Core.LAZY_DIRTY_WAKE_THRESHOLD.getIntegerValue();
            if (fullThreshold > 0 && dirtyRegions > 0 && dirtyRegions < fullThreshold) {
                scanState = ScanState.PARTIAL;
                cachedIterator = RegionTracker.INSTANCE.createDirtyRegionIterator();
            } else {
                scanState = ScanState.FULL;
                // FULL 全量扫描会覆盖整个 box，清除脏标记避免下个 tick 重复唤醒
                RegionTracker.INSTANCE.clearAllDirty();
            }
        }

        if (scanState == ScanState.PARTIAL && cachedIterator == null) {
            cachedIterator = RegionTracker.INSTANCE.createDirtyRegionIterator();
        }

        // Phase 2: 重置状态，然后执行（队列优先 + 迭代扫描）
        skipIteration.set(false);
        didWorkThisTick = false;

        int maxExecs = getMaxExecutions();
        int execCount = 0;

        // 队列消费：依次 poll 所有操作，当前 handler 能处理的就执行，不能处理的放回队尾
        // 这保证了 repair op 总能到达正确的 handler，不会被 GUI 等 handler 误消费
        // 注意：队列操作也计入 execCount，确保不会超过每 tick 最大执行次数
        if (shouldProcessQueue()) {
            int ops = OperationQueue.INSTANCE.size();
            for (int i = 0; i < ops && !skipIteration.get(); i++) {
                QueuedOperation op = OperationQueue.INSTANCE.poll();
                if (op == null) break;
                if (!isOnCooldown(op.getPos()) && canProcessPos(op.getPos())) {
                    executeIteration(op.getPos(), skipIteration);
                    didWorkThisTick = true;
                    if (maxExecs > 0 && ++execCount >= maxExecs) {
                        break;
                    }
                } else {
                    OperationQueue.INSTANCE.addLast(op);
                }
            }
        }

        if (!skipIteration.get()) {
            int remaining = maxExecs > 0 ? Math.max(0, maxExecs - execCount) : 0;
            if (remaining > 0 || maxExecs <= 0) {
                if (!iterateBlocks(remaining)) {
                    if (scanState != ScanState.PARTIAL) lastPos = null;
                }
            }
        }

        // --- 空闲跟踪：无工作 → 计数，稳定后进入惰性 ---
        // shouldProcessQueue()=false 的 handler 不依赖队列判空（如 GUI），直接走空闲计数
        if (!didWorkThisTick && (!shouldProcessQueue() || OperationQueue.INSTANCE.isEmpty())) {
            idleTicks++;
            int lazyThreshold = Configs.Core.LAZY_ENTER_TICKS.getIntegerValue();
            if (lazyThreshold > 0 && idleTicks >= lazyThreshold) {
                scanState = ScanState.LAZY;
                idleTicks = 0;
                RegionTracker.INSTANCE.clearAllDirty();
            }
        } else {
            idleTicks = 0;
        }

        // PARTIAL 扫描完成后直接进入 LAZY：脏区域已在扫描中被 DirtyRegionIterator 逐个 markClean
        // 任何新的方块变化都会通过 BlockUpdate 重新唤醒 LAZY
        if (scanState == ScanState.PARTIAL && cachedIterator == null) {
            scanState = ScanState.LAZY;
            idleTicks = 0;
            RegionTracker.INSTANCE.clearAllDirty();
        }
    }

    /**
     * 更新交互盒：根据玩家位置和配置动态调整迭代范围
     * 使用 exact range 计算盒边界 (floor/ceil)，避免 round() 偏移导致边界方块丢失
     */
    private void updateBox() {
        if (boxRef == null) return;

        BlockPos eyePos = new BlockPos(new Vec3i((int) Math.round(player.getX()), (int) Math.round(player.getEyeY()), (int) Math.round(player.getZ())));
        PrinterBox box = boxRef.get();

        double effectiveRange = ConfigUtils.getEffectiveRange();
        int currentRange = (int) Math.ceil(effectiveRange);

        // Pre-fetch layer state for rebuild detection and box clamping
        LayerRange layerRange = DataManager.getRenderLayerRange();
        LayerMode layerMode = layerRange.getLayerMode();
        //#if MC >= 260200
        //$$ Direction.Axis layerAxis = layerRange.getAxis().toVanilla();
        //#else
        Direction.Axis layerAxis = layerRange.getAxis();
        //#endif
        int layerMin = layerRange.getLayerMin();
        int layerMax = layerRange.getLayerMax();

        boolean needRebuild = box == null
                || !box.equals(lastBox)
                || lastPos == null
                || !lastPos.closerThan(eyePos, effectiveRange * 0.4)
                || expandRange != currentRange
                || layerMin != lastLayerMin
                || layerMax != lastLayerMax
                || layerAxis != lastLayerAxis;

        if (needRebuild) {
            lastPos = eyePos;
            expandRange = currentRange;
            lastLayerMin = layerMin;
            lastLayerMax = layerMax;
            lastLayerAxis = layerAxis;

            // 直接用 player 精确位置 ±range 算盒子的整数边界，避免 round() 偏移导致边界方块丢失
            int minX = (int) Math.floor(player.getX() - effectiveRange);
            int maxX = (int) Math.ceil(player.getX() + effectiveRange);
            int minY = (int) Math.floor(player.getEyeY() - effectiveRange);
            int maxY = (int) Math.ceil(player.getEyeY() + effectiveRange);
            int minZ = (int) Math.floor(player.getZ() - effectiveRange);
            int maxZ = (int) Math.ceil(player.getZ() + effectiveRange);

            // Clamp box to the active render layer so we never iterate outside it
            if (selectionType != null
                    && selectionType.getOptionListValue() instanceof SelectionType st
                    && st == SelectionType.LITEMATICA_RENDER_LAYER
                    && layerMode != LayerMode.ALL) {
                switch (layerAxis) {
                    case Y -> { minY = Math.max(minY, layerMin); maxY = Math.min(maxY, layerMax); }
                    case X -> { minX = Math.max(minX, layerMin); maxX = Math.min(maxX, layerMax); }
                    case Z -> { minZ = Math.max(minZ, layerMin); maxZ = Math.min(maxZ, layerMax); }
                }
            }

            // Clamp Y for above/below-player selection modes
            if (selectionType != null) {
                if (selectionType.getOptionListValue() instanceof SelectionType st) {
                    if (st == SelectionType.LITEMATICA_SELECTION_BELOW_PLAYER) {
                        maxY = Math.min(maxY, (int) Math.floor(player.getY()));
                    } else if (st == SelectionType.LITEMATICA_SELECTION_ABOVE_PLAYER) {
                        minY = Math.max(minY, (int) Math.ceil(player.getY()));
                    }
                }
            }

            box = new PrinterBox(minX, minY, minZ, maxX, maxY, maxZ);
            lastBox = box;
            boxRef.set(box);

            box.iterationMode = (IterationOrderType) Configs.Core.ITERATION_ORDER.getOptionListValue();
            box.xIncrement = !Configs.Core.X_REVERSE.getBooleanValue();
            box.yIncrement = !Configs.Core.Y_REVERSE.getBooleanValue();
            box.zIncrement = !Configs.Core.Z_REVERSE.getBooleanValue();

            cachedIterator = null;
            RegionTracker.INSTANCE.rebuild(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            scanState = ScanState.FULL;
            // 仅在玩家移动或配置变更时重置空闲计数；扫描周期自然完成（lastPos == null）不清零，
            // 否则空闲计数永远达不到惰性阈值
            if (box != null) { // 非首次初始化
                boolean playerMoved = lastPos != null && !lastPos.closerThan(eyePos, effectiveRange * 0.4);
                if (playerMoved || expandRange != currentRange) {
                    idleTicks = 0;
                }
            } else {
                idleTicks = 0;
            }
        }
    }

    private boolean iterateBlocks(int maxExecs) {
        if (boxRef == null || !canExecute()) return false;
    
        PrinterBox box = boxRef.get();
        if (box == null || !canIterate()) return false;
    
        if (cachedIterator == null) {
            cachedIterator = box.iterator();
            // 清理已超出渐隐时长的过期条目，保留仍在渐隐中的条目不影响显示
            long expireCutoff = System.currentTimeMillis() - Configs.Highlight.HIGHLIGHT_FADE_DURATION.getIntegerValue() * 100L;
            pendingHighlights.removeIf(ph -> ph.time() < expireCutoff);
        }
    
        int timeLimit = getIterationTimeLimit();
        int execCount = 0;
    
        boolean debugMode = Configs.Core.DEBUG_OUTPUT.getBooleanValue();
        boolean needRangeCheck = needsRangeCheck();
        boolean isSchematic = isSchematicHandler();

        // 缓存不变值到循环外，消除每位置的 getEyePosition() Vec3 分配
        Vec3 eyePos = player.getEyePosition();
        double effectiveRange = ConfigUtils.getEffectiveRange();
        RadiusShapeType shapeType = Configs.Core.ITERATOR_SHAPE.getOptionListValue() instanceof RadiusShapeType s ? s : null;

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

            if (shapeType != null) {
                if (!PlayerUtils.canInteracted(pos, eyePos, effectiveRange, shapeType)) continue;
            } else if (!PlayerUtils.canInteracted(pos)) continue;
    
            if (needRangeCheck) {
                if (isSchematic ? !SchematicSnapshot.INSTANCE.contains(pos)
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
    
                if (!isOnCooldown(pos) && canProcessPos(pos)) {
                    executeIteration(pos, skipIteration);
                    didWorkThisTick = true;

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

    /**
     * 当前 handler 是否需要处理操作队列中的修复操作。
     * 仅实际执行方块操作的 handler（PRINT、FILL 等）应返回 true；
     * GUI handler 仅负责 HUD 渲染，不应消费队列操作。
     */
    protected boolean shouldProcessQueue() {
        return true;
    }

    public boolean canProcessPos(BlockPos pos) {
        return true;
    }

    /**
     * 获取当前 handler 的默认高亮类型
     */
    protected HighlightType getHighlightType() {
        return HighlightType.PLACE;
    }

    /**
     * 添加一个高亮方块到队列
     */
    protected void addHighlight(BlockPos pos, HighlightType type) {
        BlockPos immutable = pos.immutable();
        // 如果同一位置已有未完成的高亮，替换为最新的请求
        pendingHighlights.removeIf(ph -> ph.pos().equals(immutable));
        pendingHighlights.add(new PendingHighlight(immutable, System.currentTimeMillis(), type));
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