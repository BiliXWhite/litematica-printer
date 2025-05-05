package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import net.fabricmc.fabric.mixin.content.registry.AxeItemAccessor;
import net.minecraft.block.*;
import net.minecraft.block.enums.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.aleksilassila.litematica.printer.printer.Printer.*;
import static me.aleksilassila.litematica.printer.printer.qwer.PrintWater.*;

public class PlacementGuide extends PrinterUtils {
    public static Map<BlockPos, Integer> posMap = new HashMap<>();
    public static boolean breakIce = false;
    @NotNull
    protected final MinecraftClient client;

    public PlacementGuide(@NotNull MinecraftClient client) {
        this.client = client;
    }

    public @Nullable Action getAction(World world, WorldSchematic worldSchematic, BlockPos pos) {
        // 提前缓存 blockState 提升性能
        var blockState = worldSchematic.getBlockState(pos);
        for (ClassHook hook : ClassHook.values()) {
            for (Class<?> clazz : hook.classes) {
                if (clazz != null && clazz.isInstance(blockState.getBlock())) {
                    return buildAction(world, worldSchematic, pos, hook);
                }
            }
        }
        return buildAction(world, worldSchematic, pos, ClassHook.DEFAULT);
    }

    public @Nullable Action water(BlockState requiredState, BlockState currentState, BlockPos pos) {
        // 缓存 player 避免重复访问 client.player
        var player = client.player;
        if (player == null) {
            return null;
        }

        Integer count = posMap.get(pos);
        if (count != null) {
            posMap.put(pos, count + 1);
            if (count + 1 > 10) {
                posMap.remove(pos);
            }
            List<BlockPos> removeList = new ArrayList<>();
            for (BlockPos key : posMap.keySet()) {
                if (player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(key)) < 36) {
                    removeList.add(key);
                }
            }
            for (BlockPos key : removeList) {
                posMap.remove(key);
            }
        }

        if (currentState.isOf(Blocks.ICE)) {
            searchPickaxes(player);
            BlockPos tempPos = excavateBlock(pos);
            if (tempPos != null && !posMap.containsKey(pos)) {
                posMap.put(tempPos, 0);
                breakIce = true;
                return null;
            }
            return null;
        }

        if (!spawnWater(pos)) {
            return null;
        }

        if (posMap.containsKey(pos)) {
            return null;
        }

        State state = State.get(requiredState, currentState);
        if (state != State.MISSING_BLOCK) {
            return null;
        }

