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
    // 玩家交互盒：存储迭代范围，null表示不使用迭代功能
    @Getter
    @Nullable
    public final AtomicReference<PrinterBox> playerInteractionBox;

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
    
    // GUI 方块信息队列（用于渲染）
    private final Queue<GuiBlockInfo> guiBlockInfoQueue = new ConcurrentLinkedQueue<>();
    
    // 迭代状态缓存（性能优化关键）
    private Iterator<BlockPos> cachedIterator = null;
    private final BlockPos lastBasePos = null;
    private int cachedExpandRange = -1;

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
    private PrinterBox lastPlayerInteractionBox;
    @Nullable
    private BlockPos lastPlayerPos;

    private long lastTickTime = -1L;

    @Getter
    private int renderIndex = 0;

    private int guiBlockPosCacheTicks;

    protected ClientPlayerTickHandler(String id, @Nullable PrintModeType printMode, @Nullable ConfigBoolean enableConfig, @Nullable ConfigOptionList selectionType, boolean useBox) {
        this.id = id;
        this.printMode = printMode;
        this.enableConfig = enableConfig;
        this.selectionType = selectionType;
        this.playerInteractionBox = useBox ? new AtomicReference<>() : null;
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
        if (guiBlockPosCacheTicks > 0) {
            guiBlockPosCacheTicks--;
        } else {
            guiBlockInfoQueue.clear();
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
        if (!isEnable()) {
            lastPlayerPos = null;
            return;
        }

        updateVariables();
        if (mc == null || level == null || player == null || connection == null || gameMode == null || gameType == null) {
            lastPlayerPos = null;
            return;
        }

        updatePlayerInteractionBox();
        preprocess();

        if (!isConfigAllowExecute()) {
            lastPlayerPos = null;
            return;
        }

        // 执行方块迭代
        if (!executeBlockIteration()) {
            lastPlayerPos = null;
        }
    }

    /**
     * 更新玩家交互盒：根据玩家位置和配置动态调整迭代范围
     * 性能优化：只在必要时重建交互盒，复用迭代器状态
     */
    private void updatePlayerInteractionBox() {
        if (playerInteractionBox == null) return;

        BlockPos playerPos = player.getOnPos();
        PrinterBox box = playerInteractionBox.get();

        // 计算当前需要的扩展范围（但不立即使用）
        int currentExpandRange = Configs.Core.CHECK_PLAYER_INTERACTION_RANGE.getBooleanValue()
                ? (int) PlayerUtils.getPlayerBlockInteractionRange(5)
                : getWorkRange();

        // 检查是否需要重建交互盒（首次创建、配置变化或玩家移动超过阈值）
        boolean needRebuild = box == null 
                || !box.equals(lastPlayerInteractionBox) 
                || lastPlayerPos == null
                || !lastPlayerPos.closerThan(playerPos, getWorkRange() * 0.7)
                || cachedExpandRange != currentExpandRange;

        if (needRebuild) {
            lastPlayerPos = playerPos;
            cachedExpandRange = currentExpandRange;

            // 创建新的交互盒
            box = new PrinterBox(playerPos).expand(cachedExpandRange);
            lastPlayerInteractionBox = box;
            playerInteractionBox.set(box);

            // 同步迭代配置（只在重建时设置）
            box.iterationMode = (IterationOrderType) Configs.Core.ITERATION_ORDER.getOptionListValue();
            box.xIncrement = !Configs.Core.X_REVERSE.getBooleanValue();
            box.yIncrement = !Configs.Core.Y_REVERSE.getBooleanValue();
            box.zIncrement = !Configs.Core.Z_REVERSE.getBooleanValue();
            
            // 重置迭代器，下次迭代时会重新初始化
            cachedIterator = null;
        }
    }

    /**
     * 执行方块迭代：遍历交互盒内的方块并执行处理逻辑
     * 性能优化：使用缓存的迭代器，避免重复遍历已处理的方块
     * @return 是否被中断
     */
    private boolean executeBlockIteration() {
        if (playerInteractionBox == null || !canExecute()) return false;
    
        PrinterBox box = playerInteractionBox.get();
        if (box == null || !canIterate()) return false;
    
        // 初始化或复用迭代器
        if (cachedIterator == null) {
            cachedIterator = box.iterator();
        }
    
        int maxEffectiveExec = getMaxEffectiveExecutionsPerTick();
        int timeLimit = getIterationTimeLimit();
        int effectiveExecCount = 0;
    
        // 性能优化：缓存配置值和状态
        boolean debugMode = Configs.Core.DEBUG_OUTPUT.getBooleanValue();
        boolean needRangeCheck = isNeedRangeCheck();
        boolean isSchematicHandler = isSchematicBlockHandler();
    
        // 时间限制优化：记录开始时间并设置检查间隔
        long startTime = timeLimit > 0 ? System.nanoTime() : 0;
        long timeLimitNanos = timeLimit * 1_000_000L;
        int timeCheckInterval = 10; // 每 10 次迭代检查一次时间
        int iterCount = 0;
    
        skipIteration.set(false);
        guiBlockInfoQueue.clear();
        renderIndex = 0;
    
        // 使用缓存的迭代器继续上次的位置
        while (cachedIterator.hasNext()) {
            // 性能优化：减少时间检查频率
            if (timeLimit > 0 && ++iterCount % timeCheckInterval == 0) {
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
    
            // 性能优化：提前检查可交互性，避免创建 GUI 对象
            if (!ConfigUtils.canInteracted(pos)) continue;
    
            // 性能优化：提前进行范围检查
            if (needRangeCheck) {
                if (isSchematicHandler ? !LitematicaUtils.isSchematicBlock(pos)
                        : !LitematicaUtils.isWithinSelection1ModeRange(pos)) {
                    continue;
                }
    
                if (selectionType != null && !ConfigUtils.isPositionInSelectionRange(player, pos, selectionType)) {
                    continue;
                }
            }
    
            // 性能优化：只在调试模式下创建 GUI 对象
            if (debugMode) {
                GuiBlockInfo gui = isSchematicHandler
                        ? new GuiBlockInfo(level, SchematicWorldHandler.getSchematicWorld(), pos)
                        : new GuiBlockInfo(level, null, pos);
                gui.interacted = true;
                gui.posInSelectionRange = true;
                gui.execute = canIterationBlockPos(pos) && !isBlockPosOnCooldown(pos);
                addGuiBlockInfoToQueue(gui);
            }
    
            // 执行迭代处理
            if (canIterationBlockPos(pos) && !isBlockPosOnCooldown(pos)) {
                executeIteration(pos, skipIteration);
    
                if (skipIteration.get() || (maxEffectiveExec > 0 && ++effectiveExecCount >= maxEffectiveExec)) {
                    stopIteration(true);
                    return true;
                }
            }
        }
    
        // 迭代完成，清空缓存以便下次重建
        cachedIterator = null;
        stopIteration(false);
        return false;
    }

    protected void stopIteration(boolean interrupt) {
        // 如果被打断，保留迭代器状态以便下次继续
        // 如果完成迭代，cachedIterator 会在 executeBlockIteration 中被置 null
    }

    protected boolean isSchematicBlockHandler() {
        return false;
    }

    /**
     * 添加方块信息到队列并重置缓存时长
     */
    private void addGuiBlockInfoToQueue(GuiBlockInfo guiBlockInfo) {
        if (guiBlockInfo != null) {
            guiBlockInfoQueue.add(guiBlockInfo);
            guiBlockPosCacheTicks = 20;
        }
    }

    /**
     * 获取当前帧要显示的方块信息（渲染阶段调用）
     */
    @Nullable
    public GuiBlockInfo getCurrentRenderGuiBlockInfo() {
        if (guiBlockInfoQueue.isEmpty()) return null;

        GuiBlockInfo[] infoArray = guiBlockInfoQueue.toArray(new GuiBlockInfo[0]);
        if (renderIndex >= infoArray.length) {
            renderIndex = 0;
            return infoArray[infoArray.length - 1];
        }
        return infoArray[renderIndex++];
    }

    /**
     * 获取队列中最后一个方块信息
     */
    @Nullable
    public GuiBlockInfo getGuiBlockInfo() {
        if (guiBlockInfoQueue.isEmpty()) return null;
        GuiBlockInfo[] infoArray = guiBlockInfoQueue.toArray(new GuiBlockInfo[0]);
        return infoArray[infoArray.length - 1];
    }

    public void setGuiBlockInfo(@Nullable GuiBlockInfo guiBlockInfo) {
        addGuiBlockInfoToQueue(guiBlockInfo);
    }

    public int getGuiBlockInfoQueueSize() {
        return guiBlockInfoQueue.size();
    }

    /**
     * 配置层面的执行权限校验
     */
    private boolean isConfigAllowExecute() {
        if (!ConfigUtils.isEnable()) return false;

        if (printMode != null && enableConfig != null) {
            WorkingModeType modeType = (WorkingModeType) Configs.Core.WORK_MODE.getOptionListValue();
            return switch (modeType) {
                case SINGLE -> Configs.Core.WORK_MODE_TYPE.getOptionListValue().equals(printMode);
                case MULTI -> enableConfig.getBooleanValue();
            };
        }

        return enableConfig == null || enableConfig.getBooleanValue();
    }

    protected int getTickInterval() {
        return -1;
    }

    protected int getMaxEffectiveExecutionsPerTick() {
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

    public boolean canIterationBlockPos(BlockPos pos) {
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
    public boolean isBlockPosOnCooldown(@Nullable BlockPos pos) {
        if (level == null || pos == null) return true;
        return BlockPosCooldownManager.INSTANCE.isOnCooldown(level, id, pos);
    }

    public boolean isBlockPosOnCooldown(String name, @Nullable BlockPos pos) {
        if (level == null || pos == null) return true;
        return BlockPosCooldownManager.INSTANCE.isOnCooldown(level, id + "_" + name, pos);
    }

    /**
     * 设置方块冷却时间
     */
    public void setBlockPosCooldown(@Nullable BlockPos pos, int cooldownTicks) {
        if (level == null || pos == null || cooldownTicks < 1) return;
        BlockPosCooldownManager.INSTANCE.setCooldown(level, id, pos, cooldownTicks);
    }

    public void setBlockPosCooldown(String name, @Nullable BlockPos pos, int cooldownTicks) {
        if (level == null || pos == null || cooldownTicks < 1) return;
        BlockPosCooldownManager.INSTANCE.setCooldown(level, id + "_" + name, pos, cooldownTicks);
    }


    protected Direction[] getPlayerOrderedByNearest() {
        return Direction.orderedByNearest(player);
    }

    protected Direction getPlayerPlacementDirection() {
        return Direction.orderedByNearest(player)[0].getOpposite();
    }

    protected boolean isNeedRangeCheck() {
        return true;
    }
}