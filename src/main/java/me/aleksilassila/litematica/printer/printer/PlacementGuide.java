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
import net.minecraft.state.property.Property;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static me.aleksilassila.litematica.printer.printer.Printer.*;
import static me.aleksilassila.litematica.printer.printer.qwer.PrintWater.*;
import static net.minecraft.block.enums.BlockFace.WALL;

public class PlacementGuide extends PrinterUtils {
    public static Map<BlockPos, Integer> posMap = new HashMap<>();
    public static boolean breakIce = false;
    @NotNull
    protected final MinecraftClient client;
    public static long createPortalTick = 1;

    public PlacementGuide(@NotNull MinecraftClient client) {
        this.client = client;
    }

    public @Nullable Action getAction(World world, WorldSchematic worldSchematic, BlockPos pos) {
        for (ClassHook hook : ClassHook.values()) {
            for (Class<?> clazz : hook.classes) {
                if (clazz != null && clazz.isInstance(worldSchematic.getBlockState(pos).getBlock())) {
                    return buildAction(world, worldSchematic, pos, hook);
                }
            }
        }

        return buildAction(world, worldSchematic, pos, ClassHook.DEFAULT);
    }

    public @Nullable Action water(BlockState requiredState, BlockState currentState, BlockPos pos) {
        posMap.compute(pos, (k, v) -> v == null ? 0 : v + 1);
        if (posMap.get(pos) > 10) {
            posMap.remove(pos);
        }

        if (posMap.size() > 10) {
            posMap.entrySet().removeIf(entry ->
                    client.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(entry.getKey())) < 36
            );
        }

        //产生水有延迟，需要等待一会儿
        if (currentState.isOf(Blocks.ICE)) {
            if (client.player != null) searchPickaxes(client.player);
            BlockPos tempPos;
            if (!posMap.containsKey(pos) && (tempPos = excavateBlock(pos)) != null) {
                posMap.put(tempPos, 0);
                breakIce = true;
                return null;
            }
            return null;
        }
        if (!spawnWater(pos)) return null;

        if (posMap.keySet().stream().anyMatch(p -> p.equals(pos))) return null;
        State state = State.get(requiredState, currentState);
        if (state != State.MISSING_BLOCK) return null;

