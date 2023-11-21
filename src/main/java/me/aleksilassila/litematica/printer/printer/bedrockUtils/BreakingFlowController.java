package me.aleksilassila.litematica.printer.printer.bedrockUtils;

import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
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
        if (world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
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
                if (!minecraftClient.world.getBlockState(pos.up()).isOf(Blocks.AIR) || !minecraftClient.world.getBlockState(pos.up().up()).isOf(Blocks.AIR)) {
                    if (!minecraftClient.world.getBlockState(pos.up()).isOf(Blocks.BEDROCK)) poslist.add(pos.up());
                    if (!minecraftClient.world.getBlockState(pos.up().up()).isOf(Blocks.BEDROCK)) poslist.add(pos.up().up());
                    return;
                }

                cachedTargetBlockList.add(new TargetBlock(pos, world));
                //Suggest also an english version, for debug reasons.
//                System.out.println("新任务");
            }
    }

    public static ArrayList<BlockPos> poslist = new ArrayList<>();

    static void deleteBlock() {

        for (int i = 0; i < poslist.size(); i++) {
            if(MinecraftClient.getInstance().world.getBlockState(poslist.get(i)).isAir()){
                poslist.remove(i);
                continue;
            }
            if (!blockInPlayerRange(poslist.get(i).down(), MinecraftClient.getInstance().player, 5f)) continue;
//            if (Printer.getPrinter().wanJiaFanWeiNei(4, poslist.get(i))) continue;
            if (poslist.get(i) != null && !MinecraftClient.getInstance().world.getBlockState(poslist.get(i)).isAir()){
//                BlockBreaker.breakBlock(MinecraftClient.getInstance().world, poslist.get(i));
                InventoryManager.switchToItem(Items.DIAMOND_PICKAXE);
                Printer.waJue(poslist.get(i));
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
        PlayerEntity player = minecraftClient.player;
        for (int i = 0; i < cachedTargetBlockList.size(); i++) {
            TargetBlock selectedBlock = cachedTargetBlockList.get(i);

//            if (!DataManager.getRenderLayerRange().isPositionWithinRange(selectedBlock.getBlockPos())) {
            if (!blockInPlayerRange(selectedBlock.getBlockPos(), player, 5f)) {
//            if (Printer.getPrinter().wanJiaFanWeiNei(4,selectedBlock.getBlockPos())) {
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
            TargetBlock.Status status = cachedTargetBlockList.get(i).tick();
            if (status == TargetBlock.Status.RETRACTING) {
                continue;
            } else if (status == TargetBlock.Status.FAILED || status == TargetBlock.Status.RETRACTED) {
                for (BlockPos temppo : cachedTargetBlockList.get(i).temppos) {
                    if(!minecraftClient.world.getBlockState(temppo).isAir()) poslist.add(temppo);
                }
                cachedTargetBlockList.remove(i);
            }/* else {
                    break;
                }*/

//            }
        }
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
