package me.aleksilassila.litematica.printer.utils;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.BlockMatchingType;
import me.aleksilassila.litematica.printer.enums.QuickShulkerModeType;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import net.kyrptonaught.quickshulker.client.ClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BlockUtils {
    @NotNull public static final Minecraft client = Minecraft.getInstance();
    private static final BooleanProperty wallUpProperty = WallBlock.UP;
    //#if MC > 12104
    private static final EnumProperty<WallSide> wallNorthProperty = WallBlock.NORTH;
    private static final EnumProperty<WallSide> wallSouthProperty = WallBlock.SOUTH;
    private static final EnumProperty<WallSide> wallWestProperty = WallBlock.WEST;
    private static final EnumProperty<WallSide> wallEastProperty = WallBlock.EAST;
    //#else
    //$$ private final static EnumProperty<WallSide> wallNorthProperty = WallBlock.NORTH_WALL;
    //$$ private final static EnumProperty<WallSide> wallSouthProperty = WallBlock.SOUTH_WALL;
    //$$ private final static EnumProperty<WallSide> wallWestProperty = WallBlock.WEST_WALL;
    //$$ private final static EnumProperty<WallSide> wallEastProperty = WallBlock.EAST_WALL;
    //#endif

    private static final float YAW_MIN = -180.0F;
    private static final float YAW_MAX = 180.0F;
    private static final int ROTATION_MIN = 0;
    private static final int ROTATION_MAX = 15;
    private static final float ROTATION_TO_YAW_FACTOR = 22.5F;

    public static Direction[] horizontalDirections =
            new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public static boolean isReplaceable(BlockState blockState) {
        //#if MC > 11902
        return blockState.canBeReplaced();
        //#else
        //$$ return blockState.getMaterial().isReplaceable();
        //#endif
    }

    public static @NotNull Block getBlock(Identifier blockId) {
        //#if MC > 12101
        return BuiltInRegistries.BLOCK.getValue(blockId);
        //#else
        //$$ return BuiltInRegistries.BLOCK.get(blockId);
        //#endif
    }

    public static Identifier getKey(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    public static String getKeyString(Block block) {
        return getKey(block).toString();
    }

    public static boolean statesEqualIgnoreProperties(
            BlockState state1, BlockState state2, Property<?>... propertiesToIgnore) {
        if (state1.getBlock() != state2.getBlock()) {
            return false;
        }
        loop:
        for (Property<?> property : state1.getProperties()) {
            if (property == BlockStateProperties.WATERLOGGED
                    && !(state1.getBlock() instanceof CoralPlantBlock)
                    && state2.getValue(BlockStateProperties.WATERLOGGED)) {
                continue;
            }
            for (Property<?> ignoredProperty : propertiesToIgnore) {
                if (property == ignoredProperty) {
                    continue loop;
                }
            }
            try {
                if (!state1.getValue(property).equals(state2.getValue(property))) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    public static <T extends Comparable<T>> Optional<T> getProperty(
            BlockState blockState, Property<T> property) {
        if (blockState.hasProperty(property)) {
            return Optional.of(blockState.getValue(property));
        }
        return Optional.empty();
    }

    public static Comparable<?> getPropertyByName(BlockState state, String name) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equalsIgnoreCase(name)) {
                return state.getValue(prop);
            }
        }
        return null;
    }

    public static Optional<Property<?>> getWallFacingProperty(Direction wallFacing) {
        switch (wallFacing) {
            case UP:
                return Optional.of(wallUpProperty);
            case NORTH:
                return Optional.of(wallNorthProperty);
            case SOUTH:
                return Optional.of(wallSouthProperty);
            case WEST:
                return Optional.of(wallWestProperty);
            case EAST:
                return Optional.of(wallEastProperty);
        }
        return Optional.empty();
    }

    public static Optional<Property<?>> getCrossCollisionBlock(Direction wallFacing) {
        switch (wallFacing) {
            case NORTH:
                return Optional.of(wallNorthProperty);
            case SOUTH:
                return Optional.of(wallSouthProperty);
            case WEST:
                return Optional.of(wallWestProperty);
            case EAST:
                return Optional.of(wallEastProperty);
        }
        return Optional.empty();
    }

    /**
     * 判断该方块是否需要水
     *
     * @param blockState 要判断的方块
     * @return 是否含水（是水）
     */
    public static boolean isWaterBlock(BlockState blockState) {
        return blockState.is(Blocks.WATER) && blockState.getValue(LiquidBlock.LEVEL) == 0
                || (blockState.getProperties().contains(BlockStateProperties.WATERLOGGED)
                        && blockState.getValue(BlockStateProperties.WATERLOGGED))
                || blockState.getBlock() instanceof BubbleColumnBlock
                || blockState.getBlock() instanceof SeagrassBlock;
    }

    public static boolean isCorrectWaterLevel(BlockState requiredState, BlockState currentState) {
        if (!currentState.is(Blocks.WATER)) return false;
        if (requiredState.is(Blocks.WATER)
                && currentState
                        .getValue(LiquidBlock.LEVEL)
                        .equals(requiredState.getValue(LiquidBlock.LEVEL))) {
            return true;
        } else {
            return currentState.getValue(LiquidBlock.LEVEL) == 0;
        }
    }

    public static float getRequiredYaw(Direction playerShouldBeFacing) {
        if (playerShouldBeFacing != null && playerShouldBeFacing.getAxis().isHorizontal()) {
            return playerShouldBeFacing.toYRot();
        } else {
            return 0;
        }
    }

    public static float getRequiredPitch(Direction playerShouldBeFacing) {
        if (playerShouldBeFacing != null && playerShouldBeFacing.getAxis().isVertical()) {
            return playerShouldBeFacing == Direction.DOWN ? 90 : -90;
        } else {
            return 0;
        }
    }

    public static Vec3i getVector(Direction direction) {
        //#if MC >= 12103
        return direction.getUnitVec3i();
        //#else
        //$$ return direction.getNormal();
        //#endif
    }

    public static Direction[] orderedByNearest(float yaw, float pitch) {
        double pitchRad = pitch * (Math.PI / 180.0);
        double yawRad = -yaw * (Math.PI / 180.0);
        float sinPitch = (float) Math.sin(pitchRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosYaw = (float) Math.cos(yawRad);
        boolean isEastFacing = sinYaw > 0.0F;
        boolean isUpFacing = sinPitch < 0.0F;
        boolean isSouthFacing = cosYaw > 0.0F;
        float eastWestMagnitude = isEastFacing ? sinYaw : -sinYaw;
        float upDownMagnitude = isUpFacing ? -sinPitch : sinPitch;
        float northSouthMagnitude = isSouthFacing ? cosYaw : -cosYaw;
        float adjustedX = eastWestMagnitude * cosPitch;
        float adjustedZ = northSouthMagnitude * cosPitch;
        Direction primaryXDirection = isEastFacing ? Direction.EAST : Direction.WEST;
        Direction primaryYDirection = isUpFacing ? Direction.UP : Direction.DOWN;
        Direction primaryZDirection = isSouthFacing ? Direction.SOUTH : Direction.NORTH;
        if (eastWestMagnitude > northSouthMagnitude) {
            if (upDownMagnitude > adjustedX) {
                return makeDirectionArray(primaryYDirection, primaryXDirection, primaryZDirection);
            } else {
                return adjustedZ > upDownMagnitude
                        ? makeDirectionArray(primaryXDirection, primaryZDirection, primaryYDirection)
                        : makeDirectionArray(primaryXDirection, primaryYDirection, primaryZDirection);
            }
        } else if (upDownMagnitude > adjustedZ) {
            return makeDirectionArray(primaryYDirection, primaryZDirection, primaryXDirection);
        } else {
            return adjustedX > upDownMagnitude
                    ? makeDirectionArray(primaryZDirection, primaryXDirection, primaryYDirection)
                    : makeDirectionArray(primaryZDirection, primaryYDirection, primaryXDirection);
        }
    }

    private static Direction[] makeDirectionArray(Direction dir1, Direction dir2, Direction dir3) {
        return new Direction[]{dir1, dir2, dir3, dir3.getOpposite(), dir2.getOpposite(), dir1.getOpposite()};
    }

    public static Direction getHorizontalDirection(float yaw) {
        return Direction.fromYRot(yaw);
    }

    /**
     * 将方块Rotation转换为玩家视角Yaw（带范围防范）
     *
     * @param rotation 方块Rotation值（会自动修正到0-15范围）
     * @return 玩家视角Yaw值（严格约束在[-180, 180]）
     */
    public static float rotationToPlayerYaw(int rotation) {
        // 防范性处理：确保rotation在0-15范围内（即使传入非法值也能修正）
        rotation = clampRotation(rotation);
        float blockFrontYaw = rotation * ROTATION_TO_YAW_FACTOR;
        float playerLookYaw = blockFrontYaw + 180.0F;
        // 强制归一化到[-180, 180]（核心防范逻辑）
        playerLookYaw = normalizeYaw(playerLookYaw);
        return playerLookYaw;
    }

    /**
     * 获取方块Rotation的相反方向Rotation（带范围防范）
     *
     * @param rotation 方块Rotation值（会自动修正到0-15范围）
     * @return 相反方向的Rotation（严格约束在0-15）
     */
    public static int getOppositeRotation(int rotation) {
        // 先修正输入的Rotation范围
        rotation = clampRotation(rotation);
        float playerYaw = rotationToPlayerYaw(rotation);
        float oppositeYaw = getOppositeYaw(playerYaw);
        // 转换回Rotation前，先归一化Yaw，再计算
        float normalizedYaw = oppositeYaw < 0.0F ? oppositeYaw + 360.0F : oppositeYaw;
        float blockFrontYaw = normalizedYaw - 180.0F;
        if (blockFrontYaw < 0.0F) {
            blockFrontYaw += 360.0F;
        }
        int oppositeRotation = Math.round(blockFrontYaw / ROTATION_TO_YAW_FACTOR);
        // 最后再修正Rotation范围，双重保障
        oppositeRotation = clampRotation(oppositeRotation);
        return oppositeRotation;
    }

    /**
     * 获取玩家视角Yaw的相反方向Yaw（带范围防范）
     *
     * @param playerLookYaw 玩家视角Yaw（任意值都能被修正）
     * @return 相反方向Yaw（严格约束在[-180, 180]）
     */
    public static float getOppositeYaw(float playerLookYaw) {
        // 先归一化输入的Yaw（即使传入非法值，比如500、-400也能修正）
        playerLookYaw = normalizeYaw(playerLookYaw);
        float oppositeYaw = playerLookYaw + 180.0F;
        // 再次归一化，确保输出在[-180, 180]
        oppositeYaw = normalizeYaw(oppositeYaw);
        return oppositeYaw;
    }

    /**
     * 将任意Yaw值强制归一化到[-180, 180]范围（核心防范方法）
     *
     * @param yaw 任意浮点数的Yaw值（比如370、-200、500等）
     * @return 归一化后的Yaw值（严格在[-180, 180]）
     */
    private static float normalizeYaw(float yaw) {
        // 先取模360，将值约束到[-360, 360]
        yaw = yaw % 360.0F;
        // 再调整到[-180, 180]
        if (yaw > YAW_MAX) {
            yaw -= 360.0F;
        } else if (yaw < YAW_MIN) {
            yaw += 360.0F;
        }
        return yaw;
    }

    /**
     * 将任意整数Rotation强制约束到0-15范围（核心防范方法）
     *
     * @param rotation 任意整数（比如-5、20、100等）
     * @return 修正后的Rotation（严格在0-15）
     */
    private static int clampRotation(int rotation) {
        // 取模16，将值约束到[-15, 15]
        rotation = rotation % (ROTATION_MAX + 1);
        // 处理负数，修正到0-15
        if (rotation < ROTATION_MIN) {
            rotation += (ROTATION_MAX + 1);
        }
        return rotation;
    }

    public static void openShulker(ItemStack stack, int shulkerBoxSlot) {
        if (Configs.Placement.QUICK_SHULKER_MODE.getOptionListValue()
                == QuickShulkerModeType.CLICK_SLOT) {
            client.gameMode.handleInventoryMouseClick(
                    client.player.containerMenu.containerId,
                    shulkerBoxSlot,
                    1,
                    ClickType.PICKUP,
                    client.player);
        } else if (Configs.Placement.QUICK_SHULKER_MODE.getOptionListValue()
                == QuickShulkerModeType.INVOKE) {
            if (ModUtils.isQuickShulkerLoaded()) {
                try {
                    ClientUtil.CheckAndSend(stack, shulkerBoxSlot);
                } catch (Exception ignored) {
                }
            } else MessageUtils.addMessage(MessageUtils.literal("快捷潜影盒模组未加载！"));
        }
    }

    public static boolean canBeClicked(ClientLevel world, BlockPos pos) {
        return getOutlineShape(world, pos) != Shapes.empty();
    }

    public static VoxelShape getOutlineShape(ClientLevel world, BlockPos pos) {
        return world.getBlockState(pos).getShape(world, pos);
    }

    public static Map<Direction, Vec3> getSlabSides(
            Level world, BlockPos pos, SlabType requiredHalf) {
        if (requiredHalf == SlabType.DOUBLE) requiredHalf = SlabType.BOTTOM;
        Direction requiredDir = requiredHalf == SlabType.TOP ? Direction.UP : Direction.DOWN;
        Map<Direction, Vec3> sides = new HashMap<>();
        sides.put(requiredDir, new Vec3(0, 0, 0));
        if (world.getBlockState(pos).hasProperty(SlabBlock.TYPE)) {
            sides.put(
                    requiredDir.getOpposite(),
                    Vec3.atLowerCornerOf(getVector(requiredDir)).scale(0.5));
        }
        for (Direction side : horizontalDirections) {
            BlockState neighborCurrentState = world.getBlockState(pos.relative(side));
            if (neighborCurrentState.hasProperty(SlabBlock.TYPE)
                    && neighborCurrentState.getValue(SlabBlock.TYPE) != SlabType.DOUBLE) {
                if (neighborCurrentState.getValue(SlabBlock.TYPE) != requiredHalf) {
                    continue;
                }
            }
            sides.put(side, Vec3.atLowerCornerOf(getVector(requiredDir)).scale(0.25));
        }
        return sides;
    }


    public static boolean checkObserverChain(SchematicBlockContext start) {
        SchematicBlockContext temp = start;
        while (temp.requiredState.getBlock() instanceof ObserverBlock) {
            @Nullable
            Direction tempObserverFacing = temp.getRequiredStateProperty(ObserverBlock.FACING).orElse(null);
            SchematicBlockContext offset = temp.offset(tempObserverFacing);
            if (tempObserverFacing != null && BlockMatchingType.get(offset) != BlockMatchingType.CORRECT) {
                return false;
            }
            temp = offset;
        }
        return true;
    }
}