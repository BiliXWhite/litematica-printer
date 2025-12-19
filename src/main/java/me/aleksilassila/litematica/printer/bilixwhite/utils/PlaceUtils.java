package me.aleksilassila.litematica.printer.bilixwhite.utils;

import com.google.common.collect.Lists;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.mixin.masa.InventoryUtilsAccessor;
import me.aleksilassila.litematica.printer.printer.State;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

//#if MC >= 12109
import me.aleksilassila.litematica.printer.mixin.bilixwhite.accessors.EasyPlaceUtilsAccessor;
//#else
//$$ import fi.dy.masa.litematica.util.WorldUtils;
//#endif

//#if MC >= 12105
import net.minecraft.network.HashedStack;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
//#endif
import java.util.List;

public class PlaceUtils {
    @NotNull
    static Minecraft client = Minecraft.getInstance();

    /**
     * 判断该方块是否需要水
     *
     * @param blockState 要判断的方块
     * @return 是否含水（是水）
     */
    public static boolean isWaterRequired(BlockState blockState) {
        return
                blockState.is(Blocks.WATER) &&
                        blockState.getValue(LiquidBlock.LEVEL) == 0 || (
                        blockState.getProperties().contains(BlockStateProperties.WATERLOGGED) &&
                                blockState.getValue(BlockStateProperties.WATERLOGGED)
                ) ||
                        blockState.getBlock() instanceof BubbleColumnBlock;
    }

    public static boolean isCorrectWaterLevel(BlockState requiredState, BlockState currentState) {
        if (!currentState.is(Blocks.WATER)) return false;
        if (requiredState.is(Blocks.WATER) && currentState.getValue(LiquidBlock.LEVEL).equals(requiredState.getValue(LiquidBlock.LEVEL)))
            return true;
        else return currentState.getValue(LiquidBlock.LEVEL) == 0;
    }

    public static boolean isReplaceable(BlockState state) {
        //#if MC < 11904
        //$$ return state.getMaterial().isReplaceable();
        //#else
        return state.canBeReplaced();
        //#endif
    }

    public static Direction getFillModeFacing() {
        if (InitHandler.FILL_BLOCK_FACING.getOptionListValue() instanceof State.FillModeFacingType fillModeFacingType) {
            return switch (fillModeFacingType) {
                case DOWN -> Direction.DOWN;
                case UP -> Direction.UP;
                case WEST -> Direction.WEST;
                case EAST -> Direction.EAST;
                case NORTH -> Direction.NORTH;
                case SOUTH -> Direction.SOUTH;
            };
        }
        return Direction.UP;
    }


    public static double getPlayerBlockInteractionRange(double defaultRange) {
        //#if MC>=12005
        if (client.player != null) {
            return client.player.blockInteractionRange();
        }
        //#else
        //$$ if (client.gameMode != null) {
        //$$    return client.gameMode.getPickRange();
        //$$ }
        //#endif
        return defaultRange;
    }

    public static double getPlayerBlockInteractionRange() {
        return getPlayerBlockInteractionRange(4.5F);
    }

    // 判断是否可交互
    public static boolean canInteracted(BlockPos blockPos) {
        int workRange = InitHandler.PRINTER_RANGE.getIntegerValue();
        // TODO: 临时性注释, 后续添加配置做成可配置的
//        int playerRange = (int) Math.ceil(getPlayerBlockInteractionRange());
//        int range = Math.min(workRange, playerRange);
        int range = workRange;
        if (InitHandler.ITERATOR_SHAPE.getOptionListValue() instanceof State.RadiusShapeType radiusShapeType) {
            return switch (radiusShapeType) {
                case SPHERE -> canInteractedEuclidean(blockPos, range);
                case OCTAHEDRON -> canInteractedManhattan(blockPos, range);
                case CUBE -> canInteractedCube(blockPos, range);
            };
        }
        return true;
    }

