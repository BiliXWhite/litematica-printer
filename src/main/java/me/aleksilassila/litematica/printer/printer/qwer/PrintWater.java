package me.aleksilassila.litematica.printer.printer.qwer;

import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.bedrockUtils.Messager;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.getEnchantmentLevel;

public class PrintWater {

    // 判断方块是否含水
    public static boolean canWaterLogged(BlockState blockState) {
        try {
            if (blockState.isOf(Blocks.WATER)) {
                return blockState.get(FluidBlock.LEVEL) == 0;
            }else {
                return blockState.get(Properties.WATERLOGGED);
            }
        } catch (Throwable e) {
            // 这样写应该没问题吧
            return false;
        }
    }// 潜行右键单击
    private static void rightClickBlock(@NotNull ClientPlayerEntity player, BlockPos pos,Printer printer) {
        Printer.getPrinter().queue.setShift(player, true);
        //#if MC > 11802
        printer.client.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN, pos, true));
        //#else
        //$$ printer.client.interactionManager.interactBlock(player, player.clientWorld, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN, pos, true));
        //#endif
        Printer.getPrinter().queue.setShift(player, true);
    }
    public static void searchPickaxes(@NotNull ClientPlayerEntity player){
        for (int i = 36; i < player.playerScreenHandler.slots.size()-2; i++) {
            ItemStack stack = player.playerScreenHandler.slots.get(i).getStack();
            if((stack.isOf(Items.DIAMOND_PICKAXE)||
                    stack.isOf(Items.NETHERITE_PICKAXE)) &&
                    !(getEnchantmentLevel(stack,Enchantments.SILK_TOUCH) > 0)){
                InventoryUtils.setSelectedSlot(i-36);
                return;
            }
        }
        Messager.actionBar("快捷栏中没有可用镐子，碎冰速度较慢");
    }
    public static boolean spawnWater(BlockPos pos){
        MinecraftClient client = ZxyUtils.client;
        //冰碎后无法产生水
        //#if MC > 11904
        BlockState material = client.world.getBlockState(pos.down());
        //#else
        //$$ Material material = client.world.getBlockState(pos.down()).getMaterial();
        //#endif

        if (material.blocksMovement() || material.isLiquid()) {
            return true;
        }else {
            Messager.actionBar("冰碎后无法产生水");
            return false;
        }
    }
}
