package me.aleksilassila.litematica.printer.utils;

import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.bilixwhite.utils.TweakerooUtils;
import me.aleksilassila.litematica.printer.config.Configs;
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
    public static InteractionUtils INSTANCE = new InteractionUtils();

    private InteractionUtils() {
    }

    private final Minecraft client = Minecraft.getInstance();

    private BlockPos destroyBlockPos = new BlockPos(-1, -1, -1);
    private float destroyProgress;
    private boolean isDestroying;
    private float destroyTicks;

    private final Deque<BlockPos> breakQueue = new ArrayDeque<>();
    private BlockPos activePos = null;

    public void addQueue(BlockPos pos) {
        if (pos == null) return;
        if (!breakQueue.contains(pos)) {
            breakQueue.addLast(pos);
        }
    }

    public void clearQueue() {
        breakQueue.clear();
        resetBreaking();
        activePos = null;
    }

    public void onTick() {
        if (breakQueue.isEmpty()) {
            if (isDestroying) {
                resetBreaking();
                activePos = null;
            }
            return;
        }
        if (client.level == null || client.player == null || client.gameMode == null) {
            clearQueue();
            return;
        }

        while (!breakQueue.isEmpty()) {
            if (activePos == null) {
                activePos = breakQueue.pollFirst();
                if (activePos == null) {
                    return; // 队列空
                }
                resetBreaking();
            }
            BlockBreakResult result = updateBlockBreakingProgress(activePos, Direction.DOWN);
            destroyTicks++;
            switch (result) {
                case COMPLETED, ABORTED, FAILED -> {
                    activePos = null;
                    resetBreaking();
                }
            }
        }
    }


    public boolean canBreakBlock(BlockPos pos) {
        if (client.level == null || client.player == null || client.gameMode == null || pos == null) {
            return false;
        }
        GameType gameType = client.gameMode.getPlayerMode();
        ClientLevel world = client.level;
        BlockState currentState = world.getBlockState(pos);
        // 非创造无法破坏无硬度的方块
        if (!gameType.isCreative() && currentState.getBlock().defaultDestroyTime() < 0) {
            return false;
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
                if (localPrediction){
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

    public BlockBreakResult updateBlockBreakingProgress(BlockPos pos, Direction direction) {
        return updateBlockBreakingProgress(pos, direction, true);
    }

    public BlockBreakResult updateBlockBreakingProgress(BlockPos pos) {
        return updateBlockBreakingProgress(pos, Direction.DOWN);
    }

    public void resetBreaking() {
        destroyTicks = 0;
        setDestroying(false);
    }

    public boolean isDestroying() {
        return isDestroying;
    }

    public void setDestroying(boolean destroying) {
        this.isDestroying = destroying;
    }

    public BlockPos getDestroyBlockPos() {
        return destroyBlockPos;
    }

    public float getDestroyProgress() {
        return destroyProgress;
    }
}