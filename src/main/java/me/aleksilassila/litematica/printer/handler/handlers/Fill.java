package me.aleksilassila.litematica.printer.handler.handlers;

import lombok.Getter;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.FillBlockModeType;
import me.aleksilassila.litematica.printer.enums.HighlightType;
import me.aleksilassila.litematica.printer.handler.Module;
import me.aleksilassila.litematica.printer.printer.*;
import me.aleksilassila.litematica.printer.printer.action.Action;
import me.aleksilassila.litematica.printer.utils.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Fill extends Module {
    public final static String NAME = "fill";

    private List<String> fillCacheBlocklist = new ArrayList<>();
    @Getter
    private Item[] fillModeItemList = new Item[0];

    // 可替换列表缓存，避免每位置都做拼音转换
    private List<String> replaceableCache = new ArrayList<>();
    // 可能被后台搜索线程读取，使用并发 Map
    private final Map<Block, Boolean> replaceableMatchCache = new java.util.concurrent.ConcurrentHashMap<>();

    public Fill() {
        super(NAME, Configs.Fill.ENABLED, Configs.Fill.FILL_SELECTION_TYPE, true);
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
                                .filter(item -> PinYinSearchUtils.matchName(itemName, new ItemStack(item)))
                                .toList()
                        );
                    }
                    fillModeItemList = items.toArray(new Item[0]);
                }
                break;
            case HANDHELD:  // 手持物品
                if (Configs.Fill.FILL_BLOCK_MODE.getOptionListValue() == FillBlockModeType.HANDHELD) {
                    ItemStack heldStack = player.getMainHandItem();
                    if (!heldStack.isEmpty() && heldStack.getCount() > 0) {
                        Item heldItem = heldStack.getItem();
                        List<String> blacklist = Configs.Fill.FILL_HANDHELD_BLACKLIST.getStrings();
                        if (!blacklist.isEmpty() && blacklist.stream().anyMatch(s -> PinYinSearchUtils.matchName(s, heldStack))) {
                            fillModeItemList = new Item[0];
                        } else {
                            fillModeItemList = new Item[]{heldItem};
                        }
                    } else {
                        fillModeItemList = new Item[0];
                    }
                }
                break;
        }
    }

    /**
     * 缓存版可替换判断：同种方块只做一次拼音匹配
     */
    private boolean isReplaceable(BlockState state) {
        List<String> current = Configs.Print.REPLACEABLE_LIST.getStrings();
        if (!current.equals(replaceableCache)) {
            replaceableCache = new ArrayList<>(current);
            replaceableMatchCache.clear();
        }
        if (current.isEmpty()) return false;
        return replaceableMatchCache.computeIfAbsent(state.getBlock(),
                block -> current.stream().anyMatch(s -> PinYinSearchUtils.matchName(s, state)));
    }

    @Override
    protected boolean canIterate() {
        return fillModeItemList.length > 0;
    }

    @Override
    public boolean canProcessPos(BlockPos pos) {
        if (Configs.Fill.FILL_BLOCK_MODE.getOptionListValue() == FillBlockModeType.HANDHELD) {
            ItemStack heldStack = player.getMainHandItem();
            if (heldStack.isEmpty() || heldStack.getCount() <= 0) return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        if (state.getBlock() instanceof LiquidBlock) return true;
        if (isReplaceable(state)) return true;
        return false;
    }

    @Override
    public boolean isCorrectBlock(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.getBlock() instanceof LiquidBlock) return false;
        if (isReplaceable(state)) return false;
        return true;
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        BlockState currentState = level.getBlockState(blockPos);
        if (currentState.isAir()
                || (currentState.getBlock() instanceof LiquidBlock)
                || isReplaceable(currentState)
        ) {
            if (!InventoryUtils.switchToItems(player, this.fillModeItemList)) {
                if (this.fillModeItemList != null && this.fillModeItemList.length > 0 && this.fillModeItemList[0] != null) {
                    MissingMaterialTracker.getInstance().recordMissing(this.fillModeItemList[0],
                            //#if MC >= 260100
                            //$$ this.fillModeItemList[0].getName(this.fillModeItemList[0].getDefaultInstance())
                            //#elseif MC > 12101
                            this.fillModeItemList[0].getName()
                            //#else
                            //$$ this.fillModeItemList[0].getDescription()
                            //#endif
                    );
                }
                return;
            }
            if (Configs.Placement.FALLING_CHECK.getBooleanValue() &&
                player.getMainHandItem().getItem() instanceof BlockItem item &&
                item.getBlock() instanceof FallingBlock block &&
                FallingBlock.isFree(level.getBlockState(blockPos.below()))
            ) {
                MessageUtils.setOverlayMessage(I18n.BLOCK_NO_SUPPORT.getName(block.getName().getString()));
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
            addHighlight(blockPos, HighlightType.PLACE);
            ActionManager.INSTANCE.setLook(action.getPlayerLook());
            ActionManager.INSTANCE.setNeedWaitModifyLookFromAction(action.getNeedWaitModifyLook());
            if (ActionManager.INSTANCE.sendQueue(player).needWaitModifyLook){
                skipIteration.set(true);
            } else {
                this.setCooldown(blockPos, ConfigUtils.getPlaceCooldown());
            }
        }
    }

}