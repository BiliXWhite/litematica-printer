package me.aleksilassila.litematica.printer.printer.bedrockUtils;

import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

import static me.aleksilassila.litematica.printer.printer.bedrockUtils.BreakingFlowController.addPosList;
import static me.aleksilassila.litematica.printer.printer.bedrockUtils.BreakingFlowController.cachedTargetBlockList;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.client;

public class TargetBlock {
    public static boolean switchPickaxe = false;
    private BlockPos blockPos;
    private BlockPos redstoneTorchBlockPos;
    private BlockPos pistonBlockPos;
    private ClientWorld world;
    private Status status;
    private BlockPos slimeBlockPos;
    private int tickTimes;
    private boolean hasTried;
    private int stuckTicksCounter;
    public boolean pistonIsBreak = false;
    public ArrayList<BlockPos> temppos = new ArrayList<>();

    public TargetBlock(BlockPos pos, ClientWorld world) {
        this.hasTried = false;
        this.stuckTicksCounter = 0;
        this.status = Status.UNINITIALIZED;
        this.blockPos = pos;
        this.world = world;
        this.pistonBlockPos = pos.up();
        this.redstoneTorchBlockPos = CheckingEnvironment.findNearbyFlatBlockToPlaceRedstoneTorch(this.world, this.blockPos);
        if (redstoneTorchBlockPos == null) {
            this.slimeBlockPos = CheckingEnvironment.findPossibleSlimeBlockPos(world, pos);
            if (slimeBlockPos != null) {
                BlockPlacer.simpleBlockPlacement(this,slimeBlockPos, Blocks.SLIME_BLOCK);
                redstoneTorchBlockPos = slimeBlockPos.up();
            } else {
                this.status = Status.FAILED;
            }
        }
    }

    public Status tick() {
        this.tickTimes++;
        if(!pistonIsBreak) updateStatus();
        switch (this.status) {
            case UNINITIALIZED:
                InventoryManager.switchToItem(Blocks.PISTON);
                BlockPlacer.pistonPlacement(this.pistonBlockPos, Direction.UP);
                InventoryManager.switchToItem(Blocks.REDSTONE_TORCH);
                BlockPlacer.simpleBlockPlacement(this,this.redstoneTorchBlockPos, Blocks.REDSTONE_TORCH);
                break;
            case UNEXTENDED_WITH_POWER_SOURCE:
                break;
            case EXTENDED:
                //#if MC > 12006
                //$$ Item item = client.player.getMainHandStack().getItem();
                //$$ System.out.println(item.toString());
                //$$ if(!(item.equals(Items.NETHERITE_PICKAXE) || item.equals(Items.DIAMOND_PICKAXE)) || !switchPickaxe) break;
                //#endif
                //打掉红石火把
                ArrayList<BlockPos> nearByRedstoneTorchPosList = CheckingEnvironment.findNearbyRedstoneTorch(world, pistonBlockPos);
                for (BlockPos pos : nearByRedstoneTorchPosList) {
                    BlockBreaker.breakBlock(world, pos);
                }
                //打掉活塞
                BlockBreaker.breakBlock(this.world, this.pistonBlockPos);
                for (int i = 1; i < 6; i++) {
                    addPosList(pistonBlockPos.up(i));
                }
                //放置朝下的活塞
                //#if MC > 12006
                //$$ pistonIsBreak = true;
                //$$ List<TargetBlock> list = cachedTargetBlockList.stream().filter(targetBlock -> targetBlock.status == Status.EXTENDED).toList();
                //$$ if (list.stream().allMatch(targetBlock -> targetBlock.pistonIsBreak)) {
                //$$     list.forEach(targetBlock -> {
                //$$         BlockPlacer.pistonPlacement(targetBlock.pistonBlockPos, Direction.DOWN);
                //$$         targetBlock.hasTried = true;
                //$$         targetBlock.status = Status.NEEDS_WAITING;
                //$$     });
                //$$ }
                //$$ break;
                //#else
                BlockPlacer.pistonPlacement(this.pistonBlockPos, Direction.DOWN);
                this.hasTried = true;
                break;
                //#endif
            case RETRACTED:
                addPosList(pistonBlockPos);
                addPosList(pistonBlockPos.up());
//                BlockBreaker.breakBlock(world, pistonBlockPos);
//                BlockBreaker.breakBlock(world, pistonBlockPos.up());
                if (this.slimeBlockPos != null) {
                    addPosList(slimeBlockPos);
//                    BlockBreaker.breakBlock(world, slimeBlockPos);
                }
                return Status.RETRACTED;
            case RETRACTING:
                return Status.RETRACTING;
            case UNEXTENDED_WITHOUT_POWER_SOURCE:
                InventoryManager.switchToItem(Blocks.REDSTONE_TORCH);
                BlockPlacer.simpleBlockPlacement(this,this.redstoneTorchBlockPos, Blocks.REDSTONE_TORCH);
                break;
            case FAILED:
                addPosList(pistonBlockPos);
                addPosList(pistonBlockPos.up());
//                BlockBreaker.breakBlock(world, pistonBlockPos);
//                BlockBreaker.breakBlock(world, pistonBlockPos.up());
                return Status.FAILED;
            case STUCK:
                addPosList(pistonBlockPos);
                addPosList(pistonBlockPos.up());
//                BlockBreaker.breakBlock(world, pistonBlockPos);
//                BlockBreaker.breakBlock(world, pistonBlockPos.up());
                break;
            case NEEDS_WAITING:
                //#if MC > 12006
                //$$ if (pistonIsBreak) {
                //$$     pistonIsBreak = false;
                //$$     break;
                //$$ }
                //#endif
                break;
        }
        return null;
    }

