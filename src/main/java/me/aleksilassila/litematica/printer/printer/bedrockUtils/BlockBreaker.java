package me.aleksilassila.litematica.printer.printer.bedrockUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
//import net.minecraft.block.RedstoneTorchBlock;
//import net.minecraft.util.math.Direction;

//import java.util.ArrayList;

//import static net.minecraft.block.Block.sideCoversSmallSquare;

public class BlockBreaker {
    public static void breakBlock(ClientWorld world, BlockPos pos) {
        InventoryManager.switchToItem(Items.DIAMOND_PICKAXE);
        MinecraftClient.getInstance().interactionManager.attackBlock(pos, Direction.DOWN);
//        upBreakBlock(world,pos);
    }
    public static void upBreakBlock(ClientWorld world, BlockPos pos) {
        MinecraftClient.getInstance().interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
    }
}