package me.aleksilassila.litematica.printer.utils;

import lombok.Getter;
import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.bilixwhite.utils.TweakerooUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.BlockContext;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;

@Environment(EnvType.CLIENT)
public class InteractionUtils {
    private static final Minecraft client = Minecraft.getInstance();
    public static InteractionUtils INSTANCE = new InteractionUtils();

    private InteractionUtils() {
    }

    private BlockPos destroyBlockPos = new BlockPos(-1, -1, -1);
    private float destroyProgress;
    private @Getter boolean isDestroying;
    private float destroyTicks;

    private final Deque<BlockPos> breakQueue = new ArrayDeque<>();
    private BlockPos activePos = null;

    private long lastBreakOperationTime = System.currentTimeMillis();
    private static final long BREAK_STATE_TIMEOUT_MS = 3000L;

    public void add(BlockPos pos) {
        if (pos == null) return;
        if (!breakQueue.contains(pos)) {
            breakQueue.addLast(pos);
        }
    }

    public void add(BlockContext ctx) {
        if (ctx == null) return;
        this.add(ctx.blockPos);
    }

    public void clear() {
        breakQueue.clear();
        resetBreaking();
        activePos = null;
    }

    public void onTick() {
        // 周期检查：处于破坏状态且超时未操作，自动释放
        if (isDestroying) {
            long currentTime = System.currentTimeMillis();
            // 超出阈值，执行强制重置
            if (currentTime - lastBreakOperationTime > BREAK_STATE_TIMEOUT_MS) {
                resetBreaking(); // 重置本地破坏状态
                activePos = null; // 清空活跃目标
                // 向服务端发送中止包，确保服务端同步状态（避免客户端重置但服务端仍在处理）
                NetworkUtils.sendPacket(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                        this.destroyBlockPos,
                        Direction.DOWN
                ));
            }
        }
        if (client.player == null || client.level == null || client.gameMode == null) {
            resetBreaking(); // 兜底重置破坏状态，避免内存泄漏
            activePos = null;
            return;
        }
        if (breakQueue.isEmpty()) {
            activePos = null;
            return;
        }
        // 有任务正在调用本 TICK 不进行处理
        if (isDestroying && !activePos.equals(destroyBlockPos)) {
            activePos = null;
            return;
        }
        // 无活跃破坏方块时，从队列取队首作为新的活跃目标（FIFO 先进先出）
        if (activePos == null && !isDestroying()) {
            BlockPos nextPos = breakQueue.peekFirst();
            // 校验队列首方块是否仍可破坏（避免方块已被移除/失效），无效则出队并继续取
            while (nextPos != null && !canBreakBlock(nextPos)) {
                breakQueue.pollFirst();
                nextPos = breakQueue.peekFirst();
            }
            activePos = nextPos; // 赋值有效活跃方块
        }