        Direction look = null;
        for (Property<?> prop : requiredState.getProperties()) {
            if (prop instanceof EnumProperty<?> enumProperty &&
                    enumProperty.getType().equals(Direction.class) &&
                    prop.getName().equalsIgnoreCase("FACING")) {
                look = ((Direction) requiredState.get(prop)).getOpposite();
                break;
            }
        }
        return new Action().setLookDirection(look).setItem(Items.ICE);
    }

    private @Nullable Action buildAction(World world, WorldSchematic worldSchematic, BlockPos pos, ClassHook requiredType) {
        BlockState requiredState = worldSchematic.getBlockState(pos);
        BlockState currentState = world.getBlockState(pos);

        // 缓存重复调用的结果
        var waterLoggedRequired = canWaterLogged(requiredState);
        var waterLoggedCurrent = canWaterLogged(currentState);
        var canPlace = requiredState.canPlaceAt(world, pos);
        var state = State.get(requiredState, currentState);

        if (LitematicaMixinMod.PRINT_WATER_LOGGED_BLOCK.getBooleanValue()
                && waterLoggedRequired && !waterLoggedCurrent) {
            if (breakIce) {
                breakIce = false;
            } else {
                return water(requiredState, currentState, pos);
            }
        } else if (requiredState.getBlock() instanceof FluidBlock) {
            return null;
        }
        if (LitematicaMixinMod.BREAK_ERROR_BLOCK.getBooleanValue()
                && canBreakBlock(pos)
                && isSchematicBlock(pos)
                && state == State.WRONG_BLOCK) {
            excavateBlock(pos);
        }

        if (!canPlace) {
            return null;
        }

        if (state == State.CORRECT) {
            return null;
        }

        if (state == State.MISSING_BLOCK) switch (requiredType) {
            case WALL_TORCH -> {
                Direction facing = requiredState.get(Properties.HORIZONTAL_FACING);
                if (facing != null) {
                    return new Action().setSides(facing.getOpposite()).setRequiresSupport();
                }
            }
            case AMETHYST -> {
                return new Action()
                        .setSides((requiredState.get(Properties.FACING))
                                .getOpposite())
                        .setRequiresSupport();
            }
            case SHULKER -> {
                return new Action().setSides(
                        (requiredState.get(Properties.FACING))
                                .getOpposite());
            }
            case SLAB -> {
                return new Action().setSides(getSlabSides(world, pos, requiredState.get(SlabBlock.TYPE)));
            }
            case STAIR -> {
                Direction facing = requiredState.get(StairsBlock.FACING);
                BlockHalf half = requiredState.get(StairsBlock.HALF);

                Map<Direction, Vec3d> sides = new HashMap<>();
                if (half == BlockHalf.BOTTOM) {
                    sides.put(Direction.DOWN, new Vec3d(0, 0, 0));
                    sides.put(facing, new Vec3d(0, 0, 0));
                } else {
                    sides.put(Direction.UP, new Vec3d(0, 0.75, 0));
                    sides.put(facing.getOpposite(), new Vec3d(0, 0.75, 0));
                }

                return new Action()
                        .setSides(sides)
                        .setLookDirection(facing);
            }
            case TRAPDOOR -> {
                Direction half = getHalf(requiredState.get(TrapdoorBlock.HALF));

                Map<Direction, Vec3d> sides = new HashMap<>() {{
                    put(half, new Vec3d(0, 0, 0));
                }};

                return new Action()
                        .setSides(sides)
                        .setLookDirection(requiredState.get(TrapdoorBlock.FACING).getOpposite());
            }
            case PILLAR -> {
                Action action = new Action().setSides(requiredState.get(PillarBlock.AXIS));

                //如果是剥皮原木且应该使用普通原木替换
                if (AxeItemAccessor.getStrippedBlocks().containsValue(requiredState.getBlock()) &&
                        LitematicaMixinMod.STRIP_LOGS.getBooleanValue()) {
                    Block stripped = requiredState.getBlock();

                    for (Block log : AxeItemAccessor.getStrippedBlocks().keySet()) {
                        if (AxeItemAccessor.getStrippedBlocks().get(log) != stripped) continue;

                        if (!playerHasAccessToItem(client.player, stripped.asItem()) &&
                                playerHasAccessToItem(client.player, log.asItem())) {
                            action.setItem(log.asItem());
                        }
                        break;

                    }
                }

                return action;
            }
            case ANVIL -> {
                return new Action().setLookDirection(requiredState.get(AnvilBlock.FACING).rotateYCounterclockwise()).setSides(Direction.UP);
            }
            case HOPPER -> {
                Direction facing = requiredState.get(Properties.HOPPER_FACING);
                return new Action().setSides(facing).setLookDirection(facing);
            }
            case NETHER_PORTAL -> {

                boolean canCreatePortal = net.minecraft.world.dimension.NetherPortal.getNewPortal(world, pos, Direction.Axis.X).isPresent();
                if (canCreatePortal) {
                    return new Action().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                }
            }
            case COCOA -> {
                return new Action().setSides(requiredState.get(Properties.HORIZONTAL_FACING)).setLookDirection(requiredState.get(Properties.HORIZONTAL_FACING));
            }
            //#if MC >= 12003
            case CRAFTER -> {
                var orientation = requiredState.get(Properties.ORIENTATION);
                Direction facing = orientation.getFacing().getOpposite();
                Direction rotation = orientation.getRotation().getOpposite();
                if (facing == Direction.UP) {
                    return new Action().setSides(rotation).setLookDirection(rotation, Direction.UP);
                } else if (facing == Direction.DOWN) {
                    return new Action().setSides(rotation.getOpposite()).setLookDirection(rotation.getOpposite(), Direction.DOWN);
                } else {
                    return new Action().setSides(facing).setLookDirection(facing, facing);
                }
            }
            //#endif
            case CHEST -> {
                Direction facing = requiredState.get(Properties.HORIZONTAL_FACING).getOpposite();
                ChestType type = requiredState.get(Properties.CHEST_TYPE);
                if (type == ChestType.SINGLE) {
                    return new Action().setLookDirection(facing, Direction.DOWN).setUseShift();
                } else {
                    return new Action().setLookDirection(facing);
                }
            }
            case WALL_MOUNTED_BLOCK -> {
                Direction side = requiredState.get(Properties.HORIZONTAL_FACING);
                BlockFace face = requiredState.get(Properties.BLOCK_FACE);
                Direction sidePitch = switch (face) {
                    case CEILING -> Direction.UP;
                    case FLOOR -> Direction.DOWN;
                    default -> side;
                };
                if (face != BlockFace.WALL) {
                    side = side.getOpposite();
                }
                return new Action().setSides(side).setLookDirection(side.getOpposite(), sidePitch);
            }
            case CAMPFIRE -> {
                return new Action()
                        .setLookDirection(requiredState.get(Properties.FACING));
            }
            case BED -> {
                if (requiredState.get(BedBlock.PART) == BedPart.FOOT)
                    return new Action().setLookDirection(requiredState.get(BedBlock.FACING));
            }
            case BELL -> {
                Direction side;
                switch (requiredState.get(BellBlock.ATTACHMENT)) {
                    case FLOOR -> side = Direction.DOWN;
                    case CEILING -> side = Direction.UP;
                    default -> side = requiredState.get(BellBlock.FACING);
                }

                Direction look = requiredState.get(BellBlock.ATTACHMENT) != Attachment.SINGLE_WALL &&
                        requiredState.get(BellBlock.ATTACHMENT) != Attachment.DOUBLE_WALL ?
                        requiredState.get(BellBlock.FACING) : null;

                return new Action().setSides(side).setLookDirection(look);
            }
            case DOOR -> {
                Direction facing = requiredState.get(DoorBlock.FACING);
                DoorHinge hinge = requiredState.get(DoorBlock.HINGE);
                BlockPos upperPos = pos.up();

                // 获取门铰链方向
                Direction hingeSide = hinge == DoorHinge.RIGHT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();

                double offset = hinge == DoorHinge.RIGHT ? 0.25 : -0.25;
                Vec3d hingeVec = facing.getAxis() == Direction.Axis.X ? new Vec3d(0, 0, offset) : new Vec3d(offset, 0, 0);

                Map<Direction, Vec3d> sides = new HashMap<>();
                sides.put(hingeSide, Vec3d.ZERO); // 靠墙方向需要支撑
                sides.put(Direction.DOWN, hingeVec); // 底部点击偏移
                sides.put(facing, hingeVec); // 正面点击偏移

                // 获取左右方块状态
                Direction left = facing.rotateYCounterclockwise();
                Direction right = facing.rotateYClockwise();
                BlockState leftState = world.getBlockState(pos.offset(left));
                BlockState leftUpperState = world.getBlockState(upperPos.offset(left));
                BlockState rightState = world.getBlockState(pos.offset(right));
                BlockState rightUpperState = world.getBlockState(upperPos.offset(right));

                int occupancy = (leftState.isFullCube(world, pos.offset(left)) ? -1 : 0)
                        + (leftUpperState.isFullCube(world, upperPos.offset(left)) ? -1 : 0)
                        + (rightState.isFullCube(world, pos.offset(right)) ? 1 : 0)
                        + (rightUpperState.isFullCube(world, upperPos.offset(right)) ? 1 : 0);

                boolean isLeftDoor = leftState.getBlock() instanceof DoorBlock &&
                        leftState.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
                boolean isRightDoor = rightState.getBlock() instanceof DoorBlock &&
                        rightState.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;

                boolean condition = (hinge == DoorHinge.RIGHT && ((isLeftDoor && !isRightDoor) || occupancy > 0))
                        || (hinge == DoorHinge.LEFT && ((isRightDoor && !isLeftDoor) || occupancy < 0))
                        || (occupancy == 0 && (isLeftDoor == isRightDoor));
                if (condition)
                    return new Action().setSides(sides).setLookDirection(facing).setRequiresSupport();
            }
            case WALL_SKULL -> {
                return new Action().setSides(requiredState.get(WallSkullBlock.FACING).getOpposite());
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
                    if ((Boolean) getPropertyByName(requiredState, direction.getName())) {
                        return new Action().setSides(direction).setLookDirection(direction);
                    }
                }
            }
            //超级无敌写史高手请求出战
            case CORAL -> {
                boolean isDead = requiredState.getBlock().getTranslationKey().contains("dead");
                if (isDead) {
                    if (LitematicaMixinMod.REPLACE_CORAL.getBooleanValue()) {
                        if (playerHasAccessToItem(client.player, requiredState.getBlock().asItem())) {
                            return (requiredState.getBlock() instanceof CoralWallFanBlock)
                                    ? new Action().setSides(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite()).setLookDirection(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                                    : new Action();
                        } else {
                            String key = requiredState.getBlock().getTranslationKey();
                            return switch (key) {
                                case "block.minecraft.dead_tube_coral_block" ->
                                        new Action().setItem(Items.TUBE_CORAL_BLOCK);
                                case "block.minecraft.dead_brain_coral_block" ->
                                        new Action().setItem(Items.BRAIN_CORAL_BLOCK);
                                case "block.minecraft.dead_bubble_coral_block" ->
                                        new Action().setItem(Items.BUBBLE_CORAL_BLOCK);
                                case "block.minecraft.dead_fire_coral_block" ->
                                        new Action().setItem(Items.FIRE_CORAL_BLOCK);
                                case "block.minecraft.dead_horn_coral_block" ->
                                        new Action().setItem(Items.HORN_CORAL_BLOCK);
                                case "block.minecraft.dead_tube_coral_fan" ->
                                        new Action().setItem(Items.TUBE_CORAL_FAN)
                                                .setSides(Direction.DOWN)
                                                .setLookDirection(Direction.UP)
                                                .setRequiresSupport();
                                case "block.minecraft.dead_tube_coral_wall_fan" ->
                                        new Action().setItem(Items.TUBE_CORAL_FAN)
                                                .setSides(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                                                .setLookDirection(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                                                .setRequiresSupport();
                                case "block.minecraft.dead_brain_coral_fan" ->
                                        new Action().setItem(Items.BRAIN_CORAL_FAN)
                                                .setSides(Direction.DOWN)
                                                .setLookDirection(Direction.UP)
                                                .setRequiresSupport();
                                case "block.minecraft.dead_brain_coral_wall_fan" ->
                                        new Action().setItem(Items.BRAIN_CORAL_FAN)
                                                .setSides(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                                                .setLookDirection(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                                                .setRequiresSupport();
                                case "block.minecraft.dead_bubble_coral_fan" ->
                                        new Action().setItem(Items.BUBBLE_CORAL_FAN)
                                                .setSides(Direction.DOWN)
                                                .setLookDirection(Direction.UP)
                                                .setRequiresSupport();
                                case "block.minecraft.dead_bubble_coral_wall_fan" ->
                                        new Action().setItem(Items.BUBBLE_CORAL_FAN)
                                                .setSides(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                                                .setLookDirection(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                                                .setRequiresSupport();
                                case "block.minecraft.dead_fire_coral_fan" ->
                                        new Action().setItem(Items.FIRE_CORAL_FAN)
                                                .setSides(Direction.DOWN)
                                                .setLookDirection(Direction.UP)
                                                .setRequiresSupport();
                                case "block.minecraft.dead_fire_coral_wall_fan" ->
                                        new Action().setItem(Items.FIRE_CORAL_FAN)
                                                .setSides(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                                                .setLookDirection(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                                                .setRequiresSupport();
                                case "block.minecraft.dead_horn_coral_fan" ->
                                        new Action().setItem(Items.HORN_CORAL_FAN)
                                                .setSides(Direction.DOWN)
                                                .setLookDirection(Direction.UP)
                                                .setRequiresSupport();
                                case "block.minecraft.dead_horn_coral_wall_fan" ->
                                        new Action().setItem(Items.HORN_CORAL_FAN)
                                                .setSides(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                                                .setLookDirection(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                                                .setRequiresSupport();
                                case "block.minecraft.dead_tube_coral" -> new Action().setItem(Items.TUBE_CORAL);
                                case "block.minecraft.dead_brain_coral" -> new Action().setItem(Items.BRAIN_CORAL);
                                case "block.minecraft.dead_bubble_coral" -> new Action().setItem(Items.BUBBLE_CORAL);
                                case "block.minecraft.dead_fire_coral" -> new Action().setItem(Items.FIRE_CORAL);
                                case "block.minecraft.dead_horn_coral" -> new Action().setItem(Items.HORN_CORAL);
                                default -> new Action();
                            };
                        }
                    } else {
                        return (requiredState.getBlock() instanceof CoralWallFanBlock)
                                ? new Action().setSides(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite()).setLookDirection(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                                : new Action();
                    }
                } else {
                    return (requiredState.getBlock() instanceof CoralWallFanBlock)
                            ? new Action().setSides(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite()).setLookDirection(requiredState.get(Properties.HORIZONTAL_FACING).getOpposite())
                            : new Action();
                }
            }
            case FIRE -> {
                return new Action().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
            }
            case FACING_BLOCK -> {
                Direction facing = requiredState.get(Properties.FACING);
                if (requiredState.getBlock() instanceof ObserverBlock ||
                    requiredState.getBlock() instanceof RodBlock) {
                    facing = facing.getOpposite();
                }
                return new Action().setSides(facing).setLookDirection(facing.getOpposite());
            }
            case HORIZONTAL_FACING_BLOCK -> {
                Direction facing = requiredState.get(Properties.HORIZONTAL_FACING);
                if (requiredState.getBlock() instanceof FenceGateBlock) {
                    return new Action().setSides(facing).setLookDirection(facing);
                }
                return new Action().setSides(facing).setLookDirection(facing.getOpposite());
            }
            case SKIP -> {
                return null;
            }
            default -> {
                return new Action().setLookDirection(Direction.DOWN);
            }
        }
        else if (state == State.WRONG_STATE) switch (requiredType) {
            case SLAB -> {
                if (requiredState.get(SlabBlock.TYPE) == SlabType.DOUBLE) {
//                        SlabType requiredHalf1 = currentState.get(SlabBlock.TYPE) == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP;
//                        return new Action().setSides(getSlabSides(world, pos, requiredHalf1));
                    Direction requiredHalf = currentState.get(SlabBlock.TYPE) == SlabType.BOTTOM ? Direction.DOWN : Direction.UP;

                    return new Action().setSides(requiredHalf);
                }

            }
            case SNOW -> {
                int layers = currentState.get(SnowBlock.LAYERS);
                if (layers < requiredState.get(SnowBlock.LAYERS)) {
                    Map<Direction, Vec3d> sides = new HashMap<>() {{
                        put(Direction.UP, new Vec3d(0, (layers / 8d) - 1, 0));
                    }};
                    return new ClickAction().setItem(Items.SNOW).setSides(sides);
                }

            }
            case DOOR, TRAPDOOR, FENCE_GATE -> {
                //判断门是不是铁制的，如果是就直接返回
                if (requiredState.isOf(Blocks.IRON_DOOR) || requiredState.isOf(Blocks.IRON_TRAPDOOR)) break;
                if (requiredState.get(Properties.OPEN) != currentState.get(Properties.OPEN))
                    return new ClickAction();

            }
            case LEVER -> {
                if (requiredState.get(LeverBlock.POWERED) != currentState.get(LeverBlock.POWERED))
                    return new ClickAction();

            }
            case CANDLES -> {
                if (currentState.get(Properties.CANDLES) < requiredState.get(Properties.CANDLES))
                    return new ClickAction().setItem(requiredState.getBlock().asItem());

            }
            case PICKLES -> {
                if (currentState.get(SeaPickleBlock.PICKLES) < requiredState.get(SeaPickleBlock.PICKLES))
                    return new ClickAction().setItem(Items.SEA_PICKLE);

            }
            case REPEATER -> {
                if (!requiredState.get(RepeaterBlock.DELAY).equals(currentState.get(RepeaterBlock.DELAY)))
                    return new ClickAction();

            }
            case COMPARATOR -> {
                if (requiredState.get(ComparatorBlock.MODE) != currentState.get(ComparatorBlock.MODE))
                    return new ClickAction();

            }
            case NOTE_BLOCK -> {
                if (!Objects.equals(requiredState.get(NoteBlock.NOTE), currentState.get(NoteBlock.NOTE)))
                    return new ClickAction();

            }
            case CAMPFIRE -> {
                if (requiredState.get(CampfireBlock.LIT) != currentState.get(CampfireBlock.LIT))
                    return new ClickAction().setItems(Implementation.SHOVELS);

            }
            case PILLAR -> {
                Block stripped = AxeItemAccessor.getStrippedBlocks().get(currentState.getBlock());
                if (stripped != null && stripped == requiredState.getBlock()) {
                    return new ClickAction().setItems(Implementation.AXES);
                }
            }
            case END_PORTAL_FRAME -> {
                if (requiredState.get(EndPortalFrameBlock.EYE) && !currentState.get(EndPortalFrameBlock.EYE))
                    return new ClickAction().setItem(Items.ENDER_EYE);

            }
            //#if MC >= 11904
            case FLOWERBED -> {
                if (currentState.get(FlowerbedBlock.FLOWER_AMOUNT) <= requiredState.get(FlowerbedBlock.FLOWER_AMOUNT)) {
                    return new ClickAction().setItem(requiredState.getBlock().asItem());
                }
            }
            //#endif
            case REDSTONE -> {
                // 在Java版中，对于没有连接到任何红石元件的十字形的红石线，可以按使用键使其变为点状，从而不与任何方向连接，再按一次可以恢复。
                boolean allNoneRequired = requiredState.get(RedstoneWireBlock.WIRE_CONNECTION_NORTH) == WireConnection.NONE &&
                        requiredState.get(RedstoneWireBlock.WIRE_CONNECTION_SOUTH) == WireConnection.NONE &&
                        requiredState.get(RedstoneWireBlock.WIRE_CONNECTION_EAST) == WireConnection.NONE &&
                        requiredState.get(RedstoneWireBlock.WIRE_CONNECTION_WEST) == WireConnection.NONE;

                boolean allSideCurrent = currentState.get(RedstoneWireBlock.WIRE_CONNECTION_NORTH) == WireConnection.SIDE &&
                        currentState.get(RedstoneWireBlock.WIRE_CONNECTION_SOUTH) == WireConnection.SIDE &&
                        currentState.get(RedstoneWireBlock.WIRE_CONNECTION_EAST) == WireConnection.SIDE &&
                        currentState.get(RedstoneWireBlock.WIRE_CONNECTION_WEST) == WireConnection.SIDE;

                if (allNoneRequired && allSideCurrent) {
                    return new ClickAction().setItem(Items.AIR);
                }
            }
            case VINES, GLOW_LICHEN -> {
                for (Direction direction : Direction.values()) {
                    if (direction == Direction.DOWN) continue;
                    if ((Boolean) getPropertyByName(requiredState, direction.getName())) {
                        return new Action().setSides(direction).setLookDirection(direction);
                    }
                }
            }
        }
        else if (state == State.WRONG_BLOCK) switch (requiredType) {
            case FARMLAND -> {
                Block[] soilBlocks = new Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT_PATH};

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
                    Block content = potBlock.getContent();
                    if (content != Blocks.AIR) {
                        return new ClickAction().setItem(content.asItem());
                    }
                }
            }
            case CORAL -> {
                if (currentState.getBlock() instanceof CoralBlock ||
                currentState.getBlock() instanceof CoralBlockBlock ||
                currentState.getBlock() instanceof CoralFanBlock ||
                currentState.getBlock() instanceof CoralWallFanBlock) {
                    if (currentState.getBlock().getTranslationKey().contains("dead")) {
                        return null;
                    }
                }
            }
            default -> {
                return null;
            }
        }

        return null;
    }

    enum ClassHook {
        // 放置
        WALL_TORCH(WallTorchBlock.class, WallRedstoneTorchBlock.class), // 墙上的火把
        SLAB(SlabBlock.class), // 台阶
        STAIR(StairsBlock.class), // 楼梯
        TRAPDOOR(TrapdoorBlock.class), // 活板门
        PILLAR(PillarBlock.class), // 去皮原木
        ANVIL(AnvilBlock.class), // 铁砧
        HOPPER(HopperBlock.class), // 漏斗
        CAMPFIRE(CampfireBlock.class), // 营火
        SHULKER(ShulkerBoxBlock.class), // 潜影盒
        BED(BedBlock.class), // 床
        BELL(BellBlock.class), // 钟
        AMETHYST(AmethystBlock.class), // 紫水晶
        DOOR(DoorBlock.class), // 门
        COCOA(CocoaBlock.class), // 可可豆
        WALL_SKULL(WallSkullBlock.class), // 墙上的头颅
        NETHER_PORTAL(NetherPortalBlock.class), // 下界传送门
        //#if MC >= 12003
        CRAFTER(CrafterBlock.class), // 合成器
        //#endif
        CHEST(ChestBlock.class), // 箱子
        FACING_BLOCK(FacingBlock.class, DispenserBlock.class), // 六面朝向方块
        HORIZONTAL_FACING_BLOCK(HorizontalFacingBlock.class), // 水平朝向方块
        WALL_MOUNTED_BLOCK(WallMountedBlock.class), // 墙上挂载方块

        // 点击
        FLOWER_POT(FlowerPotBlock.class), // 花盆
        BIG_DRIPLEAF_STEM(BigDripleafStemBlock.class), // 大垂叶茎
        CAVE_VINES(CaveVinesHeadBlock.class, CaveVinesBodyBlock.class), // 洞穴藤蔓
        WEEPING_VINES(WeepingVinesBlock.class, WeepingVinesPlantBlock.class), // 垂泪藤
        TWISTING_VINES(TwistingVinesBlock.class, TwistingVinesPlantBlock.class), // 缠怨藤
        SNOW(SnowBlock.class), // 雪
        CANDLES(CandleBlock.class), // 蜡烛
        REPEATER(RepeaterBlock.class), // 中继器
        COMPARATOR(ComparatorBlock.class), // 比较器
        PICKLES(SeaPickleBlock.class), // 海泡菜
        NOTE_BLOCK(NoteBlock.class), // 音符盒
        END_PORTAL_FRAME(EndPortalFrameBlock.class), // 末地传送门框架
        //#if MC >= 11904
        FLOWERBED(FlowerbedBlock.class), // 花簇（ojng你看看你这是什么抽象命名）
        //#endif
        VINES(VineBlock.class), // 藤蔓
        GLOW_LICHEN(GlowLichenBlock.class), // 荧光地衣
        FIRE(FireBlock.class, SoulFireBlock.class), // 火焰
        REDSTONE(RedstoneWireBlock.class), //红石粉

        // 两者皆有
        FENCE_GATE(FenceGateBlock.class), // 栅栏门
        LEVER(LeverBlock.class), // 拉杆

        // 其他
        FARMLAND(FarmlandBlock.class), // 耕地
        DIRT_PATH(DirtPathBlock.class), // 泥土小径
        CORAL(AbstractBlock.class, CoralBlockBlock.class, CoralBlock.class, CoralFanBlock.class, CoralWallFanBlock.class, DeadCoralBlock.class, DeadCoralFanBlock.class, DeadCoralWallFanBlock.class), // 珊瑚块
        SKIP(SkullBlock.class, SignBlock.class, FluidBlock.class), // 跳过
        DEFAULT; // 默认

        private final Class<?>[] classes;

        ClassHook(Class<?>... classes) {
            this.classes = classes;
        }
    }

    public static class Action {
        protected Map<Direction, Vec3d> sides;
        protected Direction lookHorizontalDirection;
        protected Direction lookDirectionPitch;
        @Nullable
        protected Item[] clickItems; // null == any

        protected boolean requiresSupport = false;
        protected boolean useShift = false;

        // If true, click target block, not neighbor

        public Action() {
            this.sides = new HashMap<>();
            for (Direction direction : Direction.values()) {
                sides.put(direction, new Vec3d(0, 0, 0));
            }
        }

        /**
         * 检查一个方块是否可以被替换。
         * <p>
         *   简单来说，就是判断这个方块是否“碍事”，能不能直接在上面放新的方块。
         *   例如，草是可以直接被覆盖放置的，而石头、木头这些则不行。
         * </p>
         * <p>
         *   在老版本（Minecraft 1.19.4 之前）会检查方块的材质是否被认为是可替换的。
         *   新版本则直接使用方块状态自带的 {@code isReplaceable()} 方法来判断。
         * </p>
         *
         * @param state 要检查的方块状态
         * @return 如果方块状态可以被替换，则返回 {@code true}；否则返回 {@code false}
         */
        public static boolean isReplaceable(BlockState state) {
            //#if MC < 11904
            //$$ return state.getMaterial().isReplaceable();
            //#else
            return state.isReplaceable();
            //#endif
        }

        public @Nullable Direction getLookHorizontalDirection() {
            return lookHorizontalDirection;
        }

        public @Nullable Direction getLookDirectionPitch() {
            return lookDirectionPitch;
        }

        /**
         * 设置放置时玩家的视角朝向
         *
         * @param lookDirection 视角朝向
         * @return 当前 Action 实例
         */
        public Action setLookDirection(Direction lookDirection) {
            this.lookHorizontalDirection = lookDirection;
            this.lookDirectionPitch = lookDirection;
            return this;
        }

        /**
         * 设置放置时玩家的视角朝向
         *
         * @param lookHorizontalDirection 横轴视角朝向
         * @param lookDirectionPitch 纵轴视角朝向
         * @return 当前 Action 实例
         */
        public Action setLookDirection(Direction lookHorizontalDirection, Direction lookDirectionPitch) {
            this.lookHorizontalDirection = lookHorizontalDirection;
            this.lookDirectionPitch = lookDirectionPitch;
            return this;
        }

        public @Nullable Item[] getRequiredItems(Block backup) {
            return clickItems == null ? new Item[]{backup.asItem()} : clickItems;
        }

        public @NotNull Map<Direction, Vec3d> getSides() {
            if (this.sides == null) {
                this.sides = new HashMap<>();
                for (Direction d : Direction.values()) {
                    this.sides.put(d, new Vec3d(0, 0, 0));
                }
            }

            return this.sides;
        }

        /**
         * 设置可以和方块交互的所有方向（例如：上下左右）。
         * <p>
         * 这个方法会找到所有指定的方向轴（比如：X轴、Y轴、Z轴）上的所有方向，
         * 然后把这些方向都设置为可以交互的方向，并且设置默认的点击偏移量为 (0,0,0)。
         * 简单来说，就是设置你可以从哪些方向点击这个方块。
         *
         * @param axis 要设置的方向轴列表（例如：只允许上下方向，不允许左、右方向）
         * @return 当前 Action 实例，方便你继续设置其他属性
         */
        public Action setSides(Direction.Axis... axis) {
            Map<Direction, Vec3d> sides = new HashMap<>();

            for (Direction.Axis a : axis) {
                for (Direction d : Direction.values()) {
                    if (d.getAxis() == a) {
                        sides.put(d, new Vec3d(0, 0, 0));
                    }
                }
            }

            this.sides = sides;
            return this;
        }

        /**
         * 设置操作的有效方向，并指定每个方向对应的偏移量。
         * <p>
         *   这个方法允许你详细地指定在放置方块时，哪些方向是可以进行交互的。
         *   例如，你可以设置只有在方块的上方或下方才能进行放置，并为这些方向设置特定的偏移量。
         * </p>
         * <p>
         *   通过调整偏移量，你可以更精确的控制点击的位置，从而实现一些特殊的放置效果。
         *   例如，你可以通过偏移量来点击方块的边缘，而不是中心。
         * </p>
         *
         * @param sides 包含方向和偏移量的 Map，其中 Key 是方向 (Direction)，Value 是偏移量 (Vec3d)。
         *              如果某个方向没有对应的偏移量，则使用默认的 (0, 0, 0)。
         * @return 当前 Action 实例，便于链式调用。
         *         通过链式调用，你可以连续设置多个属性，使代码更加简洁易读。
         */
        public Action setSides(Map<Direction, Vec3d> sides) {
            this.sides = sides;
            return this;
        }

        /**
         * 设置操作的有效方向。
         * <p>
         * 为每个传入的方向设置默认的偏移值 (0, 0, 0)。
         *
         * @param directions 要设置的方向数组
         * @return 当前 Action 实例，便于链式调用
         */
        public Action setSides(Direction... directions) {
            Map<Direction, Vec3d> sides = new HashMap<>();

            for (Direction d : directions) {
                sides.put(d, new Vec3d(0, 0, 0));
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
        public @Nullable Direction getValidSide(ClientWorld world, BlockPos pos) {
            Map<Direction, Vec3d> sides = getSides();

            List<Direction> validSides = new ArrayList<>();

            for (Direction side : sides.keySet()) {
                if (LitematicaMixinMod.PRINT_IN_AIR.getBooleanValue() && !this.requiresSupport) {
                    return side;
                } else {
                    BlockPos neighborPos = pos.offset(side);
                    BlockState neighborState = world.getBlockState(neighborPos);

                    if (neighborState.contains(SlabBlock.TYPE) && neighborState.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
                        continue;
                    }

                    if (canBeClicked(world, pos.offset(side)) && // Handle unclickable grass for example
                            !isReplaceable(world.getBlockState(pos.offset(side))))
                        validSides.add(side);
                }
            }

            if (validSides.isEmpty()) return null;

            // Try to pick a side that doesn't require shift
            for (Direction validSide : validSides) {
                if (!Implementation.isInteractive(world.getBlockState(pos.offset(validSide)).getBlock())) {
                    return validSide;
                }
            }

            return validSides.get(0);
        }

        public Action setItem(Item item) {
            return this.setItems(item);
        }

        public Action setItems(Item... items) {
            this.clickItems = items;
            return this;
        }

        public Action setRequiresSupport(boolean requiresSupport) {
            this.requiresSupport = requiresSupport;
            return this;
        }

        /**
         * 设置是否需要支撑方块才能放置，默认为需要。
         * <p>
         *   如果设置为 {@code true}，则在放置方块时，会检查目标位置的周围是否有其他方块支撑。
         *   如果设置为 {@code false}，则无论目标位置周围是否有支撑，都可以放置方块。
         * </p>
         *
         * @return 当前 Action 实例，便于链式调用。
         */
        public Action setRequiresSupport() {
            return this.setRequiresSupport(true);
        }

        /**
         * 设置是否需要按住 Shift 键才能放置，默认为需要。
         * <p>
         *   如果设置为 {@code true}，则在放置方块时，会检查玩家是否按住了 Shift 键。
         *   如果设置为 {@code false}，则无论玩家是否按住 Shift 键，都可以放置方块。
         * </p>
         *
         * @return 当前 Action 实例，便于链式调用。
         */
        public Action setUseShift(boolean useShift) {
            this.useShift = useShift;
            return this;
        }

        public Action setUseShift() {
            return this.setUseShift(true);
        }

        public void queueAction(Printer.Queue queue, BlockPos center, Direction side, boolean useShift) {
//            System.out.println("Queued click?: " + center.offset(side).toString() + ", side: " + side.getOpposite());

            if (LitematicaMixinMod.PRINT_IN_AIR.getBooleanValue() && !this.requiresSupport) {
                queue.queueClick(center, side.getOpposite(), getSides().get(side),
                        useShift);
            } else {
                queue.queueClick(center.offset(side), side.getOpposite(), getSides().get(side),
                        useShift);
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
         * @param world 当前的 ClientWorld 实例
         * @param pos   块的位置
         * @return 第一个有效侧面，如果不存在则返回 null
         */
        @Override
        public @Nullable Direction getValidSide(ClientWorld world, BlockPos pos) {
            for (Direction side : getSides().keySet()) {
                return side;
            }
            return null;
        }
    }
}
