package me.aleksilassila.litematica.printer.handler;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import lombok.Getter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.*;
import me.aleksilassila.litematica.printer.printer.*;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.CooldownUtils;
import me.aleksilassila.litematica.printer.utils.mods.LitematicaUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.PlayerUtils;
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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ClientPlayerTickHandler extends ConfigUtils {
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
    private final AtomicReference<Boolean> skipIteration = new AtomicReference<>(false);
    private final Queue<GuiBlockInfo> guiBlockInfoQueue = new ConcurrentLinkedQueue<>();

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
        this.updateVariables();
    }

    protected void updateVariables() {
        this.mc = Minecraft.getInstance();
        this.level = mc.level;
        this.player = mc.player;
        this.connection = mc.getConnection();
        this.gameMode = mc.gameMode;
        this.gameType = mc.gameMode == null ? null : mc.gameMode.getPlayerMode();
        this.hitResult = mc.hitResult;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            this.blockHitResult = (BlockHitResult) mc.hitResult;
        } else {
            this.blockHitResult = null;
        }
    }

    public void tick() {
        // GUI迭代信息缓存处理：每Tick递减缓存计数，计数为0时清空队列
        if (this.guiBlockPosCacheTicks > 0) {
            this.guiBlockPosCacheTicks--;
        } else {
            this.guiBlockInfoQueue.clear(); // 缓存时间到，清空队列
            this.renderIndex = 0; // 重置渲染索引
        }
        int tickInterval = this.getTickInterval(); // 工作间隔
        if (tickInterval > 0) {
            long currentTickTime = ClientPlayerTickManager.getCurrentHandlerTime();
            if (this.lastTickTime != -1L) {
                // 非首次执行
                if (currentTickTime - this.lastTickTime < tickInterval) {
                    return;
                }
            }
            this.lastTickTime = currentTickTime; // 更新上次执行时间，首次执行也会初始化
        }
        if (!isEnable()) {
            this.lastPlayerPos = null;
            return;
        }
        this.updateVariables();
        if (this.mc == null || this.level == null || this.player == null || this.connection == null || this.gameMode == null || this.gameType == null) {
            this.lastPlayerPos = null;
            return;
        }
        // 更新迭代范围
        if (this.playerInteractionBox != null) {
            //#if MC > 11802
            BlockPos playerPos = new BlockPos((int) Math.round(player.getX()), (int) Math.round(player.getEyeY()), (int) Math.round(player.getZ()));
            //#else
            //$$ BlockPos playerPos = new BlockPos((int) Math.round(player.getX()), (int) Math.round(player.getY() + 1.5), (int) Math.round(player.getZ()));
            //#endif
            double threshold = getWorkRange() * 0.7; // 玩家移动阈值：工作范围的70%
            @Nullable PrinterBox playerInteractionBox = this.playerInteractionBox.get();
            if (playerInteractionBox == null
                    || !playerInteractionBox.equals(this.lastPlayerInteractionBox)
                    || this.lastPlayerPos == null
                    || !this.lastPlayerPos.closerThan(playerPos, threshold)
            ) {
                this.lastPlayerPos = playerPos;
                PrinterBox box = new PrinterBox(playerPos);
                if (Configs.Core.CHECK_PLAYER_INTERACTION_RANGE.getBooleanValue()) {
                    playerInteractionBox = box.expand((int) Math.ceil(PlayerUtils.getPlayerBlockInteractionRange(5) + 3));
                } else {
                    playerInteractionBox = box.expand(getWorkRange());
                }
                this.lastPlayerInteractionBox = playerInteractionBox;
                this.playerInteractionBox.set(playerInteractionBox);
            }
            // 同步交互盒的迭代配置：从全局配置读取迭代顺序、方向等
            playerInteractionBox.iterationMode = (IterationOrderType) Configs.Core.ITERATION_ORDER.getOptionListValue();
            playerInteractionBox.xIncrement = !Configs.Core.X_REVERSE.getBooleanValue();
            playerInteractionBox.yIncrement = !Configs.Core.Y_REVERSE.getBooleanValue();
            playerInteractionBox.zIncrement = !Configs.Core.Z_REVERSE.getBooleanValue();
        }
        this.preprocess(); // 运行前处理的事情
        if (!this.isConfigAllowExecute()) {
            this.lastPlayerPos = null;
            return;
        }
        boolean interrupt = false;
        // 执行迭代业务任务：基于玩家交互盒的方块迭代处理（防主线程阻塞）
        if (this.playerInteractionBox != null && this.canExecute()) {
            PrinterBox playerInteractionBox = this.playerInteractionBox.get();
            // 交互盒非空且满足迭代执行条件时，执行迭代逻辑
            if (playerInteractionBox != null && canIterate()) {
                int maxEffectiveExec = this.getMaxEffectiveExecutionsPerTick();
                int maxTotalIter = this.getMaxTotalIterationsPerTick();
                int totalIterCount = 0;
                int effectiveExecCount = 0;
                this.skipIteration.set(false);
                this.guiBlockInfoQueue.clear(); // 重置渲染信息
                this.renderIndex = 0;   // 重置渲染信息
                for (BlockPos pos : playerInteractionBox) {
                    // 单Tick迭代次数限制：达到最大次数则终止循环（防主线程阻塞）
                    if (maxTotalIter > 0 && ++totalIterCount >= maxTotalIter) {
                        interrupt = true;
                        break;
                    }
                    if (this.skipIteration.get() || ActionManager.INSTANCE.needWaitModifyLook) {
                        interrupt = true;
                        break;
                    }
                    if (pos == null) continue;
                    GuiBlockInfo gui;
                    if (isSchematicBlockHandler()) {
                        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
                        gui = new GuiBlockInfo(level, schematic, pos);
                    } else {
                        gui = new GuiBlockInfo(level, null, pos);
                    }
                    // 仅调试时候加入队列, 避免队列储存无用位置信息
                    if (Configs.Core.DEBUG_OUTPUT.getBooleanValue()) {
                        this.addGuiBlockInfoToQueue(gui);
                    }
                    if (ConfigUtils.canInteracted(pos)) {
                        gui.interacted = true;
                    } else {
                        gui.interacted = false;
                        continue;
                    }
                    if (isSchematicBlockHandler()) {
                        if (!LitematicaUtils.isSchematicBlock(pos)) {
                            continue;
                        }
                    } else if (!LitematicaUtils.isWithinSelection1ModeRange(pos)) {
                        continue;
                    }
                    if (selectionType != null && !ConfigUtils.isPositionInSelectionRange(player, pos, selectionType)) {
                        gui.posInSelectionRange = false;
                        continue;
                    }
                    gui.posInSelectionRange = true;
                    // 方块迭代权限校验：子类可重写实现自定义过滤逻辑
                    if (this.canIterationBlockPos(pos) && !isBlockPosOnCooldown(pos)) {
                        this.executeIteration(pos, this.skipIteration);
                        gui.execute = true;
                        if (this.skipIteration.get() || maxEffectiveExec > 0 && ++effectiveExecCount >= maxEffectiveExec) {
                            interrupt = true;
                        }
                    }
                    if (interrupt) {
                        break;
                    }
                }
                stopIteration(interrupt);
            }
        }
        if (!interrupt) {
            this.lastPlayerPos = null;
        }
    }

    protected void stopIteration(boolean interrupt) {
    }

    protected boolean isSchematicBlockHandler() {
        return false;
    }


    private void addGuiBlockInfoToQueue(GuiBlockInfo guiBlockInfo) {
        if (guiBlockInfo != null) {
            this.guiBlockInfoQueue.add(guiBlockInfo);
            this.guiBlockPosCacheTicks = 20; // 重置缓存Tick数为20
        }
    }

    @Nullable
    public GuiBlockInfo getCurrentRenderGuiBlockInfo() {
        if (guiBlockInfoQueue.isEmpty()) {
            return null;
        }
        GuiBlockInfo[] infoArray = guiBlockInfoQueue.toArray(new GuiBlockInfo[0]);
        // 渲染索引超出队列长度时，返回最后一个元素并重置索引
        if (renderIndex >= infoArray.length) {
            renderIndex = 0; // 循环展示（可选：也可返回null）
            return infoArray[infoArray.length - 1];
        }
        // 获取当前帧的信息并推进索引
        GuiBlockInfo currentInfo = infoArray[renderIndex];
        renderIndex++;
        return currentInfo;
    }

    @Nullable
    public GuiBlockInfo getGuiBlockInfo() {
        if (guiBlockInfoQueue.isEmpty()) {
            return null;
        }
        // 返回队列最后一个元素（兼容原有逻辑）
        return ((GuiBlockInfo[]) guiBlockInfoQueue.toArray(new GuiBlockInfo[0]))[guiBlockInfoQueue.size() - 1];
    }

    public void setGuiBlockInfo(@Nullable GuiBlockInfo guiBlockInfo) {
        this.addGuiBlockInfoToQueue(guiBlockInfo);
    }

    public int getGuiBlockInfoQueueSize() {
        return guiBlockInfoQueue.size();
    }

    private boolean isConfigAllowExecute() {
        // 全局打印机功能未启用，直接禁止所有处理器执行
        if (!ConfigUtils.isEnable()) {
            return false;
        }
        // 处理器绑定了模式和配置，按当前游戏模式校验
        if (this.printMode != null && this.enableConfig != null) {
            WorkingModeType modeType = (WorkingModeType) Configs.Core.WORK_MODE.getOptionListValue();
            return switch (modeType) {
                case SINGLE -> Configs.Core.WORK_MODE_TYPE.getOptionListValue().equals(this.printMode);
                case MULTI -> this.enableConfig.getBooleanValue();
            };
        }
        // 仅绑定了启用配置，直接校验配置是否启用
        if (this.enableConfig != null) {
            return this.enableConfig.getBooleanValue();
        }
        // 无任何配置绑定，默认允许执行（由全局配置控制）
        return true;
    }

    protected int getTickInterval() {
        return -1;
    }

    protected int getMaxEffectiveExecutionsPerTick() {
        return -1;
    }

    protected int getMaxTotalIterationsPerTick() {
        return Configs.Core.ITERATOR_TOTAL_PER_TICK.getIntegerValue();
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

    protected void executeIteration(BlockPos pos, AtomicReference<Boolean> skipIteration) {
    }

    public boolean isBlockPosOnCooldown(@Nullable BlockPos pos) {
        if (this.level == null || pos == null) return true;
        return CooldownUtils.INSTANCE.isOnCooldown(this.level, this.getId(), pos);
    }

    public boolean isBlockPosOnCooldown(String name, @Nullable BlockPos pos) {
        if (this.level == null || pos == null) return true;
        return CooldownUtils.INSTANCE.isOnCooldown(this.level, this.getId() + "_" + name, pos);
    }

    public void setBlockPosCooldown(@Nullable BlockPos pos, int cooldownTicks) {
        if (this.level == null || pos == null || cooldownTicks < 1) return;
        CooldownUtils.INSTANCE.setCooldown(this.level, this.getId(), pos, cooldownTicks);
    }

    public void setBlockPosCooldown(String name, @Nullable BlockPos pos, int cooldownTicks) {
        if (this.level == null || pos == null || cooldownTicks < 1) return;
        CooldownUtils.INSTANCE.setCooldown(this.level, this.getId() + "_" + name, pos, cooldownTicks);
    }

    protected Direction[] getPlayerOrderedByNearest() {
        return Direction.orderedByNearest(player);
    }

    protected Direction getPlayerPlacementDirection() {
        return getPlayerOrderedByNearest()[0].getOpposite();
    }
}