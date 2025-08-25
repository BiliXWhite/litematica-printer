package me.aleksilassila.litematica.printer.mixin;

import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.bedrockUtils.Messager;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.BlockTask;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

//#if MC >= 12001
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//#endif

import static me.aleksilassila.litematica.printer.printer.Printer.isEnablePrinter;
@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    @Final
	@Shadow
	protected MinecraftClient client;

	@Inject(at = @At("HEAD"), method = "closeHandledScreen")
	public void close(CallbackInfo ci) {
		//#if MC >= 12001
 		if(Statistics.loadChestTracker) MemoryUtils.saveMemory(((ClientPlayerEntity)(Object)this).currentScreenHandler);
 		OpenInventoryPacket.reSet();
		//#endif
	}
	@Inject(at = @At("TAIL"), method = "tick")
	public void tick(CallbackInfo ci) {
		Printer printer = Printer.getPrinter();
		ZxyUtils.tick();
		printer.myTick();
		if(!(isEnablePrinter())){
			PlacementGuide.posMap = new HashMap<>();
			printer.basePos = null;
			printer.replaceTaskMap = new HashMap<>();
			return;
		}
		if(Printer.up){
			checkForUpdates();
			Printer.up = false;
		}
		printer.tick();
        BlockTask.BlockTaskManager.tick();
	}
	@Unique
	public void checkForUpdates() {
        new Thread(() -> {
            MutableText bv1 = Messager.createOpenUrlText("BV1q44y1T7hE", "https://www.bilibili.com/video/BV1q44y1T7hE");
            MutableText bv2 = Messager.createOpenUrlText("BV1Fv411P7Vc", "https://www.bilibili.com/video/BV1Fv411P7Vc");
            MutableText source = Messager.createOpenUrlText("Github", "https://github.com/aleksilassila/litematica-printer");
            client.inGameHud.getChatHud().addMessage(
                    Text.of("").copy().append("[Litematica-Printer]\n此版本为宅闲鱼二改版，初版视频：")
                            .append(bv1)
                            .append("\n投影打印机原作😁：")
                            .append(source)
                            .append("\n破基岩视频：")
                            .append(bv2));
        }).start();
    }
}