        // 有活跃方块时，驱动破坏进度更新
        if (activePos != null) {
            BlockBreakResult result = updateBlockBreakingProgress(activePos, Direction.DOWN);
            // 根据破坏结果做状态和队列维护
            switch (result) {
                case COMPLETED:
                    // 破坏完成：出队、重置活跃状态、清空当前破坏进度
                    breakQueue.pollFirst();
                    activePos = null;
                    resetBreaking();
                    break;
                case ABORTED, FAILED:
                    // 破坏中止/失败：出队、重置活跃状态（失败方块不重新入队，避免死循环）
                    breakQueue.pollFirst();
                    activePos = null;
                    resetBreaking();
                    break;
                case IN_PROGRESS:
                    // 正在破坏：无需处理，下一个tick继续推进进度
                    break;
            }
        }
    }

    public static boolean canBreakBlock(BlockPos pos) {
        if (client.level == null || client.player == null || client.gameMode == null || pos == null) {
            return false;
        }
        GameType gameType = client.gameMode.getPlayerMode();
        ClientLevel world = client.level;
        BlockState currentState = world.getBlockState(pos);
        // 部分人特殊需求
        if (Configs.Break.BREAK_CHECK_BLOCK_HARDNESS.getBooleanValue()) {
            // 非创造无法破坏无硬度的方块
            if (!gameType.isCreative() && currentState.getBlock().defaultDestroyTime() < 0) {
                return false;
            }
        }
        return !currentState.isAir() &&
                !(currentState.getBlock() instanceof LiquidBlock) &&
                !currentState.is(Blocks.AIR) &&
                !currentState.is(Blocks.CAVE_AIR) &&
                !currentState.is(Blocks.VOID_AIR) &&
                !client.player.blockActionRestricted(client.level, pos, gameType);
    }

    // 方块破坏结果枚举（核心新增）
    public enum BlockBreakResult {
        COMPLETED,    // 破坏完成
        IN_PROGRESS,  // 正在破坏，需要继续tick
        ABORTED,      // 破坏被中止（切换方块等）
        FAILED        // 破坏失败（无权限/超出边界/无法交互等）
    }

    public float getBreakingProgressMax() {
        int value = Configs.Break.BREAK_PROGRESS_THRESHOLD.getIntegerValue();
        if (value < 70) {
            value = 70;
        } else if (value > 100) {
            value = 100;
        }
        return (float) value / 100;
    }

    private int getDestroyStage() {
        float breakingProgress = destroyProgress >= getBreakingProgressMax() ? 1.0F : destroyProgress;
        return breakingProgress > 0.0F ? (int) (breakingProgress * 10.0F) : -1;
    }

    private ServerboundPlayerActionPacket getServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action action, BlockPos blockPos, Direction direction, int sequence) {
        //#if MC > 11802
        return new ServerboundPlayerActionPacket(action, blockPos, direction, sequence);
        //#else
        //$$ return new ServerboundPlayerActionPacket(action, blockPos, direction);
        //#endif
    }

    private boolean sameDestroyTarget(BlockPos blockPos) {
        return blockPos.equals(this.destroyBlockPos);
    }

    private BlockBreakResult startDestroyBlock(BlockPos blockPos, Direction direction, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode, boolean localPrediction) {
        if (player.getAbilities().instabuild) {
            NetworkUtils.sendSequencedPacket(sequence -> {
                if (localPrediction) {
                    gameMode.destroyBlock(blockPos);
                }
                return getServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, direction, sequence);
            });
            return BlockBreakResult.COMPLETED;
        } else if (!this.isDestroying || !this.sameDestroyTarget(blockPos)) {
            if (this.isDestroying) {
                NetworkUtils.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, direction));
            }
            BlockState blockState = level.getBlockState(blockPos);
            boolean bl = !blockState.isAir();
            if (bl && this.destroyProgress == 0.0F) {
                if (localPrediction) {
                    blockState.attack(level, blockPos, player);
                }
            }
            boolean instant = PlayerUtils.calcBlockBreakingDelta(player, blockState) >= 1.0F;
            if (bl && instant) {
                if (localPrediction) {
                    gameMode.destroyBlock(blockPos);
                }
            } else {
                isDestroying = true;
                destroyBlockPos = blockPos;
                destroyProgress = 0.0F;
                if (localPrediction) {
                    level.destroyBlockProgress(player.getId(), this.destroyBlockPos, getDestroyStage());
                }
            }
            NetworkUtils.sendSequencedPacket(sequence -> getServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, direction, sequence));
            if (instant) {
                return BlockBreakResult.COMPLETED;
            }
        }
        return BlockBreakResult.IN_PROGRESS;
    }

    public BlockBreakResult updateBlockBreakingProgress(BlockPos blockPos, Direction direction, boolean localPrediction) {
        this.lastBreakOperationTime = System.currentTimeMillis();
        ClientLevel level = client.level;
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (level == null || player == null || gameMode == null) {
            return BlockBreakResult.FAILED;
        }
        if (!level.getWorldBorder().isWithinBounds(blockPos)) {
            return BlockBreakResult.FAILED;
        }
        if (!PlayerUtils.canInteractWithBlockAt(player, blockPos, 1F)) {
            return BlockBreakResult.FAILED;
        }
        if (!canBreakBlock(blockPos)) {
            return BlockBreakResult.FAILED;
        }
        if (player.getAbilities().instabuild && level.getWorldBorder().isWithinBounds(blockPos)) {
            NetworkUtils.sendSequencedPacket(sequence -> {
                if (localPrediction) {
                    gameMode.destroyBlock(blockPos);
                }
                return getServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, direction, sequence);
            });
            return BlockBreakResult.COMPLETED;
        }
        if (ModLoadStatus.isTweakerooLoaded()) {
            if (TweakerooUtils.isToolSwitchEnabled()) {
                TweakerooUtils.trySwitchToEffectiveTool(blockPos);
            }
        }
        if (this.sameDestroyTarget(blockPos)) {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.isAir()) {
                this.isDestroying = false;
                return BlockBreakResult.COMPLETED;
            } else {
                this.destroyProgress = this.destroyProgress + PlayerUtils.calcBlockBreakingDelta(player, blockState);
                if (localPrediction) {
                    if (this.destroyTicks % 4.0F == 0.0F) {
                        SoundType soundType = blockState.getSoundType();
                        client
                                .getSoundManager()
                                .play(
                                        new SimpleSoundInstance(
                                                soundType.getHitSound(),
                                                SoundSource.BLOCKS,
                                                (soundType.getVolume() + 1.0F) / 8.0F,
                                                soundType.getPitch() * 0.5F,
                                                //#if MC>11802
                                                SoundInstance.createUnseededRandom(),
                                                //#endif
                                                blockPos
                                        )
                                );

                    }
                    this.destroyTicks++;
                }
                if (this.destroyProgress >= getBreakingProgressMax()) {
                    this.isDestroying = false;
                    NetworkUtils.sendSequencedPacket(sequence -> {
                        if (localPrediction) {
                            gameMode.destroyBlock(blockPos);
                        }
                        return getServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction, sequence);
                    });
                    this.destroyProgress = 0.0F;
                    this.destroyTicks = 0.0F;
                    return BlockBreakResult.COMPLETED;
                }
                if (localPrediction) {
                    level.destroyBlockProgress(player.getId(), this.destroyBlockPos, this.getDestroyStage());
                }
                return BlockBreakResult.IN_PROGRESS;
            }
        } else {
            return this.startDestroyBlock(blockPos, direction, player, level, gameMode, localPrediction);
        }
    }

    public BlockBreakResult updateBlockBreakingProgress(BlockPos blockPos, Direction direction) {
        return this.updateBlockBreakingProgress(blockPos, direction, !Configs.Break.BREAK_PLACE_USE_PACKET.getBooleanValue());
    }

    public BlockBreakResult updateBlockBreakingProgress(BlockPos blockPos) {
        return this.updateBlockBreakingProgress(blockPos, Direction.DOWN);
    }

    public void resetBreaking() {
        destroyTicks = 0.0F;
        destroyProgress = 0.0F; // 新增：重置破坏进度
        isDestroying = false;
        destroyBlockPos = new BlockPos(-1, -1, -1); // 新增：重置破坏目标位置
        lastBreakOperationTime = System.currentTimeMillis(); // 新增：重置计时器
    }
}