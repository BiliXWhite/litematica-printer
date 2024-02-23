package me.aleksilassila.litematica.printer.printer.qwer;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.block.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class PrintWater {
    /**
     * 打印原理图中的含水方块，在放置原理图方块前先放置一块冰并打破来使方块含水
     *
     * @param requiredState 原理图中需要放置的方块
     * @param pEntity       要放置方块的客户端玩家实体
     * @param lookDir       干啥用的，不知道啊
     * @param pos           要在世界放置方块的位置
     * @return 是否需要取消放置
     */

    static BlockPos waJuePos = null;
    static int startTick = -1;
    public static void printWaterLoggedBlock(BlockState requiredState, ClientPlayerEntity pEntity, Direction lookDir, BlockPos pos,int tick) {
        Printer printer = Printer.getPrinter();
        if(printer == null || printer.client.world == null)return;

        if(startTick != -1 && startTick == tick){
            return;
        }else if(waJuePos != null){

            if (Printer.waJue(waJuePos)) {
                waJuePos = null;
            }
            return;
        }
        // 放置含水方块
        if (LitematicaMixinMod.PRINT_WATER_LOGGED_BLOCK.getBooleanValue()
                && canWaterLogged(requiredState)) {
            // 如果要放置方块的位置已经有水了，并且是水源，直接取消放置冰
            BlockState blockState = pEntity.getWorld().getBlockState(pos);
            if (blockState.isOf(Blocks.WATER) && blockState.get(FluidBlock.LEVEL) == 0) {
                return;
            }
            //冰碎后无法产生水
            //#if MC > 11904
            BlockState material = printer.client.world.getBlockState(pos.down());
            //#else
            //$$ Material material = printer.client.world.getBlockState(pos.down()).getMaterial();
            //#endif
            if (!(material.blocksMovement() || material.isLiquid())) {
                pEntity.sendMessage(Text.of("冰碎后无法产生水"), true);
                return;
            }

            // 如果玩家处于创造模式，取消放置
            if (pEntity.isCreative()) {
                pEntity.sendMessage(Text.of("需要处于生存模式"), true);
                return;
            }
            // 从背包中拿取冰
            printer.switchToItems(pEntity, new Item[]{Items.ICE});
            // 如果背包中没有冰，取消放置
            if (!pEntity.getMainHandStack().isOf(Items.ICE)) {
                pEntity.sendMessage(Text.of("你物品栏里可能没有冰"), true);
                return;
            }
            // 看向冰方块
            printer.sendLook(pEntity, lookDir);
            // 如果要放置方块的位置已经是冰了，不再放置冰
            if (!pEntity.getWorld().getBlockState(pos).isOf(Blocks.ICE)) {
                // 右键点击
                rightClickBlock(pEntity, pos,printer);
            }
            // 从物品栏拿取镐子
            printer.switchToItems(pEntity, new Item[]{Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE});
            // 获取玩家主手上的物品
            ItemStack mainHandStack = pEntity.getMainHandStack();
            // 检查镐子是否不带精准采集
            if ((mainHandStack.isOf(Items.DIAMOND_PICKAXE) || mainHandStack.isOf(Items.NETHERITE_PICKAXE))
                    // 镐子不能带有精准采集附魔
                    && !(EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, mainHandStack) > 0)) {
                // 破坏冰
                // 如果冰没有成功破坏，waJue()返回true，取消放置
                if (!Printer.waJue(pos)) {
                    waJuePos = pos;
                }
            }
        }
    }

    // 判断方块是否含水
    private static boolean canWaterLogged(BlockState requiredState) {
        try {
            return (requiredState.isOf(Blocks.WATER) && requiredState.get(FluidBlock.LEVEL) == 0) || requiredState.get(BooleanProperty.of("waterlogged"));
        } catch (Throwable e) {
            // 这样写应该没问题吧
            return false;
        }
    }// 潜行右键单击
    private static void rightClickBlock(@NotNull ClientPlayerEntity player, BlockPos pos,Printer printer) {
        player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        //#if MC > 11802
        printer.client.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(pos.toCenterPos(), Direction.DOWN, pos, true));
        //#else
        //$$ printer.client.interactionManager.interactBlock(player, player.clientWorld, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN, pos, true));
        //#endif
        player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
    }
    private static ItemStack searchPickaxes(@NotNull ClientPlayerEntity player){
        AtomicReference<ItemStack> itemStack = new AtomicReference<>();
        for (int i = 9; i < player.playerScreenHandler.slots.size(); i++) {
            ItemStack stack = player.playerScreenHandler.slots.get(i).getStack();
            if((stack.isOf(Items.DIAMOND_PICKAXE)||
                    stack.isOf(Items.NETHERITE_PICKAXE)) &&
                    !(EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH,stack) > 0)
            ) itemStack.set(stack);
        }
        return itemStack.get();
    }
}
