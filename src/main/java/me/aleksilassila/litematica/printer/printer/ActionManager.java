package me.aleksilassila.litematica.printer.printer;

//#if MC > 12100
import fi.dy.masa.litematica.util.EasyPlaceUtils;
//#endif
import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.mixin_extension.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.utils.mods.QuickShulkerBridge;
import me.aleksilassila.litematica.printer.utils.minecraft.DirectionUtils;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.NetworkUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

//#if MC > 12105
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
//#else
//$$ import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
//#endif

@SuppressWarnings("SpellCheckingInspection")
public class ActionManager {
    private static final float LOOK_SETTLED_EPSILON_DEGREES = 1.0F;
    private static final double STALE_WAIT_MOVE_DISTANCE_SQR = 0.75D * 0.75D;
    private QueuedClick queuedClick;
    private final InteractionScreenSessions screenSessions = new InteractionScreenSessions();
    @Nullable
    private PlayerLook look;
    private boolean needWaitModifyLook = false;
    private boolean waitForHorizontalLook = true;
    private boolean actionRequiresWaitModifyLook = false;
    private long lastQueuedLookTick = Long.MIN_VALUE;
    private float lastQueuedLookYaw;
    private float lastQueuedLookPitch;
    private boolean printerInteractionActive;
    private boolean easyPlaceProtocolActive;
    private ActionSource activeSource = ActionSource.GENERIC;

    public enum ActionSource {
        GENERIC,
        PRINT,
        FILL,
        COVER,
        FLUID
    }

    public enum SendResult {
        /** Local useItemOn was accepted for dispatch; server placement is confirmed elsewhere. */
        SENT,
        WAITING_FOR_LOOK,
        NO_QUEUED_ACTION,
        NO_PLAYER,
        STALE_POSITION,
        HELD_ITEM_CHANGED,
        RESERVE_LIMIT,
        NO_GAME_MODE,
        INTERACTION_REJECTED;

        public boolean isSent() {
            return this == SENT;
        }

        public boolean isWaiting() {
            return this == WAITING_FOR_LOOK;
        }
    }

    public ActionManager() {
    }

    public boolean isWaitingForLook() {
        return this.needWaitModifyLook;
    }

    @Nullable
    public PlayerLook getLook() {
        return this.look;
    }

    public void setLook(@Nullable PlayerLook look) {
        this.look = look;
    }

    public boolean queueClick(@NotNull BlockPos target, @NotNull Direction side, @NotNull Vec3 hitModifier, boolean useShift) {
        return this.queueClick(target, side, hitModifier, useShift, 1);
    }

    public boolean queueClick(@NotNull BlockPos target, @NotNull Direction side, @NotNull Vec3 hitModifier, boolean useShift, int clickRepeatCount) {
        return this.queueClick(target, side, hitModifier, useShift, clickRepeatCount, null);
    }

    public boolean queueClick(@NotNull BlockPos target, @NotNull Direction side, @NotNull Vec3 hitModifier, boolean useShift, int clickRepeatCount, @Nullable Item[] expectedItems) {
        return this.queueClick(target, side, hitModifier, useShift, clickRepeatCount, expectedItems, ActionSource.GENERIC);
    }

    public boolean queueClick(
            @NotNull BlockPos target,
            @NotNull Direction side,
            @NotNull Vec3 hitModifier,
            boolean useShift,
            int clickRepeatCount,
            @Nullable Item[] expectedItems,
            @NotNull ActionSource source
    ) {
        if (this.queuedClick != null) {
            return false;
        }
        this.queuedClick = new QueuedClick(target, side, hitModifier, useShift, clickRepeatCount, source);
        this.queuedClick.expectItems(expectedItems);
        return true;
    }

    public void useProtocolHitModifier(@NotNull Vec3 hitModifier) {
        if (this.queuedClick != null) {
            this.queuedClick.useProtocolHit(hitModifier);
        }
    }

    public boolean setQueueCompletionListener(@Nullable Consumer<SendResult> completionListener) {
        if (this.queuedClick == null) {
            return false;
        }
        this.queuedClick.onCompletion(completionListener);
        return true;
    }

