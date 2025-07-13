package me.aleksilassila.litematica.printer.mixin;

import com.mojang.authlib.GameProfile;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.UpdateChecker;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.HashSet;

import static me.aleksilassila.litematica.printer.printer.Printer.updateChecked;

//#if MC >= 12001
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//#else
//$$ import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;
//#endif

//#if MC < 11904
//$$ import net.minecraft.text.LiteralText;
//#else
import net.minecraft.text.Text;
//#endif

//#if MC >= 12105
//$$ import java.net.URI;
//#endif
@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
	public MixinClientPlayerEntity(ClientWorld world, GameProfile profile
	//#if MC == 11902
	//$$ , @Nullable PlayerPublicKey publicKey) {super(world, profile, publicKey);
	//#else
	) {super(world, profile);
	//#endif
	}

    @Final
	@Shadow
	protected MinecraftClient client;

	@Inject(at = @At("HEAD"), method = "closeHandledScreen")
	public void close(CallbackInfo ci) {
		//#if MC >= 12001
		if(Statistics.loadChestTracker) MemoryUtils.saveMemory(this.currentScreenHandler);
		OpenInventoryPacket.reSet();
		//#endif
	}
	@Inject(at = @At("TAIL"), method = "tick")
	public void tick(CallbackInfo ci) {
		Printer printer = Printer.getPrinter();
		ZxyUtils.tick();
		printer.myTick();
		if (!(LitematicaMixinMod.PRINT_SWITCH.getBooleanValue() || LitematicaMixinMod.PRINT.getKeybind().isPressed())) {
			PlacementGuide.posMap = new HashMap<>();
			printer.basePos = null;
			Printer.fluidModeItemList = new HashSet<>();
			return;
		}
		if(!updateChecked && client.player != null && client.world != null) {
			checkForUpdates();
			updateChecked = true;
		}
		printer.tick();
	}

	@Unique
	public void checkForUpdates() {
		new Thread(() -> {
            String version = UpdateChecker.version;
            String newVersion = UpdateChecker.getPrinterVersion();

            if (!version.equals(newVersion))
				//#if MC > 11802
				client.inGameHud.getChatHud().addMessage(
						Text.literal("投影打印机检测到更新！当前版本: " + version + " 最新版本: " + newVersion + "\n强烈建议更新到最新版本，旧版本可能有一些恶性bug。\n" +
										"仓库地址：")
								.append(Text.literal("https://github.com/BiliXWhite/litematica-printer\n")
										.setStyle(Style.EMPTY
												//#if MC >= 12105
												//$$.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/BiliXWhite/litematica-printer")))
												//#else
												.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/BiliXWhite/litematica-printer"))
												//#endif
												.withUnderline(true)
												.withColor(Formatting.BLUE)))
								.append(Text.literal(">>点击此处获取最新版本<<\n")
										.setStyle(Style.EMPTY
												//#if MC >= 12105
												//$$.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://xeno.lanzoue.com/b00l1v20vi")))
												//#else
												.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://xeno.lanzoue.com/b00l1v20vi"))
												//#endif
												.withUnderline(true)
												.withColor(Formatting.GREEN)))
								.append("密码:cgxw"));
				//#else
				//$$client.inGameHud.getChatHud().addMessage(
				//$$		new LiteralText("投影打印机检测到更新！当前版本: " + version + " 最新版本: " + newVersion + "\n强烈建议更新到最新版本，旧版本可能有一些恶性bug。\n" +
				//$$				"仓库地址：")
				//$$				.append(new LiteralText("https://github.com/BiliXWhite/litematica-printer\n"))
				//$$				.setStyle(Style.EMPTY
				//$$						.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/BiliXWhite/litematica-printer"))
				//$$						.withUnderline(true)
				//$$						.withColor(Formatting.BLUE))
				//$$				.append(new LiteralText(">>点击此处获取最新版本<<\n"))
				//$$				.setStyle(Style.EMPTY
				//$$						.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://xeno.lanzoue.com/b00l1v20vi"))
				//$$						.withUnderline(true)
				//$$						.withColor(Formatting.GREEN))
				//$$				.append("密码:cgxw"));
			    //#endif
        }).start();
	}
}