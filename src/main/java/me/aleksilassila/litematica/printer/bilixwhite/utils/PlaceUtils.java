package me.aleksilassila.litematica.printer.bilixwhite.utils;

import com.google.common.collect.Lists;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.mixin.masa.InventoryUtilsAccessor;
import me.aleksilassila.litematica.printer.printer.State;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import static me.aleksilassila.litematica.printer.mixin.masa.InventoryUtilsAccessor.getEmptyPickBlockableHotbarSlot;
import static me.aleksilassila.litematica.printer.mixin.masa.InventoryUtilsAccessor.getPickBlockTargetSlot;

//#if MC >= 12109
import me.aleksilassila.litematica.printer.mixin.bilixwhite.accessors.EasyPlaceUtilsAccessor;
//#else
//$$ import fi.dy.masa.litematica.util.WorldUtils;
//#endif

//#if MC >= 12105
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import net.minecraft.screen.sync.ItemStackHash;
//#endif

import java.util.List;

public class PlaceUtils {
    @NotNull
    static MinecraftClient client = MinecraftClient.getInstance();

    /**
     * 判断该方块是否需要水
     * @param blockState 要判断的方块
     * @return 是否含水（是水）
     */
    public static boolean isWaterRequired(BlockState blockState) {
        return
            blockState.isOf(Blocks.WATER) &&
                    blockState.get(FluidBlock.LEVEL) == 0 || (
                blockState.getProperties().contains(Properties.WATERLOGGED) &&
                blockState.get(Properties.WATERLOGGED)
            ) ||
                blockState.getBlock() instanceof BubbleColumnBlock;
    }

    public static boolean isCorrectWaterLevel(BlockState requiredState, BlockState currentState) {
        if (!currentState.isOf(Blocks.WATER)) return false;
        if (requiredState.isOf(Blocks.WATER) && currentState.get(FluidBlock.LEVEL).equals(requiredState.get(FluidBlock.LEVEL)))
            return true;
        else return currentState.get(FluidBlock.LEVEL) == 0;
    }

    public static boolean isReplaceable(BlockState state) {
        //#if MC < 11904
        //$$ return state.getMaterial().isReplaceable();
        //#else
        return state.isReplaceable();
        //#endif
    }

    public static Direction getFillModeFacing() {
        return switch (LitematicaPrinterMod.FILL_BLOCK_FACING.getOptionListValue().getStringValue()) {
            case "down" -> Direction.DOWN;
            case "east" -> Direction.EAST;
            case "south" -> Direction.SOUTH;
            case "west" -> Direction.WEST;
            case "north" -> Direction.NORTH;
            default -> Direction.UP;
        };
    }


    public static boolean canInteracted(BlockPos blockPos) {
        var range = LitematicaPrinterMod.PRINTER_RANGE.getIntegerValue();
        return switch (LitematicaPrinterMod.ITERATOR_SHAPE.getOptionListValue().getStringValue()) {
            case "sphere" -> canInteractedEuclidean(blockPos, range);
            case "octahedron" -> canInteractedManhattan(blockPos, range);
            default -> true;
        };
    }

