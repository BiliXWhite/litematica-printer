package me.aleksilassila.litematica.printer.handler.handlers;

import lombok.Getter;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.FillBlockModeType;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.printer.Action;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.FilterUtils;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.switchItem;

public class FillHandler extends ClientPlayerTickHandler {
    private List<String> fillCacheBlocklist = new ArrayList<>();
    @Getter
    private Item[] fillModeItemList = new Item[0];

    public FillHandler() {
        super("fill", PrintModeType.FILL, Configs.Core.FILL, Configs.Fill.FILL_SELECTION_TYPE, true);
    }

    @Override
    protected int getTickInterval() {
        return Configs.Placement.PLACE_INTERVAL.getIntegerValue();
    }

    @Override
    protected int getMaxEffectiveExecutionsPerTick() {
        return Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue();
    }

    @Override
    protected void preprocess() {
        FillBlockModeType fillMode = (FillBlockModeType) Configs.Fill.FILL_BLOCK_MODE.getOptionListValue();
        switch (fillMode) {
            case BLOCKLIST:
                // 每次去MC注册表中获取会造成大量卡顿, 所以仅在玩家修改了填充列表, 再去读取以便注册表
                List<String> strings = Configs.Fill.FILL_BLOCK_LIST.getStrings();
                if (!strings.equals(fillCacheBlocklist)) {
                    fillCacheBlocklist = new ArrayList<>(strings);
                    if (strings.isEmpty()) {
                        return;
                    }
                    for (String itemName : fillCacheBlocklist) {
                        fillModeItemList = BuiltInRegistries.ITEM
                                .stream()
                                .filter(item -> FilterUtils.matchName(itemName, new ItemStack(item)))
                                .toArray(Item[]::new);
                    }
                }
                break;
            case HANDHELD:  // 手持物品
                if (Configs.Fill.FILL_BLOCK_MODE.getOptionListValue() == FillBlockModeType.HANDHELD) {
                    ItemStack heldStack = player.getMainHandItem(); // 获取主手物品
                    if (!heldStack.isEmpty() && heldStack.getCount() > 0) {
                        fillModeItemList = new Item[]{player.getMainHandItem().getItem()};
                    } else {
                        fillModeItemList = new Item[0];
                    }
                }
                break;
        }
    }

    @Override
    protected boolean canExecuteIteration() {
        return fillModeItemList.length > 0;
    }

    @Override
    public boolean canIterationBlockPos(BlockPos blockPos) {
        if (isOpenHandler || switchItem()) {
            return false;
        }
        if (Configs.Fill.FILL_BLOCK_MODE.getOptionListValue() == FillBlockModeType.HANDHELD) {
            ItemStack heldStack = player.getMainHandItem(); // 获取主手物品
            return !heldStack.isEmpty() && heldStack.getCount() > 0;
        }
        return !this.isBlockPosOnCooldown(blockPos);
    }


    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        boolean handheld = Configs.Fill.FILL_BLOCK_MODE.getOptionListValue() == FillBlockModeType.HANDHELD;
        BlockState currentState = level.getBlockState(blockPos);
        if (currentState.isAir()
                || (currentState.getBlock() instanceof LiquidBlock)
                || Configs.Print.REPLACEABLE_LIST.getStrings().stream().anyMatch(s -> FilterUtils.matchName(s, currentState))
        ) {
            if (handheld || InventoryUtils.switchToItems(player, this.fillModeItemList)) {
                if (PlaceUtils.getFillModeFacing() != null) {
                    new Action()
                            .setLookDirection(PlaceUtils.getFillModeFacing().getOpposite())
                            .queueAction(blockPos, PlaceUtils.getFillModeFacing(), false, player);
                } else {
                    new Action()
                            .queueAction(blockPos, null, false, player);
                }
                ActionManager.INSTANCE.sendQueue(player);
            }
        }
        this.setBlockPosCooldown(blockPos, ConfigUtils.getPlaceCooldown());
    }

}
