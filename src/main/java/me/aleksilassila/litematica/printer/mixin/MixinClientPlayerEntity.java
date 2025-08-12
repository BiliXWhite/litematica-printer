package me.aleksilassila.litematica.printer.mixin;

import com.mojang.authlib.GameProfile;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.UpdateChecker;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
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
import java.util.concurrent.CompletableFuture;

//#if MC >= 12001
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//#else
//$$ import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;
//#endif

//#if MC < 11904
//$$ import net.minecraft.text.LiteralText;
//$$ import me.aleksilassila.litematica.printer.bilixwhite.utils.I18nUtils;
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

	@Inject(at = @At("HEAD"), method = "init")
	public void init(CallbackInfo ci) {
		if (!Printer.updateChecked)
			CompletableFuture.runAsync(this::checkForUpdates);
		Printer.updateChecked = true;
	}

	@Inject(at = @At("HEAD"), method = "closeHandledScreen")
	public void close(CallbackInfo ci) {
		//#if MC >= 12001
		if(Statistics.loadChestTracker) MemoryUtils.saveMemory(this.currentScreenHandler);
		OpenInventoryPacket.reSet();
		//#endif
	}

	@Inject(at = @At("HEAD"), method = "tick")
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
		BreakManager.instance().onTick();
		printer.tick();
	}

	@Unique
	public void checkForUpdates() {
		CompletableFuture.runAsync(() -> {
			String version = UpdateChecker.version;
			String newVersion = UpdateChecker.getPrinterVersion();

			if (newVersion == null)
				return;

			if (!version.equals(newVersion)) {
				client.execute(() -> {
					//#if MC > 11802
					client.inGameHud.getChatHud().addMessage(
							Text.translatable("litematica_printer.update.available", version, newVersion)
									.formatted(Formatting.YELLOW));
					client.inGameHud.getChatHud().addMessage(
							Text.translatable("litematica_printer.update.recommendation")
									.formatted(Formatting.RED));
					client.inGameHud.getChatHud().addMessage(
							Text.translatable("litematica_printer.update.repository")
									.formatted(Formatting.WHITE));
					client.inGameHud.getChatHud().addMessage(
							Text.literal("https://github.com/BiliXWhite/litematica-printer")
									.setStyle(Style.EMPTY
											//#if MC >= 12105
											//$$ .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/BiliXWhite/litematica-printer")))
											//#else
											.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/BiliXWhite/litematica-printer"))
											//#endif
											.withUnderline(true)
											.withColor(Formatting.BLUE)));
					client.inGameHud.getChatHud().addMessage(
							Text.translatable("litematica_printer.update.download")
									.setStyle(Style.EMPTY
											//#if MC >= 12105
											//$$ .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://xeno.lanzoue.com/b00l1v20vi")))
											//#else
											.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://xeno.lanzoue.com/b00l1v20vi"))
											//#endif
											.withBold(true)
											.withColor(Formatting.GREEN)));
					client.inGameHud.getChatHud().addMessage(
							Text.translatable("litematica_printer.update.password", "cgxw")
									.formatted(Formatting.WHITE));
					client.inGameHud.getChatHud().addMessage(
							Text.literal("------------------------").formatted(Formatting.GRAY));
					//#else
					//$$ client.inGameHud.getChatHud().addMessage(
					//$$         new LiteralText(String.format(I18nUtils.get("update.available"), version, newVersion))
					//$$                 .formatted(Formatting.YELLOW));
					//$$ client.inGameHud.getChatHud().addMessage(
					//$$         new LiteralText(I18nUtils.get("update.recommendation"))
					//$$                 .formatted(Formatting.RED));
					//$$ client.inGameHud.getChatHud().addMessage(
					//$$         new LiteralText(I18nUtils.get("update.repository"))
					//$$                 .formatted(Formatting.WHITE));
					//$$ client.inGameHud.getChatHud().addMessage(
					//$$         new LiteralText("https://github.com/BiliXWhite/litematica-printer")
					//$$                 .setStyle(Style.EMPTY
					//$$                         .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/BiliXWhite/litematica-printer"))
					//$$                         .withUnderline(true)
					//$$                         .withColor(Formatting.BLUE)));
					//$$ client.inGameHud.getChatHud().addMessage(
					//$$         new LiteralText(I18nUtils.get("update.download"))
					//$$                 .setStyle(Style.EMPTY
					//$$                         .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://xeno.lanzoue.com/b00l1v20vi"))
					//$$                         .withBold(true)
					//$$                         .withColor(Formatting.GREEN)));
					//$$ client.inGameHud.getChatHud().addMessage(
					//$$         new LiteralText(String.format(I18nUtils.get("update.password"), "cgxw")
					//$$                 .formatted(Formatting.WHITE)));
					//$$ client.inGameHud.getChatHud().addMessage(
					//$$         new LiteralText("------------------------").formatted(Formatting.GRAY));
					//#endif
				});
			}
		});
	}
}