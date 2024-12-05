package me.aleksilassila.litematica.printer.printer.bedrockUtils;

import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

import static me.aleksilassila.litematica.printer.printer.Printer.bedrockModeRange;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;
//import java.util.List;

public class BreakingFlowController {
    public static ArrayList<TargetBlock> cachedTargetBlockList = new ArrayList<>();

    public static boolean isWorking() {
        return working;
    }

    private static boolean working = true;


    static {

    }

    //加入破坏列表
    public static void addBlockPosToList(BlockPos pos) {

        if (cachedTargetBlockList.size() > 5) return;

        ClientWorld world = MinecraftClient.getInstance().world;
//        if (world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();

        String haveEnoughItems = InventoryManager.warningMessage();
        if (haveEnoughItems != null) {
            Messager.actionBar(haveEnoughItems);
            return;
        }
        for (TargetBlock block : cachedTargetBlockList) {
            if (pos.equals(block.getBlockPos()) || pos.equals(block.getnyk()) || pos.equals(block.geths()))
                return;
        }
        if (!minecraftClient.world.getBlockState(pos.up()).isAir() || !minecraftClient.world.getBlockState(pos.up().up()).isAir()) {
            if (!Printer.bedrockModeTarget(minecraftClient.world.getBlockState(pos.up()).getBlock()))
                addPosList(pos.up());
            if (!Printer.bedrockModeTarget(minecraftClient.world.getBlockState(pos.up().up()).getBlock()))
                addPosList(pos.up().up());
            return;
        }

        cachedTargetBlockList.add(new TargetBlock(pos, world));
        //Suggest also an english version, for debug reasons.
//                System.out.println("新任务");
//            }
    }

    public static ArrayList<BlockPos> poslist = new ArrayList<>();

    public static void addPosList(BlockPos pos) {
        if (poslist.stream().noneMatch(pos1 -> pos1.equals(pos))) poslist.add(pos);
    }

    static void deleteBlock() {
        for (int i = 0; i < poslist.size(); i++) {
            BlockPos blockPos = poslist.get(i);

            if (MinecraftClient.getInstance().world.getBlockState(blockPos).isAir() && ZxyUtils.bedrockCanInteracted(blockPos, bedrockModeRange())) {
                InventoryManager.switchToItem(Items.DIAMOND_PICKAXE);
                //#if MC < 11904
                //$$ client.interactionManager.interactBlock(client.player,client.world, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(blockPos), Direction.UP, poslist.get(i), false));
                //#else
                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(blockPos), Direction.UP, poslist.get(i), false));
                //#endif
                if (MinecraftClient.getInstance().world.getBlockState(blockPos).isAir()) {
                    poslist.remove(i);
                    i--;
                    continue;
                }
            }
            if (!ZxyUtils.bedrockCanInteracted(blockPos, bedrockModeRange() * 2)) {
                poslist.remove(i);
                i--;
                continue;
            }


            Printer printer = Printer.getPrinter();
            if (printer == null) return;
            if (!ZxyUtils.bedrockCanInteracted(blockPos, bedrockModeRange())) continue;
            if (!MinecraftClient.getInstance().world.getBlockState(blockPos).isAir()) {
//                BlockBreaker.breakBlock(MinecraftClient.getInstance().world, poslist.get(i));
                InventoryManager.switchToItem(Items.DIAMOND_PICKAXE);
                Printer.waJue(blockPos);
            }


        }
    }

    public static void tick() {
        deleteBlock();
        //检测是否符合条件 物品是否带起 游戏模式 是否有信标
        if (InventoryManager.warningMessage() != null) {
            return;
        }

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        for (int i = 0; i < cachedTargetBlockList.size(); i++) {
            TargetBlock selectedBlock = cachedTargetBlockList.get(i);

//            if (!blockInPlayerRange(selectedBlock.getBlockPos(), player, 5f)) {
            if (!ZxyUtils.bedrockCanInteracted(selectedBlock.getBlockPos(), getRage() - 1.5)) {
                cachedTargetBlockList.remove(i);
                continue;
            }
            //玩家切换世界，或离目标方块太远时，删除所有缓存的任务
            if (selectedBlock.getWorld() != MinecraftClient.getInstance().world) {
                cachedTargetBlockList = new ArrayList<TargetBlock>();
                break;
            }

//            if (blockInPlayerRange(selectedBlock.getBlockPos(), player, 5f)) {
//            if (DataManager.getRenderLayerRange().isPositionWithinRange(selectedBlock.getBlockPos())) {
            //#if MC > 12006
            ItemStack mainHandStack = client.player.getMainHandStack();
            cachedTargetBlockList.stream().filter( targetBlock -> targetBlock.getStatus() == TargetBlock.Status.EXTENDED).forEach(TargetBlock::tick);
            //#endif
            TargetBlock.Status status = cachedTargetBlockList.get(i).tick();
            if (status == TargetBlock.Status.RETRACTING) {
                continue;
            } else if (status == TargetBlock.Status.FAILED || status == TargetBlock.Status.RETRACTED) {
                for (BlockPos temppo : cachedTargetBlockList.get(i).temppos) {
                    if (!minecraftClient.world.getBlockState(temppo).isAir()) addPosList(temppo);
                }
                cachedTargetBlockList.remove(i);
            }/* else {
                    break;
                }*/

//            }
        }
        //#if MC > 12006
        if (cachedTargetBlockList.stream().anyMatch(targetBlock -> targetBlock.getStatus() == TargetBlock.Status.EXTENDED)) {
            InventoryManager.switchToItem(Items.DIAMOND_PICKAXE);
            TargetBlock.switchPickaxe = true;
        }
        //#endif
    }

    private static boolean blockInPlayerRange(BlockPos blockPos, PlayerEntity player, float range) {
        return blockPos.isWithinDistance(player.getPos(), range);
    }

    public static WorkingMode getWorkingMode() {
        return WorkingMode.VANILLA;
    }

    private static boolean shouldAddNewTargetBlock(BlockPos pos) {
        for (int i = 0; i < cachedTargetBlockList.size(); i++) {
            if (cachedTargetBlockList.get(i).getBlockPos().getManhattanDistance(pos) == 0) {
                return false;
            }
        }
        return true;
    }

    public static void switchOnOff() {
    }


    //测试用的。使用原版模式已经足以满足大多数需求。
    //just for test. The VANILLA mode is powerful enough.
    enum WorkingMode {
        CARPET_EXTRA,
        VANILLA,
        MANUALLY;
    }
}
