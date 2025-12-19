package me.aleksilassila.litematica.printer.implementation.mixin;

import com.mojang.authlib.GameProfile;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.*;
import me.aleksilassila.litematica.printer.printer.UpdateChecker;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import me.aleksilassila.litematica.printer.utils.StringUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;
import java.util.Optional;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer extends AbstractClientPlayer {
    @Unique
    private static boolean didCheckForUpdates = false;

    @Final
    @Shadow
    protected Minecraft minecraft;

    @Final
    @Shadow
    public ClientPacketListener connection;

    public MixinLocalPlayer(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(at = @At("TAIL"), method = "tick")
    public void tick(CallbackInfo ci) {
        LocalPlayer clientPlayer = (LocalPlayer) (Object) this;

        if (!didCheckForUpdates) {
            didCheckForUpdates = true;
            checkForUpdates();
        }

        if (LitematicaPrinterMod.printer == null || LitematicaPrinterMod.printer.player != clientPlayer) {
            Debug.write("Initializing printer, player: {}, client: {}", clientPlayer, minecraft);
            LitematicaPrinterMod.printer = new Printer2(minecraft, clientPlayer);
        }

        LitematicaPrinterMod.printer.onGameTick();
    }

    @Unique
    public void checkForUpdates() {
        new Thread(() -> {
            String version = UpdateChecker.version;
            String newVersion = UpdateChecker.getPrinterVersion();

            Debug.write("Current version: [{}], detected version [{}]", version, newVersion);

            if (!version.equals(newVersion)) {
                minecraft.execute(() -> {
                    MessageUtils.addMessage(I18n.UPDATE_AVAILABLE.getKeyComponent(version, newVersion)
                            .withStyle(ChatFormatting.YELLOW));
                    MessageUtils.addMessage(I18n.UPDATE_RECOMMENDATION.getKeyComponent()
                            .withStyle(ChatFormatting.RED));
                    MessageUtils.addMessage(I18n.UPDATE_REPOSITORY.getKeyComponent()
                            .withStyle(ChatFormatting.WHITE));
                    MessageUtils.addMessage(StringUtils.literal("https://github.com/BiliXWhite/litematica-printer")
                            .setStyle(Style.EMPTY
                                    //#if MC >= 12105
                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/BiliXWhite/litematica-printer")))
                                    //#else
                                    //$$ .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/BiliXWhite/litematica-printer"))
                                    //#endif
                                    .withUnderlined(true)
                                    .withColor(ChatFormatting.BLUE)));
                    MessageUtils.addMessage(I18n.UPDATE_DOWNLOAD.getKeyComponent()
                            .setStyle(Style.EMPTY
                                    //#if MC >= 12105
                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://xeno.lanzoue.com/b00l1v20vi")))
                                    //#else
                                    //$$ .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://xeno.lanzoue.com/b00l1v20vi"))
                                    //#endif
                                    .withBold(true)
                                    .withColor(ChatFormatting.GREEN)));
                    MessageUtils.addMessage(I18n.UPDATE_PASSWORD.getKeyComponent("cgxw")
                            .withStyle(ChatFormatting.WHITE));
                    MessageUtils.addMessage(
                            StringUtils.literal("------------------------").withStyle(ChatFormatting.GRAY));
                });
            }
        }).start();
    }

//    @Inject(method = "openTextEdit", at = @At("HEAD"), cancellable = true)
//    public void openEditSignScreen(SignBlockEntity sign, boolean front, CallbackInfo ci) {
//        getTargetSignEntity(sign).ifPresent(signBlockEntity ->
//        {
//            ServerboundSignUpdatePacket packet = new ServerboundSignUpdatePacket(sign.getBlockPos(),
//                    front,
//                    signBlockEntity.getText(front).getMessage(0, false).getString(),
//                    signBlockEntity.getText(front).getMessage(1, false).getString(),
//                    signBlockEntity.getText(front).getMessage(2, false).getString(),
//                    signBlockEntity.getText(front).getMessage(3, false).getString());
//            this.connection.send(packet);
//            ci.cancel();
//        });
//    }


    @Inject(method = "openTextEdit", at = @At("HEAD"), cancellable = true)
    public void openEditSignScreen(SignBlockEntity sign, boolean front, CallbackInfo ci) {
        getTargetSignEntity(sign).ifPresent(signBlockEntity ->
        {
            // 定义4行文本字符串
            String line1, line2, line3, line4;

            //#if MC > 11904
            net.minecraft.world.level.block.entity.SignText signText = signBlockEntity.getText(front);
            line1 = signText.getMessage(0, false).getString();
            line2 = signText.getMessage(1, false).getString();
            line3 = signText.getMessage(2, false).getString();
            line4 = signText.getMessage(3, false).getString();
            //#else
            //$$ line1 = signBlockEntity.getMessage(0, false).getString();
            //$$ line2 = signBlockEntity.getMessage(1, false).getString();
            //$$ line3 = signBlockEntity.getMessage(2, false).getString();
            //$$ line4 = signBlockEntity.getMessage(3, false).getString();
            //#endif

            ServerboundSignUpdatePacket packet;

            packet = new ServerboundSignUpdatePacket(
                    sign.getBlockPos(),

                    //#if MC > 11904
                    front,
                    //#endif

                    line1,
                    line2,
                    line3,
                    line4
            );

            // 发送数据包并取消原方法执行
            this.connection.send(packet);
            ci.cancel();
        });
    }
    @Unique
    private Optional<SignBlockEntity> getTargetSignEntity(SignBlockEntity sign) {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        if (sign.getLevel() == null || worldSchematic == null) {
            return Optional.empty();
        }
        SchematicBlockState state = new SchematicBlockState(sign.getLevel(), worldSchematic, sign.getBlockPos());
        BlockEntity targetBlockEntity = worldSchematic.getBlockEntity(state.blockPos);
        if (targetBlockEntity instanceof SignBlockEntity targetSignEntity) {
            return Optional.of(targetSignEntity);
        }
        return Optional.empty();
    }
}
