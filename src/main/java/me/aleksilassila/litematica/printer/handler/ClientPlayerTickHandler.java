package me.aleksilassila.litematica.printer.handler;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import lombok.Getter;
import lombok.Setter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.*;
import me.aleksilassila.litematica.printer.printer.*;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import me.aleksilassila.litematica.printer.utils.LitematicaUtils;
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
import java.util.concurrent.atomic.AtomicReference;

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.switchItem;

/**
 * 打印机客户端玩家Tick抽象处理器
 * 基于模板方法模式设计，封装所有打印处理器的通用逻辑与执行流程
 * 子类仅需按需重写对应抽象/空实现方法，即可实现自定义打印处理器逻辑
 */
public abstract class ClientPlayerTickHandler extends ConfigUtils {
    /**
     * 全局Tick计数器，所有处理器共享的时间基准
     * 由外部定时调用{@link #updateTickHandlerTime()}递增，代表处理程序运行的总Tick数(非游戏实际时间)
     */
    @Getter
    private static long currentHandlerTime;

    @Getter
    @Setter
    private static int packetTick;

    /**
     * 递增全局Tick计数器
     */
    public static void updateTickHandlerTime() {
        currentHandlerTime++;
    }

    /**
     * 处理器唯一标识
     */
    @Getter
    private final String id;

    /**
     * 该处理器绑定的打印模式类型，用于单模式下的处理器匹配
     * 为null时表示该处理器不绑定具体模式，多模式下生效
     */
    @Getter
    @Nullable
    private final PrintModeType printMode;

    /**
     * 多模式下该处理器的启用配置项，控制处理器是否生效
     * 为null时表示该处理器无需单独配置启用，由全局配置控制
     */
    @Getter
    @Nullable
    private final ConfigBoolean enableConfig;

    /**
     * 多模式下该处理器的启用配置项，控制处理器是否生效
     * 为null时表示该处理器无需单独配置启用，由全局配置控制
     */
    @Getter
    @Nullable
    private final ConfigOptionList selectionType;

    /***
     * 跳过迭代(可传递对象)
     */
    private final AtomicReference<Boolean> skipIteration = new AtomicReference<>(false);

    /**
     * 玩家交互盒原子引用，用于存储当前玩家的迭代范围（方块检测范围）
     * 原子引用保证多线程环境下的安全访问，null表示该处理器不使用迭代功能
     */
    @Getter
    @Nullable
    public final AtomicReference<PrinterBox> playerInteractionBox;

    /**
     * 上次Tick的玩家交互盒对象，用于检测玩家位置/范围是否发生变化
     * 避免每Tick重复创建交互盒，提升性能
     */
    @Nullable
    private PrinterBox lastPlayerInteractionBox;

    /**
     * 上次Tick的玩家所在位置，用于检测玩家是否发生移动
     * 玩家移动超过阈值时，会重新创建玩家交互盒
     */
    @Nullable
    private BlockPos lastPlayerPos;

    /**
     * 该处理器上次执行的全局Tick时间，用于控制处理器执行间隔
     * 初始值-1L作为首次执行的判断标识，兼容全局Tick从0开始的场景
     */
    private long lastTickTime = -1L;

    /**
     * GUI展示的当前迭代目标方块信息，用于0Tick实时渲染迭代结果
     * 为null时表示当前无有效迭代目标，由{@link #setGuiBlockInfo(GuiBlockInfo)}更新
     */
    @Getter
    @Nullable
    protected GuiBlockInfo guiBlockInfo;

    /**
     * GUI方块信息的缓存剩余Tick数，控制GUI信息的展示时长
     * 每次更新GUI信息时重置为20，每Tick递减，为0时清空GUI信息
     */
    private int guiBlockPosCacheTicks;

    /**
     * Minecraft客户端核心实例，所有游戏对象的获取入口
     */
    protected Minecraft mc;

    /**
     * 客户端当前所在的游戏世界实例，用于方块状态、世界操作等
     */
    protected ClientLevel level;

    /**
     * 客户端当前的本地玩家实例，用于玩家位置、操作等
     */
    protected LocalPlayer player;

