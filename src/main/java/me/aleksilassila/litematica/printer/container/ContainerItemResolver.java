package me.aleksilassila.litematica.printer.container;

import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.RemoteResultType;
import me.aleksilassila.litematica.printer.network.payload.ScanContainerResultPayload;
import net.minecraft.core.BlockPos;
//#if MC >= 11903
import net.minecraft.core.registries.BuiltInRegistries;
//#else
//$$ import net.minecraft.core.Registry;
//#endif
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ContainerItemResolver {

    public record ResolveResult(RemoteResultType type, int extractedCount) {
        public static final ResolveResult ZERO = new ResolveResult(RemoteResultType.INTERNAL_ERROR, 0);
    }

    public static ResolveResult resolveItem(ServerPlayer player, BlockPos pos,
                                                String itemIdStr, int slot) {
        //#if MC >= 12000
        ServerLevel level = player.level();
        //#else
        //$$ ServerLevel level = player.getLevel();
        //#endif

        double maxDist = getMaxInteractionDistance();
        double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distance > maxDist * maxDist) {
            return new ResolveResult(RemoteResultType.PLAYER_TOO_FAR, 0);
        }

        if (!level.isLoaded(pos)) {
            return new ResolveResult(RemoteResultType.CONTAINER_NOT_LOADED, 0);
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return new ResolveResult(RemoteResultType.CONTAINER_NOT_FOUND, 0);
        }

        if (!(blockEntity instanceof Container container)) {
            return new ResolveResult(RemoteResultType.NOT_A_CONTAINER, 0);
        }

        if (slot < 0 || slot >= container.getContainerSize()) {
            return new ResolveResult(RemoteResultType.SLOT_EMPTY, 0);
        }

        ItemStack stackInSlot = container.getItem(slot);
        if (stackInSlot.isEmpty()) {
            return new ResolveResult(RemoteResultType.SLOT_EMPTY, 0);
        }

        Item requestedItem = resolveItemFromId(itemIdStr);
        if (requestedItem == null) {
            return new ResolveResult(RemoteResultType.ITEM_NOT_MATCH, 0);
        }

        if (!stackInSlot.is(requestedItem)) {
            return new ResolveResult(RemoteResultType.ITEM_NOT_MATCH, 0);
        }

        return giveToPlayer(player, container, slot, stackInSlot);
    }

    private static double getMaxInteractionDistance() {
        try {
            double distance = Configs.Print.REMOTE_INTERACTION_DISTANCE.getDoubleValue();
            return distance == 0.0 ? Double.MAX_VALUE : distance;
        } catch (Exception ignored) {
            return 32.0;
        }
    }

    private static Item resolveItemFromId(String itemIdStr) {
        //#if MC >= 12105
        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.parse(itemIdStr);
        //#elseif MC >= 12101
        //$$ net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse(itemIdStr);
        //#else
        //$$ net.minecraft.resources.ResourceLocation id = new net.minecraft.resources.ResourceLocation(itemIdStr);
        //#endif
        //#if MC >= 11903
        return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        //#else
        //$$ return Registry.ITEM.getOptional(id).orElse(null);
        //#endif
    }

    private static ResolveResult giveToPlayer(ServerPlayer player, Container container,
                                                  int slot, ItemStack stack) {
        try {
            int maxAddable = computeMaxAddable(player.getInventory(), stack);
            if (maxAddable <= 0) {
                return new ResolveResult(RemoteResultType.INVENTORY_FULL, 0);
            }

            int toExtract = Math.min(stack.getCount(), maxAddable);
            ItemStack extracted = container.removeItem(slot, toExtract);
            int initialCount = extracted.getCount();
            if (initialCount <= 0) {
                return new ResolveResult(RemoteResultType.INTERNAL_ERROR, 0);
            }

            player.getInventory().add(extracted);
            int actuallyAdded = initialCount - extracted.getCount();
            if (!extracted.isEmpty()) {
                Reference.LOGGER.warn(
                    "giveToPlayer: couldn't add all to inv, dropping {}x {} at player {}",
                    extracted.getCount(), extracted.getItem(), player.getName().getString());
                player.drop(extracted, false);
            }

            container.setChanged();
            RemoteResultType type = toExtract < stack.getCount()
                    ? RemoteResultType.PARTIAL
                    : RemoteResultType.SUCCESS;
            return new ResolveResult(type, actuallyAdded);
        } catch (Exception e) {
            return new ResolveResult(RemoteResultType.INTERNAL_ERROR, 0);
        }
    }

    private static int computeMaxAddable(Inventory inventory, ItemStack template) {
        int maxStack = template.getMaxStackSize();
        int canAdd = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack existing = inventory.getItem(i);
            if (existing.isEmpty()) {
                canAdd += maxStack;
            } else {
                //#if MC >= 12005
                if (ItemStack.isSameItemSameComponents(existing, template)) {
                    canAdd += maxStack - existing.getCount();
                }
                //#else
                //$$ if (ItemStack.isSameItem(existing, template)) {
                //$$     canAdd += maxStack - existing.getCount();
                //$$ }
                //#endif
            }
        }
        return canAdd;
    }

    public static java.util.List<ScanContainerResultPayload.SlotEntry> scanContainer(
            ServerPlayer player, BlockPos pos) {
        //#if MC >= 12000
        ServerLevel level = player.level();
        //#else
        //$$ ServerLevel level = player.getLevel();
        //#endif

        double maxDist = getMaxInteractionDistance();
        double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distance > maxDist * maxDist || !level.isLoaded(pos)) {
            return java.util.List.of();
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) {
            return java.util.List.of();
        }

        java.util.List<ScanContainerResultPayload.SlotEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                //#if MC >= 11903
                String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(stack.getItem()).toString();
                //#else
                //$$ String itemId = net.minecraft.core.Registry.ITEM
                //$$         .getKey(stack.getItem()).toString();
                //#endif
                entries.add(new ScanContainerResultPayload.SlotEntry(i, itemId, stack.getCount()));
            }
        }
        return entries;
    }
}