package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PreprocessUtils;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.utils.BlockUtils;
import me.aleksilassila.litematica.printer.utils.DirectionUtils;
import me.aleksilassila.litematica.printer.utils.ResourceLocationUtils;
import net.fabricmc.fabric.mixin.content.registry.AxeItemAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlacementGuide extends PrinterUtils {
    private static final Map<Block, Block> STRIPPED_LOGS = AxeItemAccessor.getStrippedBlocks();
    @NotNull
    protected final Minecraft mc;
    Item[] compostableItems = Arrays.stream(ComposterBlock.COMPOSTABLES.keySet().toArray(ItemLike[]::new))
            .map(ItemLike::asItem)
            .toArray(Item[]::new);


    public PlacementGuide(@NotNull Minecraft client) {
        this.mc = client;
    }

    public @Nullable Action getAction(Level world, WorldSchematic worldSchematic, BlockPos pos) {
        var requiredState = worldSchematic.getBlockState(pos);
        // 提前缓存 requiredState 提升性能
        var state = State.get(requiredState, world.getBlockState(pos));
        if (!requiredState.canSurvive(world, pos) || state == State.CORRECT)
            return null;
        for (ClassHook hook : ClassHook.values()) {
            for (Class<?> clazz : hook.classes) {
                if (clazz != null && clazz.isInstance(requiredState.getBlock())) {
                    @Nullable Action action = buildAction(world, worldSchematic, hook, pos, state);
                    if (action != null) {   // 珊瑚直接使用了 Block.class, 为了传递性所以只有不为null时进行返回
                        return action;
                    }
                }
            }
        }
        return buildAction(world, worldSchematic, ClassHook.DEFAULT, pos, state); // 兜底处理
    }

    private @Nullable Action buildAction(Level world, WorldSchematic worldSchematic, ClassHook requiredType, BlockPos pos, State state) {
        BlockState currentState = world.getBlockState(pos);
        BlockState requiredState = worldSchematic.getBlockState(pos);

        if (InitHandler.SKIP_WATERLOGGED_BLOCK.getBooleanValue() && PlaceUtils.isWaterRequired(requiredState))
            return null;

        if (InitHandler.PRINT_WATER.getBooleanValue() && PlaceUtils.isWaterRequired(requiredState)) {
            if (currentState.getBlock() instanceof IceBlock) {
                BreakManager.addBlockToBreak(pos);
                return null;
            }
            if (!PlaceUtils.isCorrectWaterLevel(requiredState, currentState)) {
                if (!currentState.isAir() && !(currentState.getBlock() instanceof LiquidBlock)) {
                    if (InitHandler.BREAK_WRONG_BLOCK.getBooleanValue()) {
                        BreakManager.addBlockToBreak(pos);
                    }
                    return null;
                }
                return new Action().setItem(Items.ICE);
            }
        }


        if (state == State.MISSING_BLOCK) switch (requiredType) {
            case TORCH -> {
                return Optional.ofNullable(requiredState.hasProperty(WallTorchBlock.FACING) ?
                                requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING) :
                                null)

                        .map(direction -> new Action()
                                .setSides(direction.getOpposite())
                                .setLookDirection(direction)
                                .setRequiresSupport()
                        )
                        .orElseGet(() -> new Action().setSides(Direction.DOWN).setLookDirection(Direction.DOWN));

            }
            case AMETHYST -> {
                return new Action()
                        .setSides((requiredState.getValue(BlockStateProperties.FACING))
                                .getOpposite())
                        .setRequiresSupport();
            }
            case SLAB -> {
                return new Action().setSides(getSlabSides(world, pos, requiredState.getValue(SlabBlock.TYPE)));
            }
            case STAIR -> {
                Direction facing = requiredState.getValue(StairBlock.FACING);
                Half half = requiredState.getValue(StairBlock.HALF);

                Map<Direction, Vec3> sides = new HashMap<>();
                if (half == Half.BOTTOM) {
                    sides.put(Direction.DOWN, new Vec3(0, 0, 0));
                    sides.put(facing, new Vec3(0, 0, 0));
                } else {
                    sides.put(Direction.UP, new Vec3(0, 0.75, 0));
                    sides.put(facing.getOpposite(), new Vec3(0, 0.75, 0));
                }

                return new Action()
                        .setSides(sides)
                        .setLookDirection(facing);
            }
            case TRAPDOOR -> {

                return new Action()
                        .setSides(getHalf(requiredState.getValue(TrapDoorBlock.HALF)))
                        .setLookDirection(requiredState.getValue(TrapDoorBlock.FACING).getOpposite());
            }
            case STRIP_LOG -> {
                Action action = new Action().setSides(requiredState.getValue(RotatedPillarBlock.AXIS));
                Item[] items = {requiredState.getBlock().asItem()};

                if (InitHandler.STRIP_LOGS.getBooleanValue()) {
                    for (Map.Entry<Block, Block> entry : STRIPPED_LOGS.entrySet()) {
                        if (requiredState.getBlock() == entry.getValue()) {
                            items = new Item[]{entry.getValue().asItem(), entry.getKey().asItem()};
                            break;
                        }
                    }
                }

                action.setItems(items);
                return action;
            }
            case ANVIL -> {
                return new Action().setLookDirection(requiredState.getValue(AnvilBlock.FACING).getCounterClockWise());
            }
            case HOPPER -> {
                Direction facing = requiredState.getValue(HopperBlock.FACING);
                return new Action().setSides(facing);
            }
            case NETHER_PORTAL -> {

                boolean canCreatePortal = PortalShape.findEmptyPortalShape(world, pos, Direction.Axis.X).isPresent();
                if (canCreatePortal) {
                    return new Action().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                }
            }
            case COCOA -> {
                return new Action().setSides(requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING));
            }
            //#if MC >= 12003
            case CRAFTER -> {
                var frontAndTop = requiredState.getValue(BlockStateProperties.ORIENTATION);
                Direction facing = frontAndTop.front().getOpposite();
                Direction rotation = frontAndTop.top().getOpposite();
                if (facing == Direction.UP) {
                    return new Action().setLookDirection(rotation, Direction.UP);
                } else if (facing == Direction.DOWN) {
                    return new Action().setLookDirection(rotation.getOpposite(), Direction.DOWN);
                } else {
                    return new Action().setLookDirection(facing, facing);
                }
            }
            //#endif
            case CHEST -> {
                Direction facing = requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
                ChestType type = requiredState.getValue(BlockStateProperties.CHEST_TYPE);
                Map<Direction, Vec3> noChestSides = new HashMap<>();

                for (Direction side : Direction.values()) {
                    if (world.getBlockState(pos.relative(side)).getBlock() instanceof ChestBlock) {
                        continue;
                    }
                    noChestSides.put(side, Vec3.ZERO);
                }


                if (type == ChestType.SINGLE) {
                    for (Direction side : BlockStateProperties.HORIZONTAL_FACING.getPossibleValues()) {
                        if (!noChestSides.containsKey(side)) {
                            return new Action().setLookDirection(facing).setUseShift();
                        }
                        return new Action().setSides(noChestSides).setLookDirection(facing);
                    }
                } else {
                    Direction chestFacing = facing;
                    if (type == ChestType.LEFT) {
                        chestFacing = facing.getCounterClockWise();
                    } else if (type == ChestType.RIGHT) {
                        chestFacing = facing.getClockWise();
                    }
                    if (world.getBlockState(pos.relative(chestFacing)).getBlock() instanceof ChestBlock) {
                        return new Action().setSides(Map.of(chestFacing, Vec3.ZERO)).setLookDirection(facing).setUseShift(false);
                    } else {
                        return new Action().setSides(noChestSides).setLookDirection(facing).setUseShift();
                    }
                }
            }
            case BED -> {
                if (requiredState.getValue(BedBlock.PART) == BedPart.FOOT)
                    return new Action().setLookDirection(requiredState.getValue(BedBlock.FACING));
            }
            case BELL -> {
                Direction side;
                switch (requiredState.getValue(BellBlock.ATTACHMENT)) {
                    case FLOOR -> side = Direction.DOWN;
                    case CEILING -> side = Direction.UP;
                    default -> side = requiredState.getValue(BellBlock.FACING);
                }

                Direction look = requiredState.getValue(BellBlock.ATTACHMENT) != BellAttachType.SINGLE_WALL &&
                        requiredState.getValue(BellBlock.ATTACHMENT) != BellAttachType.DOUBLE_WALL ?
                        requiredState.getValue(BellBlock.FACING) : null;

                return new Action().setSides(side).setLookDirection(look);
            }
            case DOOR -> {
                Direction facing = requiredState.getValue(DoorBlock.FACING);
                DoorHingeSide hinge = requiredState.getValue(DoorBlock.HINGE);
                BlockPos upperPos = pos.above();

                // 获取门铰链方向
                Direction hingeSide = facing.getCounterClockWise();

                double offset = hinge == DoorHingeSide.RIGHT ? 0.25 : -0.25;
                Vec3 hingeVec = facing.getAxis() == Direction.Axis.X ? new Vec3(0, 0, offset) : new Vec3(offset, 0, 0);

                Map<Direction, Vec3> sides = new HashMap<>();
                sides.put(hingeSide, Vec3.ZERO); // 靠墙方向需要支撑
                sides.put(Direction.DOWN, hingeVec); // 底部点击偏移
                sides.put(facing, hingeVec); // 正面点击偏移

                // 获取左右方块状态
                Direction left = facing.getCounterClockWise();
                Direction right = facing.getCounterClockWise();
                BlockState leftState = world.getBlockState(pos.relative(left));
                BlockState leftUpperState = world.getBlockState(upperPos.relative(left));
                BlockState rightState = world.getBlockState(pos.relative(right));
                BlockState rightUpperState = world.getBlockState(upperPos.relative(right));

                int occupancy = (leftState.isCollisionShapeFullBlock(world, pos.relative(left)) ? -1 : 0)
                        + (leftUpperState.isCollisionShapeFullBlock(world, upperPos.relative(left)) ? -1 : 0)
                        + (rightState.isCollisionShapeFullBlock(world, pos.relative(right)) ? 1 : 0)
                        + (rightUpperState.isCollisionShapeFullBlock(world, upperPos.relative(right)) ? 1 : 0);

                boolean isLeftDoor = leftState.getBlock() instanceof DoorBlock &&
                        leftState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
                boolean isRightDoor = rightState.getBlock() instanceof DoorBlock &&
                        rightState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;

                boolean condition = (hinge == DoorHingeSide.RIGHT && ((isLeftDoor && !isRightDoor) || occupancy > 0))
                        || (hinge == DoorHingeSide.LEFT && ((isRightDoor && !isLeftDoor) || occupancy < 0))
                        || (occupancy == 0 && (isLeftDoor == isRightDoor));
                if (condition)
                    return new Action().setSides(sides).setLookDirection(facing).setRequiresSupport();
            }
            case DIRT_PATH, FARMLAND -> {
                return new Action().setItems(Items.DIRT, Items.GRASS_BLOCK, Items.COARSE_DIRT, Items.ROOTED_DIRT, Items.MYCELIUM, Items.PODZOL);
            }
            case BIG_DRIPLEAF_STEM -> {
                return new Action().setItem(Items.BIG_DRIPLEAF);
            }
            case CAVE_VINES -> {
                return new Action().setItem(Items.GLOW_BERRIES).setRequiresSupport();
            }
            case WEEPING_VINES -> {
                return new Action().setItem(Items.WEEPING_VINES).setRequiresSupport();
            }
            case TWISTING_VINES -> {
                return new Action().setItem(Items.TWISTING_VINES).setRequiresSupport();
            }
            case FLOWER_POT -> {
                return new Action().setItem(Items.FLOWER_POT);
            }
            case VINES, GLOW_LICHEN -> {
                for (Direction direction : Direction.values()) {
                    if (direction == Direction.DOWN && requiredState.getBlock() == Blocks.VINE) continue;
                    if ((Boolean) getPropertyByName(requiredState, direction.name())) {
                        return new Action().setSides(direction);
                    }
                }
            }
            case DEAD_CORAL -> {
                Block block = requiredState.getBlock();
                ResourceLocation blockId1 = BlockUtils.getIdentifier(block);
                if (!blockId1.toString().contains("coral")) {
                    return null;
                }
                ResourceLocation blockId2 = ResourceLocationUtils.of(blockId1.toString().replace("dead_", ""));
                boolean isBlock = blockId1.toString().contains("block");
                boolean isWallFan = block instanceof BaseCoralWallFanBlock;
                Direction facing = isWallFan ? requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()
                        : Direction.DOWN;
                List<Item> items = new ArrayList<>();
                items.add(block.asItem());
                if (InitHandler.REPLACE_CORAL.getBooleanValue()) {
                    if (!blockId1.equals(blockId2)) {
                        items.add(BlockUtils.getBlock(blockId2).asItem());
                    }
                }
                Item[] itemsArray = items.toArray(new Item[0]);

                Action action = new Action().setItems(itemsArray);
                if (!isBlock) {
                    action.setSides(facing).setRequiresSupport();
                }
                return action;
            }
            case FIRE -> {
                if (requiredState.getBlock() instanceof SoulFireBlock)
                    return new Action().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                for (Direction direction : Direction.values()) {
                    if (direction == Direction.DOWN) continue;
                    if ((Boolean) getPropertyByName(requiredState, direction.name())) {
                        return new Action().setSides(direction).setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                    }
                }
                return new Action().setSides(Direction.DOWN).setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
            }
            case OBSERVER -> {
                var facing = requiredState.getValue(BlockStateProperties.FACING);
                var beObserveBlockState = world.getBlockState(pos.relative(facing));
                var outputBlockPos = pos.relative(facing.getOpposite());
                BlockState outputState = worldSchematic.getBlockState(outputBlockPos);
                BlockPos observerPosSchematic = PlaceUtils.getObserverPosition(pos, worldSchematic);
                if (InitHandler.SAFELY_OBSERVER.getBooleanValue()) {
                    if (State.get(pos.relative(facing)) == State.CORRECT) {
                        // 如果侦测面也是侦测器，那么检查这个侦测器的侦测面是否正确
                        if (beObserveBlockState.getBlock() instanceof ObserverBlock) {
                            if (State.get(pos.relative(facing).relative(beObserveBlockState.getValue(BlockStateProperties.FACING))) == State.CORRECT) {
                                return new Action().setWaitTick(2).setLookDirection(facing);
                            } else {
                                return null;
                            }
                        }
                        return new Action().setLookDirection(facing);
                    } else if (facing == Direction.UP || outputState.isAir())
                        if (observerPosSchematic != null) {
                            if (State.get(observerPosSchematic) == State.MISSING_BLOCK)
                                return new Action().setLookDirection(facing);
                        } else return new Action().setLookDirection(facing);
                    return null;
                }

                return new Action().setLookDirection(facing);
            }
            case LADDER -> {
                var facing = requiredState.getValue(LadderBlock.FACING);
                return new Action().setSides(facing).setLookDirection(facing.getOpposite());
            }
            case LANTERN -> {
                if (requiredState.getValue(LanternBlock.HANGING))
                    return new Action().setLookDirection(Direction.UP);
                return new Action().setLookDirection(Direction.DOWN);
            }
            case ROD -> {
                var requiredBlock = requiredState.getBlock();
                var facing = requiredState.getValue(EndRodBlock.FACING);

                // 如果前面朝向自己的末地烛，而放置方式相反，那么反向放置
                if (requiredBlock instanceof EndRodBlock) {
                    var forwardState = world.getBlockState(pos.relative(facing));
                    var forwardStateSchematic = worldSchematic.getBlockState(pos.relative(facing));
                    if (forwardState.is(requiredBlock)
                            && forwardState.getValue(EndRodBlock.FACING) == facing.getOpposite()) {
                        return new Action().setSides(facing);
                    }
                    // 如果投影中后面有相同朝向的末地烛，则先跳过放置
                    if (forwardStateSchematic.is(requiredBlock)
                            && forwardStateSchematic.getValue(EndRodBlock.FACING) == facing) {
                        // 但是这个投影已经被正确填装时可以打印
                        if (forwardStateSchematic == forwardState) return new Action().setSides(facing.getOpposite());
                        return null;
                    }
                }
                return new Action().setSides(facing.getOpposite());
            }
            case TRIPWIRE_HOOK -> {
                var facing = requiredState.getValue(TripWireHookBlock.FACING);
                return new Action().setSides(facing);
            }
            case RAIL -> {
                Action action = new Action();
                RailShape shape;
                if (requiredState.getBlock() instanceof RailBlock)
                    shape = requiredState.getValue(RailBlock.SHAPE);
                else
                    shape = requiredState.getValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT);

                switch (shape) {
                    case EAST_WEST, ASCENDING_EAST -> action.setLookDirection(Direction.EAST);
                    case NORTH_SOUTH, ASCENDING_NORTH -> action.setLookDirection(Direction.NORTH);
                    case ASCENDING_WEST -> action.setLookDirection(Direction.WEST);
                    case ASCENDING_SOUTH -> action.setLookDirection(Direction.SOUTH);
                }
                if (requiredState.getBlock() instanceof RailBlock) {
                    if (shape == RailShape.SOUTH_EAST) {
                        return action;
                    }
                    // TODO)) 完成这非常恶心的铁轨算法
                }
                return action;
            }
            case PISTON -> {
                Direction facing = requiredState.getValue(BlockStateProperties.FACING);
                return new Action().setLookDirection(facing.getOpposite());
            }
            // 新增：告示牌16方向处理
            case SIGN -> {
                Block signBlock = requiredState.getBlock();
                // 站立告示牌：处理0-15的16方向旋转值
                if (signBlock instanceof StandingSignBlock) {
                    int rotation = requiredState.getValue(StandingSignBlock.ROTATION);
                    return new Action()
                            .setSides(Direction.DOWN)
                            .setLookRotation(rotation)
                            .setRequiresSupport();
                }
                // 墙告示牌：保留原有4方向逻辑
                if (signBlock instanceof WallSignBlock) {
                    Direction facing = requiredState.getValue(WallSignBlock.FACING);
                    return new Action()
                            .setSides(facing.getOpposite())
                            .setLookDirection(facing.getOpposite())
                            .setRequiresSupport();
                }
                // 天花板悬挂告示牌处理逻辑
                //#if MC >= 12002
                if (signBlock instanceof WallHangingSignBlock) {
                    //TODO: 视乎方向还是有点问题, 待处理
                    Direction facing = requiredState.getValue(WallHangingSignBlock.FACING);
                    List<Direction> sides = new ArrayList<>();
                    if (facing.getAxis() == Direction.Axis.X) {
                        sides.add(Direction.NORTH);
                        sides.add(Direction.SOUTH);
                    } else if (facing.getAxis() == Direction.Axis.Z) {
                        sides.add(Direction.EAST);
                        sides.add(Direction.WEST);
                    }
                    return new Action()
                            .setSides(sides.toArray(new Direction[0]))
                            .setLookDirection(facing.getOpposite())
                            .setRequiresSupport();
                }
                if (signBlock instanceof CeilingHangingSignBlock) {
                    int rotation = requiredState.getValue(CeilingHangingSignBlock.ROTATION);
                    boolean attachFace = requiredState.getValue(CeilingHangingSignBlock.ATTACHED);
                    return new Action()
                            .setUseShift(attachFace)
                            .setSides(Direction.UP)
                            .setLookRotation(rotation)
                            .setRequiresSupport();
                }
                //#endif
                return null;
            }
            case SKIP -> {
                return null;
            }
            default -> {
                Action action = new Action();
                Block block = requiredState.getBlock();

                if (block instanceof FaceAttachedHorizontalDirectionalBlock) {
                    Direction side = requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    AttachFace face = requiredState.getValue(BlockStateProperties.ATTACH_FACE);

                    // 简化方向判断逻辑
                    Direction sidePitch = face == AttachFace.CEILING ? Direction.UP
                            : face == AttachFace.FLOOR ? Direction.DOWN
                            : side;

                    if (face != AttachFace.WALL) {
                        side = side.getOpposite();
                    }

                    return new Action().setSides(side).setLookDirection(side.getOpposite(), sidePitch);
                }

                if (block instanceof HorizontalDirectionalBlock ||
                        block instanceof StonecutterBlock
                        //#if MC >= 11904
                        || block instanceof
                        //#if MC >= 12105
                        FlowerBedBlock
                    //#else
                    //$$ PinkPetalsBlock
                    //#endif
                    //#endif
                ) {
                    Direction facing = requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    if (block instanceof FenceGateBlock) // 栅栏门
                        facing = facing.getOpposite();
                    action.setLookDirection(facing.getOpposite());
                }

                if (block instanceof BaseEntityBlock) {
                    Direction facing;
                    if (requiredState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                        facing = requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                        //#if MC >= 11904
                        if (block instanceof DecoratedPotBlock
                                || block instanceof CampfireBlock)
                            facing = facing.getOpposite();
                        //#endif
                        action.setSides(facing).setLookDirection(facing.getOpposite());
                    }
                    if (requiredState.hasProperty(BlockStateProperties.FACING)) {
                        facing = requiredState.getValue(BlockStateProperties.FACING);
                        if (requiredState.getBlock() instanceof ShulkerBoxBlock) {
                            facing = facing.getOpposite();
                        }
                        action.setSides(facing).setLookDirection(facing.getOpposite());
                    }
                }

                //方块型珊瑚的替换
                if (InitHandler.REPLACE_CORAL.getBooleanValue() && block.getDescriptionId().endsWith("_coral_block")) {
                    //例子：block.minecraft.dead_tube_coral
                    String type = block.getDescriptionId().replace("block.minecraft.dead_", "").replace("_coral_block", "");
                    switch (type) {
                        case "tube" -> action.setItem(Items.TUBE_CORAL_BLOCK);
                        case "brain" -> action.setItem(Items.BRAIN_CORAL_BLOCK);
                        case "bubble" -> action.setItem(Items.BUBBLE_CORAL_BLOCK);
                        case "fire" -> action.setItem(Items.FIRE_CORAL_BLOCK);
                        case "horn" -> action.setItem(Items.HORN_CORAL_BLOCK);
                    }
                    action.setRequiresSupport();
                }

                return action;
            }
        }
        else if (state == State.WRONG_STATE) {
            boolean printerBreakWrongStateBlock = InitHandler.BREAK_WRONG_STATE_BLOCK.getBooleanValue();
            switch (requiredType) {
                case SLAB -> {
                    if (requiredState.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
                        Direction requiredHalf = currentState.getValue(SlabBlock.TYPE) == SlabType.BOTTOM ? Direction.DOWN : Direction.UP;

                        return new Action().setSides(requiredHalf);
                    } else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(pos);
                }
                case SNOW -> {
                    int layers = currentState.getValue(SnowLayerBlock.LAYERS);
                    if (layers < requiredState.getValue(SnowLayerBlock.LAYERS)) {
                        Map<Direction, Vec3> sides = new HashMap<>() {{
                            put(Direction.UP, new Vec3(0, (layers / 8d) - 1, 0));
                        }};
                        return new ClickAction().setItem(Items.SNOW).setSides(sides);
                    } else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(pos);

                }
                case DOOR, TRAPDOOR -> {
                    //判断门是不是铁制的，如果是就直接返回
                    if (requiredState.is(Blocks.IRON_DOOR) || requiredState.is(Blocks.IRON_TRAPDOOR)) break;
                    if (requiredState.getValue(BlockStateProperties.OPEN) != currentState.getValue(BlockStateProperties.OPEN)) {
                        return new ClickAction();
                    } else if (printerBreakWrongStateBlock && requiredState.getValue(DoorBlock.FACING) != currentState.getValue(DoorBlock.FACING))
                        BreakManager.addBlockToBreak(pos);
                }
                case FENCE_GATE -> {
                    var facing = requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    if (facing.getOpposite() == currentState.getValue(BlockStateProperties.HORIZONTAL_FACING)
                            || requiredState.getValue(BlockStateProperties.OPEN) != currentState.getValue(BlockStateProperties.OPEN)
                    ) {
                        return new ClickAction().setSides(facing.getOpposite()).setLookDirection(facing);
                    } else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(pos);
                }
                case LEVER -> {
                    if (requiredState.getValue(LeverBlock.POWERED) != currentState.getValue(LeverBlock.POWERED))
                        return new ClickAction();
                    else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(pos);
                }
                case CANDLES -> {
                    if (currentState.getValue(BlockStateProperties.CANDLES) < requiredState.getValue(BlockStateProperties.CANDLES))
                        return new ClickAction().setItem(requiredState.getBlock().asItem());
                    else if (!currentState.getValue(CandleBlock.LIT) && requiredState.getValue(CandleBlock.LIT))
                        return new ClickAction().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE);
                    else if (currentState.getValue(CandleBlock.LIT) && !requiredState.getValue(CandleBlock.LIT))
                        return new ClickAction();
                    else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(pos);
                }
                case PICKLES -> {
                    if (currentState.getValue(SeaPickleBlock.PICKLES) < requiredState.getValue(SeaPickleBlock.PICKLES))
                        return new ClickAction().setItem(Items.SEA_PICKLE);
                    else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(pos);
                }
                case REPEATER -> {
                    if (!requiredState.getValue(RepeaterBlock.DELAY).equals(currentState.getValue(RepeaterBlock.DELAY)))
                        return new ClickAction();
                    else if (
                            printerBreakWrongStateBlock &&
                                    requiredState.getValue(RepeaterBlock.POWERED) == currentState.getValue(RepeaterBlock.POWERED) &&
                                    requiredState.getValue(RepeaterBlock.LOCKED) == currentState.getValue(RepeaterBlock.LOCKED)
                    ) BreakManager.addBlockToBreak(pos);
                }
                case COMPARATOR -> {
                    if (requiredState.getValue(ComparatorBlock.MODE) != currentState.getValue(ComparatorBlock.MODE))
                        return new ClickAction();
                    else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(pos);
                }
                case NOTE_BLOCK -> {
                    if (InitHandler.NOTE_BLOCK_TUNING.getBooleanValue() && !Objects.equals(requiredState.getValue(NoteBlock.NOTE), currentState.getValue(NoteBlock.NOTE)))
                        return new ClickAction();
                }
                case CAMPFIRE -> {
                    if (!requiredState.getValue(CampfireBlock.LIT) && currentState.getValue(CampfireBlock.LIT))
                        return new ClickAction().setItems(Implementation.SHOVELS).setSides(Direction.UP);
                    else if (requiredState.getValue(CampfireBlock.LIT) && !currentState.getValue(CampfireBlock.LIT))
                        return new ClickAction().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE);
                    else if (printerBreakWrongStateBlock && requiredState.getValue(CampfireBlock.FACING) != currentState.getValue(CampfireBlock.FACING))
                        BreakManager.addBlockToBreak(pos);
                }
                case END_PORTAL_FRAME -> {
                    if (requiredState.getValue(EndPortalFrameBlock.HAS_EYE) && !currentState.getValue(EndPortalFrameBlock.HAS_EYE))
                        return new ClickAction().setItem(Items.ENDER_EYE);
                    else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(pos);
                }
                //#if MC >= 11904
                case FLOWERBED -> {
                    if (currentState.getValue(BlockStateProperties.FLOWER_AMOUNT) <= requiredState.getValue(BlockStateProperties.FLOWER_AMOUNT)) {
                        return new ClickAction().setItem(requiredState.getBlock().asItem());
                    } else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(pos);
                }
                //#endif
                case REDSTONE -> {
                    // 在Java版中，对于没有连接到任何红石元件的十字形的红石线，可以按使用键使其变为点状，从而不与任何方向连接，再按一次可以恢复。
                    boolean allNoneRequired = requiredState.getValue(RedStoneWireBlock.NORTH) == RedstoneSide.NONE &&
                            requiredState.getValue(RedStoneWireBlock.SOUTH) == RedstoneSide.NONE &&
                            requiredState.getValue(RedStoneWireBlock.EAST) == RedstoneSide.NONE &&
                            requiredState.getValue(RedStoneWireBlock.WEST) == RedstoneSide.NONE;

                    boolean allSideCurrent = currentState.getValue(RedStoneWireBlock.NORTH) == RedstoneSide.SIDE &&
                            currentState.getValue(RedStoneWireBlock.SOUTH) == RedstoneSide.SIDE &&
                            currentState.getValue(RedStoneWireBlock.EAST) == RedstoneSide.SIDE &&
                            currentState.getValue(RedStoneWireBlock.WEST) == RedstoneSide.SIDE;

                    if (allNoneRequired && allSideCurrent) {
                        return new ClickAction().setItem(Items.AIR);
                    }
                }
                case VINES, GLOW_LICHEN -> {
                    for (Direction direction : Direction.values()) {
                        if (direction == Direction.DOWN) continue;
                        if ((Boolean) getPropertyByName(requiredState, direction.name())) {
                            return new Action().setSides(direction).setLookDirection(direction);
                        }
                    }
                    BreakManager.addBlockToBreak(pos);
                }
                case CAULDRON -> {
                    if (currentState.getValue(LayeredCauldronBlock.LEVEL) > requiredState.getValue(LayeredCauldronBlock.LEVEL)) {
                        if (playerHasAccessToItem(mc.player, Items.GLASS_BOTTLE))
                            return new ClickAction().setItem(Items.GLASS_BOTTLE);
                        else
                            mc.gui.setOverlayMessage(Component.nullToEmpty("降低炼药锅内水位需要 §l§6" + PreprocessUtils.getNameFromItem(Items.GLASS_BOTTLE)), false);
                    }
                    if (currentState.getValue(LayeredCauldronBlock.LEVEL) < requiredState.getValue(LayeredCauldronBlock.LEVEL))
                        if (playerHasAccessToItem(mc.player, Items.POTION))
                            return new ClickAction().setItem(Items.POTION);
                        else
                            mc.gui.setOverlayMessage(Component.nullToEmpty("增加炼药锅内水位需要 §l§6" + PreprocessUtils.getNameFromItem(Items.GLASS_BOTTLE)), false);
                }
                case DAYLIGHT_DETECTOR -> {
                    if (currentState.getValue(DaylightDetectorBlock.INVERTED) != requiredState.getValue(DaylightDetectorBlock.INVERTED))
                        return new ClickAction();
                }
                case FIRE -> {
                    if (!requiredState.getValue(FireBlock.AGE).equals(currentState.getValue(FireBlock.AGE)))
                        return null;
                    if (requiredState.getBlock() instanceof SoulFireBlock) return null;
                    for (Direction direction : Direction.values()) {
                        if (direction == Direction.DOWN) continue;
                        if ((Boolean) getPropertyByName(requiredState, direction.name())) {
                            return new Action().setSides(direction).setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                        }
                    }
                    return new Action().setSides(Direction.DOWN).setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                }
                case COMPOSTER -> {
                    if (!InitHandler.FILL_COMPOSTER.getBooleanValue()) return null;
                    if (currentState.getValue(ComposterBlock.LEVEL) < requiredState.getValue(ComposterBlock.LEVEL)) {
                        return new ClickAction().setItems(compostableItems);
                    }
                }
                case STAIR -> {
                    if (printerBreakWrongStateBlock &&
                            (requiredState.getValue(StairBlock.FACING) != currentState.getValue(StairBlock.FACING) ||
                                    requiredState.getValue(StairBlock.HALF) != currentState.getValue(StairBlock.HALF))) {
                        BreakManager.addBlockToBreak(pos);
                    }
                }
                case DEFAULT -> {
                    Class<?>[] ignored = new Class<?>[]
                            {FenceBlock.class,
                                    WallBlock.class,
                                    IronBarsBlock.class,
                                    PressurePlateBlock.class,
                            };
                    if (printerBreakWrongStateBlock && !Arrays.asList(ignored).contains(requiredState.getBlock().getClass()))
                        BreakManager.addBlockToBreak(pos);
                }
            }
        } else if (state == State.WRONG_BLOCK) switch (requiredType) {
            case FARMLAND -> {
                Block[] soilBlocks = new Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT_PATH, Blocks.COARSE_DIRT};

                for (Block soilBlock : soilBlocks) {
                    if (currentState.getBlock().equals(soilBlock))
                        return new ClickAction().setItems(Implementation.HOES);
                }

            }
            case DIRT_PATH -> {
                Block[] soilBlocks = new Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT,
                        Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.MYCELIUM, Blocks.PODZOL};

                for (Block soilBlock : soilBlocks) {
                    if (currentState.getBlock().equals(soilBlock))
                        return new ClickAction().setItems(Implementation.SHOVELS);
                }

            }
            case FLOWER_POT -> {
                if (requiredState.getBlock() instanceof FlowerPotBlock potBlock) {
                    Block content = potBlock.getPotted();
                    if (content != Blocks.AIR) {
                        return new ClickAction().setItem(content.asItem());
                    }
                }
            }
            case CAULDRON -> {
                if (Arrays.asList(requiredType.classes).contains(currentState.getBlock().getClass()))
                    return null;
                else if (InitHandler.BREAK_WRONG_BLOCK.getBooleanValue() && BreakManager.canBreakBlock(pos))
                    BreakManager.addBlockToBreak(pos);
            }
            case STRIP_LOG -> {
                Block stripped = STRIPPED_LOGS.get(currentState.getBlock());
                if (stripped != null && stripped == requiredState.getBlock())
                    return new ClickAction().setItems(Implementation.AXES);
            }
            // 新增：告示牌WrongBlock逻辑
            case SIGN -> {
                if (InitHandler.BREAK_WRONG_BLOCK.getBooleanValue() && BreakManager.canBreakBlock(pos)) {
                    boolean isLegitimateSign = currentState.getBlock() instanceof StandingSignBlock
                            || currentState.getBlock() instanceof WallSignBlock
                            //#if MC >= 12002
                            || currentState.getBlock() instanceof WallHangingSignBlock
                            || currentState.getBlock() instanceof CeilingHangingSignBlock
                            //#endif
                            ;

                    if (!isLegitimateSign) {
                        BreakManager.addBlockToBreak(pos);
                    }
                    BreakManager.addBlockToBreak(pos);
                }
            }
            default -> {
                if (InitHandler.REPLACE_CORAL.getBooleanValue() && requiredState.getBlock().getDescriptionId().contains("coral")) {
                    return null;
                }

                boolean printBreakWrongBlock = InitHandler.BREAK_WRONG_BLOCK.getBooleanValue();
                boolean printerBreakExtraBlock = InitHandler.BREAK_EXTRA_BLOCK.getBooleanValue();

                if (printBreakWrongBlock || printerBreakExtraBlock) {
                    if (BreakManager.canBreakBlock(pos)) {
                        if (printBreakWrongBlock && !requiredState.is(Blocks.AIR)) {
                            BreakManager.addBlockToBreak(pos);
                        } else if (printerBreakExtraBlock && requiredState.is(Blocks.AIR)) {
                            BreakManager.addBlockToBreak(pos);
                        }
                    }
                }
            }
        }

        return null;
    }

    enum ClassHook {
        // 放置
        TORCH(
                //#if MC > 12002
                BaseTorchBlock.class
                //#else
                //$$ TorchBlock.class
                //#endif
        ), // 火把
        SLAB(SlabBlock.class), // 台阶
        STAIR(StairBlock.class), // 楼梯
        TRAPDOOR(TrapDoorBlock.class), // 活板门
        STRIP_LOG(RotatedPillarBlock.class), // 去皮原木
        ANVIL(AnvilBlock.class), // 铁砧
        HOPPER(HopperBlock.class), // 漏斗
        CAMPFIRE(CampfireBlock.class), // 营火
        BED(BedBlock.class), // 床
        BELL(BellBlock.class), // 钟
        AMETHYST(AmethystClusterBlock.class), // 紫水晶
        DOOR(DoorBlock.class), // 门
        COCOA(CocoaBlock.class), // 可可豆
        //#if MC >= 12003
        CRAFTER(CrafterBlock.class), // 合成器
        //#endif
        CHEST(ChestBlock.class), // 箱子
        OBSERVER(ObserverBlock.class), // 侦测器
        LADDER(LadderBlock.class), // 梯子
        LANTERN(LanternBlock.class), // 灯笼
        ROD(RodBlock.class), // 末地烛 避雷针
        TRIPWIRE_HOOK(TripWireHookBlock.class), // 绊线钩
        RAIL(BaseRailBlock.class), // 铁轨
        PISTON(PistonBaseBlock.class), // 活塞 （为了避免被破坏错误状态破坏）
        SIGN(
                StandingSignBlock.class,
                WallSignBlock.class
                //#if MC >= 12002
                , WallHangingSignBlock.class
                , CeilingHangingSignBlock.class
                //#endif
        ),
        // 点击
        FLOWER_POT(FlowerPotBlock.class), // 花盆
        BIG_DRIPLEAF_STEM(BigDripleafStemBlock.class), // 大垂叶茎
        CAVE_VINES(CaveVinesBlock.class, CaveVinesPlantBlock.class), // 洞穴藤蔓
        WEEPING_VINES(WeepingVinesBlock.class, WeepingVinesPlantBlock.class), // 垂泪藤
        TWISTING_VINES(TwistingVinesBlock.class, TwistingVinesPlantBlock.class), // 缠怨藤
        SNOW(SnowLayerBlock.class), // 雪
        CANDLES(CandleBlock.class), // 蜡烛
        REPEATER(RepeaterBlock.class), // 中继器
        COMPARATOR(ComparatorBlock.class), // 比较器
        PICKLES(SeaPickleBlock.class), // 海泡菜
        NOTE_BLOCK(NoteBlock.class), // 音符盒
        END_PORTAL_FRAME(EndPortalFrameBlock.class), // 末地传送门框架
        //#if MC >= 11904
        FLOWERBED(
                //#if MC >= 12105
                FlowerBedBlock
                        //#else
                        //$$ PinkPetalsBlock
                        //#endif
                        .class), // 花簇（ojng你看看你这是什么抽象命名）
        //#endif
        VINES(VineBlock.class), // 藤蔓
        GLOW_LICHEN(GlowLichenBlock.class), // 发光地衣
        FIRE(FireBlock.class, SoulFireBlock.class), // 火，灵魂火
        REDSTONE(RedStoneWireBlock.class), //红石粉
        FENCE_GATE(FenceGateBlock.class), // 栅栏门
        LEVER(LeverBlock.class), // 拉杆
        CAULDRON(CauldronBlock.class, LavaCauldronBlock.class, LayeredCauldronBlock.class), // 炼药锅
        DAYLIGHT_DETECTOR(DaylightDetectorBlock.class), // 阳光探测器
        COMPOSTER(ComposterBlock.class), // 堆肥桶

        // 其他
        FARMLAND(FarmBlock.class), // 耕地
        DIRT_PATH(DirtPathBlock.class), // 土径
        DEAD_CORAL(Block.class), // 死珊瑚
        NETHER_PORTAL(NetherPortalBlock.class), // 下界传送门
        SKIP(SkullBlock.class, LiquidBlock.class, BubbleColumnBlock.class, WaterlilyBlock.class), // 跳过：移除SignBlock
        DEFAULT; // 默认

        private final Class<?>[] classes;

        ClassHook(Class<?>... classes) {
            this.classes = classes;
        }
    }

    public static class Action {
        protected Map<Direction, Vec3> sides;
        protected @Nullable Float lookYaw;
        protected @Nullable Float lookPitch;
        @Nullable
        protected Item[] clickItems; // null == 空手
        protected boolean requiresSupport = false;
        protected boolean useShift = false;
        protected int waitTick = 0;

        public Action() {
            this.sides = new HashMap<>();
            for (Direction direction : Direction.values()) {
                sides.put(direction, new Vec3(0, 0, 0));
            }
        }

        public Action setLookDirection(Direction lookDirection) {
            this.lookYaw = DirectionUtils.getRequiredYaw(lookDirection);
            this.lookPitch = DirectionUtils.getRequiredPitch(lookDirection);
            return this;
        }

        public Action setLookDirection(Direction lookDirectionYaw, Direction lookDirectionPitch) {
            this.lookYaw = DirectionUtils.getRequiredYaw(lookDirectionYaw);
            this.lookPitch = DirectionUtils.getRequiredPitch(lookDirectionPitch);
            return this;
        }

        public Action setLookYawPitch(float lookYaw, float lookPitch) {
            this.lookYaw = lookYaw;
            this.lookPitch = lookPitch;
            return this;
        }

        public Action setLookRotation(int rotation) {
            setLookYawPitch(DirectionUtils.rotationToPlayerYaw(rotation), 0);
            return this;
        }

        public @Nullable Float getLookYaw() {
            return lookYaw;
        }

        public @Nullable Float getLookPitch() {
            return lookPitch;
        }

        public @Nullable Item[] getRequiredItems(Block backup) {
            return clickItems == null ? new Item[]{backup.asItem()} : clickItems;
        }

        public @NotNull Map<Direction, Vec3> getSides() {
            if (this.sides == null) {
                this.sides = new HashMap<>();
                for (Direction d : Direction.values()) {
                    this.sides.put(d, new Vec3(0, 0, 0));
                }
            }

            return this.sides;
        }

        /**
         * 设置可以和方块交互的所有方向。
         * <p>
         * 这个方法会找到所有指定的方向轴（比如：X轴、Y轴、Z轴）上的所有方向，
         * 然后把这些方向都设置为可以交互的方向，并且设置默认的点击偏移量为 (0,0,0)。
         * 简单来说，就是设置你可以从哪些方向点击这个方块。
         *
         * @param axis 要设置的方向轴列表（例如：只允许上下方向，不允许左、右方向）
         * @return 当前 Action 实例，方便你继续设置其他属性
         */
        public Action setSides(Direction.Axis... axis) {
            Map<Direction, Vec3> sides = new HashMap<>();

            for (Direction.Axis a : axis) {
                for (Direction d : Direction.values()) {
                    if (d.getAxis() == a) {
                        sides.put(d, new Vec3(0, 0, 0));
                    }
                }
            }

            this.sides = sides;
            return this;
        }

        /**
         * 设置放置的有效面，以及指定每个面对应的偏移位置。
         * <p>
         * 这个方法允许你指定放置方块时，可以用哪些方向交互。
         * 例如，你可以设置只有在方块的上方或下方才能进行放置。
         * </p>
         * <p>
         * 你也可以调整偏移量，进行更精确的控制点击的位置，从而实现一些特殊的放置效果。
         * 例如，你可以通过偏移量来点击方块的边缘，而不是中心。
         * </p>
         *
         * @param sides 包含方向和偏移量的 Map，其中 Key 是方向 (Direction)，Value 是偏移量 (Vec3)。
         *              如果某个方向没有对应的偏移量，则使用默认的 (0, 0, 0)。
         * @return 当前 Action 实例，便于链式调用。
         * 通过链式调用，你可以连续设置多个属性，使代码更加简洁易读。
         */
        public Action setSides(Map<Direction, Vec3> sides) {
            this.sides = sides;
            return this;
        }

        /**
         * 设置放置的有效面。
         * <p>
         * 传入的方向参数均设置默认的偏移值 (0, 0, 0)。
         * </p>
         *
         * @param directions 要设置的方向（可以是多个）
         * @return 当前 Action 实例
         */
        public Action setSides(Direction... directions) {
            Map<Direction, Vec3> sides = new HashMap<>();

            for (Direction d : directions) {
                sides.put(d, new Vec3(0, 0, 0));
            }

            this.sides = sides;
            return this;
        }

        /**
         * 获取有效的侧面。
         * <p>
         * 遍历所有侧面并返回第一个可用的方向，
         * 如果没有可用的侧面，则返回 null 。
         *
         * @param world 当前的客户端世界
         * @param pos   方块的位置
         * @return 第一个有效侧面，如果不存在则返回 null
         */
        public @Nullable Direction getValidSide(ClientLevel world, BlockPos pos) {
            Map<Direction, Vec3> sides = getSides();
            List<Direction> validSides = new ArrayList<>();

            // 遍历所有侧面，检查每个侧面是否可用
            for (Direction side : sides.keySet()) {
                BlockPos neighborPos = pos.relative(side);
                BlockState neighborState = world.getBlockState(neighborPos);

                if (InitHandler.PRINT_IN_AIR.getBooleanValue() &&
                        !this.requiresSupport &&
                        !Implementation.isInteractive(neighborState.getBlock())
                ) return side;


                // 检查该侧面是否可以被点击且不可替换
                if (canBeClicked(world, neighborPos) && !BlockUtils.isReplaceable(neighborState)) {
                    validSides.add(side);
                }
            }

            if (validSides.isEmpty()) return null;

            // 选择一个不需要潜行放置的面
            for (Direction validSide : validSides) {
                BlockState requiredState = world.getBlockState(pos);
                BlockState sideBlockState = world.getBlockState(pos.relative(validSide));
                if (!Implementation.isInteractive(sideBlockState.getBlock()) && requiredState.canSurvive(world, pos)) {
                    return validSide;
                }
            }

            return validSides.getFirst();
        }

        /**
         * 设置打印这种方块对应使用的物品
         *
         * @param item 要选择的物品
         * @return 当前 Action 实例
         */
        public Action setItem(Item item) {
            return this.setItems(item);
        }

        /**
         * 设置打印这种方块对应使用的物品（多个）
         *
         * @param items 要选择的物品（多个）
         * @return 当前 Action 实例
         */
        public Action setItems(Item... items) {
            this.clickItems = items;
            return this;
        }

        public Action setRequiresSupport(boolean requiresSupport) {
            this.requiresSupport = requiresSupport;
            return this;
        }

        /**
         * 需有支撑面才能放置
         *
         * @return 当前 Action 实例
         */
        public Action setRequiresSupport() {
            return this.setRequiresSupport(true);
        }

        public Action setUseShift(boolean useShift) {
            this.useShift = useShift;
            return this;
        }

        /**
         * 需要按下 Shift 键进行放置
         *
         * @return 当前 Action 实例
         */
        public Action setUseShift() {
            return this.setUseShift(true);
        }

        /**
         * 获取放置后需要等待的游戏刻
         *
         * @return 整数
         */
        public int getWaitTick() {
            return this.waitTick;
        }

        /**
         * 设置放置后需要等待的游戏刻
         *
         * @param waitTick 整数
         * @return 当前 Action 实例
         */
        public Action setWaitTick(int waitTick) {
            this.waitTick = waitTick;
            return this;
        }

        public void queueAction(Printer.Queue queue, BlockPos center, Direction side, boolean useShift) {
//            System.out.println("Queued click?: " + center.relative(side).toString() + ", side: " + side.getOpposite());

            if (InitHandler.PRINT_IN_AIR.getBooleanValue() && !this.requiresSupport) {
                queue.queueClick(
                        center,
                        side.getOpposite(),
                        getSides().get(side),
                        useShift
                );
            } else {
                queue.queueClick(
                        center.relative(side),
                        side.getOpposite(),
                        getSides().get(side),
                        useShift
                );
            }

        }
    }

    public static class ClickAction extends Action {
        @Override
        public void queueAction(Printer.Queue queue, BlockPos center, Direction side, boolean useShift) {
            queue.queueClick(center, side, getSides().get(side), false);
        }

        @Override
        public @Nullable Item[] getRequiredItems(Block backup) {
            return this.clickItems;
        }

        /**
         * 获取有效的侧面。
         * <p>
         * 遍历所有侧面并返回第一个可用的方向，
         * 如果没有可用的侧面，则返回 null 。
         *
         * @param world 当前的 ClientLevel 实例
         * @param pos   块的位置
         * @return 第一个有效侧面，如果不存在则返回 null
         */
        @Override
        public @Nullable Direction getValidSide(ClientLevel world, BlockPos pos) {
            for (Direction side : getSides().keySet()) {
                return side;
            }
            return null;
        }
    }
}