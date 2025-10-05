package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.fabricmc.fabric.mixin.content.registry.AxeItemAccessor;
import net.minecraft.block.*;
import net.minecraft.block.enums.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlacementGuide extends PrinterUtils {
    public static Map<BlockPos, Integer> posMap = new HashMap<>();
    @NotNull
    protected final MinecraftClient client;

    Item[] compostableItems = Arrays.stream(ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.keySet().toArray(ItemConvertible[]::new))
            .map(ItemConvertible::asItem)
            .toArray(Item[]::new);

    public PlacementGuide(@NotNull MinecraftClient client) {
        this.client = client;
    }

    public @Nullable Action getAction(World world, WorldSchematic worldSchematic, BlockPos pos) {
        var requiredState = worldSchematic.getBlockState(pos);
        // 提前缓存 requiredState 提升性能
        var state = State.get(requiredState, world.getBlockState(pos));
        if (!requiredState.canPlaceAt(world, pos) || state == State.CORRECT)
            return null;
        for (ClassHook hook : ClassHook.values()) {
            for (Class<?> clazz : hook.classes) {
                if (clazz != null && clazz.isInstance(requiredState.getBlock())) {
                    return buildAction(world, worldSchematic, hook, pos, state);
                }
            }
        }
        return buildAction(world, worldSchematic, ClassHook.DEFAULT, pos, state);
    }

    private @Nullable Action buildAction(World world, WorldSchematic worldSchematic, ClassHook requiredType, BlockPos pos, State state) {
        BlockState currentState = world.getBlockState(pos);
        BlockState requiredState = worldSchematic.getBlockState(pos);

        if (LitematicaMixinMod.SKIP_WATERLOGGED_BLOCK.getBooleanValue() && PlaceUtils.isWaterRequired(requiredState))
            return null;

        if (LitematicaMixinMod.PRINT_WATER.getBooleanValue() && PlaceUtils.isWaterRequired(requiredState)) {
            if (currentState.getBlock() instanceof IceBlock) {
                BreakManager.addBlockToBreak(pos);
                return null;
            }
            if (!PlaceUtils.isCorrectWaterLevel(requiredState, currentState)) {
                if (!currentState.isAir() && !(currentState.getBlock() instanceof FluidBlock)) {
                    if (LitematicaMixinMod.BREAK_WRONG_BLOCK.getBooleanValue()) {
                        BreakManager.addBlockToBreak(pos);
                    }
                    return null;
                }
                return new Action().setItem(Items.ICE);
            }
        }


        if (state == State.MISSING_BLOCK) switch (requiredType) {
            case TORCH -> {
                return Optional.ofNullable(requiredState.contains(WallTorchBlock.FACING) ?
                                requiredState.get(Properties.HORIZONTAL_FACING) :
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
                        .setSides((requiredState.get(Properties.FACING))
                                .getOpposite())
                        .setRequiresSupport();
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
                return new Action().setLookDirection(requiredState.get(AnvilBlock.FACING).rotateYCounterclockwise());
            }
            case HOPPER -> {
                Direction facing = requiredState.get(Properties.HOPPER_FACING);
                return new Action().setSides(facing);
            }
            case NETHER_PORTAL -> {

                boolean canCreatePortal = net.minecraft.world.dimension.NetherPortal.getNewPortal(world, pos, Direction.Axis.X).isPresent();
                if (canCreatePortal) {
                    return new Action().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                }
            }
            case COCOA -> {
                return new Action().setSides(requiredState.get(Properties.HORIZONTAL_FACING));
            }
            //#if MC >= 12003
            case CRAFTER -> {
                var orientation = requiredState.get(Properties.ORIENTATION);
                Direction facing = orientation.getFacing().getOpposite();
                Direction rotation = orientation.getRotation().getOpposite();
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
                Direction facing = requiredState.get(Properties.HORIZONTAL_FACING).getOpposite();
                ChestType type = requiredState.get(Properties.CHEST_TYPE);
                Map<Direction, Vec3d> noChestSides = new HashMap<>();

                for (Direction side : Direction.values()) {
                    if (world.getBlockState(pos.offset(side)).getBlock() instanceof ChestBlock) {
                        continue;
                    }
                    noChestSides.put(side, Vec3d.ZERO);
                }


                if (type == ChestType.SINGLE) {
                    for (Direction side : Properties.HORIZONTAL_FACING.getValues()) {
                        if (!noChestSides.containsKey(side)) {
                            return new Action().setLookDirection(facing).setUseShift();
                        }
                        return new Action().setSides(noChestSides).setLookDirection(facing);
                    }
                } else {
                    Direction chestFacing = facing;
                    if (type == ChestType.LEFT) {
                        chestFacing = facing.rotateYCounterclockwise();
                    } else if (type == ChestType.RIGHT) {
                        chestFacing = facing.rotateYClockwise();
                    }
                    if (world.getBlockState(pos.offset(chestFacing)).getBlock() instanceof ChestBlock) {
                        return new Action().setSides(Map.of(chestFacing, Vec3d.ZERO));
                    } else {
                        return new Action().setSides(noChestSides).setLookDirection(facing).setUseShift();
                    }
                }
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
            //超级无敌写史高手请求出战
            case DEAD_CORAL -> {
                // 获取基本属性
                Block block = requiredState.getBlock();
                boolean isWallFan = block instanceof DeadCoralWallFanBlock;
                Direction facing = isWallFan ? requiredState.get(Properties.HORIZONTAL_FACING).getOpposite()
                        : Direction.DOWN;

                // 如果不是死亡珊瑚或不需要替换，直接返回基础Action
                if (!LitematicaMixinMod.REPLACE_CORAL.getBooleanValue()) {
                    return new Action()
                            .setSides(facing)
                            .setRequiresSupport();
                }

                if (playerHasAccessToItem(client.player, block.asItem()) || client.player.isCreative()) {
                    return new Action()
                            .setSides(facing)
                            .setRequiresSupport();
                }

                String key = block.getTranslationKey();
                //珊瑚扇
                if (block instanceof DeadCoralFanBlock) {
                    String type = key.replace("block.minecraft.dead_", "")
                            .replace("_coral_wall_fan", "")
                            .replace("_coral_fan", "");

                    Item fanItem = switch (type) {
                        case "tube" -> Items.TUBE_CORAL_FAN;
                        case "brain" -> Items.BRAIN_CORAL_FAN;
                        case "bubble" -> Items.BUBBLE_CORAL_FAN;
                        case "fire" -> Items.FIRE_CORAL_FAN;
                        case "horn" -> Items.HORN_CORAL_FAN;
                        default -> null;
                    };

                    return new Action()
                            .setItem(fanItem)
                            .setSides(facing)
                            .setRequiresSupport();
                }

                //非方块型珊瑚
                if (block instanceof DeadCoralBlock) {
                    //例子：block.minecraft.dead_tube_coral
                    String type = key.replace("block.minecraft.dead_", "").replace("_coral", "");
                    return new Action().setItem(
                            switch (type) {
                                case "tube" -> Items.TUBE_CORAL;
                                case "brain" -> Items.BRAIN_CORAL;
                                case "bubble" -> Items.BUBBLE_CORAL;
                                case "fire" -> Items.FIRE_CORAL;
                                case "horn" -> Items.HORN_CORAL;
                                default -> null;
                            }
                    ).setSides(Direction.DOWN).setRequiresSupport();
                }

            }
            case FIRE -> {
                if (requiredState.getBlock() instanceof SoulFireBlock) return new Action().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                for (Direction direction : Direction.values()) {
                    if (direction == Direction.DOWN) continue;
                    if ((Boolean) getPropertyByName(requiredState, direction.name())) {
                        return new Action().setSides(direction).setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                    }
                }
                return new Action().setSides(Direction.DOWN).setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
            }
            case OBSERVER -> {
                var facing = requiredState.get(Properties.FACING);
                var detectBlockPos = pos.offset(facing);
                if (LitematicaMixinMod.SAFELY_OBSERVER.getBooleanValue() &&
                        playerHasAccessToItem(client.player, Items.OBSERVER) &&
                        world.getBlockState(detectBlockPos) != worldSchematic.getBlockState(detectBlockPos)) {
                    ZxyUtils.actionBar("[侦测器安全放置] 侦测面方块不正确，跳过放置！");
                    return null;
                }
                return new Action().setLookDirection(facing);
            }
            case LADDER -> {
                var facing = requiredState.get(LadderBlock.FACING);
                return new Action().setSides(facing).setLookDirection(facing.getOpposite());
            }
            case LANTERN -> {
                if (requiredState.get(LanternBlock.HANGING))
                    return new Action().setLookDirection(Direction.UP);
                return new Action().setLookDirection(Direction.DOWN);
            }
            case ROD -> {
                var requiredBlock = requiredState.getBlock();
                var facing = requiredState.get(RodBlock.FACING);
                var forwardState = world.getBlockState(pos.offset(facing));
//                var backState = world.getBlockState(pos.offset(facing.getOpposite()));
                var forwardStateSchematic = worldSchematic.getBlockState(pos.offset(facing));
//                var backStateSchematic = worldSchematic.getBlockState(pos.offset(facing.getOpposite()));
//                StringUtils.printChatMessage("前 " + forwardState.getBlock().getName().getString());
//                StringUtils.printChatMessage("后 " + backState.getBlock().getName().getString());
//                StringUtils.printChatMessage("投影-前 " + forwardStateSchematic.getBlock().getName().getString());
//                StringUtils.printChatMessage("投影-后 " + backStateSchematic.getBlock().getName().getString());

                // 如果前面朝向自己的末地烛，而放置方式相反，那么反向放置
                if (forwardState.isOf(requiredBlock)
                        && forwardState.get(RodBlock.FACING) == facing.getOpposite()) {
                    return new Action().setSides(facing);
                }
                // 如果投影中后面有相同朝向的末地烛，则先跳过放置
                if (forwardStateSchematic.isOf(requiredBlock)
                        && forwardStateSchematic.get(RodBlock.FACING) == facing) {
                    // 但是这个投影已经被正确填装时可以打印
                    if (forwardStateSchematic == forwardState) return new Action().setSides(facing.getOpposite());
                    return null;
                }
                return new Action().setSides(facing.getOpposite());
            }
            case TRIPWIRE_HOOK -> {
                var facing = requiredState.get(TripwireHookBlock.FACING);
                return new Action().setSides(facing);
            }
            case RAIL -> {
                Action action = new Action();
                RailShape shape;
                if (requiredState.getBlock() instanceof RailBlock)
                    shape = requiredState.get(Properties.RAIL_SHAPE);
                else
                    shape = requiredState.get(Properties.STRAIGHT_RAIL_SHAPE);

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
            case SKIP -> {
                return null;
            }
            default -> {
                Action action = new Action();
                Block block = requiredState.getBlock();

                if (block instanceof WallMountedBlock) {
                    Direction side = requiredState.get(Properties.HORIZONTAL_FACING);
                    BlockFace face = requiredState.get(Properties.BLOCK_FACE);

                    // 简化方向判断逻辑
                    Direction sidePitch = face == BlockFace.CEILING ? Direction.UP
                            : face == BlockFace.FLOOR ? Direction.DOWN
                            : side;

                    if (face != BlockFace.WALL) {
                        side = side.getOpposite();
                    }

                    return new Action().setSides(side).setLookDirection(side.getOpposite(), sidePitch);
                }

                if (block instanceof HorizontalFacingBlock ||
                        block instanceof StonecutterBlock
                        //#if MC >= 11904
                        || block instanceof FlowerbedBlock
                        //#endif
                ) {
                    Direction facing = requiredState.get(Properties.HORIZONTAL_FACING);
                    if (block instanceof FenceGateBlock // 栅栏门
                    ) facing = facing.getOpposite();
                    action.setLookDirection(facing.getOpposite());
                }

                if (block instanceof FacingBlock) {
                    Direction facing = requiredState.get(Properties.FACING);

                    if (block instanceof PistonBlock) {
                        if (client.isInSingleplayer()) action.setWaitTick(2);
                    }

                    action.setLookDirection(facing.getOpposite());
                }

                if (block instanceof BlockWithEntity) {
                    Direction facing;
                    if (requiredState.contains(Properties.HORIZONTAL_FACING)) {
                        facing = requiredState.get(Properties.HORIZONTAL_FACING);
                        //#if MC >= 11904
                        if (block instanceof DecoratedPotBlock
                            ||block instanceof CampfireBlock)
                            facing = facing.getOpposite();
                        //#endif
                        action.setSides(facing).setLookDirection(facing.getOpposite());
                    }
                    if (requiredState.contains(Properties.FACING)) {
                        facing = requiredState.get(Properties.FACING);
                        action.setSides(facing).setLookDirection(facing.getOpposite());
                    }
                }

                //方块型珊瑚的替换
                if (LitematicaMixinMod.REPLACE_CORAL.getBooleanValue() && block.getTranslationKey().endsWith("_coral_block")) {
                    //例子：block.minecraft.dead_tube_coral
                    String type = block.getTranslationKey().replace("block.minecraft.dead_", "").replace("_coral_block", "");
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
        else if (state == State.WRONG_STATE) switch (requiredType) {
            case SLAB -> {
                if (requiredState.get(SlabBlock.TYPE) == SlabType.DOUBLE) {
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
            case DOOR, TRAPDOOR -> {
                //判断门是不是铁制的，如果是就直接返回
                if (requiredState.isOf(Blocks.IRON_DOOR) || requiredState.isOf(Blocks.IRON_TRAPDOOR)) break;
                if (requiredState.get(Properties.OPEN) != currentState.get(Properties.OPEN)) {
                    return new ClickAction();
                }

            }
            case FENCE_GATE -> {
                var facing = requiredState.get(Properties.HORIZONTAL_FACING);
                if (facing.getOpposite() == currentState.get(Properties.HORIZONTAL_FACING) ||
                        requiredState.get(Properties.OPEN) != currentState.get(Properties.OPEN))
                    return new ClickAction().setSides(facing.getOpposite()).setLookDirection(facing);
            }
            case LEVER -> {
                if (requiredState.get(LeverBlock.POWERED) != currentState.get(LeverBlock.POWERED))
                    return new ClickAction();

            }
            case CANDLES -> {
                if (currentState.get(Properties.CANDLES) < requiredState.get(Properties.CANDLES))
                    return new ClickAction().setItem(requiredState.getBlock().asItem()).setRequiresSupport();

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
                if (LitematicaMixinMod.NOTE_BLOCK_TUNING.getBooleanValue() && !Objects.equals(requiredState.get(NoteBlock.NOTE), currentState.get(NoteBlock.NOTE)))
                    return new ClickAction();

            }
            case CAMPFIRE -> {
                if (!requiredState.get(CampfireBlock.LIT) && currentState.get(CampfireBlock.LIT))
                    return new ClickAction().setItems(Implementation.SHOVELS).setSides(Direction.UP);
                if (requiredState.get(CampfireBlock.LIT) && !currentState.get(CampfireBlock.LIT))
                    return new ClickAction().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE);
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
                    if ((Boolean) getPropertyByName(requiredState, direction.name())) {
                        return new Action().setSides(direction).setLookDirection(direction);
                    }
                }
            }
            case CAULDRON -> {
                if (currentState.get(LeveledCauldronBlock.LEVEL) > requiredState.get(LeveledCauldronBlock.LEVEL)) {
                    if (playerHasAccessToItem(client.player, Items.GLASS_BOTTLE))
                        return new ClickAction().setItem(Items.GLASS_BOTTLE);
                    else
                        client.inGameHud.setOverlayMessage(Text.of("降低炼药锅内水位需要 §l§6" + Items.GLASS_BOTTLE.getName().toString()), false);
                }
                if (currentState.get(LeveledCauldronBlock.LEVEL) < requiredState.get(LeveledCauldronBlock.LEVEL))
                    if (playerHasAccessToItem(client.player, Items.POTION))
                        return new ClickAction().setItem(Items.POTION);
                    else
                        client.inGameHud.setOverlayMessage(Text.of("增加炼药锅内水位需要 §l§6" + Items.POTION.getName().toString()), false);
            }
            case DAYLIGHT_DETECTOR -> {
                if (currentState.get(DaylightDetectorBlock.INVERTED) != requiredState.get(DaylightDetectorBlock.INVERTED)) return new ClickAction();
            }
            case FIRE -> {
                if (!requiredState.get(FireBlock.AGE).equals(currentState.get(FireBlock.AGE))) return null;
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
                if (!LitematicaMixinMod.FILL_COMPOSTER.getBooleanValue()) return null;
                if (currentState.get(ComposterBlock.LEVEL) < requiredState.get(ComposterBlock.LEVEL)) {
                    return new ClickAction().setItems(compostableItems);
                }
            }
        }
        else if (state == State.WRONG_BLOCK) switch (requiredType) {
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
                    Block content = potBlock.getContent();
                    if (content != Blocks.AIR) {
                        return new ClickAction().setItem(content.asItem());
                    }
                }
            }

            case CAULDRON -> {
                if (Arrays.asList(requiredType.classes).contains(currentState.getBlock().getClass()))
                    return null;
                else if (LitematicaMixinMod.BREAK_WRONG_BLOCK.getBooleanValue() && BreakManager.canBreakBlock(pos))
                    BreakManager.addBlockToBreak(pos);
            }

            default -> {
                if (LitematicaMixinMod.REPLACE_CORAL.getBooleanValue() && requiredState.getBlock().getTranslationKey().contains("coral")) {
                    return null;
                }

                boolean breakWrongBlock = LitematicaMixinMod.BREAK_WRONG_BLOCK.getBooleanValue();
                boolean breakExtraBlock = LitematicaMixinMod.BREAK_EXTRA_BLOCK.getBooleanValue();

                if (breakWrongBlock || breakExtraBlock) {
                    if (BreakManager.canBreakBlock(pos)) {
                        if (breakWrongBlock && !requiredState.isOf(Blocks.AIR)) {
                            BreakManager.addBlockToBreak(pos);
                        } else if (breakExtraBlock && requiredState.isOf(Blocks.AIR)) {
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
                AbstractTorchBlock.class
                //#else
                //$$ TorchBlock.class
                //#endif
        ), // 火把
        SLAB(SlabBlock.class), // 台阶
        STAIR(StairsBlock.class), // 楼梯
        TRAPDOOR(TrapdoorBlock.class), // 活板门
        PILLAR(PillarBlock.class), // 去皮原木
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
        TRIPWIRE_HOOK(TripwireHookBlock.class), // 绊线钩
        RAIL(AbstractRailBlock.class), // 铁轨

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
        GLOW_LICHEN(GlowLichenBlock.class), // 发光地衣
        FIRE(FireBlock.class, SoulFireBlock.class), // 火，灵魂火
        REDSTONE(RedstoneWireBlock.class), //红石粉
        FENCE_GATE(FenceGateBlock.class), // 栅栏门
        LEVER(LeverBlock.class), // 拉杆
        CAULDRON(CauldronBlock.class, LavaCauldronBlock.class, LeveledCauldronBlock.class), // 炼药锅
        DAYLIGHT_DETECTOR(DaylightDetectorBlock.class), // 阳光探测器
        COMPOSTER(ComposterBlock.class), // 堆肥桶

        // 其他
        FARMLAND(FarmlandBlock.class), // 耕地
        DIRT_PATH(DirtPathBlock.class), // 泥土小径
        DEAD_CORAL(AbstractCoralBlock.class), // 死珊瑚
        NETHER_PORTAL(NetherPortalBlock.class), // 下界传送门
        SKIP(SkullBlock.class, SignBlock.class, FluidBlock.class, BubbleColumnBlock.class, LilyPadBlock.class), // 跳过
        DEFAULT; // 默认

        private final Class<?>[] classes;

        ClassHook(Class<?>... classes) {
            this.classes = classes;
        }
    }

    public static class Action {
        protected Map<Direction, Vec3d> sides;
        protected Direction lookDirection;
        protected Direction lookDirectionPitch;
        @Nullable
        protected Item[] clickItems; // null == 任意方块
        protected boolean requiresSupport = false;
        protected boolean useShift = false;
        protected int waitTick = 0;

        public Action() {
            this.sides = new HashMap<>();
            for (Direction direction : Direction.values()) {
                sides.put(direction, new Vec3d(0, 0, 0));
            }
        }

        public @Nullable Direction getLookDirection() {
            return lookDirection;
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
            this.lookDirection = lookDirection;
            this.lookDirectionPitch = lookDirection;
            return this;
        }

        /**
         * 设置放置时玩家的视角朝向
         *
         * @param lookDirection 横轴视角朝向
         * @param lookDirectionPitch 纵轴视角朝向
         * @return 当前 Action 实例
         */
        public Action setLookDirection(Direction lookDirection, Direction lookDirectionPitch) {
            this.lookDirection = lookDirection;
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
         * 设置放置的有效面，以及指定每个面对应的偏移位置。
         * <p>
         *   这个方法允许你指定放置方块时，可以用哪些方向交互。
         *   例如，你可以设置只有在方块的上方或下方才能进行放置。
         * </p>
         * <p>
         *   你也可以调整偏移量，进行更精确的控制点击的位置，从而实现一些特殊的放置效果。
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
         * 设置放置的有效面。
         * <p>
         * 传入的方向参数均设置默认的偏移值 (0, 0, 0)。
         * </p>
         * @param directions 要设置的方向（可以是多个）
         * @return 当前 Action 实例
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

            // 遍历所有侧面，检查每个侧面是否可用
            for (Direction side : sides.keySet()) {
                BlockPos neighborPos = pos.offset(side);
                BlockState neighborState = world.getBlockState(neighborPos);

                if (LitematicaMixinMod.PRINT_IN_AIR.getBooleanValue() &&
                        !this.requiresSupport &&
                        !Implementation.isInteractive(neighborState.getBlock())
                ) return side;


                // 检查该侧面是否可以被点击且不可替换
                if (canBeClicked(world, neighborPos) && !PlaceUtils.isReplaceable(neighborState)) {
                    validSides.add(side);
                }
            }

            if (validSides.isEmpty()) return null;

            // 选择一个不需要潜行放置的面
            for (Direction validSide : validSides) {
                BlockState requiredState = world.getBlockState(pos);
                BlockState sideBlockState = world.getBlockState(pos.offset(validSide));
                if (!Implementation.isInteractive(sideBlockState.getBlock()) && requiredState.canPlaceAt(world, pos)) {
                    return validSide;
                }
            }

            return validSides.get(0);
        }

        /**
         * 设置打印这种方块对应使用的物品
         * @param item 要选择的物品
         * @return 当前 Action 实例
         */
        public Action setItem(Item item) {
            return this.setItems(item);
        }

        /**
         * 设置打印这种方块对应使用的物品（多个）
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
         * @return 当前 Action 实例
         */
        public Action setUseShift() {
            return this.setUseShift(true);
        }

        /**
         * 设置放置后等待的刻数
         * @param waitTick 游戏刻数量
         * @return 当前 Action 实例
         */
        public Action setWaitTick(int waitTick) {
            this.waitTick = waitTick;
            return this;
        }

        /**
         * 获取放置后需要等待的游戏刻
         * @return 整数
         */
        public int getWaitTick() {
            return this.waitTick;
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
