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
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.aleksilassila.litematica.printer.printer.Printer.*;
import static me.aleksilassila.litematica.printer.printer.qwer.PrintWater.*;
import static net.minecraft.block.enums.BlockFace.WALL;

public class PlacementGuide extends PrinterUtils {
    public static Map<BlockPos, Integer> posMap = new HashMap<>();
    public static boolean breakIce = false;
    @NotNull
    protected final MinecraftClient client;

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
            // 用普通 for 循环替换 stream 操作，减少额外对象分配
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

        // 在此处直接判断 posMap 是否含有该位置，避免重复计算
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

    @SuppressWarnings("EnhancedSwitchMigration")
    private @Nullable Action buildAction(World world, WorldSchematic worldSchematic, BlockPos pos, ClassHook requiredType) {
        BlockState requiredState = worldSchematic.getBlockState(pos);
        BlockState currentState = world.getBlockState(pos);

        if (LitematicaMixinMod.PRINT_WATER_LOGGED_BLOCK.getBooleanValue()
                && canWaterLogged(requiredState)
                && !canWaterLogged(currentState)) {
            Action water = water(requiredState, currentState, pos);
            if (breakIce) {
                breakIce = false;
            } else return water;
        }
        if (LitematicaMixinMod.BREAK_ERROR_BLOCK.getBooleanValue() && canBreakBlock(pos) && isSchematicBlock(pos) && State.get(requiredState, currentState) == State.WRONG_BLOCK) {
            excavateBlock(pos);
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
                case WALLTORCH:{
                    Direction facing = (Direction)getPropertyByName(requiredState, "FACING");
                    if(facing != null){
                        return new Action().setSides(facing.getOpposite()).setRequiresSupport();
                    }
                    break;
                }
                case AMETHYST: {
                    return new Action()
                            .setSides((requiredState.get(Properties.FACING))
                                    .getOpposite())
                            .setRequiresSupport();
                }
                case ROD:
                case SHULKER: {
                    return new Action().setSides(
                            (requiredState.get(Properties.FACING))
                                    .getOpposite());
                }
                case SLAB: {
                    return new Action().setSides(getSlabSides(world, pos, requiredState.get(SlabBlock.TYPE)));
                }
                case STAIR: {
                    Direction facing = requiredState.get(StairsBlock.FACING);
                    BlockHalf half = requiredState.get(StairsBlock.HALF);

                    Map<Direction, Vec3d> sides = new HashMap<>();
                    if (half == BlockHalf.BOTTOM) {
                        sides.put(Direction.DOWN, new Vec3d(0, 0, 0));
                    } else {
                        sides.put(Direction.UP, new Vec3d(0, 0, 0));
                    }
                    sides.put(facing.getOpposite(), new Vec3d(0, 0, 0));

                    return new Action()
                            .setSides(sides)
                            .setLookDirection(facing);
                }
                case TRAPDOOR: {
                    Direction half = getHalf(requiredState.get(TrapdoorBlock.HALF));

                    Map<Direction, Vec3d> sides = new HashMap<>() {{
                        put(half, new Vec3d(0, 0, 0));
                    }};

                    return new Action()
                            .setSides(sides)
                            .setLookDirection(requiredState.get(TrapdoorBlock.FACING).getOpposite());
                }
                case PILLAR: {
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
                case ANVIL: {
                    return new Action().setLookDirection(requiredState.get(AnvilBlock.FACING).rotateYCounterclockwise()).setSides(Direction.UP);
                }
                //FIXME)) add all sides
                case HOPPER: {
                    Map<Direction, Vec3d> sides = new HashMap<>();
                    for (Direction direction : Direction.values()) {
                        if (direction.getAxis() == Direction.Axis.Y) {
                            sides.put(direction, new Vec3d(0, 0, 0));
                        } else {
                            sides.put(direction, Vec3d.of(direction.getVector()).multiply(0.5));
                        }
                    }

                    return new Action()
                            .setSides(sides)
                            .setLookDirection(requiredState.get(HopperBlock.FACING).getOpposite());
                }
                case NETHER_PORTAL: {

                    boolean canCreatePortal = net.minecraft.world.dimension.NetherPortal.getNewPortal(world, pos, Direction.Axis.X).isPresent();
                    if (canCreatePortal) {
                        return new Action().setItems(Items.FLINT_AND_STEEL,Items.FIRE_CHARGE).setRequiresSupport();
                    }
                    break;
                }
                case COCOA: {
                    return new Action().setSides(requiredState.get(Properties.FACING));
                }
                //#if MC >= 12003
                case CRAFTER: {
                    Direction facing = requiredState.get(Properties.ORIENTATION).getFacing().getOpposite();
                    Direction rotation = requiredState.get(Properties.ORIENTATION).getRotation().getOpposite();
                    client.inGameHud.getChatHud().addMessage(Text.of("方块facing: " + facing + " rotation: " + rotation));
                    return new Action().setLookDirection(facing).setLookDirection2(rotation);
                }
                //#endif

                case OBSERVER: {
                    Direction side = requiredState.get(Properties.FACING).getOpposite();
                    Direction look = requiredState.get(Properties.FACING);

                    return new Action().setSides(side).setLookDirection(look);
                }

                case BUTTON: {
                    Direction side;
                    switch ((BlockFace) getPropertyByName(requiredState, "FACE")) {
                        case FLOOR: {
                            side = Direction.DOWN;
                            break;
                        }
                        case CEILING: {
                            side = Direction.UP;
                            break;
                        }
                        default: {
                            side = (requiredState.get(Properties.FACING)).getOpposite();
                            break;
                        }
                    }

                    Direction look = getPropertyByName(requiredState, "FACE") == WALL ?
                            null : requiredState.get(Properties.FACING);

                    return new Action().setSides(side).setLookDirection(look).setRequiresSupport();
                }
                case GRINDSTONE: { // Tese are broken
                    Direction side = switch ((BlockFace) getPropertyByName(requiredState, "FACE")) {
                        case FLOOR -> Direction.DOWN;
                        case CEILING -> Direction.UP;
                        default -> requiredState.get(Properties.FACING);
                    };

                    Direction look = getPropertyByName(requiredState, "FACE") == WALL ?
                            null : requiredState.get(Properties.FACING);

                    Map<Direction, Vec3d> sides = new HashMap<>();
                    sides.put(Direction.DOWN, Vec3d.of(side.getVector()).multiply(0.5));

                    return new Action().setSides(sides).setLookDirection(look);
                }
                case GATE:
                case CAMPFIRE: {
                    return new Action()
                            .setLookDirection(requiredState.get(Properties.FACING));
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

                    Direction facing = requiredState.get(DoorBlock.FACING);
                    Direction hinge = requiredState.get(DoorBlock.HINGE) == DoorHinge.RIGHT
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

                //主要修复两种方块类型的物品选择问题
                case CAVE_VINES: {
                    //发光浆果
                    return new Action().setItem(Items.GLOW_BERRIES);
                }

                case FLOWER_POT: {
                    return new Action().setItem(Items.FLOWER_POT);
                }
                case SKIP: {
                    break;
                }
                case WATER: {

                }
                case DEFAULT:
                default: { // 尝试猜测剩余方块如何放置
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
//                        SlabType requiredHalf1 = currentState.get(SlabBlock.TYPE) == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP;
//                        return new Action().setSides(getSlabSides(world, pos, requiredHalf1));
                        Direction requiredHalf = currentState.get(SlabBlock.TYPE) == SlabType.BOTTOM ? Direction.DOWN : Direction.UP;

                        return new Action().setSides(requiredHalf);
                    }

                    break;
                }
                case SNOW: {
                    int layers = currentState.get(SnowBlock.LAYERS);
                    if (layers < requiredState.get(SnowBlock.LAYERS)) {
                        Map<Direction, Vec3d> sides = new HashMap<>() {{
                            put(Direction.UP, new Vec3d(0, (layers / 8d) - 1, 0));
                        }};
                        return new ClickAction().setItem(Items.SNOW).setSides(sides);
                    }

                    break;
                }
                case DOOR: {
                    //判断门是不是铁制的，如果是就直接返回
                    if (requiredState.isOf(Blocks.IRON_DOOR)) break;
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
                    //判断活版门是不是铁制的，如果是就直接返回
                    if (requiredState.isOf(Blocks.IRON_TRAPDOOR)) break;
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
                //#if MC >= 11904
                case FLOWERBED: {
                    client.inGameHud.getChatHud().addMessage(Text.of("所需花瓣数量: " + requiredState.get(FlowerbedBlock.FLOWER_AMOUNT)));
                    if (currentState.get(FlowerbedBlock.FLOWER_AMOUNT) <= requiredState.get(FlowerbedBlock.FLOWER_AMOUNT)) {
                        client.inGameHud.getChatHud().addMessage(Text.of("尝试放置" + requiredState.getBlock().asItem().toString()));
                        return new ClickAction().setItem(requiredState.getBlock().asItem());
                    }
                    break;
                }
                //#endif
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
                case FLOWER_POT: {
                    //所以aleksilassila你在这里干了什么？明明这么简单还要再mixin个访问器
                    if (requiredState.getBlock() instanceof FlowerPotBlock potBlock) {
                        Block content = potBlock.getContent();
                        if (content != Blocks.AIR) {
                            return new ClickAction().setItem(content.asItem());
                        }
                    }
                }
                case WATER: {

                }
                default: {
                    return null;
                }
            }

        }

        return null;
    }

    enum ClassHook {
        // 放置
        ROD(RodBlock.class), // 杆
        WALLTORCH(WallTorchBlock.class, WallRedstoneTorchBlock.class), // 墙上的火把
        TORCH(TorchBlock.class), // 火把
        SLAB(SlabBlock.class), // 台阶
        STAIR(StairsBlock.class), // 楼梯
        TRAPDOOR(TrapdoorBlock.class), // 活板门
        PILLAR(PillarBlock.class), // 柱子
        ANVIL(AnvilBlock.class), // 铁砧
        HOPPER(HopperBlock.class), // 漏斗
        GRINDSTONE(GrindstoneBlock.class), // 砂轮
        BUTTON(ButtonBlock.class), // 按钮
        CAMPFIRE(CampfireBlock.class), // 营火
        SHULKER(ShulkerBoxBlock.class), // 潜影盒
        BED(BedBlock.class), // 床
        BELL(BellBlock.class), // 钟
        AMETHYST(AmethystBlock.class), // 紫水晶
        DOOR(DoorBlock.class), // 门
        COCOA(CocoaBlock.class), // 可可豆
        WALLSKULL(WallSkullBlock.class), // 墙上的头颅
        NETHER_PORTAL(NetherPortalBlock.class), // 下界传送门
        //#if MC >= 12003
        CRAFTER(CrafterBlock.class), // 合成器
        //#endif
        OBSERVER(ObserverBlock.class), // 侦测器

        // 仅点击
        FLOWER_POT(FlowerPotBlock.class), // 花盆
        BIG_DRIPLEAF_STEM(BigDripleafStemBlock.class), // 大垂叶茎
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

        private final Class<?>[] classes;

        ClassHook(Class<?>... classes) {
            this.classes = classes;
        }
    }

    public static class Action {
        protected Map<Direction, Vec3d> sides;
        protected Direction lookDirection;
        protected Direction lookDirection2;
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
         * @param side     要点击的方块的哪一面
         * @param modifier  点击位置的偏移量。
         *                  x: 左右偏移, y: 上下偏移, z: 前后偏移。
         *                  (0, 0, 0): 点击面的中心,
         *                  (0.5, -0.5, 0): 垂直面右下角。
         *                  水平面只需调整 z 轴。
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

        public @Nullable Direction getLookDirection() {
            return lookDirection;
        }

        public @Nullable Direction getLookDirection2() {
            return lookDirection2;
        }

        /**
         * 设置放置时玩家的视角朝向
         *
         * @param lookDirection 视角朝向
         * @return 当前 Action 实例
         */
        public Action setLookDirection(Direction lookDirection) {
            this.lookDirection = lookDirection;
            return this;
        }

        public Action setLookDirection2(Direction lookDirection) {
            this.lookDirection = lookDirection;
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
         * 设置操作动作的所有有效方向。
         * <p>
         * 该方法会遍历所有给定的轴，收集每个轴上所有对应的方向，
         * 并为每个方向生成默认的偏移值（0,0,0）。
         *
         * @param axis 要设置的方向轴列表
         * @return 当前操作动作实例
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
