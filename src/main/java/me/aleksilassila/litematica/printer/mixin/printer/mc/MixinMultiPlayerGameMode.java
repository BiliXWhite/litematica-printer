package me.aleksilassila.litematica.printer.mixin.printer.mc;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickManager;
import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.handler.handlers.MineDebugLog;
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
    @Unique
    private BlockPos litematica_printer$lastLoggedInProgressPos;
    @Unique
    private long litematica_printer$lastLoggedInProgressTick = Long.MIN_VALUE;
    @Unique
    private long litematica_printer$serverDrivenDestroyStartTick = Long.MIN_VALUE;
    @Unique
    private long litematica_printer$serverDrivenLastStopTick = Long.MIN_VALUE;

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

    @Unique
    private void litematica_printer$clearServerDrivenDestroyState() {
        this.isDestroying = false;
        this.destroyProgress = 0.0F;
        this.litematica_printer$serverDrivenDestroyStartTick = Long.MIN_VALUE;
        this.litematica_printer$serverDrivenLastStopTick = Long.MIN_VALUE;
    }

    @Unique
    private int litematica_printer$getServerDrivenTimeoutTicks(LocalPlayer player, ClientLevel level, BlockPos blockPos, BlockState blockState) {
        float localDestroyProgress = blockState.getDestroyProgress(player, level, blockPos);
        if (localDestroyProgress <= 0.0F) {
            return 200;
        }
        int estimatedTicks = (int) Math.ceil(1.0F / localDestroyProgress);
        return Math.max(8, Math.min(estimatedTicks + 10, 200));
    }

    @Unique
    private BlockBreakResult litematica_printer$continueServerDrivenDestroy(LocalPlayer player, ClientLevel level, BlockPos blockPos, Direction direction, BlockState blockState) {
        long currentTick = ClientPlayerTickManager.getCurrentHandlerTime();
        if (this.sameDestroyTarget(blockPos)) {
            if (blockState.isAir()) {
                long waitedTicks = this.litematica_printer$serverDrivenDestroyStartTick == Long.MIN_VALUE
                        ? 0
                        : currentTick - this.litematica_printer$serverDrivenDestroyStartTick;
                this.litematica_printer$clearServerDrivenDestroyState();
                MineDebugLog.write("mine break completed pos=" + MineDebugLog.pos(blockPos)
                        + " path=fast_break_server waitedTicks=" + waitedTicks);
                return BlockBreakResult.COMPLETED;
            }

            int timeoutTicks = this.litematica_printer$getServerDrivenTimeoutTicks(player, level, blockPos, blockState);
            if (this.litematica_printer$serverDrivenDestroyStartTick != Long.MIN_VALUE
                    && currentTick - this.litematica_printer$serverDrivenDestroyStartTick >= timeoutTicks) {
                NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, blockPos, direction, sequence));
                this.litematica_printer$clearServerDrivenDestroyState();
                MineDebugLog.write("mine break completed_wait pos=" + MineDebugLog.pos(blockPos)
                        + " path=fast_break_server_timeout timeoutTicks=" + timeoutTicks
                        + " state=" + MineDebugLog.describeState(blockState));
                return BlockBreakResult.COMPLETED_WAIT;
            }

            if (this.litematica_printer$serverDrivenLastStopTick != currentTick) {
                NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction, sequence));
                this.litematica_printer$serverDrivenLastStopTick = currentTick;
            }
            if (!blockPos.equals(this.litematica_printer$lastLoggedInProgressPos)
                    || currentTick - this.litematica_printer$lastLoggedInProgressTick >= 10) {
                long waitedTicks = this.litematica_printer$serverDrivenDestroyStartTick == Long.MIN_VALUE
                        ? 0
                        : currentTick - this.litematica_printer$serverDrivenDestroyStartTick;
                MineDebugLog.write("mine break in_progress pos=" + MineDebugLog.pos(blockPos)
                        + " path=fast_break_server waitedTicks=" + waitedTicks
                        + " timeoutTicks=" + timeoutTicks
                        + " state=" + MineDebugLog.describeState(blockState));
                this.litematica_printer$lastLoggedInProgressPos = blockPos;
                this.litematica_printer$lastLoggedInProgressTick = currentTick;
            }
            return BlockBreakResult.IN_PROGRESS;
        }

        if (this.isDestroying) {
            MineDebugLog.write("mine break abort previous=" + MineDebugLog.pos(this.destroyBlockPos)
                    + " next=" + MineDebugLog.pos(blockPos)
                    + " path=fast_break_server_switch");
            NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, direction, sequence));
            this.litematica_printer$clearServerDrivenDestroyState();
        }

        this.isDestroying = true;
        this.destroyBlockPos = blockPos;
        this.destroyProgress = 0.0F;
        this.destroyingItem = player.getMainHandItem();
        this.litematica_printer$serverDrivenDestroyStartTick = currentTick;
        this.litematica_printer$serverDrivenLastStopTick = currentTick;
        NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, direction, sequence));
        NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction, sequence));
        MineDebugLog.write("mine break start pos=" + MineDebugLog.pos(blockPos)
                + " path=fast_break_server"
                + " timeoutTicks=" + this.litematica_printer$getServerDrivenTimeoutTicks(player, level, blockPos, blockState)
                + " state=" + MineDebugLog.describeState(blockState));
        return BlockBreakResult.IN_PROGRESS;
    }

    @Override
    public BlockBreakResult litematica_printer$continueDestroyBlock(boolean localPrediction, BlockPos blockPos, Direction direction) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        if (player == null || level == null || gameMode == null) {
            MineDebugLog.write("mine break failed pos=" + MineDebugLog.pos(blockPos) + " reason=missing_player_or_level_or_gamemode");
            return BlockBreakResult.FAILED;
        }
        if (player.getAbilities().instabuild && level.getWorldBorder().isWithinBounds(blockPos)) {
            NetworkUtils.sendPacket(sequence -> {
                if (localPrediction) {
                    destroyBlock(blockPos);
                }
                return litematica_printer$getServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, direction, sequence);
            });
            MineDebugLog.write("mine break completed pos=" + MineDebugLog.pos(blockPos) + " path=instabuild");
            return BlockBreakResult.COMPLETED;
        }
        if (ModLoadUtils.isTweakerooLoaded()) {
            if (TweakerooUtils.isToolSwitchEnabled()) {
                TweakerooUtils.trySwitchToEffectiveTool(blockPos);
            }
        } else {
            ensureHasSentCarriedItem();
        }
        BlockState blockState = level.getBlockState(blockPos);
        boolean isAir = blockState.isAir();
        if (isAir) {
            MineDebugLog.write("mine break completed pos=" + MineDebugLog.pos(blockPos) + " path=air");
            return BlockBreakResult.COMPLETED;
        }
        if (Configs.Break.FAST_BREAK.getBooleanValue()) {
            return this.litematica_printer$continueServerDrivenDestroy(player, level, blockPos, direction, blockState);
        }

        float currentDestroyProgress = blockState.getDestroyProgress(player, level, blockPos);
        if (currentDestroyProgress >= 1.0F || (Configs.Break.BREAK_INSTANT_MINE.getBooleanValue() && currentDestroyProgress > 0.5F)) {
            if (localPrediction) {
                destroyBlock(blockPos);
            }
            NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, direction, sequence));
            NetworkUtils.sendPacket(sequence -> litematica_printer$getServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, direction, sequence));
            if (currentDestroyProgress > 0.6F) {
                MineDebugLog.write("mine break completed pos=" + MineDebugLog.pos(blockPos)
                        + " path=instant_progress localProgress=" + currentDestroyProgress);
                return BlockBreakResult.COMPLETED;
            } else {
                MineDebugLog.write("mine break completed_wait pos=" + MineDebugLog.pos(blockPos)
                        + " path=instant_progress localProgress=" + currentDestroyProgress);
                return BlockBreakResult.COMPLETED_WAIT;
            }
        }
        if (this.sameDestroyTarget(blockPos)) {
            if (blockState.isAir()) {
                this.isDestroying = false;
                MineDebugLog.write("mine break completed pos=" + MineDebugLog.pos(blockPos) + " path=same_target_became_air");
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
                MineDebugLog.write("mine break completed pos=" + MineDebugLog.pos(blockPos)
                        + " path=same_target_threshold destroyProgress=" + this.destroyProgress
                        + " threshold=" + ConfigUtils.getBreakProgressThreshold());
                return BlockBreakResult.COMPLETED;
            }
            long currentTick = me.aleksilassila.litematica.printer.handler.ClientPlayerTickManager.getCurrentHandlerTime();
            if (!blockPos.equals(this.litematica_printer$lastLoggedInProgressPos)
                    || currentTick - this.litematica_printer$lastLoggedInProgressTick >= 10) {
                MineDebugLog.write("mine break in_progress pos=" + MineDebugLog.pos(blockPos)
                        + " path=same_target destroyProgress=" + this.destroyProgress
                        + " threshold=" + ConfigUtils.getBreakProgressThreshold());
                this.litematica_printer$lastLoggedInProgressPos = blockPos;
                this.litematica_printer$lastLoggedInProgressTick = currentTick;
            }
            return BlockBreakResult.IN_PROGRESS;

        } else if (!this.isDestroying || !this.sameDestroyTarget(blockPos)) {
            if (this.isDestroying) {
                MineDebugLog.write("mine break abort previous=" + MineDebugLog.pos(this.destroyBlockPos)
                        + " next=" + MineDebugLog.pos(blockPos));
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
                MineDebugLog.write("mine break completed pos=" + MineDebugLog.pos(blockPos)
                        + " path=start_only destroyProgress=" + destroyProgress);
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
                    MineDebugLog.write("mine break completed_wait pos=" + MineDebugLog.pos(blockPos)
                            + " path=delayed_destroy destroyProgress=" + destroyProgress);
                    return BlockBreakResult.COMPLETED_WAIT;
                }
                MineDebugLog.write("mine break start pos=" + MineDebugLog.pos(blockPos)
                        + " path=new_target destroyProgress=" + destroyProgress
                        + " localPrediction=" + localPrediction
                        + " state=" + MineDebugLog.describeState(blockState));
            }
            if (destroyProgress >= 1.0F) {
                return BlockBreakResult.COMPLETED;
            } else {
                return BlockBreakResult.IN_PROGRESS;
            }
        }
        MineDebugLog.write("mine break failed pos=" + MineDebugLog.pos(blockPos) + " reason=fell_through_state_machine");
        return BlockBreakResult.FAILED;
    }
}