    public boolean setExpectedStackPredicate(@Nullable Predicate<ItemStack> expectedStackPredicate) {
        if (this.queuedClick == null) {
            return false;
        }
        this.queuedClick.expectStack(expectedStackPredicate);
        return true;
    }

    public SendResult sendQueue(@Nullable LocalPlayer player) {
        QueuedClick click = this.queuedClick;
        if (click == null) {
            return SendResult.NO_QUEUED_ACTION;
        }
        if (player == null) {
            return this.finish(click, SendResult.NO_PLAYER);
        }
        if (shouldDropStaleQueuedClick(player, click)) {
            return this.finish(click, SendResult.STALE_POSITION);
        }
        if (!needWaitModifyLook && look != null && shouldSendQueuedLook(look)) {
            NetworkUtils.sendLookPacket(player, look);
            this.recordQueuedLook(look);
        }
        if (shouldWaitForServerLook(player, click)) {
            needWaitModifyLook = true;
            return SendResult.WAITING_FOR_LOOK;
        }
        if (needWaitModifyLook) {
            needWaitModifyLook = false;
        }
        if (!isHoldingExpectedItem(player, click)) {
            return this.finish(click, SendResult.HELD_ITEM_CHANGED);
        }
        int reserveAllowance = getReserveAllowance(player, click);
        if (reserveAllowance <= 0) {
            return this.finish(click, SendResult.RESERVE_LIMIT);
        }
        Direction direction;
        if (look == null) {
            direction = click.side;
        } else {
            direction = DirectionUtils.getHorizontalDirection(look.yaw);
        }
        Vec3 hitVec;
        if (!click.useProtocol) {
            Vec3 targetCenter = Vec3.atCenterOf(click.target);
            Vec3 sideOffset = Vec3.atLowerCornerOf(DirectionUtils.getVector(click.side)).scale(0.5);
            Vec3 rotatedHitModifier = click.hitModifier.yRot((direction.toYRot() + 90) % 360).scale(0.5);
            hitVec = targetCenter.add(sideOffset).add(rotatedHitModifier);
        } else {
            hitVec = click.hitModifier;
        }
        QuickShulkerBridge.onMainHandUse(player);
        boolean wasSneak = player.isShiftKeyDown();
        if (click.useShift && !wasSneak) {
            setShift(player, true);
        } else if (!click.useShift && wasSneak) {
            setShift(player, false);
        }
        if (!(Reference.MINECRAFT.gameMode instanceof MultiPlayerGameModeExtension gameModeExtension)) {
            restoreShift(player, click, wasSneak);
            return this.finish(click, SendResult.NO_GAME_MODE);
        }

        boolean accepted = false;
        this.printerInteractionActive = true;
        this.easyPlaceProtocolActive = click.useProtocol;
        this.activeSource = click.source;
        boolean litematicaEasyPlaceWasHandling = false;
        //#if MC > 12100
        litematicaEasyPlaceWasHandling = EasyPlaceUtils.isHandling();
        if (!litematicaEasyPlaceWasHandling) {
            EasyPlaceUtils.setHandling(true);
        }
        //#endif
        try {
            BlockHitResult blockHitResult = new BlockHitResult(hitVec, click.side, click.target, false);
            for (int i = 0; i < click.repeatCount; i++) {
                if (reserveAllowance <= 0) {
                    break;
                }
                me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerDiagnostics.placement(
                        player, click.target, click.side, click.expectedItems);
                boolean interactionAccepted = gameModeExtension.litematica_printer$useItemOn(
                        true,
                        InteractionHand.MAIN_HAND,
                        blockHitResult
                ) != net.minecraft.world.InteractionResult.FAIL;
                accepted |= interactionAccepted;
                if (interactionAccepted) {
                    this.armTaskAnvilScreenSuppression(click);
                }
                if (interactionAccepted && reserveAllowance != Integer.MAX_VALUE) {
                    reserveAllowance--;
                }
            }
        } finally {
            //#if MC > 12100
            if (!litematicaEasyPlaceWasHandling) {
                EasyPlaceUtils.setHandling(false);
            }
            //#endif
            this.printerInteractionActive = false;
            this.easyPlaceProtocolActive = false;
            this.activeSource = ActionSource.GENERIC;
            restoreShift(player, click, wasSneak);
        }
        return this.finish(click, accepted ? SendResult.SENT : SendResult.INTERACTION_REJECTED);
    }