    public static boolean canInteractedEuclidean(BlockPos blockPos, double range) {
        var player = client.player;
        if (player == null || blockPos == null) return false;
        return player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos)) <= range * range;
    }

    public static boolean canInteractedManhattan(BlockPos pos, int range) {
        BlockPos center = client.player.getBlockPos();
        int dx = Math.abs(pos.getX() - center.getX());
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = Math.abs(pos.getZ() - center.getZ());
        return dx + dy + dz <= range;
    }

    /**
     * 获取面向这个位置的侦测器的位置。
     * 该方法会检查给定位置周围的六个方向，查找是否有朝向相反方向的观察者方块。
     * 如果找到，则返回该观察者方块的位置；否则返回 null。
     *
     * @param pos 要检查的中心位置
     * @param world 当前的世界对象
     * @return 观察者方块的位置，如果未找到则返回 null
     */
    public static BlockPos getObserverPosition(BlockPos pos, World world) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = world.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof ObserverBlock) {
                Direction facing = neighborState.get(ObserverBlock.FACING);
                if (facing == direction.getOpposite()) {
                    return neighborPos;
                }
            }
        }
        return null;
    }

    /**
     * 获取输出到这个位置的侦测器的位置。
     * 该方法会检查给定位置周围的六个方向，查找是否有朝向该位置方向的观察者方块。
     * 如果找到，则返回该观察者方块的位置；否则返回 null。
     *
     * @param pos 要检查的中心位置
     * @param world 当前的世界对象
     * @return 观察者方块的位置，如果未找到则返回 null
     */
    public static BlockPos getObserverOutputPosition(BlockPos pos, World world) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = world.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof ObserverBlock) {
                Direction facing = neighborState.get(ObserverBlock.FACING);
                if (facing == direction) {
                    return neighborPos;
                }
            }
        }
        return null;
    }

    /**
     * 获取给定位置的侦测器的侦测面方块状态。
     * <p>
     * 如果给定的位置不是侦测器，则返回null
     *
     * @param pos 要检查的位置
     * @return 前方面块的状态组合
     */
    public static State getObverseFacingState(BlockPos pos) {
        BlockState requiredState = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
        if (!(requiredState.getBlock() instanceof ObserverBlock)) return null;
        var obverseFacing = requiredState.get(Properties.FACING);
        var beObverseBlockSchematic = SchematicWorldHandler.getSchematicWorld().getBlockState(pos.offset(obverseFacing));
        var beObverseBlock = client.world.getBlockState(pos.offset(obverseFacing));
        return State.get(beObverseBlockSchematic, beObverseBlock);
    }

    public static boolean setPickedItemToHand(ItemStack stack, MinecraftClient mc)
    {
        if (mc.player == null) return false;
        int slotNum = mc.player.getInventory().getSlotWithStack(stack);
        return setPickedItemToHand(slotNum, stack, mc);
    }

    public static void setHotbarSlot(int slot, PlayerInventory inventory) {
        boolean usePacket = LitematicaPrinterMod.PLACE_USE_PACKET.getBooleanValue();
        if (usePacket) {
            client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
        PreprocessUtils.setSelectedSlot(inventory, slot);
    }

    public static boolean setPickedItemToHand(int sourceSlot, ItemStack stack, MinecraftClient mc) {
        if (mc.player == null) return false;
        PlayerEntity player = mc.player;
        PlayerInventory inventory = player.getInventory();

        if (PlayerInventory.isValidHotbarIndex(sourceSlot)) {
            setHotbarSlot(sourceSlot, inventory);
            return true;
        } else {
            if (InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().isEmpty()) {
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
                return false;
            }

            int hotbarSlot = sourceSlot;

            // 尝试寻找一个空的可拾取方块的热键栏槽位
            if (sourceSlot == -1 || !PlayerInventory.isValidHotbarIndex(sourceSlot)) {
                hotbarSlot = getEmptyPickBlockableHotbarSlot(inventory);
            }

            // 如果没有空槽位，则寻找一个可拾取方块的热键栏槽位
            if (hotbarSlot == -1) {
                hotbarSlot = getPickBlockTargetSlot(player);
            }

            if (hotbarSlot != -1) {
                setHotbarSlot(hotbarSlot, inventory);

                if (EntityUtils.isCreativeMode(player)) {
                    PreprocessUtils.getMainStacks(inventory).set(hotbarSlot, stack.copy());
                    client.interactionManager.clickCreativeStack(client.player.getStackInHand(Hand.MAIN_HAND), 36 + hotbarSlot);
                    return true;
                }
                //#if MC >= 12109
                EasyPlaceUtilsAccessor.callSetEasyPlaceLastPickBlockTime();
                //#else
                //$$ WorldUtils.setEasyPlaceLastPickBlockTime();
                //#endif
                return swapItemToMainHand(stack.copy(), mc);
            } else {
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
                return false;
            }
        }
    }

    public static boolean swapItemToMainHand(ItemStack stackReference, MinecraftClient mc) {
        PlayerEntity player = mc.player;

        //#if MC > 12004
        if (InventoryUtils.areStacksEqualIgnoreNbt(stackReference, player.getMainHandStack())) {
        //#else
        //$$ if (InventoryUtils.areStacksEqual(stackReference, player.getMainHandStack())) {
        //#endif
            return false;
        }

        int slot = InventoryUtils.findSlotWithItem(player.playerScreenHandler, stackReference, true);

        if (slot != -1) {
            int currentHotbarSlot = PreprocessUtils.getSelectedSlot(player.getInventory());
            if (LitematicaPrinterMod.PLACE_USE_PACKET.getBooleanValue()) {
                DefaultedList<Slot> slots = player.currentScreenHandler.slots;
                int totalSlots = slots.size();
                List<ItemStack> copies = Lists.newArrayListWithCapacity(totalSlots);
                for (Slot slotItem : slots) {
                    copies.add(slotItem.getStack().copy());
                }

                Int2ObjectMap<
                        //#if MC >= 12105
                        ItemStackHash
                        //#else
                        //$$ ItemStack
                        //#endif
                        > snapshot = new Int2ObjectOpenHashMap<>();
                for (int j = 0; j < totalSlots; j++) {
                    ItemStack original = copies.get(j);
                    ItemStack current = slots.get(j).getStack();
                    if (!ItemStack.areEqual(original, current)) {
                        snapshot.put(j,
                                //#if MC >=12105
                                ItemStackHash.fromItemStack(current, client.getNetworkHandler().getComponentHasher())
                                //#else
                                //$$ current.copy()
                                //#endif
                        );
                    }
                }

                //#if MC >= 12105
                ItemStackHash itemStackHash = ItemStackHash.fromItemStack(player.currentScreenHandler.getCursorStack(), client.getNetworkHandler().getComponentHasher());
                //#endif
                client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                        player.playerScreenHandler.syncId,
                        player.currentScreenHandler.getRevision(),
                        //#if MC >= 12105
                        Shorts.checkedCast(slot),
                        SignedBytes.checkedCast(currentHotbarSlot),
                        //#else
                        //$$ slot,
                        //$$ currentHotbarSlot,
                        //#endif
                        SlotActionType.SWAP,
                        //#if MC >= 12105
                        snapshot,
                        itemStackHash
                        //#else
                        //$$ player.playerScreenHandler.getCursorStack().copy(),
                        //$$ snapshot
                        //#endif
                ));
                client.player.currentScreenHandler.onSlotClick(slot, currentHotbarSlot, SlotActionType.SWAP, player);
            } else {
                client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot, currentHotbarSlot, SlotActionType.SWAP, player);
            }
            return true;
        }
        return false;
    }
}
