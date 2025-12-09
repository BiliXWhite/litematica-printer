package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsBlockName;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsItemName;

public class FunctionFillMode extends FunctionMode {
    private final HashSet<Item> fillModeItemList = new HashSet<>();
    private final List<String> fillBlocklist = new ArrayList<>();

    @Override
    public State.PrintModeType getPrintModeType() {
        return State.PrintModeType.FILL;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return InitHandler.FILL;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        Printer.getInstance().requiredState = null;
        boolean handheld = false;
        fillModeItemList.clear();
        // 手持物品
        if (InitHandler.FILL_BLOCK_MODE.getOptionListValue() == State.FileBlockModeType.HANDHELD) {
            handheld = true;
            ItemStack heldStack = player.getMainHandItem(); // 获取主手物品
            if (heldStack.isEmpty() || heldStack.getCount() <= 0) return; // 主手无物品时跳过填充
            fillModeItemList.add(heldStack.getItem());
        }
        // 白名单模式
        if (InitHandler.FILL_BLOCK_MODE.getOptionListValue() == State.FileBlockModeType.WHITELIST) {
            if (!InitHandler.FILL_BLOCK_LIST.getStrings().equals(fillBlocklist)) {
                fillBlocklist.clear();
                fillBlocklist.addAll(InitHandler.FILL_BLOCK_LIST.getStrings());
                if (InitHandler.FILL_BLOCK_LIST.getStrings().isEmpty()) return;
                for (String itemName : fillBlocklist) {
                    List<Item> list = BuiltInRegistries.ITEM.stream().filter(item -> equalsItemName(itemName, new ItemStack(item))).toList();
                    fillModeItemList.addAll(list);
                }
            }
        }
        BlockPos pos;
        while ((pos = printer.getBlockPos()) != null) {
            if (InitHandler.BLOCKS_PER_TICK.getIntegerValue() != 0 && printer.printerWorkingCountPerTick == 0)
                return;
            if (!Printer.TempData.xuanQuFanWeiNei_p(pos))
                continue;
            // 跳过冷却中的位置
            if (Printer.getInstance().placeCooldownList.containsKey(pos))
                continue;
            Printer.getInstance().placeCooldownList.put(pos, InitHandler.PLACE_COOLDOWN.getIntegerValue());
            var currentState = level.getBlockState(pos);
            if (currentState.isAir() || (currentState.getBlock() instanceof LiquidBlock) || InitHandler.REPLACEABLE_LIST.getStrings().stream().anyMatch(s -> equalsBlockName(s, currentState))) {
                if (handheld || printer.switchToItems(client.player, getFillItemsArray())) {
                    if (handheld){
                        ItemStack heldStack = player.getMainHandItem(); // 获取主手物品
                        if (heldStack.isEmpty() || heldStack.getCount() <= 0) return; // 主手无物品时跳过填充
                    }
                    new PlacementGuide.Action()
                            .setLookDirection(PlaceUtils.getFillModeFacing().getOpposite())
                            .queueAction(printer.queue, pos, PlaceUtils.getFillModeFacing(), false);

                    if (printer.tickRate == 0) {
                        printer.queue.sendQueue(client.player);
                        if (InitHandler.BLOCKS_PER_TICK.getIntegerValue() != 0) {
                            printer.printerWorkingCountPerTick--;
                        }
                        continue;
                    }
                    return;
                }
            }
        }
    }

    public Item[] getFillItemsArray() {
        return fillModeItemList.toArray(new Item[0]);
    }
}
