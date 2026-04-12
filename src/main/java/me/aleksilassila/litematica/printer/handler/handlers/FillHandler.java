package me.aleksilassila.litematica.printer.handler.handlers;

import lombok.Getter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.FillBlockModeType;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.printer.action.Action;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.LitematicaUtils;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FillHandler extends ClientPlayerTickHandler {
    public final static String NAME = "fill";

    private List<String> fillCacheBlocklist = new ArrayList<>();
    @Getter
    private Item[] fillModeItemList = new Item[0];

    public FillHandler() {
        super(NAME, PrintModeType.FILL, Configs.Core.FILL, Configs.Fill.FILL_SELECTION_TYPE, true);
    }

    @Override
    protected int getTickInterval() {
        return Configs.Placement.PLACE_INTERVAL.getIntegerValue();
    }

    @Override
    protected int getMaxExecutions() {
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
                    List<Item> items = new ArrayList<>();
                    for (String itemName : fillCacheBlocklist) {
                        items.addAll(BuiltInRegistries.ITEM
                                .stream()
                                .filter(item -> LitematicaUtils.matchName(itemName, new ItemStack(item)))
                                .toList()
                        );
                    }
                    fillModeItemList = items.toArray(new Item[0]);
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
    protected boolean canIterate() {
        return fillModeItemList.length > 0;
    }

    @Override
    public boolean canProcessPos(BlockPos blockPos) {
        if (Configs.Fill.FILL_BLOCK_MODE.getOptionListValue() == FillBlockModeType.HANDHELD) {
            ItemStack heldStack = player.getMainHandItem(); // 获取主手物品
            return !heldStack.isEmpty() && heldStack.getCount() > 0;
        }
        return true;
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        BlockState currentState = level.getBlockState(blockPos);
        if (currentState.isAir()
                || (currentState.getBlock() instanceof LiquidBlock)
                || Configs.Print.REPLACEABLE_LIST.getStrings().stream().anyMatch(s -> LitematicaUtils.matchName(s, currentState))
        ) {
            if (!InventoryUtils.switchToItems(player, this.fillModeItemList)) {
                return;
            }
            if (Configs.Placement.FALLING_CHECK.getBooleanValue() &&
                player.getMainHandItem().getItem() instanceof BlockItem item &&
                item.getBlock() instanceof FallingBlock block &&
                FallingBlock.isFree(level.getBlockState(blockPos.below()))
            ) {
                MessageUtils.setOverlayMessage("方块 " + block.getName().getString() + " 下方无支撑，跳过放置");
                return;
            }

            Action action;
            if (ConfigUtils.getFillModeFacing() != null) {
                action = new Action()
                        .setLookDirection(ConfigUtils.getFillModeFacing().getOpposite())
                        .queueAction(blockPos, ConfigUtils.getFillModeFacing(), false, player);
            } else {
                action = new Action()
                        .queueAction(blockPos, getPlayerPlacementDirection(), false, player);
            }
            ActionManager.INSTANCE.setLook(action.getPlayerLook());
            if (ActionManager.INSTANCE.sendQueue(player).needWaitModifyLook){
                skipIteration.set(true);
            } else {
                this.setCooldown(blockPos, ConfigUtils.getPlaceCooldown());
            }
        }
    }

}