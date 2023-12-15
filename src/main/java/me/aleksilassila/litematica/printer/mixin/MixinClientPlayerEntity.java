package me.aleksilassila.litematica.printer.mixin;

import com.mojang.authlib.GameProfile;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.UpdateChecker;
import me.aleksilassila.litematica.printer.printer.zxy.ZxyUtils;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.printer.zxy.OpenInventoryPacket.*;
import static me.aleksilassila.litematica.printer.printer.zxy.ZxyUtils.*;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
	}

    @Final
	@Shadow
	protected MinecraftClient client;
//	@Inject(at = @At("HEAD"), method = "collideWithEntity")



	@Inject(at = @At("HEAD"), method = "closeScreen")
	public void closeScreen(CallbackInfo ci) {
		ChestBlock c;
//		if(LitematicaMixinMod.INVENTORY.getBooleanValue() && (LitematicaMixinMod.PRINT_MODE.getBooleanValue() || LitematicaMixinMod.PRINT.getKeybind().isPressed() || adding) && !qw){
//			if(!client.player.currentScreenHandler.equals(client.player.playerScreenHandler)){
//				handleItemsFromScreen(client.player.currentScreenHandler);
//			}
//		}
	}
	@Inject(at = @At("TAIL"), method = "closeScreen")
	public void close(CallbackInfo ci) {
		openIng = false;
	}
	@Inject(at = @At("TAIL"), method = "tick")
	public void tick(CallbackInfo ci) {
//		if (!didCheckForUpdates) {
//			didCheckForUpdates = true;
//
//			checkForUpdates();
//		}

		if (Printer.getPrinter() == null) {
			Printer.init(client);
			return;
		}
		ZxyUtils.addInv();
		if(num==1 || num == 3)ZxyUtils.syncInv();
		ZxyUtils.tick();

		if(!(LitematicaMixinMod.PRINT_MODE.getBooleanValue() || LitematicaMixinMod.PRINT.getKeybind().isPressed())) return;
		if(Printer.up){
			checkForUpdates();
			Printer.up = false;
		}
		Printer.getPrinter().tick();
	}
	public void checkForUpdates() {
        new Thread(() -> {
            String version = UpdateChecker.version;
            String newVersion = UpdateChecker.getPrinterVersion();

            if (!version.equals(newVersion)) {
                client.inGameHud.getChatHud().addMessage(
						Text.literal("Printer: 此版本为宅闲鱼二改最初版BV号：BV1q44y1T7hE\n" +
								"投影打印机原作 https://github.com/aleksilassila/litematica-printer/releases\n" +
								"破基岩作者视频BV号: BV1q44y1T7hE"));
            }
        }).start();
	}
}