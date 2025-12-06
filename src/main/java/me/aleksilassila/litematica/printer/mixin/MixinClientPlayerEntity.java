package me.aleksilassila.litematica.printer.mixin;

import com.mojang.authlib.GameProfile;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.UpdateChecker;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

//#if MC >= 12001 && MC <= 12104
//$$ import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
//#else
//$$ import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;
//#endif
//#if MC >= 12105
import java.net.URI;
//#endif

//#if MC > 11802
import net.minecraft.network.chat.Component;
//#else
//$$ import me.aleksilassila.litematica.printer.bilixwhite.utils.StringUtils;
//$$ import net.minecraft.network.chat.TextComponent;
//#endif

@Mixin(LocalPlayer.class)
public class MixinClientPlayerEntity extends AbstractClientPlayer {
    public MixinClientPlayerEntity(ClientLevel world, GameProfile profile
                                    //#if MC == 11902
                                    //$$ , @Nullable PlayerPublicKey publicKey) {super(world, profile, publicKey);
                                    //#else
    ) {super(world, profile);
                                    //#endif
    }
    
    @Final
    @Shadow
    protected Minecraft minecraft;
    
    @Inject(at = @At("HEAD"), method = "resetPos")
    public void init(CallbackInfo ci) {
        if (InitHandler.UPDATE_CHECK.getBooleanValue() && !Printer.updateChecked)
            CompletableFuture.runAsync(this::checkForUpdates);
        Printer.updateChecked = true;
    }
    
    @Inject(at = @At("HEAD"), method = "closeContainer")
    public void close(CallbackInfo ci) {
        //#if MC >= 12001 && MC <= 12104
        //$$ if(Statistics.loadChestTracker) MemoryUtils.saveMemory(this.containerMenu);
        //$$ OpenInventoryPacket.reSet();
        //#endif
    }
    
    @Inject(at = @At("TAIL"), method = "tick")
    public void tick(CallbackInfo ci) {
        Printer printer = Printer.getPrinter();
        ZxyUtils.tick();
        printer.tick();
        if (!(InitHandler.PRINT_SWITCH.getBooleanValue() || InitHandler.PRINT.getKeybind().isPressed())) {
            return;
        }
        BreakManager.instance().onTick();
        printer.printerTick();
    }

    @Unique
    public void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            String version = UpdateChecker.version;
            String newVersion = UpdateChecker.getPrinterVersion();

            if (newVersion == null)
                return;

            if (!version.equals(newVersion)) {
                minecraft.execute(() -> {
                    //#if MC > 11802
                    minecraft.gui.getChat().addMessage(
                            Component.translatable("litematica_printer.update.available", version, newVersion)
                                    .withStyle(ChatFormatting.YELLOW));
                    minecraft.gui.getChat().addMessage(
                            Component.translatable("litematica_printer.update.recommendation")
                                    .withStyle(ChatFormatting.RED));
                    minecraft.gui.getChat().addMessage(
                            Component.translatable("litematica_printer.update.repository")
                                    .withStyle(ChatFormatting.WHITE));
                    minecraft.gui.getChat().addMessage(
                            Component.literal("https://github.com/BiliXWhite/litematica-printer")
                                    .setStyle(Style.EMPTY
                                            //#if MC >= 12105
                                            .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/BiliXWhite/litematica-printer")))
                                            //#else
                                            //$$ .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/BiliXWhite/litematica-printer"))
                                            //#endif
                                            .withUnderlined(true)
                                            .withColor(ChatFormatting.BLUE)));
                    minecraft.gui.getChat().addMessage(
                            Component.translatable("litematica_printer.update.download")
                                    .setStyle(Style.EMPTY
                                            //#if MC >= 12105
                                            .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://xeno.lanzoue.com/b00l1v20vi")))
                                            //#else
                                            //$$ .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://xeno.lanzoue.com/b00l1v20vi"))
                                            //#endif
                                            .withBold(true)
                                            .withColor(ChatFormatting.GREEN)));
                    minecraft.gui.getChat().addMessage(
                            Component.translatable("litematica_printer.update.password", "cgxw")
                                    .withStyle(ChatFormatting.WHITE));
                    minecraft.gui.getChat().addMessage(
                            Component.literal("------------------------").withStyle(ChatFormatting.GRAY));
                    //#else
                    //$$ minecraft.gui.getChat().addMessage(
                    //$$         new TextComponent(String.format(StringUtils.get("update.available").toString(), version, newVersion))
                    //$$                 .setStyle(Style.EMPTY.applyFormats(ChatFormatting.YELLOW)));
                    //$$ minecraft.gui.getChat().addMessage(
                    //$$         new TextComponent(StringUtils.get("update.recommendation").toString())
                    //$$                 .setStyle(Style.EMPTY.applyFormats(ChatFormatting.RED)));
                    //$$ minecraft.gui.getChat().addMessage(
                    //$$         new TextComponent(StringUtils.get("update.repository").toString())
                    //$$                 .setStyle(Style.EMPTY.applyFormats(ChatFormatting.WHITE)));
                    //$$ minecraft.gui.getChat().addMessage(
                    //$$         new TextComponent("https://github.com/BiliXWhite/litematica-printer")
                    //$$                 .setStyle(Style.EMPTY
                    //$$                         .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/BiliXWhite/litematica-printer"))
                    //$$                         .withUnderlined(true)));
                    //$$ minecraft.gui.getChat().addMessage(
                    //$$         new TextComponent(StringUtils.get("update.download").toString())
                    //$$                 .setStyle(Style.EMPTY
                    //$$                         .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://xeno.lanzoue.com/b00l1v20vi"))
                    //$$                         .withBold(true)
                    //$$                         .withColor(ChatFormatting.GREEN)));
                    //$$ minecraft.gui.getChat().addMessage(
                    //$$         new TextComponent(String.format(StringUtils.get("update.password").toString(), "cgxw")
                    //$$                 .formatted(ChatFormatting.WHITE)));
                    //$$ minecraft.gui.getChat().addMessage(
                    //$$         new TextComponent("------------------------").withStyle(ChatFormatting.GRAY));
                    //#endif
                });
            }
        });
    }
}