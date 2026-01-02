package me.aleksilassila.litematica.printer.function;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.FileBlockModeType;
import me.aleksilassila.litematica.printer.config.enums.PrintModeType;
import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsBlockName;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsItemName;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.switchItem;

public class FunctionFillMode extends FunctionModeBase {
    private List<Item> fillModeItemList = new ArrayList<>();
    private List<String> fillcaCheBlocklist = new ArrayList<>();

    @Override
    public PrintModeType getPrintModeType() {
        return PrintModeType.FILL;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return Configs.FILL;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        if (isOpenHandler || switchItem() || BreakManager.hasTargets()) {
            return;
        }
        Printer.getInstance().blockContext = null;
        boolean handheld = false;
        // 手持物品
        if (Configs.FILL_BLOCK_MODE.getOptionListValue() == FileBlockModeType.HANDHELD) {
            handheld = true;
            ItemStack heldStack = player.getMainHandItem(); // 获取主手物品
            if (heldStack.isEmpty() || heldStack.getCount() <= 0) {
                return; // 主手无物品时跳过填充
            }
            fillModeItemList = List.of(heldStack.getItem());
        }
        // 白名单模式
        if (Configs.FILL_BLOCK_MODE.getOptionListValue() == FileBlockModeType.WHITELIST) {
            // 每次去MC注册表中获取会造成大量卡顿, 所以仅在玩家修改了填充列表, 再去读取以便注册表
            List<String> strings = Configs.FILL_BLOCK_LIST.getStrings();
            if (!strings.equals(fillcaCheBlocklist)) {
                fillcaCheBlocklist = new ArrayList<>(strings);
                if (strings.isEmpty()) {
                    return;
                }
                for (String itemName : fillcaCheBlocklist) {
                    fillModeItemList = BuiltInRegistries.ITEM.stream().filter(item -> equalsItemName(itemName, new ItemStack(item))).toList();
                }
            }
        }
        BlockPos pos;
        while ((pos = printer.getBlockPos()) != null) {
            if (Configs.BLOCKS_PER_TICK.getIntegerValue() != 0 && printer.printerWorkingCountPerTick == 0)
                return;
            if (!PrinterUtils.isPositionInSelectionRange(player, pos, Configs.FILL_SELECTION_TYPE))
                continue;
            if (!Printer.TempData.xuanQuFanWeiNei_p(pos))
                continue;
            // 跳过冷却中的位置
            if (Printer.getInstance().placeCooldownList.containsKey(pos))
                continue;
            Printer.getInstance().placeCooldownList.put(pos, Configs.PLACE_COOLDOWN.getIntegerValue());
            var currentState = level.getBlockState(pos);
            if (currentState.isAir() || (currentState.getBlock() instanceof LiquidBlock) || Configs.REPLACEABLE_LIST.getStrings().stream().anyMatch(s -> equalsBlockName(s, currentState))) {
                if (handheld || printer.switchToItems(player, getFillItemsArray())) {
                    if (handheld) {
                        ItemStack heldStack = player.getMainHandItem(); // 获取主手物品
                        if (heldStack.isEmpty() || heldStack.getCount() <= 0) return; // 主手无物品时跳过填充
                    }
                    new PlacementGuide.Action()
                            .setLookDirection(PlaceUtils.getFillModeFacing().getOpposite())
                            .queueAction(printer.queue, pos, PlaceUtils.getFillModeFacing(), false);

                    if (printer.tickRate == 0) {
                        printer.queue.sendQueue(client.player);
                        if (Configs.BLOCKS_PER_TICK.getIntegerValue() != 0) {
                            printer.printerWorkingCountPerTick--;
                        }
                        continue;
                    }
                    printer.queue.sendQueue(player);
                    return;
                }
            }
        }
    }

    public Item[] getFillItemsArray() {
        return fillModeItemList.toArray(new Item[0]);
    }
}
