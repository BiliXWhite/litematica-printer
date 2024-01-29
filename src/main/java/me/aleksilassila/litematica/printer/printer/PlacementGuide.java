package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.mixin.FlowerPotBlockAccessor;
import net.fabricmc.fabric.mixin.content.registry.AxeItemAccessor;
import net.minecraft.block.*;
import net.minecraft.block.enums.Attachment;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlacementGuide extends PrinterUtils {
    @NotNull
    protected final MinecraftClient client;

    public PlacementGuide(@NotNull MinecraftClient client) {
        this.client = client;
    }

    public @Nullable Action getAction(World world, WorldSchematic worldSchematic, BlockPos pos) {
        for (ClassHook hook : ClassHook.values()) {
            for (Class<?> clazz : hook.classes) {
                if (clazz != null  && clazz.isInstance(worldSchematic.getBlockState(pos).getBlock())) {
                    return buildAction(world, worldSchematic, pos, hook);
                }
            }
        }

        return buildAction(world, worldSchematic, pos, ClassHook.DEFAULT);
    }

//    public static Placement getPlacement(BlockState requiredState, MinecraftClient client) {
//        Placement placement = _getPlacement(requiredState, client);
//        return placement.setItem(placement.item == null ? requiredState.getBlock().asItem() : placement.item);
//    }

    @SuppressWarnings("EnhancedSwitchMigration")
    private @Nullable Action buildAction(World world, WorldSchematic worldSchematic, BlockPos pos, ClassHook requiredType) {
        BlockState requiredState = worldSchematic.getBlockState(pos);
        BlockState currentState = world.getBlockState(pos);

        if (requiredState.getBlock() instanceof FluidBlock) {
            return null;
        } else if (currentState.getBlock() instanceof FluidBlock) {
            if (currentState.get(FluidBlock.LEVEL) == 0 && !LitematicaMixinMod.shouldReplaceFluids) {
                return null;
            }
        }

        if (!requiredState.canPlaceAt(world, pos)) {
            return null;
        }

        State state = State.get(requiredState, currentState);

        if (state == State.CORRECT) return null;
        else if (state == State.MISSING_BLOCK &&
                !requiredState.canPlaceAt(world, pos)) {
            return null;
        }

        if (state == State.MISSING_BLOCK) {
            switch (requiredType) {
                case WALLTORCH:
                case AMETHYST: {
                    return new Action()
                            .setSides(((Direction) getPropertyByName(requiredState, "FACING"))
                                    .getOpposite())
                            .setRequiresSupport();
                }
                case ROD:
                case SHULKER: {
                    return new Action().setSides(
                            ((Direction) getPropertyByName(requiredState, "FACING"))
                                    .getOpposite());
                }
                case SLAB: {
                    return new Action().setSides(getSlabSides(world, pos, requiredState.get(SlabBlock.TYPE)));
                }
                case STAIR: {
                    Direction half = getHalf(requiredState.get(StairsBlock.HALF));

                    Map<Direction, Vec3d> sides = new HashMap<>();
                    for (Direction direction : horizontalDirections) {
                        sides.put(direction, Vec3d.of(half.getVector()).multiply(0.25));
                    }

                    sides.put(half, new Vec3d(0, 0, 0));

                    return new Action()
                        .setSides(sides)
                        .setLookDirection(requiredState.get(StairsBlock.FACING));
                }
                case TRAPDOOR: {
                    Direction half = getHalf(requiredState.get(TrapdoorBlock.HALF));

                    Map<Direction, Vec3d> sides = new HashMap<>(){{
                        put(requiredState.get(StairsBlock.FACING).getOpposite(),
                            Vec3d.of(half.getVector()).multiply(0.25));
                        put(half, new Vec3d(0, 0, 0));
                    }};

                    return new Action()
                            .setSides(sides)
                            .setLookDirection(requiredState.get(StairsBlock.FACING).getOpposite());
                }
                case PILLAR: {
                    Action action = new Action().setSides(requiredState.get(PillarBlock.AXIS));

                    // If is stripped log && should use normal log instead
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
                case ANVIL: {
                    return new Action().setLookDirection(requiredState.get(AnvilBlock.FACING).rotateYCounterclockwise());
                }
                case HOPPER: // FIXME add all sides
                case COCOA: {
                    return new Action().setSides((Direction) getPropertyByName(requiredState, "FACING"));
                }
                case LEVER:
//                case BUTTON: {
//                    Direction side;
//                    switch ((WallMountLocation) getPropertyByName(requiredState, "FACE")) {
//                        case FLOOR: {
//                            side = Direction.DOWN;
//                            break;
//                        }
//                        case CEILING: {
//                            side = Direction.UP;
//                            break;
//                        }
//                        default: {
//                            side = ((Direction) getPropertyByName(requiredState, "FACING")).getOpposite();
//                            break;
//                        }
//                    }
//
//                    Direction look = getPropertyByName(requiredState, "FACE") == WallMountLocation.WALL ?
//                            null : (Direction) getPropertyByName(requiredState, "FACING");
//
//                    return new Action().setSides(side).setLookDirection(look).setRequiresSupport();
//                }
    //            case GRINDSTONE -> { // Tese are broken
    //                Direction side = switch ((WallMountLocation) getPropertyByName(requiredState, "FACE")) {
    //                    case FLOOR -> Direction.DOWN;
    //                    case CEILING -> Direction.UP;
    //                    default -> (Direction) getPropertyByName(requiredState, "FACING");
    //                };
    //
    //                Direction look = getPropertyByName(requiredState, "FACE") == WallMountLocation.WALL ?
    //                        null : (Direction) getPropertyByName(requiredState, "FACING");
    //
    //                return new Placement(Direction.DOWN, // FIXME test
    //                        Vec3d.of(side.getVector()).multiply(0.5),
    //                        look);
    //            }
                case GATE:
                case OBSERVER:
                case CAMPFIRE: {
                    return new Action()
                            .setLookDirection((Direction) getPropertyByName(requiredState, "FACING"));
                }
                case BED: {
                    if (requiredState.get(BedBlock.PART) != BedPart.FOOT) {
                        break;
                    } else {
                        return new Action().setLookDirection(requiredState.get(BedBlock.FACING));
                    }
                }
                case BELL: {
                    Direction side;
                    switch (requiredState.get(BellBlock.ATTACHMENT)) {
                        case FLOOR: {
                            side = Direction.DOWN;
                            break;
                        }
                        case CEILING: {
                            side = Direction.UP;
                            break;
                        }
                        default: {
                            side = requiredState.get(BellBlock.FACING);
                            break;
                        }
                    }

                    Direction look = requiredState.get(BellBlock.ATTACHMENT) != Attachment.SINGLE_WALL &&
                            requiredState.get(BellBlock.ATTACHMENT) != Attachment.DOUBLE_WALL ?
                            requiredState.get(BellBlock.FACING) : null;

                    return new Action().setSides(side).setLookDirection(look);
                }
                case DOOR: {
                    Map<Direction, Vec3d> sides = new HashMap<>();

                    Direction facing, hinge;
                    facing = hinge = requiredState.get(DoorBlock.FACING);

                    Vec3d hingeVec = new Vec3d(requiredState.get(DoorBlock.HINGE) == DoorHinge.RIGHT ?
                            0.25 : -0.25, 0, 0);

                    if (requiredState.get(DoorBlock.HINGE) == DoorHinge.RIGHT) {
                        hinge = hinge.rotateYClockwise();
                    } else {
                        hinge = hinge.rotateYCounterclockwise();
                    }

                    sides.put(hinge, new Vec3d(0, 0, 0));
                    sides.put(Direction.DOWN, hingeVec);
                    sides.put(facing, hingeVec);

                    return new Action().setLookDirection(requiredState.get(DoorBlock.FACING)).setSides(sides).setRequiresSupport();
                }
                case WALLSKULL: {
                    return new Action().setSides(requiredState.get(WallSkullBlock.FACING).getOpposite());
                }
                case FARMLAND:
                case DIRT_PATH: {
                    return new Action().setItem(Items.DIRT);
                }
                case BIG_DRIPLEAF_STEM: {
                    return new Action().setItem(Items.BIG_DRIPLEAF);
                }
                case SKIP: {
                    break;
                }
                case DEFAULT:
                default: { // Try to guess how the rest of the blocks are placed.
                    Direction look = null;

                    for (Property<?> prop : requiredState.getProperties()) {
                        if (prop instanceof DirectionProperty && prop.getName().equalsIgnoreCase("FACING")) {
                            look = ((Direction) requiredState.get(prop)).getOpposite();
                        }
                    }

                    Action placement = new Action().setLookDirection(look);

                    // If required == dirt path place dirt
                    if (requiredState.getBlock().equals(Blocks.DIRT_PATH) && !playerHasAccessToItem(client.player, requiredState.getBlock().asItem())) {
                        placement.setItem(Items.DIRT);
                    }

                    return placement;
                }
            }
        } else if (state == State.WRONG_STATE) {
            switch (requiredType) {
                case SLAB: {
                    if (requiredState.get(SlabBlock.TYPE) == SlabType.DOUBLE) {
                        SlabType requiredHalf = currentState.get(SlabBlock.TYPE) == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP;
                        return new Action().setSides(getSlabSides(world, pos, requiredHalf));
                    }

                    break;
                }
                case SNOW: {
                    int layers = currentState.get(SnowBlock.LAYERS);
                    if (layers < requiredState.get(SnowBlock.LAYERS)) {
                        Map<Direction, Vec3d> sides = new HashMap<>(){{
                            put(Direction.UP, new Vec3d(0,  (layers / 8d) - 1, 0));
                        }};
                        return new ClickAction().setItem(Items.SNOW).setSides(sides);
                    }

                    break;
                }
                case DOOR: {
                    if (requiredState.get(DoorBlock.OPEN) != currentState.get(DoorBlock.OPEN))
                        return new ClickAction();

                    break;
                }
                case LEVER: {
                    if (requiredState.get(LeverBlock.POWERED) != currentState.get(LeverBlock.POWERED))
                        return new ClickAction();

                    break;
                }
                case CANDLES: {
                    if ((Integer) getPropertyByName(currentState, "CANDLES") < (Integer) getPropertyByName(requiredState, "CANDLES"))
                        return new ClickAction().setItem(requiredState.getBlock().asItem());

                    break;
                }
                case PICKLES: {
                    if (currentState.get(SeaPickleBlock.PICKLES) < requiredState.get(SeaPickleBlock.PICKLES))
                        return new ClickAction().setItem(Items.SEA_PICKLE);

                    break;
                }
                case REPEATER: {
                    if (!Objects.equals(requiredState.get(RepeaterBlock.DELAY), currentState.get(RepeaterBlock.DELAY)))
                        return new ClickAction();

                    break;
                }
                case COMPARATOR: {
                    if (requiredState.get(ComparatorBlock.MODE) != currentState.get(ComparatorBlock.MODE))
                        return new ClickAction();

                    break;
                }
                case TRAPDOOR: {
                    if (requiredState.get(TrapdoorBlock.OPEN) != currentState.get(TrapdoorBlock.OPEN))
                        return new ClickAction();

                    break;
                }
                case GATE: {
                    if (requiredState.get(FenceGateBlock.OPEN) != currentState.get(FenceGateBlock.OPEN))
                        return new ClickAction();

                    break;
                }
                case NOTE_BLOCK: {
                    if (!Objects.equals(requiredState.get(NoteBlock.NOTE), currentState.get(NoteBlock.NOTE)))
                        return new ClickAction();

                    break;
                }
                case CAMPFIRE: {
                    if (requiredState.get(CampfireBlock.LIT) != currentState.get(CampfireBlock.LIT))
                        return new ClickAction().setItems(Implementation.SHOVELS);

                    break;
                }
                case PILLAR: {
                    Block stripped = AxeItemAccessor.getStrippedBlocks().get(currentState.getBlock());
                    if (stripped != null && stripped == requiredState.getBlock()) {
                        return new ClickAction().setItems(Implementation.AXES);
                    }
                    break;
                }
                case END_PORTAL_FRAME: {
                    if (requiredState.get(EndPortalFrameBlock.EYE) && !currentState.get(EndPortalFrameBlock.EYE))
                        return new ClickAction().setItem(Items.ENDER_EYE);

                    break;
                }
                case FLOWER_POT: { // Fixme test
                    Block content = ((FlowerPotBlockAccessor) requiredState).getContent();

                    if (content != null && content != Blocks.AIR) {
                        return new Action().setItem(content.asItem());
                    }

                    break;
                }
                case DEFAULT: {
                    if (currentState.getBlock().equals(Blocks.DIRT) && requiredState.getBlock().equals(Blocks.FARMLAND)) {
                        return new ClickAction().setItems(Implementation.HOES);
                    } else if (currentState.getBlock().equals(Blocks.DIRT) && requiredState.getBlock().equals(Blocks.DIRT_PATH)) {
                        return new ClickAction().setItems(Implementation.SHOVELS);
                    }

                    break;
                }
            }
        } else if (state == State.WRONG_BLOCK) {
            switch (requiredType) {
                case FARMLAND: {
                    Block[] soilBlocks = new Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT_PATH};

                    for (Block soilBlock : soilBlocks) {
                        if (currentState.getBlock().equals(soilBlock))
                            return new ClickAction().setItems(Implementation.HOES);
                    }

                    break;
                }
                case DIRT_PATH: {
                    Block[] soilBlocks = new Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT,
                            Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.MYCELIUM, Blocks.PODZOL};

                    for (Block soilBlock : soilBlocks) {
                        if (currentState.getBlock().equals(soilBlock))
                            return new ClickAction().setItems(Implementation.SHOVELS);
                    }

                    break;
                }
                default: {
                    return null;
                }
            }
        }

        return null;
    }

    public static class Action {
        protected Map<Direction, Vec3d> sides;
        protected Direction lookDirection;
        @Nullable
        protected Item[] clickItems; // null == any

        protected boolean crouch = false;
        protected boolean requiresSupport = false;

        // If true, click target block, not neighbor

        public Action() {
            this.sides = new HashMap<>();
            for (Direction direction : Direction.values()) {
                sides.put(direction, new Vec3d(0, 0, 0));
            }
        }

        public Action(Direction side) {
            this(side, new Vec3d(0, 0, 0));
        }

        /**
         * {@link Action#Action(Direction, Vec3d)}
         */
        public Action(Map<Direction, Vec3d> sides) {
            this.sides = sides;
        }

        /**
         *
         * @param side The side pointing to the block that should be clicked
         * @param modifier defines where should be clicked exactly. Vector's
         *                 x component defines left and right offset, y
         *                 defines height variation and z how far away from
         *                 player. (0, 0, 0) means click happens in the middle
         *                 of the side that is being clicked. (0.5, -0.5, 0)
         *                 would mean right bottom corner when clicking a
         *                 vertical side. Therefore, z should only be used when
         *                 clicking horizontal surface.
         */
        public Action(Direction side, Vec3d modifier) {
            this.sides = new HashMap<>();
            this.sides.put(side, modifier);
        }

        /**
         * {@link Action#Action(Direction, Vec3d)}
         */
        @SafeVarargs
        public Action(Pair<Direction, Vec3d>... sides) {
            this.sides = new HashMap<>();
            for (Pair<Direction, Vec3d> side : sides) {
                this.sides.put(side.getLeft(), side.getRight());
            }
        }

        public Action(Direction.Axis axis) {
            this.sides = new HashMap<>();

            for (Direction d : Direction.values()) {
                if (d.getAxis() == axis) {
                    sides.put(d, new Vec3d(0, 0, 0));
                }
            }
        }

        public @Nullable Direction getLookDirection() {
            return lookDirection;
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

        public @Nullable Direction getValidSide(ClientWorld world, BlockPos pos) {
            Map<Direction, Vec3d> sides = getSides();

            List<Direction> validSides = new ArrayList<>();

            for (Direction side : sides.keySet()) {
                if (LitematicaMixinMod.shouldPrintInAir && !this.requiresSupport) {
                    return side;
                } else {
                    BlockPos neighborPos = pos.offset(side);
                    BlockState neighborState = world.getBlockState(neighborPos);

                    if (neighborState.contains(SlabBlock.TYPE) && neighborState.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
                        continue;
                    }

                    if (canBeClicked(world, pos.offset(side)) && // Handle unclickable grass for example
                            !world.getBlockState(pos.offset(side)).isReplaceable())
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

//        public Action setInvalidNeighbors(Direction... neighbors) {
//            List<Direction> dirs = Arrays.asList(Direction.values());
//            dirs.removeAll(Arrays.asList(neighbors));
//            this.neighbors = dirs.toArray(Direction[]::new);
//            return this;
//        }

        public Action setLookDirection(Direction lookDirection) {
            this.lookDirection = lookDirection;
            return this;
        }

        public Action setSides(Map<Direction, Vec3d> sides) {
            this.sides = sides;
            return this;
        }

        public Action setSides(Direction... directions) {
            Map<Direction, Vec3d> sides = new HashMap<>();

            for (Direction d : directions) {
                sides.put(d, new Vec3d(0, 0, 0));
            }

            this.sides = sides;
            return this;
        }

        public Action setItem(Item item) {
            return this.setItems(item);
        }

        public Action setItems(Item ...items) {
            this.clickItems = items;
            return this;
        }

        public Action setRequiresSupport(boolean requiresSupport) {
            this.requiresSupport = requiresSupport;
            return this;
        }

        public Action setRequiresSupport() {
            return this.setRequiresSupport(true);
        }

        public void queueAction(Printer.Queue queue, BlockPos center, Direction side, boolean useShift, boolean didSendLook) {
//            System.out.println("Queued click?: " + center.offset(side).toString() + ", side: " + side.getOpposite());

            if (LitematicaMixinMod.shouldPrintInAir && !this.requiresSupport) {
                queue.queueClick(center, side.getOpposite(), getSides().get(side),
                        useShift, didSendLook);
            } else {
                queue.queueClick(center.offset(side), side.getOpposite(), getSides().get(side),
                        useShift, didSendLook);
            }

        }
    }

    public static class ClickAction extends Action {
        @Override
        public void queueAction(Printer.Queue queue, BlockPos center, Direction side, boolean useShift, boolean didSendLook) {
//            System.out.println("Queued click?: " + center.toString() + ", side: " + side);
            queue.queueClick(center, side, getSides().get(side), false, false);
        }

        @Override
        public @Nullable Item[] getRequiredItems(Block backup) {
            return this.clickItems;
        }

        @Override
        public @Nullable Direction getValidSide(ClientWorld world, BlockPos pos) {
            for (Direction side : getSides().keySet()) {
                return side;
            }

            return null;
        }
    }

    enum ClassHook {
        // Placements
        ROD(Implementation.NewBlocks.ROD.clazz),
        WALLTORCH(WallTorchBlock.class, WallRedstoneTorchBlock.class),
        TORCH(TorchBlock.class),
        SLAB(SlabBlock.class),
        STAIR(StairsBlock.class),
        TRAPDOOR(TrapdoorBlock.class),
        PILLAR(PillarBlock.class),
        ANVIL(AnvilBlock.class),
        HOPPER(HopperBlock.class),
        //    GRINDSTONE(GrindstoneBlock.class),
//        BUTTON(AbstractButtonBlock.class),
        CAMPFIRE(CampfireBlock.class),
        SHULKER(ShulkerBoxBlock.class),
        BED(BedBlock.class),
        BELL(BellBlock.class),
        AMETHYST(Implementation.NewBlocks.AMETHYST.clazz),
        DOOR(DoorBlock.class),
        COCOA(CocoaBlock.class),
        OBSERVER(ObserverBlock.class),
        WALLSKULL(WallSkullBlock.class),

        // Only clicks
        FLOWER_POT(FlowerPotBlock.class),
        BIG_DRIPLEAF_STEM(BigDripleafStemBlock.class),
        SNOW(SnowBlock.class),
        CANDLES(Implementation.NewBlocks.CANDLES.clazz),
        REPEATER(RepeaterBlock.class),
        COMPARATOR(ComparatorBlock.class),
        PICKLES(SeaPickleBlock.class),
        NOTE_BLOCK(NoteBlock.class),
        END_PORTAL_FRAME(EndPortalFrameBlock.class),

        // Both
        GATE(FenceGateBlock.class),
        LEVER(LeverBlock.class),

        // Other
        FARMLAND(FarmlandBlock.class),
        DIRT_PATH(DirtPathBlock.class),
        SKIP(SkullBlock.class, GrindstoneBlock.class, SignBlock.class, /*Implementation.NewBlocks.LICHEN.clazz,*/ VineBlock.class),
        DEFAULT;

        private final Class<?>[] classes;

        ClassHook(Class<?> ...classes) {
            this.classes = classes;
        }
    }
}