    enum Status {
        FAILED,
        UNINITIALIZED,
        UNEXTENDED_WITH_POWER_SOURCE,
        UNEXTENDED_WITHOUT_POWER_SOURCE,
        EXTENDED,
        NEEDS_WAITING,
        RETRACTING,
        RETRACTED,
        STUCK;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public BlockPos geths() {
        return redstoneTorchBlockPos;
    }
    public BlockPos getnyk() {
        return slimeBlockPos;
    }


    public ClientWorld getWorld() {
        return world;
    }

    public Status getStatus() {
        return status;
    }

    private void updateStatus() {
        if (this.tickTimes > 40) {
            this.status = Status.FAILED;
            return;
        }
        this.redstoneTorchBlockPos = CheckingEnvironment.findNearbyFlatBlockToPlaceRedstoneTorch(this.world, this.blockPos);
        if (this.redstoneTorchBlockPos == null) {
            this.slimeBlockPos = CheckingEnvironment.findPossibleSlimeBlockPos(world, blockPos);
            if (slimeBlockPos != null) {
                BlockPlacer.simpleBlockPlacement(this,slimeBlockPos, Blocks.SLIME_BLOCK);
                redstoneTorchBlockPos = slimeBlockPos.up();
            } else {
                this.status = Status.FAILED;
                Messager.actionBar("bedrockminer.fail.place.redstonetorch");
            }
        } else if (!Printer.bedrockModeTarget( this.world.getBlockState(this.blockPos).getBlock()) && this.world.getBlockState(this.pistonBlockPos).isOf(Blocks.PISTON)) {
            this.status = Status.RETRACTED;
        } else if (this.world.getBlockState(this.pistonBlockPos).isOf(Blocks.PISTON) && this.world.getBlockState(this.pistonBlockPos).get(PistonBlock.EXTENDED)) {
            this.status = Status.EXTENDED;
        } else if (this.world.getBlockState(this.pistonBlockPos).isOf(Blocks.MOVING_PISTON)) {
            this.status = Status.RETRACTING;
        }  else if (this.world.getBlockState(this.pistonBlockPos).isOf(Blocks.PISTON) &&
                !this.world.getBlockState(this.pistonBlockPos).get(PistonBlock.EXTENDED) &&
                CheckingEnvironment.findNearbyRedstoneTorch(this.world, this.pistonBlockPos).size() != 0 &&
                Printer.bedrockModeTarget( this.world.getBlockState(this.blockPos).getBlock())) {
            this.status = Status.UNEXTENDED_WITH_POWER_SOURCE;
        } else if (this.hasTried && this.world.getBlockState(this.pistonBlockPos).isOf(Blocks.PISTON) && this.stuckTicksCounter < 15) {
            this.status = Status.NEEDS_WAITING;
            this.stuckTicksCounter++;
        } else if (this.world.getBlockState(this.pistonBlockPos).isOf(Blocks.PISTON) &&
                this.world.getBlockState(this.pistonBlockPos).get(PistonBlock.FACING) == Direction.DOWN &&
                !this.world.getBlockState(this.pistonBlockPos).get(PistonBlock.EXTENDED) &&
                CheckingEnvironment.findNearbyRedstoneTorch(this.world, this.pistonBlockPos).size() != 0 &&
                Printer.bedrockModeTarget( this.world.getBlockState(this.blockPos).getBlock())) {
            this.status = Status.STUCK;
            this.hasTried = false;
            this.stuckTicksCounter = 0;
        }else if (this.world.getBlockState(this.pistonBlockPos).isOf(Blocks.PISTON) &&
                !this.world.getBlockState(this.pistonBlockPos).get(PistonBlock.EXTENDED) &&
                this.world.getBlockState(this.pistonBlockPos).get(PistonBlock.FACING) == Direction.UP &&
                CheckingEnvironment.findNearbyRedstoneTorch(this.world, this.pistonBlockPos).size() == 0 &&
                Printer.bedrockModeTarget( this.world.getBlockState(this.blockPos).getBlock())) {
            this.status = Status.UNEXTENDED_WITHOUT_POWER_SOURCE;
        } else if (CheckingEnvironment.has2BlocksOfPlaceToPlacePiston(world, this.blockPos)) {
            this.status = Status.UNINITIALIZED;
        } else if (!CheckingEnvironment.has2BlocksOfPlaceToPlacePiston(world, this.blockPos)) {
            this.status = Status.FAILED;
            Messager.actionBar("bedrockminer.fail.place.piston");
        } else {
            this.status = Status.FAILED;
        }
    }

}