    // 球面（欧几里得距离）
    public static boolean canInteractedEuclidean(BlockPos blockPos, double range) {
        var player = client.player;
        if (player == null || blockPos == null) return false;
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(blockPos)) <= range * range;
    }

    // 八面体（曼哈顿距离）
    public static boolean canInteractedManhattan(BlockPos pos, int range) {
        BlockPos center = client.player.blockPosition();
        int dx = Math.abs(pos.getX() - center.getX());
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = Math.abs(pos.getZ() - center.getZ());
        return dx + dy + dz <= range;
    }

    // 立方体（CUBE）：以玩家方块位置为中心
    public static boolean canInteractedCube(BlockPos pos, int range) {
        var player = client.player;
        if (player == null || pos == null) return false;
        BlockPos center = player.blockPosition();
        int dx = Math.abs(pos.getX() - center.getX());
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = Math.abs(pos.getZ() - center.getZ());
        return dx <= range && dy <= range && dz <= range;
    }

    /**
     * 获取面向这个位置的侦测器的位置。
     * 该方法会检查给定位置周围的六个方向，查找是否有朝向相反方向的观察者方块。
     * 如果找到，则返回该观察者方块的位置；否则返回 null。
     *
     * @param pos   要检查的中心位置
     * @param world 当前的世界对象
     * @return 观察者方块的位置，如果未找到则返回 null
     */
    public static BlockPos getObserverPosition(BlockPos pos, Level world) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = world.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof ObserverBlock) {
                Direction facing = neighborState.getValue(ObserverBlock.FACING);
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
     * @param pos   要检查的中心位置
     * @param world 当前的世界对象
     * @return 观察者方块的位置，如果未找到则返回 null
     */
    public static BlockPos getObserverOutputPosition(BlockPos pos, Level world) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = world.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof ObserverBlock) {
                Direction facing = neighborState.getValue(ObserverBlock.FACING);
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
        var obverseFacing = requiredState.getValue(BlockStateProperties.FACING);
        var beObverseBlockSchematic = SchematicWorldHandler.getSchematicWorld().getBlockState(pos.relative(obverseFacing));
        var beObverseBlock = client.level.getBlockState(pos.relative(obverseFacing));
        return State.get(beObverseBlockSchematic, beObverseBlock);
    }

    public static boolean setPickedItemToHand(ItemStack stack, Minecraft mc) {
        if (mc.player == null) return false;
        int slotNum = mc.player.getInventory().findSlotMatchingItem(stack);
        return setPickedItemToHand(slotNum, stack, mc);
    }

    public static void setHotbarSlot(int slot, Inventory inventory) {
        boolean usePacket = InitHandler.PLACE_USE_PACKET.getBooleanValue();
        if (usePacket) {
            client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }
        PreprocessUtils.setSelectedSlot(inventory, slot);
    }

    public static boolean setPickedItemToHand(int sourceSlot, ItemStack stack, Minecraft mc) {
        if (mc.player == null) return false;
        Player player = mc.player;
        Inventory inventory = player.getInventory();

        if (Inventory.isHotbarSlot(sourceSlot)) {
            setHotbarSlot(sourceSlot, inventory);
            return true;
        } else {
            if (InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().isEmpty()) {
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
                return false;
            }
            int hotbarSlot = sourceSlot;
            // 尝试寻找一个空的可拾取方块的热键栏槽位
            if (sourceSlot == -1 || !Inventory.isHotbarSlot(sourceSlot)) {
                hotbarSlot = InventoryUtilsAccessor.getEmptyPickBlockableHotbarSlot(inventory);
            }
            // 如果没有空槽位，则寻找一个可拾取方块的热键栏槽位
            if (hotbarSlot == -1) {
                hotbarSlot = InventoryUtilsAccessor.getPickBlockTargetSlot(player);
            }
            if (hotbarSlot != -1) {
                setHotbarSlot(hotbarSlot, inventory);
                if (EntityUtils.isCreativeMode(player)) {
                    PreprocessUtils.getMainStacks(inventory).set(hotbarSlot, stack.copy());
                    client.gameMode.handleCreativeModeItemAdd(client.player.getMainHandItem(), 36 + hotbarSlot);
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

    public static boolean swapItemToMainHand(ItemStack stackReference, Minecraft mc) {
        Player player = mc.player;
        //#if MC > 12004
        if (InventoryUtils.areStacksEqualIgnoreNbt(stackReference, player.getMainHandItem())) {
            //#else
            //$$ if (InventoryUtils.areStacksEqual(stackReference, player.getMainHandItem())) {
            //#endif
            return false;
        }
        int slot = InventoryUtils.findSlotWithItem(player.inventoryMenu, stackReference, true);
        if (slot != -1) {
            int currentHotbarSlot = PreprocessUtils.getSelectedSlot(player.getInventory());
            if (InitHandler.PLACE_USE_PACKET.getBooleanValue()) {
                NonNullList<Slot> slots = player.inventoryMenu.slots;
                int totalSlots = slots.size();
                List<ItemStack> copies = Lists.newArrayListWithCapacity(totalSlots);
                for (Slot slotItem : slots) {
                    copies.add(slotItem.getItem().copy());
                }
                Int2ObjectMap<
                        //#if MC >= 12105
                        HashedStack
                        //#else
                        //$$ ItemStack
                        //#endif
                        > snapshot = new Int2ObjectOpenHashMap<>();
                for (int j = 0; j < totalSlots; j++) {
                    ItemStack original = copies.get(j);
                    ItemStack current = slots.get(j).getItem();
                    if (!ItemStack.isSameItem(original, current)) {
                        snapshot.put(j,
                                //#if MC >=12105
                                HashedStack.create(current, client.getConnection().decoratedHashOpsGenenerator())
                                //#else
                                //$$ current.copy()
                                //#endif
                        );
                    }
                }
                //#if MC >= 12105
                HashedStack hashedStack = HashedStack.create(player.inventoryMenu.getCarried(), client.getConnection().decoratedHashOpsGenenerator());
                //#endif
                client.getConnection().send(new ServerboundContainerClickPacket(
                        player.inventoryMenu.containerId,
                        player.inventoryMenu.getStateId(),
                        //#if MC >= 12105
                        Shorts.checkedCast(slot),
                        SignedBytes.checkedCast(currentHotbarSlot),
                        //#else
                        //$$ slot,
                        //$$ currentHotbarSlot,
                        //#endif
                        ClickType.SWAP,
                        //#if MC >= 12105
                        snapshot,
                        hashedStack
                        //#else
                        //$$ player.inventoryMenu.getCarried().copy(),
                        //$$ snapshot
                        //#endif
                ));
                client.player.inventoryMenu.clicked(slot, currentHotbarSlot, ClickType.SWAP, player);
            } else {
                client.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, slot, currentHotbarSlot, ClickType.SWAP, player);
            }
            return true;
        }
        return false;
    }
}
