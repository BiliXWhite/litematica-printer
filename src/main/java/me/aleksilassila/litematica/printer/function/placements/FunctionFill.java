package me.aleksilassila.litematica.printer.function.placements;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.FileBlockModeType;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.function.FunctionPlacement;
import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.utils.FilterUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.switchItem;

public class FunctionFill extends FunctionPlacement {
    private List<Item> fillModeItemList = new ArrayList<>();
    private List<String> fillcaCheBlocklist = new ArrayList<>();
    private @Nullable BlockPos blockPos;

    @Override
    public PrintModeType getPrintModeType() {
        return PrintModeType.FILL;
    }

    @Override
    public ConfigBoolean getCurrentConfig() {
        return Configs.Fill.FILL;
    }

    public @Nullable BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public boolean canIterationTest(Printer printer, ClientLevel level, LocalPlayer player, BlockPos pos) {
        if (pos != null) {
            if (!PrinterUtils.isPositionInSelectionRange(player, pos, Configs.Fill.FILL_SELECTION_TYPE)) {
                return false;
            }
            if (isPlaceCooldown(pos)) {
                return false;
            }
        }
        // 手持物品
        if (Configs.Fill.FILL_BLOCK_MODE.getOptionListValue() == FileBlockModeType.HANDHELD) {
            ItemStack heldStack = player.getMainHandItem(); // 获取主手物品
            if (heldStack.isEmpty() || heldStack.getCount() <= 0) {
                return false; // 主手无物品时跳过填充
            }
            fillModeItemList = List.of(heldStack.getItem());
        }
        // 白名单模式
        if (Configs.Fill.FILL_BLOCK_MODE.getOptionListValue() == FileBlockModeType.WHITELIST) {
            // 每次去MC注册表中获取会造成大量卡顿, 所以仅在玩家修改了填充列表, 再去读取以便注册表
            List<String> strings = Configs.Fill.FILL_BLOCK_LIST.getStrings();
            if (!strings.equals(fillcaCheBlocklist)) {
                fillcaCheBlocklist = new ArrayList<>(strings);
                if (strings.isEmpty()) {
                    return false;
                }
                for (String itemName : fillcaCheBlocklist) {
                    fillModeItemList = BuiltInRegistries.ITEM.stream().filter(item -> FilterUtils.matchName(itemName, new ItemStack(item))).toList();
                }
            }
        }
        return true;
    }

    @Override
    public void tick(Printer printer, @NotNull Minecraft client, @NotNull ClientLevel level, @NotNull LocalPlayer player) {
        if (isOpenHandler || switchItem()) {
            return;
        }
        boolean handheld = Configs.Fill.FILL_BLOCK_MODE.getOptionListValue() == FileBlockModeType.HANDHELD;
        if (!canIterationTest(printer, level, player, blockPos)) {
            return;
        }
        int placeBlocksPerTick = Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue();
        boolean loop = true;
        while (loop && (blockPos = getBoxBlockPos()) != null) {
            if (isPlaceCooldown(blockPos)) {
                loop = false;
            }
            if (Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue() != 0 && placeBlocksPerTick == 0) {
                loop = false;
            }
            if (!PrinterUtils.isPositionInSelectionRange(player, blockPos, Configs.Fill.FILL_SELECTION_TYPE)) {
                continue;
            }
            Printer.getInstance().placeCooldownList.put(blockPos, Configs.Placement.PLACE_COOLDOWN.getIntegerValue());
            BlockState currentState = level.getBlockState(blockPos);
            if (currentState.isAir() || (currentState.getBlock() instanceof LiquidBlock) || Configs.Print.REPLACEABLE_LIST.getStrings().stream().anyMatch(s -> FilterUtils.matchName(s, currentState))) {
                if (handheld || printer.switchToItems(player, getFillItemsArray())) {
                    if (handheld) {
                        ItemStack heldStack = player.getMainHandItem(); // 获取主手物品
                        if (heldStack.isEmpty() || heldStack.getCount() <= 0) return; // 主手无物品时跳过填充
                    }
                    new PlacementGuide.Action()
                            .setLookDirection(PlaceUtils.getFillModeFacing().getOpposite())
                            .queueAction(printer.queue, blockPos, PlaceUtils.getFillModeFacing(), false);
                    printer.queue.sendQueue(player);
                    if (Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue() != 0) {
                        placeBlocksPerTick--;
                    }
                }
                setPlaceCooldown(blockPos);
            }
        }
    }

    public Item[] getFillItemsArray() {
        return fillModeItemList.toArray(new Item[0]);
    }
}
