package me.aleksilassila.litematica.printer.mixin;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.interfaces.IClientPlayerInteractionManager;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#if MC < 11904
//$$ import net.minecraft.world.World;
//$$ import net.minecraft.client.world.ClientWorld;

//#endif

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager implements IClientPlayerInteractionManager {
	@Shadow
	private MinecraftClient client;

    @Override
	public void rightClickBlock(BlockPos pos, Direction side, Vec3d hitVec)
	{
		interactBlock(client.player,
				//#if MC < 11904
//$$ 				client.world,
				//#endif
				Hand.MAIN_HAND,
			new BlockHitResult(hitVec, side, pos, false));
		interactItem(client.player,
				//#if MC < 11904
//$$ 				client.world,
				//#endif
				Hand.MAIN_HAND);
//		System.out.println("Printer interactBlock: pos: (" + pos.toShortString() + "), side: " + side.getName() + ", vector: " + hitVec.toString());
	}

	@Shadow
	public abstract ActionResult interactBlock(
            ClientPlayerEntity clientPlayerEntity_1,
			//#if MC < 11904
			//$$ClientWorld world,
			//#endif
            Hand hand_1, BlockHitResult blockHitResult_1);

	@Shadow
	public abstract ActionResult interactItem(PlayerEntity playerEntity_1,
											  //#if MC < 11904
//$$ 											   World world,
											  //#endif
                                               Hand hand_1);
}