    /**
     * 客户端网络连接实例，用于网络相关操作（如发送数据包）
     */
    protected ClientPacketListener connection;

    /**
     * 客户端游戏模式实例，用于获取当前游戏模式（生存/创造/冒险等）
     */
    protected MultiPlayerGameMode gameMode;

    /**
     * 客户端当前的游戏类型，由gameMode解析而来，简化模式判断
     */
    protected GameType gameType;

    /**
     * 客户端当前的射线检测总结果，包含所有类型的射线检测目标
     */
    @Nullable
    protected HitResult hitResult;

    /**
     * 客户端当前的方块射线检测结果，仅包含方块类型的射线检测目标
     * 为null时表示射线未命中任何方块
     */
    @Nullable
    protected BlockHitResult blockHitResult;

    /**
     * 构造器，初始化处理器核心属性
     *
     * @param id           处理器唯一标识，不可重复
     * @param printMode    绑定的打印模式类型，可为null
     * @param enableConfig 多模式下的启用配置项，可为null
     * @param useBox       是否使用玩家交互盒（迭代功能），true则初始化原子引用，false则为null
     */
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

    /**
     * 核心Tick方法，模板方法设计的核心执行流程
     */
    public void tick() {
        // GUI迭代信息缓存处理：每Tick递减缓存计数，计数为0时清空GUI信息
        if (this.guiBlockPosCacheTicks > 0) {
            this.guiBlockPosCacheTicks--;
        } else {
            this.guiBlockInfo = null; // 缓存时间到，清空GUI信息，停止渲染
        }

        int tickInterval = this.getTickInterval(); // 工作间隔
        if (tickInterval > 0) {
            long currentTickTime = ClientPlayerTickHandler.getCurrentHandlerTime();
            if (this.lastTickTime != -1L) {
                // 非首次执行
                if (currentTickTime - this.lastTickTime < tickInterval) {
                    return;
                }
            }
            this.lastTickTime = currentTickTime; // 更新上次执行时间，首次执行也会初始化
        }

        if (Configs.Core.LAG_CHECK.getBooleanValue()) {
            if (packetTick > 20) {
                return;
            }
            packetTick++;
        }

        if (!isEnable()) {
            return;
        }

        this.updateVariables();

        // 核心游戏对象空值校验：任意对象为空则终止本次Tick，避免空指针异常
        if (this.mc == null || this.level == null || this.player == null || this.connection == null || this.gameMode == null || this.gameType == null) {
            return;
        }

        if (isOpenHandler || switchItem() || InteractionUtils.INSTANCE.hasTargets()) {
            return;
        }

        ActionManager.INSTANCE.sendQueue(player);
        if (ActionManager.INSTANCE.needWait) {
            return;
        }

        // 更新迭代范围
        if (this.playerInteractionBox != null) {
            BlockPos playerPos = this.player.getOnPos();
            double threshold = getWorkRange() * 0.7; // 玩家移动阈值：工作范围的70%
            @Nullable PrinterBox playerInteractionBox = this.playerInteractionBox.get();
            // 交互盒未创建/玩家移动超阈值/交互盒不匹配时，重新创建交互盒
            if (playerInteractionBox == null
                    || !playerInteractionBox.equals(this.lastPlayerInteractionBox)
                    || this.lastPlayerPos == null
                    || !this.lastPlayerPos.closerThan(playerPos, threshold)
            ) {
                this.lastPlayerPos = playerPos;
                playerInteractionBox = new PrinterBox(playerPos).expand(getWorkRange()); // 按工作范围扩展交互盒
                this.lastPlayerInteractionBox = playerInteractionBox;
                this.playerInteractionBox.set(playerInteractionBox); // 更新原子引用
            }
            // 同步交互盒的迭代配置：从全局配置读取迭代顺序、方向等
            playerInteractionBox.iterationMode = (IterationOrderType) Configs.Core.ITERATION_ORDER.getOptionListValue();
            playerInteractionBox.xIncrement = !Configs.Core.X_REVERSE.getBooleanValue();
            playerInteractionBox.yIncrement = !Configs.Core.Y_REVERSE.getBooleanValue();
            playerInteractionBox.zIncrement = !Configs.Core.Z_REVERSE.getBooleanValue();
        }


        this.preprocess(); // 预处理一些值，例如破坏方块列表
        if (!this.isConfigAllowExecute()) {
            return;
        }

        boolean interrupt = false;
        // 执行迭代业务任务：基于玩家交互盒的方块迭代处理（防主线程阻塞）
        if (this.playerInteractionBox != null && this.canExecute()) {
            PrinterBox playerInteractionBox = this.playerInteractionBox.get();
            // 交互盒非空且满足迭代执行条件时，执行迭代逻辑
            if (playerInteractionBox != null && canExecuteIteration()) {
                int maxEffectiveExec = this.getMaxEffectiveExecutionsPerTick();
                int maxTotalIter = this.getMaxTotalIterationsPerTick();
                int totalIterCount = 0;
                int effectiveExecCount = 0;
                skipIteration.set(false);
                Iterator<BlockPos> iterator = playerInteractionBox.iterator();
                while (!this.skipIteration.get() && iterator.hasNext()) {
                    // 单Tick迭代次数限制：达到最大次数则终止循环（防主线程阻塞）
                    if (maxTotalIter > 0 && ++totalIterCount >= maxTotalIter) {
                        interrupt = true;
                    }
                    BlockPos pos = iterator.next();
                    if (pos == null) continue;
                    GuiBlockInfo gui = new GuiBlockInfo(level, pos, level.getBlockState(pos));
                    this.setGuiBlockInfo(gui);
                    if (ConfigUtils.canInteracted(pos)) {
                        gui.interacted = true;
                    } else {
                        gui.interacted = false;
                        continue;
                    }
                    boolean isPrinterRange = isPrintMode() && LitematicaUtils.isSchematicBlock(pos);
                    if (!isPrinterRange && !LitematicaUtils.xuanQuFanWeiNei_p(pos)) {
                        continue;
                    }
                    if (selectionType != null && !ConfigUtils.isPositionInSelectionRange(player, pos, selectionType)) {
                        gui.posInSelectionRange = false;
                        continue;
                    } else {
                        gui.posInSelectionRange = true;
                    }
                    // 方块迭代权限校验：子类可重写实现自定义过滤逻辑
                    if (this.canIterationBlockPos(pos)) {
                        this.executeIteration(pos, this.skipIteration);
                        gui.execute = true;
                        if (maxEffectiveExec > 0 && ++effectiveExecCount >= maxEffectiveExec) {
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

    /**
     * 设置GUI迭代目标方块信息，并初始化20Tick缓存时长
     * 用于0Tick实时渲染当前迭代的目标方块，缓存计数为0时自动清空
     *
     * @param guiBlockInfo 待展示的GUI方块信息，null则直接清空当前信息
     */
    public void setGuiBlockInfo(@Nullable GuiBlockInfo guiBlockInfo) {
        if (guiBlockInfo != null) {
            this.guiBlockInfo = guiBlockInfo;
            this.guiBlockPosCacheTicks = 20;    // 重置缓存Tick数为20，适配0TICK迭代方块渲染
        } else {
            this.guiBlockInfo = null;
            this.guiBlockPosCacheTicks = 0;
        }
    }

    /**
     * 配置层面的执行权限校验，私有方法避免子类篡改
     * 校验逻辑：全局启用 → 模式匹配（单/多）→ 配置启用，层层校验
     *
     * @return true-配置允许执行，false-配置禁止执行
     */
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

    /**
     * 获取处理器的执行间隔（单位：Tick），子类可重写自定义
     * 返回值≤0时，表示不限制执行间隔，每Tick都执行
     *
     * @return 执行间隔Tick数，默认-1（不限制）
     */
    protected int getTickInterval() {
        return -1;
    }

    /**
     * 获取单Tick内最大迭代次数，用于限制迭代逻辑防止主线程阻塞，子类可重写自定义
     * 返回值≤0时，表示不限制迭代次数，一次性迭代完所有方块
     * 推荐值：10~50（适配Minecraft 50ms/Tick的主线程限制）
     *
     * @return 单Tick最大迭代次数，默认-1（不限制）
     */
    protected int getMaxEffectiveExecutionsPerTick() {
        return -1;
    }

    /**
     * 获取单Tick内最大总迭代遍历数（技术兜底），用于限制方块遍历次数防止主线程阻塞，子类可重写自定义
     * 返回值≤0时（含0），表示无上限，一次性遍历完所有符合基础条件的方块
     * 该数值统计所有遍历的方块（无论是否过滤、是否执行成功），仅做技术兜底防卡顿
     * 推荐值：50~100（适配Minecraft 50ms/Tick的主线程耗时限制）
     *
     * @return 单Tick最大总迭代遍历数，默认-1（无上限）
     */
    protected int getMaxTotalIterationsPerTick() {
        return Configs.Core.ITERATOR_TOTAL_PER_TICK.getIntegerValue();
    }

    /**
     * 普通任务执行前的预处理逻辑，子类可重写实现
     * 执行时机：游戏对象初始化完成后，{link #execute()}执行前
     * 适用于：初始化临时变量、清理旧数据、参数准备等
     */
    protected void preprocess() {
    }

    /**
     * 处理器业务层面的执行条件判断，子类可重写实现自定义条件
     * 执行时机：配置权限校验{@link #isConfigAllowExecute()}通过后
     * 适用于：基于游戏状态、玩家状态的动态执行条件判断
     *
     * @return true-满足业务执行条件，false-不满足，默认true
     */
    protected boolean canExecute() {
        return true;
    }

    /**
     * 迭代任务的执行条件判断，子类可重写实现自定义条件
     * 执行时机：玩家交互盒非空且普通任务执行条件{link #canExecute()}满足后
     * 适用于：基于迭代范围、世界状态的迭代执行条件判断
     *
     * @return true-满足迭代执行条件，false-不满足，默认true
     */
    protected boolean canExecuteIteration() {
        return true;
    }

    /**
     * 判断指定方块是否允许被迭代处理，子类可重写实现自定义过滤逻辑
     * 执行时机：方块可交互性校验{@link ConfigUtils#canInteracted(BlockPos)}通过后
     * 适用于：基于方块状态、冷却、类型的自定义过滤
     *
     * @param pos 待判断的方块位置，不会为null
     * @return true-允许迭代处理，false-禁止，默认true
     */
    public boolean canIterationBlockPos(BlockPos pos) {
        return true;
    }

    /**
     * 单次方块迭代的核心执行方法，子类必须重写实现自定义迭代逻辑
     * 执行时机：方块可交互性和迭代权限校验均通过后
     * 适用于：方块放置、破坏、标记、冷却设置等迭代类业务
     */
    protected void executeIteration(BlockPos pos, AtomicReference<Boolean> skipIteration) {
    }

    /**
     * 判断指定方块是否处于当前处理器的冷却中，避免重复处理
     * 冷却数据由{link BlockPosCooldownManager}统一管理，按处理器id区分
     *
     * @param pos 待判断的方块位置，可为null
     * @return true-处于冷却中，false-可处理；位置/世界为空时默认返回true
     */
    public boolean isBlockPosOnCooldown(@Nullable BlockPos pos) {
        if (this.level == null || pos == null) return true;
        return BlockPosCooldownManager.INSTANCE.isOnCooldown(this.level, this.getId(), pos);
    }

    /**
     * 为指定方块设置当前处理器的冷却时间，避免短时间内重复处理
     * 冷却数据由{link BlockPosCooldownManager}统一管理，按处理器id区分
     *
     * @param pos           待设置冷却的方块位置，可为null
     * @param cooldownTicks 冷却时间（单位：Tick），小于1则不处理
     */
    public void setBlockPosCooldown(@Nullable BlockPos pos, int cooldownTicks) {
        if (this.level == null || pos == null || cooldownTicks < 1) return;
        BlockPosCooldownManager.INSTANCE.setCooldown(this.level, this.getId(), pos, cooldownTicks);
    }

    protected Direction[] getPlayerOrderedByNearest() {
        return Direction.orderedByNearest(player);
    }

    protected Direction getPlayerPlacementDirection() {
        return getPlayerOrderedByNearest()[0].getOpposite();
    }
}