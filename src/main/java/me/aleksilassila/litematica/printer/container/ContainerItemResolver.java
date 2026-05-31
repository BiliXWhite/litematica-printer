package me.aleksilassila.litematica.printer.container;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ContainerItemResolver {

    public static RemoteResultType resolveItem(ServerPlayer player, BlockPos pos,
                                                String itemIdStr, int slot) {
        //#if MC >= 12000
        ServerLevel level = player.level();
        //#else
        //$$ ServerLevel level = player.getLevel();
        //#endif

        double maxDist = getMaxInteractionDistance();
        double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distance > maxDist * maxDist) {
            return RemoteResultType.PLAYER_TOO_FAR;
        }

        if (!level.isLoaded(pos)) {
            return RemoteResultType.CONTAINER_NOT_LOADED;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return RemoteResultType.CONTAINER_NOT_FOUND;
        }

        if (!(blockEntity instanceof Container container)) {
            return RemoteResultType.NOT_A_CONTAINER;
        }

        if (slot < 0 || slot >= container.getContainerSize()) {
            return RemoteResultType.SLOT_EMPTY;
        }

        ItemStack stackInSlot = container.getItem(slot);
        if (stackInSlot.isEmpty()) {
            return RemoteResultType.SLOT_EMPTY;
        }

        Item requestedItem = resolveItemFromId(itemIdStr);
        if (requestedItem == null) {
            return RemoteResultType.ITEM_NOT_MATCH;
        }

        if (!stackInSlot.is(requestedItem)) {
            return RemoteResultType.ITEM_NOT_MATCH;
        }

        return giveToPlayer(player, container, slot, stackInSlot);
    }

    private static double getMaxInteractionDistance() {
        try {
            return Configs.Print.REMOTE_INTERACTION_DISTANCE.getDoubleValue();
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

    private static RemoteResultType giveToPlayer(ServerPlayer player, Container container,
                                                  int slot, ItemStack stack) {
        try {
            ItemStack extracted = container.removeItem(slot, stack.getCount());

            if (extracted.isEmpty()) {
                return RemoteResultType.INTERNAL_ERROR;
            }

            if (!player.getInventory().add(extracted)) {
                player.drop(extracted, false);
            }

            return RemoteResultType.SUCCESS;
        } catch (Exception e) {
            return RemoteResultType.INTERNAL_ERROR;
        }
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
