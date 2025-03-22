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
//#if MC < 12104
//$$ import net.minecraft.state.property.DirectionProperty;
//#else

//#endif

import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
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
        Integer i = posMap.get(pos);
        if (i != null) {
            posMap.put(pos, i + 1);
            if (posMap.get(pos) > 10) posMap.remove(pos);
            if (posMap.size() > 10) {
                Set<Map.Entry<BlockPos, Integer>> entries = posMap.entrySet();
                ArrayList<BlockPos> removeList = new ArrayList<>();
                entries.forEach(v -> {
                    if (client.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(v.getKey())) < 6 * 6)
                        removeList.add(v.getKey());
                });
                removeList.forEach(v -> posMap.remove(v));
            }
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

                    Map<Direction, Vec3d> sides = new HashMap<>() {{
                        put(half,
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
                    return new Action().setLookDirection(requiredState.get(AnvilBlock.FACING).rotateYCounterclockwise()).setSides(Direction.UP);
                }
                case HOPPER: // FIXME add all sides
                case NETHER_PORTAL: {

                    boolean canCreatePortal = net.minecraft.world.dimension.NetherPortal.getNewPortal(world, pos, Direction.Axis.X).isPresent();
                    if (canCreatePortal && createPortalTick == 1) {
                        createPortalTick = 0;
                        return new Action().setItems(Items.FLINT_AND_STEEL,Items.FIRE_CHARGE).setRequiresSupport();
                    }
                    break;
                }
                case COCOA: {
                    return new Action().setSides((Direction) getPropertyByName(requiredState, "FACING"));
                }
                case LEVER:
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
                            side = ((Direction) getPropertyByName(requiredState, "FACING")).getOpposite();
                            break;
                        }
                    }

                    Direction look = getPropertyByName(requiredState, "FACE") == WALL ?
                            null : (Direction) getPropertyByName(requiredState, "FACING");

                    return new Action().setSides(side).setLookDirection(look).setRequiresSupport();
                }
                case GRINDSTONE: { // Tese are broken
                    Direction side = switch ((BlockFace) getPropertyByName(requiredState, "FACE")) {
                        case FLOOR -> Direction.DOWN;
                        case CEILING -> Direction.UP;
                        default -> (Direction) getPropertyByName(requiredState, "FACING");
                    };

                    Direction look = getPropertyByName(requiredState, "FACE") == WALL ?
                            null : (Direction) getPropertyByName(requiredState, "FACING");

                    Map<Direction, Vec3d> sides = new HashMap<>();
                    sides.put(Direction.DOWN, Vec3d.of(side.getVector()).multiply(0.5));

                    return new Action().setSides(sides).setLookDirection(look);
                }
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

                //花盆修复
                case FLOWER_POT: {
                    return new Action().setItem(Items.FLOWER_POT);
                }
                case SKIP: {
                    break;
                }
                case WATER: {

                }
                case DEFAULT:
                default: { // Try to guess how the rest of the blocks are placed.
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
                    String blockTranslationKey = requiredState.getBlock().getTranslationKey();
                    if (blockTranslationKey != null && blockTranslationKey.contains("potted")) {
                        //利用字符串判断，一一对应方块
                        //potted_dandelion, potted_poppy, potted_blue_orchid, potted_allium, potted_azure_bluet, potted_red_tulip, potted_orange_tulip, potted_white_tulip, potted_pink_tulip, potted_oxeye_daisy, potted_cornflower, potted_lily_of_the_valley, potted_wither_rose, potted_oak_sapling,potted_spruce_sapling,potted_birch_sapling,potted_jungle_sapling,potted_acacia_sapling,potted_dark_oak_sapling,potted_red_mushroom,potted_brown_mushroom,potted_fern,potted_dead_bush,potted_cactus,potted_bamboo,potted_azalea_bush,potted_flowering_azalea_bush,potted_crimson_fungus,potted_crimson_roots,potted_warped_roots,potted_mangrove_propagule,potted_cherry_sapling,potted_torchflower
                        switch (blockTranslationKey) {
                            case "block.minecraft.potted_dandelion":
                                return new ClickAction().setItem(Items.DANDELION);
                            case "block.minecraft.potted_poppy":
                                return new ClickAction().setItem(Items.POPPY);
                            case "block.minecraft.potted_blue_orchid":
                                return new ClickAction().setItem(Items.BLUE_ORCHID);
                            case "block.minecraft.potted_allium":
                                return new ClickAction().setItem(Items.ALLIUM);
                            case "block.minecraft.potted_azure_bluet":
                                return new ClickAction().setItem(Items.AZURE_BLUET);
                            case "block.minecraft.potted_red_tulip":
                                return new ClickAction().setItem(Items.RED_TULIP);
                            case "block.minecraft.potted_orange_tulip":
                                return new ClickAction().setItem(Items.ORANGE_TULIP);
                            case "block.minecraft.potted_white_tulip":
                                return new ClickAction().setItem(Items.WHITE_TULIP);
                            case "block.minecraft.potted_pink_tulip":
                                return new ClickAction().setItem(Items.PINK_TULIP);
                            case "block.minecraft.potted_oxeye_daisy":
                                return new ClickAction().setItem(Items.OXEYE_DAISY);
                            case "block.minecraft.potted_cornflower":
                                return new ClickAction().setItem(Items.CORNFLOWER);
                            case "block.minecraft.potted_lily_of_the_valley":
                                return new ClickAction().setItem(Items.LILY_OF_THE_VALLEY);
                            case "block.minecraft.potted_wither_rose":
                                return new ClickAction().setItem(Items.WITHER_ROSE);
                            case "block.minecraft.potted_oak_sapling":
                                return new ClickAction().setItem(Items.OAK_SAPLING);
                            case "block.minecraft.potted_spruce_sapling":
                                return new ClickAction().setItem(Items.SPRUCE_SAPLING);
                            case "block.minecraft.potted_birch_sapling":
                                return new ClickAction().setItem(Items.BIRCH_SAPLING);
                            case "block.minecraft.potted_jungle_sapling":
                                return new ClickAction().setItem(Items.JUNGLE_SAPLING);
                            case "block.minecraft.potted_acacia_sapling":
                                return new ClickAction().setItem(Items.ACACIA_SAPLING);
                            case "block.minecraft.potted_dark_oak_sapling":
                                return new ClickAction().setItem(Items.DARK_OAK_SAPLING);
                            case "block.minecraft.potted_red_mushroom":
                                return new ClickAction().setItem(Items.RED_MUSHROOM);
                            case "block.minecraft.potted_brown_mushroom":
                                return new ClickAction().setItem(Items.BROWN_MUSHROOM);
                            case "block.minecraft.potted_fern":
                                return new ClickAction().setItem(Items.FERN);
                            case "block.minecraft.potted_dead_bush":
                                return new ClickAction().setItem(Items.DEAD_BUSH);
                            case "block.minecraft.potted_cactus":
                                return new ClickAction().setItem(Items.CACTUS);
                            case "block.minecraft.potted_bamboo":
                                return new ClickAction().setItem(Items.BAMBOO);
                            case "block.minecraft.potted_azalea_bush":
                                return new ClickAction().setItem(Items.AZALEA);
                            case "block.minecraft.potted_flowering_azalea_bush":
                                return new ClickAction().setItem(Items.FLOWERING_AZALEA);
                            case "block.minecraft.potted_crimson_fungus":
                                return new ClickAction().setItem(Items.CRIMSON_FUNGUS);
                            case "block.minecraft.potted_crimson_roots":
                                return new ClickAction().setItem(Items.CRIMSON_ROOTS);
                            case "block.minecraft.potted_warped_roots":
                                return new ClickAction().setItem(Items.WARPED_ROOTS);
                            //#if MC > 11904
                            case "block.minecraft.potted_torchflower":
                                return new ClickAction().setItem(Items.TORCHFLOWER);
                            case "block.minecraft.potted_mangrove_propagule":
                                return new ClickAction().setItem(Items.MANGROVE_PROPAGULE);
                            case "block.minecraft.potted_cherry_sapling":
                                return new ClickAction().setItem(Items.CHERRY_SAPLING);
                            //#endif
                            //#if MC > 12101
                            case "block.minecraft.potted_plae_oak_sapling":
                                return new ClickAction().setItem(Items.PALE_OAK_SAPLING);
                            //#endif
                            default:
                                return null;
                        }
                    }

                    break;
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
        ROD(Implementation.NewBlocks.ROD.clazz), // 杆
        WALLTORCH(WallTorchBlock.class, WallRedstoneTorchBlock.class), // 墙上的火把
        TORCH(TorchBlock.class), // 火把
        SLAB(SlabBlock.class), // 台阶
        STAIR(StairsBlock.class), // 楼梯
        TRAPDOOR(TrapdoorBlock.class), // 活板门
        PILLAR(PillarBlock.class), // 柱子
        ANVIL(AnvilBlock.class), // 铁砧
        HOPPER(HopperBlock.class), // 漏斗
        GRINDSTONE(GrindstoneBlock.class), // 磨石
        BUTTON(ButtonBlock.class), // 按钮
        CAMPFIRE(CampfireBlock.class), // 营火
        SHULKER(ShulkerBoxBlock.class), // 潜影盒
        BED(BedBlock.class), // 床
        BELL(BellBlock.class), // 钟
        AMETHYST(Implementation.NewBlocks.AMETHYST.clazz), // 紫水晶
        DOOR(DoorBlock.class), // 门
        COCOA(CocoaBlock.class), // 可可豆
        OBSERVER(ObserverBlock.class), // 观察者
        WALLSKULL(WallSkullBlock.class), // 墙上的头颅
        NETHER_PORTAL(NetherPortalBlock.class), // 下界传送门

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

        private final Class<?>[] classes;

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
            if (this.sides == null) {
                this.sides = new HashMap<>();
                for (Direction d : Direction.values()) {
                    this.sides.put(d, new Vec3d(0, 0, 0));
                }
            }

            return this.sides;
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
}
