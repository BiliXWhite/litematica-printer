package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.bilixwhite.utils.TweakerooUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.interfaces.IMultiPlayerGameMode;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import me.aleksilassila.litematica.printer.utils.NetworkUtils;
import me.aleksilassila.litematica.printer.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

//#if MC < 11904
//$$ import net.minecraft.world.level.Level;
//$$ import net.minecraft.client.multiplayer.ClientLevel;
//$$
//#endif

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode implements IMultiPlayerGameMode {
    @Final
    @Shadow
    private Minecraft minecraft;

    @Shadow
    private BlockPos destroyBlockPos = new BlockPos(-1, -1, -1);

    @Shadow
    private ItemStack destroyingItem = ItemStack.EMPTY;

    @Shadow
    private float destroyProgress;

    @Shadow
    private boolean isDestroying;

    @Override
    public void litematica_printer$rightClickBlock(BlockPos pos, Direction side, Vec3 hitVec) {
        useItemOn(minecraft.player,
                //#if MC < 11904
                //$$ minecraft.level,
                //#endif
                InteractionHand.MAIN_HAND,
                new BlockHitResult(hitVec, side, pos, false));
        useItem(minecraft.player,
                //#if MC < 11904
                //$$ minecraft.level,
                //#endif
                InteractionHand.MAIN_HAND);
    }

    @Shadow
    public abstract InteractionResult useItemOn(
            LocalPlayer clientPlayerEntity_1,
            //#if MC < 11904
            //$$ClientLevel level,
            //#endif
            InteractionHand hand_1, BlockHitResult blockHitResult_1);

    @Shadow
    public abstract InteractionResult useItem(Player playerEntity_1,
                                              //#if MC < 11904
                                              //$$ Level level,
                                              //#endif
                                              InteractionHand hand_1);

    @Shadow
    protected abstract boolean sameDestroyTarget(BlockPos blockPos);

    @Shadow
    protected abstract void ensureHasSentCarriedItem();

    @Unique
    public float litematica_printer$GetBreakingProgressMax() {
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

    private InteractionUtils.BlockBreakResult litematica_printer$StartDestroyBlock(BlockPos blockPos, Direction direction, LocalPlayer player, ClientLevel level, MultiPlayerGameMode gameMode, boolean localPrediction) {
        if (player.blockActionRestricted(level, blockPos, gameMode.getPlayerMode())) {
            return InteractionUtils.BlockBreakResult.FAILED;
        }
        if (!level.getWorldBorder().isWithinBounds(blockPos)) {
            return InteractionUtils.BlockBreakResult.FAILED;
        }
        if (player.getAbilities().instabuild) {
            NetworkUtils.sendPacket(i -> {
                if (localPrediction) {
                    gameMode.destroyBlock(blockPos);
                }
                return litematica_printer$GetServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, direction, i);
            });
            return InteractionUtils.BlockBreakResult.COMPLETED;
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
            float destroyProgress = PlayerUtils.getDestroyProgress(player, blockState);
            if (bl && destroyProgress >= 1.0F) {
                if (localPrediction) {
                    gameMode.destroyBlock(blockPos);
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
                return InteractionUtils.BlockBreakResult.COMPLETED;
            } else {
                return InteractionUtils.BlockBreakResult.IN_PROGRESS;
            }
        }
        return InteractionUtils.BlockBreakResult.FAILED;
    }

    @Override
    public InteractionUtils.BlockBreakResult litematica_printer$ContinueDestroyBlock(BlockPos blockPos, Direction direction, boolean localPrediction) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        if (player == null || level == null || gameMode == null) {
            return InteractionUtils.BlockBreakResult.FAILED;
        }
        if (player.getAbilities().instabuild && level.getWorldBorder().isWithinBounds(blockPos)) {
            NetworkUtils.sendPacket(i -> {
                if (localPrediction) {
                    gameMode.destroyBlock(blockPos);
                }
                return litematica_printer$GetServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, direction, i);
            });
            return InteractionUtils.BlockBreakResult.COMPLETED;
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
                return InteractionUtils.BlockBreakResult.COMPLETED;
            } else {
                this.destroyProgress = this.destroyProgress + PlayerUtils.getDestroyProgress(player, blockState);
                boolean b = this.destroyProgress >= litematica_printer$GetBreakingProgressMax();
                if (b) {
                    this.isDestroying = false;
                    NetworkUtils.sendPacket(i -> {
                        if (localPrediction) {
                            gameMode.destroyBlock(blockPos);
                        }
                        return litematica_printer$GetServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction, i);
                    });
                    this.destroyProgress = 0.0F;
                }
                if (localPrediction) {
                    level.destroyBlockProgress(player.getId(), this.destroyBlockPos, this.litematica_printer$GetDestroyStage());
                }
                if (b) {
                    return InteractionUtils.BlockBreakResult.COMPLETED;
                } else {
                    return InteractionUtils.BlockBreakResult.IN_PROGRESS;
                }
            }
        } else {
            return this.litematica_printer$StartDestroyBlock(blockPos, direction, player, level, gameMode, localPrediction);
        }
    }
}
