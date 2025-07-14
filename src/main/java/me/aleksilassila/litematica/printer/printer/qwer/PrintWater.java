package me.aleksilassila.litematica.printer.printer.qwer;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.getEnchantmentLevel;

public class PrintWater {
    @NotNull
    static MinecraftClient client = MinecraftClient.getInstance();
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
    }
    public static void searchPickaxes(@NotNull ClientPlayerEntity player){
        for (int i = 36; i < player.playerScreenHandler.slots.size()-2; i++) {
            ItemStack stack = player.playerScreenHandler.slots.get(i).getStack();
            if((stack.isOf(Items.DIAMOND_PICKAXE)||
                    stack.isOf(Items.NETHERITE_PICKAXE)) &&
                    !(getEnchantmentLevel(stack,Enchantments.SILK_TOUCH) > 0)){
                //#if MC > 12101
                player.getInventory().setSelectedSlot(i-36);
                //#else
                //$$ player.getInventory().selectedSlot = i-36;
                //#endif
                return;
            }
        }
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.of("快捷栏中没有可用镐子，碎冰速度较慢"),false);
    }
    public static boolean spawnWater(BlockPos pos){
        //冰碎后无法产生水
        //#if MC > 11904
        BlockState material = client.world.getBlockState(pos.down());
        //#else
        //$$ Material material = client.world.getBlockState(pos.down()).getMaterial();
        //#endif

        if (material.blocksMovement() || material.isLiquid()) {
            return true;
        }else {
            client.inGameHud.setOverlayMessage(Text.of("冰碎后无法产生水"), false);
            return false;
        }
    }
}