    private void armTaskAnvilScreenSuppression(QueuedClick click) {
        if (!this.screenSessions.hasManualAnvilScreenAllowance(System.nanoTime())
                && (click.source == ActionSource.PRINT || click.source == ActionSource.FILL || click.source == ActionSource.COVER)
                && Reference.MINECRAFT.level != null
                && Reference.MINECRAFT.level.getBlockState(click.target).getBlock() instanceof AnvilBlock) {
            this.screenSessions.armTaskAnvilScreen(System.nanoTime());
        }
    }

    public boolean consumeTaskAnvilScreenSuppression() {
        return this.screenSessions.consumeTaskAnvilScreenSuppression(System.nanoTime());
    }

    public void prioritizeManualAnvilScreen() {
        this.screenSessions.prioritizeManualAnvilScreen(System.nanoTime());
    }

    public boolean consumeManualAnvilScreenAllowance() {
        return this.screenSessions.consumeManualAnvilScreenAllowance(System.nanoTime());
    }

    private static int getReserveAllowance(LocalPlayer player, QueuedClick click) {
        if (click.source != ActionSource.PRINT
                || !Configs.Print.PRINT_RESERVE_ITEMS.getBooleanValue()) {
            return Integer.MAX_VALUE;
        }
        return InventoryUtils.getConsumableSurplus(
                player,
                player.getMainHandItem(),
                click.expectedStackPredicate,
                Configs.Print.PRINT_RESERVE_ITEM_COUNT.getIntegerValue()
        );
    }

    private void restoreShift(LocalPlayer player, QueuedClick click, boolean wasSneak) {
        if (click.useShift && !wasSneak) {
            setShift(player, false);
        } else if (!click.useShift && wasSneak) {
            setShift(player, true);
        }
    }

    private SendResult finish(QueuedClick click, SendResult result) {
        Consumer<SendResult> completionListener = click.completionListener;
        this.clearQueue();
        if (completionListener != null) {
            completionListener.accept(result);
        }
        return result;
    }

    /**
     * Cancel a queued action and complete its task callback. Inventory/container workflows use
     * this instead of silently dropping an action that may already have entered a queued state.
     */
    public void cancelQueue() {
        QueuedClick click = this.queuedClick;
        if (click == null) {
            this.clearQueue();
            return;
        }
        Consumer<SendResult> completionListener = click.completionListener;
        this.clearQueue();
        if (completionListener != null) {
            completionListener.accept(SendResult.NO_QUEUED_ACTION);
        }
    }

    public boolean isPrinterInteractionActive() {
        return this.printerInteractionActive;
    }

    public boolean isEasyPlaceProtocolActive() {
        return this.printerInteractionActive
                && this.activeSource == ActionSource.PRINT
                && this.easyPlaceProtocolActive;
    }

    public void armPrintSignEdit(BlockPos blockPos) {
        long now = System.nanoTime();
        this.screenSessions.armPrintSignEdit(blockPos.asLong(), now);
    }

    public void confirmPrintSignEditSent(BlockPos blockPos) {
        this.screenSessions.confirmPrintSignEditSent(blockPos.asLong(), System.nanoTime());
    }

    public void cancelPrintSignEdit(BlockPos blockPos) {
        this.screenSessions.cancelPrintSignEdit(blockPos.asLong());
    }

    public boolean consumePrintSignEdit(BlockPos blockPos) {
        return this.screenSessions.consumePrintSignEdit(blockPos.asLong(), System.nanoTime());
    }

