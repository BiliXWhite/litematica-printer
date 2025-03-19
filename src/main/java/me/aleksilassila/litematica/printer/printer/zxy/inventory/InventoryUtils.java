package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.client;
import static net.minecraft.block.ShulkerBoxBlock.FACING;

public class InventoryUtils {
    public static boolean isInventory(World world, BlockPos pos){
        return fi.dy.masa.malilib.util.InventoryUtils.getInventory(world,pos) != null;
    }
    public static boolean canOpenInv(BlockPos pos){
        if (client.world != null) {
            BlockState blockState = client.world.getBlockState(pos);
            BlockEntity blockEntity = client.world.getBlockEntity(pos);
            boolean isInventory = InventoryUtils.isInventory(client.world,pos);
            try {
                if ((isInventory && blockState.createScreenHandlerFactory(client.world,pos) == null) ||
                        (blockEntity instanceof ShulkerBoxBlockEntity entity &&
                                //#if MC > 12101
                                //$$ !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(1.0F, blockState.get(FACING), 0.0F, 0.5F, pos.toBottomCenterPos()).offset(pos).contract(1.0E-6)) &&
                                //#elseif MC <= 12101 && MC > 12004
                                !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(1.0F, blockState.get(FACING), 0.0F, 0.5F).offset(pos).contract(1.0E-6)) &&
                                //#elseif MC <= 12004
                                //$$ !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(blockState.get(FACING), 0.0f, 0.5f).offset(pos).contract(1.0E-6)) &&
                                //#endif
                                entity.getAnimationStage() == ShulkerBoxBlockEntity.AnimationStage.CLOSED)) {
                    return false;
                }else if(!isInventory){
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        }else {
            return false;
        }
    }
}
