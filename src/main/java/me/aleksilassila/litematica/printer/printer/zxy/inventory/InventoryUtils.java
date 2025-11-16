package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import fi.dy.masa.litematica.config.Configs;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.bilixwhite.utils.ShulkerUtils;
import me.aleksilassila.litematica.printer.mixin.masa.MixinInventoryFix;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
//#if MC > 11904
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.SearchItem;
import red.jackf.chesttracker.api.providers.InteractionTracker;
//#else
//$$ import net.minecraft.util.Identifier;
//$$ import net.minecraft.registry.RegistryKey;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.Memory;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils;
//#if MC > 11902
//$$ import net.minecraft.registry.RegistryKeys;
//#else
//#endif
//#endif



import java.util.HashSet;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.closeScreen;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.loadChestTracker;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.openIng;
import static net.minecraft.block.ShulkerBoxBlock.FACING;

public class InventoryUtils {
    static final MinecraftClient client = MinecraftClient.getInstance();
    public static boolean isInventory(World world, BlockPos pos) {
        return fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos) != null;
    }

    public static boolean canOpenInv(BlockPos pos) {
        if (client.world != null) {
            BlockState blockState = client.world.getBlockState(pos);
            BlockEntity blockEntity = client.world.getBlockEntity(pos);
            boolean isInventory = InventoryUtils.isInventory(client.world, pos);
            try {
                if ((isInventory && blockState.createScreenHandlerFactory(client.world, pos) == null) ||
                        (blockEntity instanceof ShulkerBoxBlockEntity entity &&
                                //#if MC > 12103
                                !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(1.0F, blockState.get(FACING), 0.0F, 0.5F, pos.toBottomCenterPos()).offset(pos).contract(1.0E-6)) &&
                                //#elseif MC <= 12103 && MC > 12004
                                //$$ //!client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(1.0F, blockState.get(FACING), 0.0F, 0.5F).offset(pos).contract(1.0E-6)) &&
                                //#elseif MC <= 12004
                                //$$ !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(blockState.get(FACING), 0.0f, 0.5f).offset(pos).contract(1.0E-6)) &&
                                //#endif
                                entity.getAnimationStage() == ShulkerBoxBlockEntity.AnimationStage.CLOSED)) {
                    return false;
                } else if (!isInventory) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public static HashSet<Item> lastNeedItemList = new HashSet<>();
    public static boolean isOpenHandler = false;

    public static boolean switchItem() {
        if (!lastNeedItemList.isEmpty() && !isOpenHandler && !openIng && OpenInventoryPacket.key == null) {
            ClientPlayerEntity player = client.player;
            ScreenHandler sc = player.currentScreenHandler;
            if (!player.currentScreenHandler.equals(player.playerScreenHandler)) return false;
            //排除合成栏 装备栏 副手
            if (LitematicaPrinterMod.STORE_ORDERLY.getBooleanValue() && sc.slots.stream().skip(9).limit(sc.slots.size() - 10).noneMatch(slot -> slot.getStack().isEmpty())
                    && (LitematicaPrinterMod.QUICK_SHULKER.getBooleanValue() || LitematicaPrinterMod.CLOUD_INVENTORY.getBooleanValue())) {
                SwitchItem.checkItems();
                return true;
            }
            if (LitematicaPrinterMod.QUICK_SHULKER.getBooleanValue() && openShulker(lastNeedItemList)) {
                return true;
            } else if (LitematicaPrinterMod.CLOUD_INVENTORY.getBooleanValue()) {
                for (Item item : lastNeedItemList) {
                    //#if MC >= 12001
                    //#if MC > 12004
                    MemoryUtils.currentMemoryKey = client.world.getRegistryKey().getValue();
                    //#else
                    //$$ MemoryUtils.currentMemoryKey = client.world.getDimensionKey().getValue();
                    //#endif
                    MemoryUtils.itemStack = new ItemStack(item);
                    if (SearchItem.search(true)) {
                        closeScreen++;
                        isOpenHandler = true;
                        Printer.printerMemorySync = true;
                        return true;
                    }
                    //#else
                    //$$
                    //$$    MemoryDatabase database = MemoryDatabase.getCurrent();
                    //$$    if (database != null) {
                    //$$        for (Identifier dimension : database.getDimensions()) {
                    //$$            for (Memory memory : database.findItems(item.getDefaultStack(), dimension)) {
                    //$$                MemoryUtils.setLatestPos(memory.getPosition());
                    //#if MC < 11904
                    //$$ OpenInventoryPacket.sendOpenInventory(memory.getPosition(), RegistryKey.of(Registry.WORLD_KEY, dimension));
                    //#else
                    //$$ OpenInventoryPacket.sendOpenInventory(memory.getPosition(), RegistryKey.of(RegistryKeys.WORLD, dimension));
                    //#endif
                    //$$                if(closeScreen == 0)closeScreen++;
                    //$$                Printer.printerMemorySync = true;
                    //$$                isOpenHandler = true;
                    //$$                return true;
                    //$$            }
                    //$$        }
                    //$$    }
                    //#endif
                }
                lastNeedItemList = new HashSet<>();
                isOpenHandler = false;
            }
        }
        return false;
    }

    static int shulkerBoxSlot = -1;

    public static void switchInv() {
//        if(true) return;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ScreenHandler sc = player.currentScreenHandler;
        if (sc.equals(player.playerScreenHandler)) {
            return;
        }
        DefaultedList<Slot> slots = sc.slots;
        for (Item item : lastNeedItemList) {
            for (int y = 0; y < slots.get(0).inventory.size(); y++) {
                if (slots.get(y).getStack().getItem().equals(item)) {

                    String[] str = Configs.Generic.PICK_BLOCKABLE_SLOTS.getStringValue().split(",");
                    if (str.length == 0) return;
                    for (String s : str) {
                        if (s == null) break;
                        try {
                            int c = Integer.parseInt(s) - 1;
                            if (Registries.ITEM.getId(player.getInventory().getStack(c).getItem()).toString().contains("shulker_box") &&
                                    LitematicaPrinterMod.QUICK_SHULKER.getBooleanValue()) {
                                MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.of("濳影盒占用了预选栏"), false);
                                continue;
                            }

                            if (OpenInventoryPacket.key != null) {
                                SwitchItem.newItem(slots.get(y).getStack(), OpenInventoryPacket.pos, OpenInventoryPacket.key, y, -1);
                            } else SwitchItem.newItem(slots.get(y).getStack(), null, null, y, shulkerBoxSlot);
                            int a = MixinInventoryFix.getEmptyPickBlockableHotbarSlot(player.getInventory()) == -1 ?
                                    MixinInventoryFix.getPickBlockTargetSlot(player) :
                                    MixinInventoryFix.getEmptyPickBlockableHotbarSlot(player.getInventory());
                            c = a == -1 ? c : a;
                            ZxyUtils.switchPlayerInvToHotbarAir(c);
                            fi.dy.masa.malilib.util.InventoryUtils.swapSlots(sc, y, c);
                            //#if MC > 12101
                            player.getInventory().setSelectedSlot(c);
                            //#else
                            //$$ player.getInventory().selectedSlot = c;
                            //#endif
                            player.closeHandledScreen();
                            //刷新濳影盒
                            if (shulkerBoxSlot != -1) {
                                client.interactionManager.clickSlot(sc.syncId, shulkerBoxSlot, 0, SlotActionType.PICKUP, client.player);
                                client.interactionManager.clickSlot(sc.syncId, shulkerBoxSlot, 0, SlotActionType.PICKUP, client.player);
                            }
                            shulkerBoxSlot = -1;
                            isOpenHandler = false;
                            lastNeedItemList = new HashSet<>();
                            return;
                        } catch (Exception e) {
                            System.out.println("切换物品异常");
                        }
                    }
                }
            }
        }
        shulkerBoxSlot = -1;
        lastNeedItemList = new HashSet<>();
        isOpenHandler = false;
        ScreenHandler sc2 = player.currentScreenHandler;
        if (!sc2.equals(player.playerScreenHandler)) {
            player.closeHandledScreen();
        }
    }

    static boolean openShulker(HashSet<Item> items) {
        if (Printer.getPrinter().shulkerCooldown > 0) {
            return false;
        }
        for (Item item : items) {
            ScreenHandler sc = MinecraftClient.getInstance().player.playerScreenHandler;
            for (int i = 9; i < sc.slots.size(); i++) {
                ItemStack stack = sc.slots.get(i).getStack();
                String itemid = Registries.ITEM.getId(stack.getItem()).toString();
                if (itemid.contains("shulker_box") && stack.getCount() == 1) {
                    DefaultedList<ItemStack> items1 = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack, -1);
                    if (items1.stream().anyMatch(s1 -> s1.getItem().equals(item))) {
                        try {
                            shulkerBoxSlot = i;
//                            ClientUtil.CheckAndSend(stack,i);
                            //#if MC >= 12001
                            if (loadChestTracker) InteractionTracker.INSTANCE.clear();
                            //#endif
                            ShulkerUtils.openShulker(stack, shulkerBoxSlot);
                            closeScreen++;
                            isOpenHandler = true;
                            Printer.getPrinter().shulkerCooldown = LitematicaPrinterMod.QUICK_SHULKER_COOLDOWN.getIntegerValue();
                            return true;
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        return false;
    }
}