    public void setShift(LocalPlayer player, boolean shift) {
        //#if MC > 12105
        Input input = new Input(player.input.keyPresses.forward(), player.input.keyPresses.backward(), player.input.keyPresses.left(), player.input.keyPresses.right(), player.input.keyPresses.jump(), shift, player.input.keyPresses.sprint());
        ServerboundPlayerInputPacket packet = new ServerboundPlayerInputPacket(input);
        //#else
        //$$ ServerboundPlayerCommandPacket packet = new ServerboundPlayerCommandPacket(player, shift ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY);
        //#endif
        player.setShiftKeyDown(shift);
        NetworkUtils.sendPacket(packet);
    }

    public void setWaitForHorizontalLook(boolean waitForHorizontalLook) {
        this.waitForHorizontalLook = waitForHorizontalLook;
    }

    public void setNeedWaitModifyLookFromAction(boolean actionRequiresWaitModifyLook) {
        this.actionRequiresWaitModifyLook = actionRequiresWaitModifyLook;
    }

    private boolean shouldWaitForServerLook(LocalPlayer player, QueuedClick click) {
        if ((!this.waitForHorizontalLook && !this.actionRequiresWaitModifyLook)
                || click.useProtocol
                || this.needWaitModifyLook
                || this.look == null) {
            return false;
        }
        Direction lookDirection = DirectionUtils.orderedByNearest(this.look.yaw, this.look.pitch)[0];
        return lookDirection.getAxis().isHorizontal()
                && !isPlayerLookSettled(player, this.look);
    }

    private boolean shouldDropStaleQueuedClick(LocalPlayer player, QueuedClick click) {
        if (!this.needWaitModifyLook || click.queuedPlayerPosition == null) {
            return false;
        }
        long currentTick = Reference.MINECRAFT.level == null ? Long.MIN_VALUE : Reference.MINECRAFT.level.getGameTime();
        if (currentTick == Long.MIN_VALUE || currentTick <= click.queuedTick) {
            return false;
        }
        return player.position().distanceToSqr(click.queuedPlayerPosition) > STALE_WAIT_MOVE_DISTANCE_SQR;
    }

    private static boolean isHoldingExpectedItem(LocalPlayer player, QueuedClick click) {
        if (click.expectedStackPredicate != null
                && !click.expectedStackPredicate.test(player.getMainHandItem())) {
            return false;
        }
        if (click.expectedItems == null || click.expectedItems.length == 0) {
            return true;
        }
        Item heldItem = player.getMainHandItem().getItem();
        for (Item expectedItem : click.expectedItems) {
            if (heldItem.equals(expectedItem)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPlayerLookSettled(LocalPlayer player, PlayerLook look) {
        return Math.abs(Mth.wrapDegrees(player.getYRot() - look.yaw)) <= LOOK_SETTLED_EPSILON_DEGREES
                && Math.abs(player.getXRot() - look.pitch) <= LOOK_SETTLED_EPSILON_DEGREES;
    }

    private boolean shouldSendQueuedLook(PlayerLook look) {
        long tick = Reference.MINECRAFT.level == null ? Long.MIN_VALUE : Reference.MINECRAFT.level.getGameTime();
        if (tick == Long.MIN_VALUE || this.lastQueuedLookTick != tick) {
            return true;
        }
        return Math.abs(Mth.wrapDegrees(this.lastQueuedLookYaw - look.yaw)) > LOOK_SETTLED_EPSILON_DEGREES
                || Math.abs(this.lastQueuedLookPitch - look.pitch) > LOOK_SETTLED_EPSILON_DEGREES;
    }

    private void recordQueuedLook(PlayerLook look) {
        this.lastQueuedLookTick = Reference.MINECRAFT.level == null ? Long.MIN_VALUE : Reference.MINECRAFT.level.getGameTime();
        this.lastQueuedLookYaw = look.yaw;
        this.lastQueuedLookPitch = look.pitch;
    }

    public void clearQueue() {
        this.queuedClick = null;
        this.needWaitModifyLook = false;
        this.waitForHorizontalLook = true;
        this.actionRequiresWaitModifyLook = false;
        this.look = null;
        this.printerInteractionActive = false;
        this.easyPlaceProtocolActive = false;
        this.activeSource = ActionSource.GENERIC;
    }

    public void resetRuntime() {
        this.clearQueue();
        this.screenSessions.reset();
    }
}
