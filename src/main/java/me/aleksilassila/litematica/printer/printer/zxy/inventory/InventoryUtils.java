package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import fi.dy.masa.litematica.config.Configs;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PreprocessUtils;
import me.aleksilassila.litematica.printer.bilixwhite.utils.ShulkerUtils;
import me.aleksilassila.litematica.printer.mixin.masa.InventoryUtilsAccessor;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//#if MC > 11904 && MC <= 12104
//$$ import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.SearchItem;
//$$ import red.jackf.chesttracker.api.providers.InteractionTracker;
//#elseif MC <= 11904
//$$ import net.minecraft.core.Registry;
//$$ import net.minecraft.resources.ResourceLocation;
//$$ import net.minecraft.resources.ResourceKey;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.Memory;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils;
    //#if MC > 11902
    //$$ import net.minecraft.core.registries.Registries;
    //#endif
//#endif

import java.util.HashSet;
import static me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus.closeScreen;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.openIng;

public class InventoryUtils {
    static final Minecraft client = Minecraft.getInstance();
    public static boolean isInventory(Level world, BlockPos pos) {
        return fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos) != null;
    }

    public static boolean canOpenInv(BlockPos pos) {
        if (client.level != null) {
            BlockState blockState = client.level.getBlockState(pos);
            BlockEntity blockEntity = client.level.getBlockEntity(pos);
            boolean isInventory = InventoryUtils.isInventory(client.level, pos);
            try {
                if ((isInventory && blockState.getMenuProvider(client.level, pos) == null) ||
                        (blockEntity instanceof ShulkerBoxBlockEntity entity &&
                                //#if MC > 12103
                                !client.level.noCollision(Shulker.getProgressDeltaAabb(1.0F, blockState.getValue(BlockStateProperties.FACING), 0.0F, 0.5F, pos.getBottomCenter()).move(pos).deflate(1.0E-6)) &&
                                //#elseif MC <= 12103 && MC > 12004
                                //$$ !client.level.noCollision(Shulker.getProgressDeltaAabb(1.0F, blockState.getValue(BlockStateProperties.FACING), 0.0F, 0.5F).move(pos).deflate(1.0E-6)) &&
                                //#elseif MC <= 12004
                                //$$ !client.level.noCollision(Shulker.getProgressDeltaAabb(blockState.getValue(BlockStateProperties.FACING), 0.0f, 0.5f).move(pos).deflate(1.0E-6)) &&
                                //#endif
                                entity.getAnimationStatus() == ShulkerBoxBlockEntity.AnimationStatus.CLOSED)) {
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
            LocalPlayer player = client.player;
            AbstractContainerMenu sc = player.containerMenu;
            if (!player.containerMenu.equals(player.inventoryMenu)) return false;
            //排除合成栏 装备栏 副手
            if (InitHandler.STORE_ORDERLY.getBooleanValue() && sc.slots.stream().skip(9).limit(sc.slots.size() - 10).noneMatch(slot -> slot.getItem().isEmpty())
                    && (InitHandler.QUICK_SHULKER.getBooleanValue() || InitHandler.CLOUD_INVENTORY.getBooleanValue())) {
                SwitchItem.checkItems();
                return true;
            }
            if (InitHandler.QUICK_SHULKER.getBooleanValue() && openShulker(lastNeedItemList)) {
                return true;
            } else if (InitHandler.CLOUD_INVENTORY.getBooleanValue()) {
                for (Item item : lastNeedItemList) {
                    //#if MC >= 12001 && MC <= 12104
                    //$$ MemoryUtils.currentMemoryKey = client.level.dimension().location();
                    //$$ MemoryUtils.itemStack = new ItemStack(item);
                    //$$ if (SearchItem.search(true)) {
                    //$$     closeScreen++;
                    //$$     isOpenHandler = true;
                    //$$     Printer.getInstance().printerMemorySync = true;
                    //$$     return true;
                    //$$ }
                    //#elseif MC < 12001
                    //$$
                    //$$    MemoryDatabase database = MemoryDatabase.getCurrent();
                    //$$    if (database != null) {
                    //$$        for (ResourceLocation dimension : database.getDimensions()) {
                    //$$            for (Memory memory : database.findItems(item.getDefaultInstance(), dimension)) {
                    //$$                MemoryUtils.setLatestPos(memory.getPosition());
                        //#if MC < 11904
                        //$$ OpenInventoryPacket.sendOpenInventory(memory.getPosition(), ResourceKey.create(Registry.DIMENSION_REGISTRY, dimension));
                        //#else
                        //$$ OpenInventoryPacket.sendOpenInventory(memory.getPosition(), ResourceKey.create(Registries.DIMENSION, dimension));
                        //#endif
                    //$$                if(closeScreen == 0)closeScreen++;
                    //$$                Printer.getInstance().printerMemorySync = true;
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

        LocalPlayer player = Minecraft.getInstance().player;
        AbstractContainerMenu sc = player.containerMenu;
        if (sc.equals(player.inventoryMenu)) {
            return;
        }
        NonNullList<Slot> slots = sc.slots;
        for (Item item : lastNeedItemList) {
            for (int y = 0; y < slots.get(0).container.getContainerSize(); y++) {
                if (slots.get(y).getItem().getItem().equals(item)) {

                    String[] str = Configs.Generic.PICK_BLOCKABLE_SLOTS.getStringValue().split(",");
                    if (str.length == 0) return;
                    for (String s : str) {
                        if (s == null) break;
                        try {
                            int c = Integer.parseInt(s) - 1;
                            if (BuiltInRegistries.ITEM.getKey(player.getInventory().getItem(c).getItem()).toString().contains("shulker_box") &&
                                    InitHandler.QUICK_SHULKER.getBooleanValue()) {
                                Minecraft.getInstance().gui.setOverlayMessage(Component.nullToEmpty("濳影盒占用了预选栏"), false);
                                continue;
                            }

                            if (OpenInventoryPacket.key != null) {
                                SwitchItem.newItem(slots.get(y).getItem(), OpenInventoryPacket.pos, OpenInventoryPacket.key, y, -1);
                            } else SwitchItem.newItem(slots.get(y).getItem(), null, null, y, shulkerBoxSlot);
                            int a = InventoryUtilsAccessor.getEmptyPickBlockableHotbarSlot(player.getInventory()) == -1 ?
                                    InventoryUtilsAccessor.getPickBlockTargetSlot(player) :
                                    InventoryUtilsAccessor.getEmptyPickBlockableHotbarSlot(player.getInventory());
                            c = a == -1 ? c : a;
                            ZxyUtils.switchPlayerInvToHotbarAir(c);
                            fi.dy.masa.malilib.util.InventoryUtils.swapSlots(sc, y, c);
                            PreprocessUtils.setSelectedSlot(player.getInventory(), c);
                            player.closeContainer();
                            //刷新濳影盒
                            if (shulkerBoxSlot != -1) {
                                client.gameMode.handleInventoryMouseClick(sc.containerId, shulkerBoxSlot, 0, ClickType.PICKUP, client.player);
                                client.gameMode.handleInventoryMouseClick(sc.containerId, shulkerBoxSlot, 0, ClickType.PICKUP, client.player);
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
        AbstractContainerMenu sc2 = player.containerMenu;
        if (!sc2.equals(player.inventoryMenu)) {
            player.closeContainer();
        }
    }

    static boolean openShulker(HashSet<Item> items) {
        if (Printer.getInstance().shulkerCooldown > 0) {
            return false;
        }
        for (Item item : items) {
            AbstractContainerMenu sc = Minecraft.getInstance().player.inventoryMenu;
            for (int i = 9; i < sc.slots.size(); i++) {
                ItemStack stack = sc.slots.get(i).getItem();
                String itemid = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (itemid.contains("shulker_box") && stack.getCount() == 1) {
                    NonNullList<ItemStack> items1 = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack, -1);
                    if (items1.stream().anyMatch(s1 -> s1.getItem().equals(item))) {
                        try {
                            shulkerBoxSlot = i;
//                            ClientUtil.CheckAndSend(stack,i);
                            //#if MC >= 12001 && MC <= 12104
                            //$$ if (me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus.isLoadChestTrackerLoaded()) InteractionTracker.INSTANCE.clear();
                            //#endif
                            ShulkerUtils.openShulker(stack, shulkerBoxSlot);
                            closeScreen++;
                            isOpenHandler = true;
                            Printer.getInstance().shulkerCooldown = InitHandler.QUICK_SHULKER_COOLDOWN.getIntegerValue();
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