        Direction look = null;
        for (Property<?> prop : requiredState.getProperties()) {
            //#if MC > 12101
            if (prop instanceof EnumProperty<?> enumProperty && enumProperty.getType().equals(Direction.class) && prop.getName().equalsIgnoreCase("FACING")) {
                //#else
                //$$ if (prop instanceof EnumProperty<?> && prop.getName().equalsIgnoreCase("FACING")) {
                //#endif
                look = ((Direction) requiredState.get(prop)).getOpposite();
            }
        }
        Action placement = new Action().setLookDirection(look);
        placement.setItem(Items.ICE);
        return placement;
    }

    private @Nullable Action buildAction(World world, WorldSchematic worldSchematic, BlockPos pos, ClassHook requiredType) {
        BlockState requiredState = worldSchematic.getBlockState(pos);
        BlockState currentState = world.getBlockState(pos);

        // Early validation checks
        if (!requiredState.canPlaceAt(world, pos)) {
            return null;
        }

        // Handle water-logged blocks
        if (shouldHandleWaterLogged(requiredState, currentState)) {
            Action water = water(requiredState, currentState, pos);
            if (!breakIce) {
                return water;
            }
            breakIce = false;
        }

        // Handle block breaking
        if (shouldBreakErrorBlock(pos, requiredState, currentState)) {
            excavateBlock(pos);
        }

        // Get block state
        State state = State.get(requiredState, currentState);
        if (state == State.CORRECT || (state == State.MISSING_BLOCK && !requiredState.canPlaceAt(world, pos))) {
            return null;
        }

        // Handle different states
        return switch (state) {
            case MISSING_BLOCK -> handleMissingBlock(world, pos, requiredState, requiredType);
            case WRONG_STATE -> handleWrongState(currentState, requiredState, requiredType);
            case WRONG_BLOCK -> handleWrongBlock(currentState, requiredState, requiredType);
            default -> null;
        };
    }

    private boolean shouldHandleWaterLogged(BlockState required, BlockState current) {
        return LitematicaMixinMod.PRINT_WATER_LOGGED_BLOCK.getBooleanValue()
                && canWaterLogged(required)
                && !canWaterLogged(current);
    }

    private boolean shouldBreakErrorBlock(BlockPos pos, BlockState required, BlockState current) {
        return LitematicaMixinMod.BREAK_ERROR_BLOCK.getBooleanValue()
                && canBreakBlock(pos)
                && isSchematicBlock(pos)
                && State.get(required, current) == State.WRONG_BLOCK;
    }

    private @Nullable Action handleMissingBlock(WorldAccess world, BlockPos pos, BlockState state, ClassHook type) {
        return switch (type) {
            case WALLTORCH -> handleWallTorch(state);
            case AMETHYST, SHULKER -> handleDirectionalBlock(state);
            case SLAB -> handleSlab((World) world, pos, state);
            case STAIR -> handleStair(state);
            case TRAPDOOR -> handleTrapdoor(state);
            case PILLAR -> handlePillar(state);
            case ANVIL -> handleAnvil(state);
            case NETHER_PORTAL -> handleNetherPortal(world, pos);
            case COCOA
            //#if MC >= 11904
            , PETAL
            //#endif
                    -> handleFacingBlock(state);
            case BUTTON -> handleButton(state);
            case GRINDSTONE -> handleGrindstone(state);
            case CAMPFIRE, BED -> handleDirectionalBlock(state);
            case BELL -> handleBell(state);
            case DOOR -> handleDoor(state);
            case WALLSKULL -> handleWallSkull(state);
            case DIRT_PATH -> new Action().setItem(Items.DIRT);
            case BIG_DRIPLEAF_STEM -> new Action().setItem(Items.BIG_DRIPLEAF);
            case CAVE_VINES -> new Action().setItem(Items.GLOW_BERRIES);
            case FLOWER_POT -> new Action().setItem(Items.FLOWER_POT);
            default -> handleDefaultPlacement(state);
        };
    }

    private @Nullable Action handleWrongState(BlockState current, BlockState required, ClassHook type) {
        return switch (type) {
            case SLAB -> handleSlabState(current, required);
            case SNOW -> handleSnowState(current, required);
            case DOOR -> handleDoorState(required);
            case LEVER -> handleLeverState(current, required);
            case CANDLES -> handleCandlesState(current, required);
            case PICKLES -> handlePicklesState(current, required);
            case REPEATER -> handleRepeaterState(current, required);
            case COMPARATOR -> handleComparatorState(current, required);
            case TRAPDOOR -> handleTrapdoorState(required);
            case GATE -> handleGateState(current, required);
            case NOTE_BLOCK -> handleNoteBlockState(current, required);
            case CAMPFIRE -> handleCampfireState(current, required);
            case PILLAR -> handlePillarState(current, required);
            default -> null;
        };
    }

    private @Nullable Action handleWrongBlock(BlockState current, BlockState required, ClassHook type) {
        return switch (type) {
            case FARMLAND -> handleFarmlandConversion(current);
            case DIRT_PATH -> handleDirtPathConversion(current);
            case FLOWER_POT -> handleFlowerPotContent(required);
            default -> null;
        };
    }

    private @Nullable Action handleDirtPathConversion(BlockState current) {
        if (current.getBlock() == Blocks.DIRT || current.getBlock() == Blocks.GRASS_BLOCK) {
            return new ClickAction().setItems(Implementation.SHOVELS);
        }
        return null;
    }

    private @Nullable Action handleFarmlandConversion(BlockState current) {
        if (current.getBlock() == Blocks.DIRT || current.getBlock() == Blocks.GRASS_BLOCK) {
            return new ClickAction().setItems(Implementation.HOES);
        }
        return null;
    }

    private @Nullable Action handleWallTorch(BlockState state) {
        Direction facing = (Direction) getPropertyByName(state, "FACING");
        return new Action().setSides(facing.getOpposite()).setRequiresSupport();
    }

    private @Nullable Action handleDirectionalBlock(BlockState state) {
        Direction facing = (Direction) getPropertyByName(state, "FACING");
        return new Action().setSides(facing.getOpposite());
    }

    private @Nullable Action handleSlab(World world, BlockPos pos, BlockState state) {
        return new Action().setSides(getSlabSides(world, pos, state.get(SlabBlock.TYPE)));
    }

    private @Nullable Action handleStair(BlockState state) {
        Direction half = getHalf(state.get(StairsBlock.HALF));
        Map<Direction, Vec3d> sides = new HashMap<>();
        for (Direction direction : horizontalDirections) {
            sides.put(direction, Vec3d.of(half.getVector()).multiply(0.25));
        }
        sides.put(half, new Vec3d(0, 0, 0));
        return new Action()
                .setSides(sides)
                .setLookDirection(state.get(StairsBlock.FACING).getOpposite());
    }

    private @Nullable Action handleTrapdoor(BlockState state) {
        Direction half = getHalf(state.get(TrapdoorBlock.HALF));
        Map<Direction, Vec3d> sides = new HashMap<>();
        sides.put(half, Vec3d.of(half.getVector()).multiply(0.25));
        return new Action()
                .setSides(sides)
                .setLookDirection(state.get(TrapdoorBlock.FACING).getOpposite());
    }

    private @Nullable Action handlePillar(BlockState state) {
        Action action = new Action().setSides(state.get(PillarBlock.AXIS));
        if (canStripLog(state)) {
            action.setItem(getUnstrippedLog(state).asItem());
        }
        return action;
    }

    private boolean canStripLog(BlockState state) {
        Map<Block, Block> strippedBlocks = AxeItemAccessor.getStrippedBlocks();
        for (Map.Entry<Block, Block> entry : strippedBlocks.entrySet()) {
            if (entry.getValue() == state.getBlock()) {
                return true;
            }
        }
        return false;
    }

    private Block getUnstrippedLog(BlockState state) {
        Map<Block, Block> strippedBlocks = AxeItemAccessor.getStrippedBlocks();
        for (Map.Entry<Block, Block> entry : strippedBlocks.entrySet()) {
            if (entry.getValue() == state.getBlock()) {
                return entry.getKey();
            }
        }
        return state.getBlock();
    }

    private @Nullable Action handleAnvil(BlockState state) {
        return new Action()
                .setLookDirection(state.get(AnvilBlock.FACING).rotateYCounterclockwise())
                .setSides(Direction.UP);
    }

    private @Nullable Action handleNetherPortal(WorldAccess world, BlockPos pos) {
        if (net.minecraft.world.dimension.NetherPortal.getNewPortal(world, pos, Direction.Axis.X).isPresent() && createPortalTick == 1) {
            createPortalTick = 0;
            return new Action()
                    .setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE)
                    .setRequiresSupport();
        }
        return null;
    }

    private @Nullable Action handleFacingBlock(BlockState state) {
        return new Action()
                .setSides((Direction) getPropertyByName(state, "FACING"));
    }

    private @Nullable Action handleButton(BlockState state) {
        BlockFace face = (BlockFace) getPropertyByName(state, "FACE");
        Direction side = switch (face) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            default -> ((Direction) getPropertyByName(state, "FACING")).getOpposite();
        };
        Direction look = face == WALL ? null : (Direction) getPropertyByName(state, "FACING");
        return new Action()
                .setSides(side)
                .setLookDirection(look)
                .setRequiresSupport();
    }

    private @Nullable Action handleGrindstone(BlockState state) {
        BlockFace face = (BlockFace) getPropertyByName(state, "FACE");
        Direction side = switch (face) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            default -> (Direction) getPropertyByName(state, "FACING");
        };
        Direction look = face == WALL ? null : (Direction) getPropertyByName(state, "FACING");
        Map<Direction, Vec3d> sides = new HashMap<>();
        sides.put(Direction.DOWN, Vec3d.of(side.getVector()).multiply(0.5));
        return new Action()
                .setSides(sides)
                .setLookDirection(look);
    }

    private @Nullable Action handleBell(BlockState state) {
        Direction side = switch (state.get(BellBlock.ATTACHMENT)) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            default -> state.get(BellBlock.FACING);
        };
        Direction look = state.get(BellBlock.ATTACHMENT) != Attachment.SINGLE_WALL &&
                state.get(BellBlock.ATTACHMENT) != Attachment.DOUBLE_WALL ?
                state.get(BellBlock.FACING) : null;
        return new Action()
                .setSides(side)
                .setLookDirection(look);
    }

    private @Nullable Action handleDoor(BlockState state) {
        Map<Direction, Vec3d> sides = new HashMap<>();
        Direction facing = state.get(DoorBlock.FACING);
        Direction hinge = state.get(DoorBlock.HINGE) == DoorHinge.RIGHT
                ? facing.rotateYClockwise()
                : facing.rotateYCounterclockwise();
        Vec3d hingeVec = new Vec3d(0.25, 0, 0.25);
        sides.put(hinge, hingeVec);
        sides.put(Direction.DOWN, hingeVec);
        sides.put(facing, hingeVec);
        return new Action()
                .setLookDirection(facing)
                .setSides(sides)
                .setRequiresSupport();
    }

    private @Nullable Action handleWallSkull(BlockState state) {
        return new Action()
                .setSides(state.get(WallSkullBlock.FACING).getOpposite());
    }

    private @Nullable Action handleSlabState(BlockState current, BlockState required) {
        if (required.get(SlabBlock.TYPE) == SlabType.DOUBLE) {
            Direction requiredHalf = current.get(SlabBlock.TYPE) == SlabType.BOTTOM
                    ? Direction.DOWN
                    : Direction.UP;
            return new Action().setSides(requiredHalf);
        }
        return null;
    }

    private @Nullable Action handleSnowState(BlockState current, BlockState required) {
        int layers = current.get(SnowBlock.LAYERS);
        if (layers < required.get(SnowBlock.LAYERS)) {
            Map<Direction, Vec3d> sides = new HashMap<>();
            sides.put(Direction.UP, new Vec3d(0, (layers / 8d) - 1, 0));
            return new ClickAction()
                    .setItem(Items.SNOW)
                    .setSides(sides);
        }
        return null;
    }

    private @Nullable Action handleDoorState(BlockState required) {
        if (!required.isOf(Blocks.IRON_DOOR)) {
            return new ClickAction();
        }
        return null;
    }

    private @Nullable Action handleLeverState(BlockState current, BlockState required) {
        if (required.get(LeverBlock.POWERED) != current.get(LeverBlock.POWERED)) {
            return new ClickAction();
        }
        return null;
    }

    private @Nullable Action handleCandlesState(BlockState current, BlockState required) {
        if ((Integer) getPropertyByName(current, "CANDLES") < (Integer) getPropertyByName(required, "CANDLES")) {
            return new ClickAction().setItem(required.getBlock().asItem());
        }
        return null;
    }

    private @Nullable Action handlePicklesState(BlockState current, BlockState required) {
        if (current.get(SeaPickleBlock.PICKLES) < required.get(SeaPickleBlock.PICKLES)) {
            return new ClickAction().setItem(Items.SEA_PICKLE);
        }
        return null;
    }

    private @Nullable Action handleRepeaterState(BlockState current, BlockState required) {
        if (!Objects.equals(required.get(RepeaterBlock.DELAY), current.get(RepeaterBlock.DELAY))) {
            return new ClickAction();
        }
        return null;
    }

    private @Nullable Action handleComparatorState(BlockState current, BlockState required) {
        if (required.get(ComparatorBlock.MODE) != current.get(ComparatorBlock.MODE)) {
            return new ClickAction();
        }
        return null;
    }

    private @Nullable Action handleTrapdoorState(BlockState required) {
        if (!required.isOf(Blocks.IRON_TRAPDOOR)) {
            return new ClickAction();
        }
        return null;
    }

    private @Nullable Action handleGateState(BlockState current, BlockState required) {
        if (required.get(FenceGateBlock.OPEN) != current.get(FenceGateBlock.OPEN)) {
            return new ClickAction();
        }
        return null;
    }

    private @Nullable Action handleNoteBlockState(BlockState current, BlockState required) {
        if (!Objects.equals(required.get(NoteBlock.NOTE), current.get(NoteBlock.NOTE))) {
            return new ClickAction();
        }
        return null;
    }

    private @Nullable Action handleCampfireState(BlockState current, BlockState required) {
        if (required.get(CampfireBlock.LIT) != current.get(CampfireBlock.LIT)) {
            return new ClickAction().setItems(Implementation.SHOVELS);
        }
        return null;
    }

    private @Nullable Action handlePillarState(BlockState current, BlockState required) {
        Block stripped = AxeItemAccessor.getStrippedBlocks().get(current.getBlock());
        if (stripped != null && stripped == required.getBlock()) {
            return new ClickAction().setItems(Implementation.AXES);
        }
        return null;
    }

    private @Nullable Action handleFlowerPotContent(BlockState required) {
        if (required.getBlock() instanceof FlowerPotBlock potBlock) {
            Block content = potBlock.getContent();
            if (content != Blocks.AIR) {
                return new ClickAction().setItem(content.asItem());
            }
        }
        return null;
    }

    private @Nullable Action handleDefaultPlacement(BlockState state) {
        // Create a default action with standard placement behavior
        Action action = new Action();

        // Check for general directional properties
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof EnumProperty<?> enumProp) {
                //#if MC > 12101
                // Handle facing/axis properties if they exist
                if (enumProp.getType().equals(Direction.class) && prop.getName().equalsIgnoreCase("facing")) {
                    action.setLookDirection(((Direction) state.get(prop)).getOpposite());
                } else if (enumProp.getType().equals(Direction.Axis.class) && prop.getName().equalsIgnoreCase("axis")) {
                    action.setSides(state.get((Property<Direction.Axis>) prop));
                }
                //#else
                //$$ if (prop.getName().equalsIgnoreCase("facing")) {
                //$$     action.setLookDirection(((Direction) state.get(prop)).getOpposite());
                //$$ } else if (prop.getName().equalsIgnoreCase("axis")) {
                //$$     action.setSides(state.get((Property<Direction.Axis>) prop));
                //$$ }
                //#endif
            }
        }

        // If no special properties were found, use all sides
        if (action.getSides().isEmpty()) {
            action.setSides(Direction.values());
        }

        // Set the item to be used from the block's item
        action.setItem(state.getBlock().asItem());

        return action;
    }





    enum ClassHook {
        // 放置
        ROD(Implementation.NewBlocks.ROD.clazz), // 杆
        WALLTORCH(WallTorchBlock.class, WallRedstoneTorchBlock.class), // 墙上的火把
        SLAB(SlabBlock.class), // 台阶
        STAIR(StairsBlock.class), // 楼梯
        TRAPDOOR(TrapdoorBlock.class), // 活板门
        PILLAR(PillarBlock.class), // 柱子
        ANVIL(AnvilBlock.class), // 铁砧
        GRINDSTONE(GrindstoneBlock.class), // 磨石
        BUTTON(ButtonBlock.class), // 按钮
        CAMPFIRE(CampfireBlock.class), // 营火
        SHULKER(ShulkerBoxBlock.class), // 潜影盒
        BED(BedBlock.class), // 床
        BELL(BellBlock.class), // 钟
        AMETHYST(Implementation.NewBlocks.AMETHYST.clazz), // 紫水晶
        DOOR(DoorBlock.class), // 门
        COCOA(CocoaBlock.class), // 可可豆
        WALLSKULL(WallSkullBlock.class), // 墙上的头颅
        NETHER_PORTAL(NetherPortalBlock.class), // 下界传送门
        //#if MC >= 11904
        PETAL(FlowerbedBlock.class), // 花簇(ojng你这是他妈什么狗操命名？)
        //#endif

        // 仅点击
        FLOWER_POT(FlowerPotBlock.class), // 花盆
        BIG_DRIPLEAF_STEM(BigDripleafStemBlock.class), // 大垂叶茎
        SNOW(SnowBlock.class), // 雪
        CANDLES(Implementation.NewBlocks.CANDLES.clazz), // 蜡烛
        REPEATER(RepeaterBlock.class), // 中继器
        COMPARATOR(ComparatorBlock.class), // 比较器
        PICKLES(SeaPickleBlock.class), // 海泡菜
        NOTE_BLOCK(NoteBlock.class), // 音符盒
        END_PORTAL_FRAME(EndPortalFrameBlock.class), // 末地传送门框架

        // 两者皆有
        GATE(FenceGateBlock.class), // 栅栏门
        LEVER(LeverBlock.class), // 拉杆

        // 其他
        FARMLAND(FarmlandBlock.class), // 耕地
        DIRT_PATH(DirtPathBlock.class), // 泥土小径
        SKIP(SkullBlock.class, GrindstoneBlock.class, SignBlock.class,VineBlock.class, EndPortalBlock.class), // 跳过
        WATER(FluidBlock.class), // 水
        CAVE_VINES(CaveVinesHeadBlock.class, CaveVinesBodyBlock.class), // 洞穴藤蔓
        DEFAULT; // 默认

        private static final Map<Class<?>, ClassHook> CLASS_MAP = new HashMap<>();
        private final Class<?>[] classes;

        static {
            for (ClassHook hook : values()) {
                if (hook.classes != null) {
                    for (Class<?> clazz : hook.classes) {
                        CLASS_MAP.put(clazz, hook);
                    }
                }
            }
        }

        ClassHook(Class<?>... classes) {
            this.classes = classes;
        }
    }

    public static class Action {
        protected Map<Direction, Vec3d> sides;
        protected Direction lookDirection;
        @Nullable
        protected Item[] clickItems; // null == any

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
         * @param side     The side pointing to the block that should be clicked
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

        public static boolean isReplaceable(BlockState state) {
            //#if MC < 11904
            //$$ return state.getMaterial().isReplaceable();
            //#else
            return state.isReplaceable();
            //#endif
        }

        public @Nullable Direction getLookDirection() {
            return lookDirection;
        }

        public Action setLookDirection(Direction lookDirection) {
            this.lookDirection = lookDirection;
            return this;
        }

        public @Nullable Item[] getRequiredItems(Block backup) {
            return clickItems == null ? new Item[]{backup.asItem()} : clickItems;
        }

        public @NotNull Map<Direction, Vec3d> getSides() {
            if (sides == null) {
                sides = Arrays.stream(Direction.values())
                        .collect(Collectors.toMap(
                                direction -> direction,
                                direction -> new Vec3d(0, 0, 0)
                        ));
            }
            return sides;
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

        public Action setRequiresSupport() {
            return this.setRequiresSupport(true);
        }

        public void queueAction(Printer.Queue queue, BlockPos center, Direction side, boolean useShift, boolean didSendLook) {
            try {
                if (LitematicaMixinMod.shouldPrintInAir && !requiresSupport) {
                    queue.queueClick(center, side.getOpposite(), sides.get(side), useShift, didSendLook);
                } else {
                    queue.queueClick(center.offset(side), side.getOpposite(), sides.get(side), useShift, didSendLook);
                }
            } catch (NullPointerException ignored) {
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
}
