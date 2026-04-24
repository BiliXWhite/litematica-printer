package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.utils.*;
import me.aleksilassila.litematica.printer.mixin_extension.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.utils.minecraft.NetworkUtils;
import me.aleksilassila.litematica.printer.utils.mods.ModLoadUtils;
import me.aleksilassila.litematica.printer.utils.mods.TweakerooUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("DataFlowIssue")
@Mixin(value = MultiPlayerGameMode.class, priority = 1020)
public abstract class MixinMultiPlayerGameMode implements MultiPlayerGameModeExtension {
    // @formatter:off
    @Shadow
    private BlockPos destroyBlockPos;
    @Shadow
    private ItemStack destroyingItem;
    @Shadow
    private float destroyProgress;
    @Shadow
    private boolean isDestroying;
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract boolean destroyBlock(final BlockPos pos);

    @Shadow
    protected abstract boolean sameDestroyTarget(final BlockPos pos);

    @Shadow
    protected abstract void ensureHasSentCarriedItem();

    //#if MC > 11802
    @Shadow public abstract InteractionResult useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult blockHitResult);
    //#else
    //$$ @Shadow public abstract InteractionResult useItemOn(LocalPlayer player,ClientLevel level, InteractionHand hand, BlockHitResult blockHitResult);
    //#endif

    // @formatter:on

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo ci) {
    }

    @Override
    public InteractionResult litematica_printer$useItemOn(boolean localPrediction, InteractionHand hand, BlockHitResult blockHit) {
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
        NetworkUtils.sendPacket(sequence -> new ServerboundUseItemOnPacket(hand, blockHit, sequence));
        //#else
        //$$ NetworkUtils.sendPacket(sequence -> new ServerboundUseItemOnPacket(hand, blockHit));
        //#endif
        return InteractionResult.PASS;
    }


    @Unique
    private int litematica_printer$getDestroyStage() {
        float breakingProgress = this.destroyProgress >= ConfigUtils.getBreakProgressThreshold() ? 1.0F : this.destroyProgress;
        return breakingProgress > 0.0F ? (int) (breakingProgress * 10.0F) : -1;
    }

    @Unique
    private ServerboundPlayerActionPacket litematica_printer$getServerboundPlayerActionPacket(Action action, BlockPos blockPos, Direction direction, int sequence) {
        //#if MC > 11802
        return new ServerboundPlayerActionPacket(action, blockPos, direction, sequence);
        //#else
        //$$ return new ServerboundPlayerActionPacket(action, blockPos, direction);
        //#endif
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
            NetworkUtils.sendPacket(sequence -> {
                if (localPrediction) {
                    destroyBlock(blockPos);
                }
                return litematica_printer$getServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, direction, sequence);
            });
            return BlockBreakResult.COMPLETED;
        }
        if (ModLoadUtils.isTweakerooLoaded()) {
            if (TweakerooUtils.isToolSwitchEnabled()) {
                TweakerooUtils.trySwitchToEffectiveTool(blockPos);
            }
        } else {
            ensureHasSentCarriedItem();
        }
        if (Configs.Break.FAST_BREAK.getBooleanValue()) {
            NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, direction, sequence));
            NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction, sequence));
            return BlockBreakResult.COMPLETED;
        }
        BlockState blockState = level.getBlockState(blockPos);
        boolean isAir = blockState.isAir();
        if (isAir) {
            return BlockBreakResult.COMPLETED;
        }

        float currentDestroyProgress = blockState.getDestroyProgress(player, level, blockPos);
        if (currentDestroyProgress >= 1.0F || (Configs.Break.BREAK_INSTANT_MINE.getBooleanValue() && currentDestroyProgress > 0.5F)) {
            if (localPrediction) {
                destroyBlock(blockPos);
            }
            NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, direction, sequence));
            NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction, sequence));
            if (currentDestroyProgress > 0.6F) {
                return BlockBreakResult.COMPLETED;
            } else {
                return BlockBreakResult.COMPLETED_WAIT;
            }
        }
        if (this.sameDestroyTarget(blockPos)) {
            if (blockState.isAir()) {
                this.isDestroying = false;
                return BlockBreakResult.COMPLETED;
            }
            this.destroyProgress = this.destroyProgress + blockState.getDestroyProgress(player, level, blockPos);
            if (this.destroyProgress >= ConfigUtils.getBreakProgressThreshold()) {
                this.isDestroying = false;
                this.destroyProgress = 0.0F;
                NetworkUtils.sendPacket(sequence -> {
                    if (localPrediction) {
                        destroyBlock(blockPos);
                    }
                    return litematica_printer$getServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction, sequence);
                });
                if (localPrediction) {
                    level.destroyBlockProgress(player.getId(), this.destroyBlockPos, this.litematica_printer$getDestroyStage());
                }
                return BlockBreakResult.COMPLETED;
            }
            return BlockBreakResult.IN_PROGRESS;

        } else if (!this.isDestroying || !this.sameDestroyTarget(blockPos)) {
            if (this.isDestroying) {
                NetworkUtils.sendPacket(litematica_printer$getServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, direction, 0));
            }
            if (this.destroyProgress == 0.0F) {
                if (localPrediction) {
                    blockState.attack(level, blockPos, player);
                }
            }
            float destroyProgress = blockState.getDestroyProgress(player, level, blockPos);
            if (destroyProgress >= 1.0F) {
                if (localPrediction) {
                    destroyBlock(blockPos);
                }
                NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, direction, sequence));
            } else {
                this.isDestroying = true;
                this.destroyBlockPos = blockPos;
                this.destroyProgress = 0.0F;
                this.destroyingItem = player.getMainHandItem();
                if (localPrediction) {
                    level.destroyBlockProgress(player.getId(), this.destroyBlockPos, this.litematica_printer$getDestroyStage());
                }
                NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, direction, sequence));
                if (Configs.Break.BREAK_USE_DELAYED_DESTROY.getBooleanValue()) {
                    this.isDestroying = false;
                    this.destroyProgress = 0.0F;
                    NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction, sequence));
                    if (localPrediction) {
                        level.destroyBlockProgress(player.getId(), this.destroyBlockPos, -1);
                    }
                    return BlockBreakResult.COMPLETED_WAIT;
                }
            }
            if (destroyProgress >= 1.0F) {
                return BlockBreakResult.COMPLETED;
            } else {
                return BlockBreakResult.IN_PROGRESS;
            }
        }
        return BlockBreakResult.FAILED;
    }
}
