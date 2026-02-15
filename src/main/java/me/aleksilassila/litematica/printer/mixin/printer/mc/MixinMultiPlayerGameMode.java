package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.utils.*;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.mixin_extension.MultiPlayerGameModeExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@SuppressWarnings("DataFlowIssue")
@Mixin(value = MultiPlayerGameMode.class, priority = 1020)
public abstract class MixinMultiPlayerGameMode implements MultiPlayerGameModeExtension {
    // @formatter:off
    @Shadow private BlockPos destroyBlockPos;
    @Shadow private ItemStack destroyingItem;
    @Shadow private float destroyProgress;
    @Shadow private boolean isDestroying;
    @Shadow @Final private Minecraft minecraft;
    @Shadow public abstract boolean destroyBlock(final BlockPos pos);
    @Shadow protected abstract boolean sameDestroyTarget(final BlockPos pos);
    @Shadow protected abstract void ensureHasSentCarriedItem();
    //#if MC > 11802
    @Shadow public abstract InteractionResult useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult blockHitResult);
    //#else
    //$$ @Shadow public abstract InteractionResult useItemOn(LocalPlayer player,ClientLevel level, InteractionHand hand, BlockHitResult blockHitResult);
    //#endif
    // @formatter:on

    @Override
    public BlockPos fabric_bedrock_miner$destroyBlockPos() {
        return destroyBlockPos;
    }

    @Override
    public boolean fabric_bedrock_miner$isDestroying() {
        return isDestroying;
    }

    @Override
    public void fabric_bedrock_miner$startPrediction(PredictiveAction predictiveAction) {
        NetworkUtils.sendPacket(predictiveAction);
    }

    @Override
    public InteractionResult fabric_bedrock_miner$useItemOn(boolean localPrediction, InteractionHand hand, BlockHitResult blockHit) {
        if (localPrediction) {
            //#if MC > 11802
            return useItemOn(minecraft.player, hand, blockHit);
            //#else
            //$$ return useItemOn(minecraft.player, minecraft.level, hand, blockHit);
            //#endif
        }
        this.ensureHasSentCarriedItem();
        if (!this.minecraft.level.getWorldBorder().isWithinBounds(blockHit.getBlockPos())) {
            return InteractionResult.FAIL;
        }
        //#if MC > 11802
        fabric_bedrock_miner$startPrediction((sequence) -> new ServerboundUseItemOnPacket(hand, blockHit, sequence));
        //#else
        //$$ fabric_bedrock_miner$startPrediction((sequence) -> new ServerboundUseItemOnPacket(hand, blockHit));
        //#endif
        return InteractionResult.PASS;
    }

    @Unique
    private float litematica_printer$GetBreakingProgressMax() {
        int value = Configs.Break.BREAK_PROGRESS_THRESHOLD.getIntegerValue();
        if (value < 70) {
            value = 70;
        } else if (value > 100) {
            value = 100;
        }
        return (float) value / 100;
    }

    @Unique
    private int litematica_printer$GetDestroyStage() {
        float breakingProgress = destroyProgress >= litematica_printer$GetBreakingProgressMax() ? 1.0F : destroyProgress;
        return breakingProgress > 0.0F ? (int) (breakingProgress * 10.0F) : -1;
    }

    @Unique
    private ServerboundPlayerActionPacket litematica_printer$GetServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action action, BlockPos blockPos, Direction direction, int sequence) {
        //#if MC > 11802
        return new ServerboundPlayerActionPacket(action, blockPos, direction, sequence);
        //#else
        //$$ return new ServerboundPlayerActionPacket(action, blockPos, direction);
        //#endif
    }

    @Unique
    private BlockBreakResult litematica_printer$startDestroyBlock(BlockPos blockPos, Direction direction, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode, boolean localPrediction) {
        if (player.blockActionRestricted(level, blockPos, gameMode.getPlayerMode())) {
            return BlockBreakResult.FAILED;
        }
        if (!level.getWorldBorder().isWithinBounds(blockPos)) {
            return BlockBreakResult.FAILED;
        }
        if (player.getAbilities().instabuild) {
            NetworkUtils.sendPacket(i -> {
                if (localPrediction) {
                    destroyBlock(blockPos);
                }
                return litematica_printer$GetServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, direction, i);
            });
            return BlockBreakResult.COMPLETED;
        }
        if (!this.isDestroying || !this.sameDestroyTarget(blockPos)) {
            if (this.isDestroying) {
                NetworkUtils.sendPacket(litematica_printer$GetServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, direction, 0));
            }
            BlockState blockState = level.getBlockState(blockPos);
            boolean bl = !blockState.isAir();
            if (bl && this.destroyProgress == 0.0F) {
                if (localPrediction) {
                    blockState.attack(level, blockPos, player);
                }
            }
            float destroyProgress = blockState.getDestroyProgress(player, level, blockPos);
            if (bl && destroyProgress >= 1.0F) {
                if (localPrediction) {
                    destroyBlock(blockPos);
                }
            } else {
                this.isDestroying = true;
                this.destroyBlockPos = blockPos;
                this.destroyProgress = 0.0F;
                this.destroyingItem = player.getMainHandItem();
                if (localPrediction) {
                    level.destroyBlockProgress(player.getId(), this.destroyBlockPos, this.litematica_printer$GetDestroyStage());
                }
            }
            NetworkUtils.sendPacket(i -> litematica_printer$GetServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, direction, i));
            if (destroyProgress >= 1.0F) {
                return BlockBreakResult.COMPLETED;
            } else {
                return BlockBreakResult.IN_PROGRESS;
            }
        }
        return BlockBreakResult.FAILED;
    }

    @Override
    public BlockBreakResult litematica_printer$continueDestroyBlock(boolean localPrediction, BlockPos blockPos, Direction direction) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        if (player == null || level == null || gameMode == null) {
            return BlockBreakResult.FAILED;
        }
        if (player.getAbilities().instabuild && level.getWorldBorder().isWithinBounds(blockPos)) {
            NetworkUtils.sendPacket(i -> {
                if (localPrediction) {
                    destroyBlock(blockPos);
                }
                return litematica_printer$GetServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, direction, i);
            });
            return BlockBreakResult.COMPLETED;
        }
        if (ModLoadStatus.isTweakerooLoaded()) {
            if (TweakerooUtils.isToolSwitchEnabled()) {
                TweakerooUtils.trySwitchToEffectiveTool(blockPos);
            }
        } else {
            ensureHasSentCarriedItem();
        }
        if (this.sameDestroyTarget(blockPos)) {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.isAir()) {
                this.isDestroying = false;
                return BlockBreakResult.COMPLETED;
            } else {
                this.destroyProgress = this.destroyProgress + blockState.getDestroyProgress(player, level, blockPos);
                boolean b = this.destroyProgress >= litematica_printer$GetBreakingProgressMax();
                if (b) {
                    this.isDestroying = false;
                    NetworkUtils.sendPacket(i -> {
                        if (localPrediction) {
                            destroyBlock(blockPos);
                        }
                        return litematica_printer$GetServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction, i);
                    });
                    this.destroyProgress = 0.0F;
                }
                if (localPrediction) {
                    level.destroyBlockProgress(player.getId(), this.destroyBlockPos, this.litematica_printer$GetDestroyStage());
                }
                if (b) {
                    return BlockBreakResult.COMPLETED;
                } else {
                    return BlockBreakResult.IN_PROGRESS;
                }
            }
        } else {
            return this.litematica_printer$startDestroyBlock(blockPos, direction, player, level, gameMode, localPrediction);
        }
    }
}
