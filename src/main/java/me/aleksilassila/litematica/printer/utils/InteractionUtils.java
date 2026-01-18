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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Queue;

@Environment(EnvType.CLIENT)
public class InteractionUtils {
    // 单例实例
    public static InteractionUtils INSTANCE = new InteractionUtils();

    private InteractionUtils() {
    }

    // 客户端实例（不变）
    private final Minecraft client = Minecraft.getInstance();

    // 实例化成员变量（原静态变量改为单例成员变量）
    private BlockPos currentBreakingPos = new BlockPos(-1, -1, -1);
    private float currentBreakingProgress;
    private boolean breakingBlock;
    private int breakingTicks;

    private final Queue<BlockPos> blockQueue = new ArrayDeque<>();

    // 方块破坏结果枚举（核心新增）
    public enum BlockBreakResult {
        COMPLETED,    // 破坏完成
        IN_PROGRESS,  // 正在破坏，需要继续tick
        ABORTED,      // 破坏被中止（切换方块等）
        FAILED        // 破坏失败（无权限/超出边界/无法交互等）
    }

    public float getBreakingProgressMax() {
        int value = Configs.General.BREAK_PROGRESS_THRESHOLD.getIntegerValue();
        if (value < 70) {
            value = 70;
        } else if (value > 100) {
            value = 100;
        }
        return (float) value / 100;
    }

    private int getBlockBreakingProgress() {
        float breakingProgress = currentBreakingProgress >= getBreakingProgressMax() ? 1.0F : currentBreakingProgress;
        return breakingProgress > 0.0F ? (int) (breakingProgress * 10.0F) : -1;
    }

    private ServerboundPlayerActionPacket getServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action action, BlockPos blockPos, Direction direction, int sequence) {
        //#if MC > 11802
        return new ServerboundPlayerActionPacket(action, blockPos, direction, sequence);
        //#else
        //$$ return new ServerboundPlayerActionPacket(action, blockPos, direction);
        //#endif
    }

    // 核心修改：返回具体的破坏结果枚举
    public BlockBreakResult updateBlockBreakingProgress(BlockPos pos, Direction direction, boolean localPrediction) {
        ClientLevel world = client.level;
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (world == null || player == null || gameMode == null) return BlockBreakResult.FAILED;
        if (!world.getWorldBorder().isWithinBounds(pos)) return BlockBreakResult.FAILED;
        if (player.blockActionRestricted(world, pos, gameMode.getPlayerMode())) return BlockBreakResult.FAILED;
        if (!PlayerUtils.canInteractWithBlockAt(player, pos, 0F)) return BlockBreakResult.FAILED;
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.getFluidState().isEmpty()) return BlockBreakResult.FAILED;
        if (blockState.isAir()) return BlockBreakResult.FAILED;
        if (ModLoadStatus.isTweakerooLoaded()) {
            if (TweakerooUtils.isToolSwitchEnabled()) {
                TweakerooUtils.trySwitchToEffectiveTool(pos);
            }
        }
        if (gameMode.getPlayerMode().isCreative()) {
            setBreakingBlock(true);
            NetworkUtils.sendSequencedPacket((sequence) -> {
                if (!blockState.isAir() && localPrediction) {
                    gameMode.destroyBlock(pos);
                }
                return getServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence);
            });
            setBreakingBlock(false);
            return BlockBreakResult.COMPLETED;
        }
        if (breakingBlock && pos.equals(currentBreakingPos)) {
            if (blockState.isAir()) {
                setBreakingBlock(false);
                return BlockBreakResult.COMPLETED;
            }
            currentBreakingProgress += PlayerUtils.calcBlockBreakingDelta(player, blockState);
            if (currentBreakingProgress >= getBreakingProgressMax()) {
                NetworkUtils.sendSequencedPacket((sequence) -> {
                    if (!blockState.isAir() && localPrediction) {
                        gameMode.destroyBlock(pos);
                    }
                    return getServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
                });
                currentBreakingProgress = 0.0F;
                if (localPrediction) {
                    world.destroyBlockProgress(player.getId(), currentBreakingPos, -1);
                }
                setBreakingBlock(false);
                return BlockBreakResult.COMPLETED;
            } else {
                if (localPrediction) {
                    world.destroyBlockProgress(player.getId(), currentBreakingPos, getBlockBreakingProgress());
                }
                return BlockBreakResult.IN_PROGRESS;
            }
        } else {
            if (breakingBlock && !pos.equals(currentBreakingPos)) {
                NetworkUtils.sendPacket(getServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, currentBreakingPos, direction, 0));
                setBreakingBlock(false);
            }
            currentBreakingProgress += PlayerUtils.calcBlockBreakingDelta(player, blockState);
            if (currentBreakingProgress >= 1.0F) {  // 服务端刚开始的瞬间破坏是>=1.0F的, 所以这个值是不应该降低
                setBreakingBlock(true);
                NetworkUtils.sendSequencedPacket((sequence) -> {
                    if (!blockState.isAir() && localPrediction) {
                        gameMode.destroyBlock(pos);
                    }
                    return getServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence);
                });
                currentBreakingProgress = 0.0F;
                setBreakingBlock(false);
                return BlockBreakResult.COMPLETED;
            } else {
                NetworkUtils.sendSequencedPacket((sequence) -> {
                    if (!blockState.isAir() && currentBreakingProgress == 0.0F) {
                        blockState.attack(world, pos, player);
                    }
                    setBreakingBlock(true);
                    currentBreakingPos = pos;
                    currentBreakingProgress = 0.0F;
                    if (localPrediction) {
                        world.destroyBlockProgress(player.getId(), currentBreakingPos, getBlockBreakingProgress());
                    }
                    return getServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence);
                });
                return BlockBreakResult.IN_PROGRESS;
            }
        }
    }

    // 重载方法：默认localPrediction=true
    public BlockBreakResult updateBlockBreakingProgress(BlockPos pos, Direction direction) {
        return updateBlockBreakingProgress(pos, direction, true);
    }

    // 重载方法：默认方向DOWN
    public BlockBreakResult updateBlockBreakingProgress(BlockPos pos) {
        return updateBlockBreakingProgress(pos, Direction.DOWN);
    }

    public void resetBreaking() {
        breakingTicks = 0;
        setBreakingBlock(false);
    }

    public void autoResetBreaking() {
        if (!breakingBlock && breakingTicks > 0) {
            resetBreaking();
        }
        if (breakingBlock) {
            resetBreaking();
        }
    }

    public boolean isBreakingBlock() {
        return breakingBlock;
    }

    public void setBreakingBlock(boolean breakingBlock) {
        this.breakingBlock = breakingBlock;
    }

    // 可选：暴露当前破坏位置和进度（便于外部监控）
    public BlockPos getCurrentBreakingPos() {
        return currentBreakingPos;
    }

    public float getCurrentBreakingProgress() {
        return currentBreakingProgress;
    }
}