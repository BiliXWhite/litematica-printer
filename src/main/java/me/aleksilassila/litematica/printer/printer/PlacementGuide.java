package me.aleksilassila.litematica.printer.printer;

import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PreprocessUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.utils.*;
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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class PlacementGuide extends PrinterUtils {
    @SuppressWarnings("all")
    protected static final Map<Block, Block> STRIPPED_LOGS = AxeItemAccessor.getStrippedBlocks();
    protected static final Item[] compostableItems = Arrays.stream(ComposterBlock.COMPOSTABLES.keySet().toArray(ItemLike[]::new))
            .map(ItemLike::asItem)
            .toArray(Item[]::new);

    protected final @NotNull Minecraft mc;
    protected final AtomicReference<Boolean> skip = new AtomicReference<>(false);

    public PlacementGuide(@NotNull Minecraft client) {
        this.mc = client;
    }

    public @Nullable Action getAction(BlockContext ctx) {
        State state = State.get(ctx);
        if (!ctx.requiredState.canSurvive(ctx.level, ctx.blockPos) || state == State.CORRECT) {
            return null;
        }
        for (ClassHook hook : ClassHook.values()) {
            for (Class<?> clazz : hook.classes) {
                if (clazz != null && clazz.isInstance(ctx.requiredState.getBlock())) {
                    skip.set(false);
                    @Nullable Action action = buildAction(ctx, hook, state, skip);
                    if (action == null && skip.get()) {   // 珊瑚直接使用了 Block.class, 为了传递性所以只有不为null时进行返回
                        continue;
                    }
                    return action;
                }
            }
        }
        return buildAction(ctx, ClassHook.DEFAULT, state, skip);    // 兜底处理
    }

    private @Nullable Action buildAction(BlockContext ctx, ClassHook requiredType, State state, AtomicReference<Boolean> skip) {
        // 跳过打印含水方块
        if (Configs.Put.SKIP_WATERLOGGED_BLOCK.getBooleanValue() && PlaceUtils.isWaterRequired(ctx.requiredState)) {
            return null;
        }

        // 破冰放水
        if (Configs.Put.PRINT_WATER.getBooleanValue() && PlaceUtils.isWaterRequired(ctx.requiredState)) {
            if (ctx.currentState.getBlock() instanceof IceBlock) {  // 冰块
                BreakManager.addBlockToBreak(ctx);
                return null;
            }
            if (!PlaceUtils.isCorrectWaterLevel(ctx.requiredState, ctx.currentState)) {
                if (!ctx.currentState.isAir() && !(ctx.currentState.getBlock() instanceof LiquidBlock)) {
                    if (Configs.Put.BREAK_WRONG_BLOCK.getBooleanValue()) {
                        BreakManager.addBlockToBreak(ctx);
                    }
                    return null;
                }
                return new Action().setItem(Items.ICE);
            }
        }

        if (state == State.MISSING_BLOCK) switch (requiredType) {
            case TORCH -> {
                Direction lookDirection = ctx.getRequiredStateProperty(WallTorchBlock.FACING)
                        .orElse(Direction.UP)
                        .getOpposite();
                return new Action()
                        .setSides(lookDirection)
                        .setLookDirection(lookDirection)
                        .setRequiresSupport();
            }
            case AMETHYST -> {
                Direction lookDirection = ctx.getRequiredStateProperty(AmethystClusterBlock.FACING)
                        .orElse(Direction.UP)
                        .getOpposite();
                return new Action()
                        .setSides(lookDirection)
                        .setRequiresSupport();
            }
            case SLAB -> {
                Map<Direction, Vec3> slabSides = getSlabSides(ctx.level, ctx.blockPos, ctx.requiredState.getValue(SlabBlock.TYPE));
                return new Action().setSides(slabSides);
            }
            case STAIR -> {
                Direction facing = ctx.requiredState.getValue(StairBlock.FACING);
                Half half = ctx.requiredState.getValue(StairBlock.HALF);

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
                        .setLookDirection(facing.getOpposite());
            }
            case TRAPDOOR -> {

                return new Action()
                        .setSides(getHalf(ctx.requiredState.getValue(TrapDoorBlock.HALF)))
                        .setLookDirection(ctx.requiredState.getValue(TrapDoorBlock.FACING).getOpposite());
            }
            case STRIP_LOG -> {
                Action action = new Action().setSides(ctx.requiredState.getValue(RotatedPillarBlock.AXIS));
                Item[] items = {ctx.requiredState.getBlock().asItem()};

                if (Configs.Put.STRIP_LOGS.getBooleanValue()) {
                    for (Map.Entry<Block, Block> entry : STRIPPED_LOGS.entrySet()) {
                        if (ctx.requiredState.getBlock() == entry.getValue()) {
                            items = new Item[]{entry.getValue().asItem(), entry.getKey().asItem()};
                            break;
                        }
                    }
                }

                action.setItems(items);
                return action;
            }
            case ANVIL -> {
                return new Action().setLookDirection(ctx.requiredState.getValue(AnvilBlock.FACING).getCounterClockWise());
            }
            case HOPPER -> {
                Direction facing = ctx.requiredState.getValue(HopperBlock.FACING);
                return new Action().setSides(facing);
            }
            case NETHER_PORTAL -> {

                boolean canCreatePortal = PortalShape.findEmptyPortalShape(ctx.level, ctx.blockPos, Direction.Axis.X).isPresent();
                if (canCreatePortal) {
                    return new Action().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                }
            }
            case COCOA -> {
                return new Action().setSides(ctx.requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING));
            }
            //#if MC >= 12003
            case CRAFTER -> {
                var frontAndTop = ctx.requiredState.getValue(BlockStateProperties.ORIENTATION);
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
                Direction facing = ctx.requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
                ChestType type = ctx.requiredState.getValue(BlockStateProperties.CHEST_TYPE);
                Map<Direction, Vec3> noChestSides = new HashMap<>();

                for (Direction side : Direction.values()) {
                    if (ctx.level.getBlockState(ctx.blockPos.relative(side)).getBlock() instanceof ChestBlock) {
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
                    if (ctx.level.getBlockState(ctx.blockPos.relative(chestFacing)).getBlock() instanceof ChestBlock) {
                        return new Action().setSides(Map.of(chestFacing, Vec3.ZERO)).setLookDirection(facing).setUseShift(false);
                    } else {
                        return new Action().setSides(noChestSides).setLookDirection(facing).setUseShift();
                    }
                }
            }
            case BED -> {
                if (ctx.requiredState.getValue(BedBlock.PART) == BedPart.FOOT)
                    return new Action().setLookDirection(ctx.requiredState.getValue(BedBlock.FACING));
            }
            case BELL -> {
                Direction side;
                switch (ctx.requiredState.getValue(BellBlock.ATTACHMENT)) {
                    case FLOOR -> side = Direction.DOWN;
                    case CEILING -> side = Direction.UP;
                    default -> side = ctx.requiredState.getValue(BellBlock.FACING);
                }

                Direction look = ctx.requiredState.getValue(BellBlock.ATTACHMENT) != BellAttachType.SINGLE_WALL &&
                        ctx.requiredState.getValue(BellBlock.ATTACHMENT) != BellAttachType.DOUBLE_WALL ?
                        ctx.requiredState.getValue(BellBlock.FACING) : null;

                return new Action().setSides(side).setLookDirection(look);
            }
            case DOOR -> {
                Direction facing = ctx.requiredState.getValue(DoorBlock.FACING);
                DoorHingeSide hinge = ctx.requiredState.getValue(DoorBlock.HINGE);
                BlockPos upperPos = ctx.blockPos.above();

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
                BlockState leftState = ctx.level.getBlockState(ctx.blockPos.relative(left));
                BlockState leftUpperState = ctx.level.getBlockState(upperPos.relative(left));
                BlockState rightState = ctx.level.getBlockState(ctx.blockPos.relative(right));
                BlockState rightUpperState = ctx.level.getBlockState(upperPos.relative(right));

                int occupancy = (leftState.isCollisionShapeFullBlock(ctx.level, ctx.blockPos.relative(left)) ? -1 : 0)
                        + (leftUpperState.isCollisionShapeFullBlock(ctx.level, upperPos.relative(left)) ? -1 : 0)
                        + (rightState.isCollisionShapeFullBlock(ctx.level, ctx.blockPos.relative(right)) ? 1 : 0)
                        + (rightUpperState.isCollisionShapeFullBlock(ctx.level, upperPos.relative(right)) ? 1 : 0);

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
                    if (direction == Direction.DOWN && ctx.requiredState.getBlock() == Blocks.VINE) continue;
                    if ((Boolean) getPropertyByName(ctx.requiredState, direction.name())) {
                        return new Action().setSides(direction);
                    }
                }
            }
            case DEAD_CORAL -> {
                Block block = ctx.requiredState.getBlock();
                ResourceLocation blockId1 = BlockUtils.getIdentifier(block);
                if (!blockId1.toString().contains("coral")) {
                    skip.set(true); // 使用了Block, 但不是该指南的方块, 让下一个指南进行处理
                    return null;
                }
                ResourceLocation blockId2 = ResourceLocationUtils.of(blockId1.toString().replace("dead_", ""));
                boolean isBlock = blockId1.toString().contains("block");
                List<Item> items = new ArrayList<>();
                items.add(block.asItem());
                if (Configs.Put.REPLACE_CORAL.getBooleanValue()) {
                    if (!blockId1.equals(blockId2)) {
                        items.add(BlockUtils.getBlock(blockId2).asItem());
                    }
                }
                Item[] itemsArray = items.toArray(new Item[0]);

                Action action = new Action().setItems(itemsArray);
                if (!isBlock) {
                    boolean isWallFan = block instanceof BaseCoralWallFanBlock;
                    Direction facing = isWallFan ? ctx.requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()
                            : Direction.DOWN;
                    action.setSides(facing).setRequiresSupport();
                }
                return action;
            }
            case FIRE -> {
                if (ctx.requiredState.getBlock() instanceof SoulFireBlock)
                    return new Action().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                for (Direction direction : Direction.values()) {
                    if (direction == Direction.DOWN) continue;
                    if ((Boolean) getPropertyByName(ctx.requiredState, direction.name())) {
                        return new Action().setSides(direction).setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                    }
                }
                return new Action().setSides(Direction.DOWN).setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
            }
            case OBSERVER -> {
                @Nullable Direction facing = ctx.getRequiredStateProperty(ObserverBlock.FACING).orElse(null);
                if (facing == null) {
                    return null;
                }
                // 安全放置
                if (Configs.Put.SAFELY_OBSERVER.getBooleanValue()) {
                    BlockContext input = ctx.offset(facing);                    // 输入端(侦测面)
                    BlockContext output = ctx.offset(facing.getOpposite());     // 输出端(红点面)
                    List<Property<?>> propertiesToIgnore = new ArrayList<>();
                    // 如果是墙, 忽略比较侦测面墙方向属性
                    if (input.requiredState.getBlock() instanceof WallBlock) {
                        State.getWallFacingProperty(facing.getOpposite())
                                .ifPresent(propertiesToIgnore::add);
                    }
                    if (output.requiredState.getBlock() instanceof CrossCollisionBlock) {
                        State.getCrossCollisionBlock(facing.getOpposite())
                                .ifPresent(propertiesToIgnore::add);
                    }
                    State inputState = State.get(input, propertiesToIgnore.toArray(new Property<?>[0]));
                    if (inputState == State.CORRECT) {
                        // 侦测器链, 查找源头状态
                        BlockContext temp = input;
                        while (temp.requiredState.getBlock() instanceof ObserverBlock) {
                            @Nullable Direction tempObserverFacing = temp.getRequiredStateProperty(ObserverBlock.FACING).orElse(null);
                            BlockContext offset = temp.offset(tempObserverFacing);
                            if (tempObserverFacing != null && State.get(offset) != State.CORRECT) {
                                return null;
                            }
                            temp = offset;
                        }
                        if (State.get(output) != State.CORRECT) {
                            return null;
                        }
                    } else {
                        // 活塞检查
                        if (output.requiredState.getBlock() instanceof PistonBaseBlock) {
                            if (!output.currentState.isAir()) {
                                return null;
                            }
                        }
                        // 侦测器隔空激活活塞
                        for (Direction direction : Direction.values()) {
                            BlockContext offset = output.offset(direction);
                            if (offset.blockPos.equals(output.blockPos)) {
                                continue;
                            }
                            if (offset.blockPos.equals(input.blockPos)) {
                                continue;
                            }
                            if (offset.blockPos.equals(ctx.blockPos)) {
                                continue;
                            }
                            if (offset.requiredState.getBlock() instanceof PistonBaseBlock) {
                                if (!offset.currentState.isAir()) {
                                    return null;
                                }
                            }
                        }
                        if (!output.requiredState.isAir()) {
                            return null;
                        }
                    }
                }
                return new Action()
                        .setWaitTick(6)
                        .setLookDirection(facing);
            }
            case LADDER -> {
                var facing = ctx.requiredState.getValue(LadderBlock.FACING);
                return new Action().setSides(facing).setLookDirection(facing.getOpposite());
            }
            case LANTERN -> {
                if (ctx.requiredState.getValue(LanternBlock.HANGING))
                    return new Action().setLookDirection(Direction.UP);
                return new Action().setLookDirection(Direction.DOWN);
            }
            case ROD -> {
                var requiredBlock = ctx.requiredState.getBlock();
                var facing = ctx.requiredState.getValue(EndRodBlock.FACING);

                // 如果前面朝向自己的末地烛，而放置方式相反，那么反向放置
                if (requiredBlock instanceof EndRodBlock) {
                    var forwardState = ctx.level.getBlockState(ctx.blockPos.relative(facing));
                    var forwardStateSchematic = ctx.level.getBlockState(ctx.blockPos.relative(facing));
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
                var facing = ctx.requiredState.getValue(TripWireHookBlock.FACING);
                return new Action().setSides(facing);
            }
            case RAIL -> {
                Action action = new Action();
                RailShape shape;
                if (ctx.requiredState.getBlock() instanceof RailBlock)
                    shape = ctx.requiredState.getValue(RailBlock.SHAPE);
                else
                    shape = ctx.requiredState.getValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT);

                switch (shape) {
                    case EAST_WEST, ASCENDING_EAST -> action.setLookDirection(Direction.EAST);
                    case NORTH_SOUTH, ASCENDING_NORTH -> action.setLookDirection(Direction.NORTH);
                    case ASCENDING_WEST -> action.setLookDirection(Direction.WEST);
                    case ASCENDING_SOUTH -> action.setLookDirection(Direction.SOUTH);
                }
                if (ctx.requiredState.getBlock() instanceof RailBlock) {
                    if (shape == RailShape.SOUTH_EAST) {
                        return action;
                    }
                    // TODO)) 完成这非常恶心的铁轨算法
                }
                return action;
            }
            case PISTON -> {
                Direction facing = ctx.requiredState.getValue(BlockStateProperties.FACING);
                // 侦测器链, 查找源头状态
                BlockContext temp = ctx.offset(facing.getOpposite());
                while (temp.requiredState.getBlock() instanceof ObserverBlock) {
                    @Nullable Direction tempObserverFacing = temp.getRequiredStateProperty(ObserverBlock.FACING).orElse(null);
                    BlockContext offset = temp.offset(tempObserverFacing);
                    if (tempObserverFacing != null && State.get(offset) != State.CORRECT) {
                        return null;
                    }
                    temp = offset;
                }
                return new Action().setLookDirection(facing.getOpposite());
            }
            // 新增：告示牌16方向处理
            case SIGN -> {
                Block signBlock = ctx.requiredState.getBlock();
                // 站立告示牌：处理0-15的16方向旋转值
                if (signBlock instanceof StandingSignBlock) {
                    int rotation = ctx.requiredState.getValue(StandingSignBlock.ROTATION);
                    return new Action()
                            .setSides(Direction.DOWN)
                            .setLookRotation(rotation)
                            .setRequiresSupport();
                }
                // 墙告示牌：保留原有4方向逻辑
                if (signBlock instanceof WallSignBlock) {
                    Direction facing = ctx.requiredState.getValue(WallSignBlock.FACING);
                    return new Action()
                            .setSides(facing.getOpposite())
                            .setLookDirection(facing.getOpposite())
                            .setRequiresSupport();
                }
                // 天花板悬挂告示牌处理逻辑
                //#if MC >= 12002
                if (signBlock instanceof WallHangingSignBlock) {
                    //TODO: 视乎方向还是有点问题, 待处理
                    Direction facing = ctx.requiredState.getValue(WallHangingSignBlock.FACING);
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
                    int rotation = ctx.requiredState.getValue(CeilingHangingSignBlock.ROTATION);
                    boolean attachFace = ctx.requiredState.getValue(CeilingHangingSignBlock.ATTACHED);
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
                Block block = ctx.requiredState.getBlock();

                if (block instanceof FaceAttachedHorizontalDirectionalBlock) {
                    Direction side = ctx.requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    AttachFace face = ctx.requiredState.getValue(BlockStateProperties.ATTACH_FACE);

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
                    Direction facing = ctx.requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    if (block instanceof FenceGateBlock) // 栅栏门
                        facing = facing.getOpposite();
                    action.setLookDirection(facing.getOpposite());
                }

                if (block instanceof BaseEntityBlock) {
                    Direction facing;
                    if (ctx.requiredState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                        facing = ctx.requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                        //#if MC >= 11904
                        if (block instanceof DecoratedPotBlock
                                || block instanceof CampfireBlock)
                            facing = facing.getOpposite();
                        //#endif
                        action.setSides(facing).setLookDirection(facing.getOpposite());
                    }
                    if (ctx.requiredState.hasProperty(BlockStateProperties.FACING)) {
                        facing = ctx.requiredState.getValue(BlockStateProperties.FACING);
                        if (ctx.requiredState.getBlock() instanceof ShulkerBoxBlock) {
                            facing = facing.getOpposite();
                        }
                        action.setSides(facing).setLookDirection(facing.getOpposite());
                    }
                }

                //方块型珊瑚的替换
                if (Configs.Put.REPLACE_CORAL.getBooleanValue() && block.getDescriptionId().endsWith("_coral_block")) {
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
            boolean printerBreakWrongStateBlock = Configs.Put.BREAK_WRONG_STATE_BLOCK.getBooleanValue();
            switch (requiredType) {
                case SLAB -> {
                    if (ctx.requiredState.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
                        Direction requiredHalf = ctx.currentState.getValue(SlabBlock.TYPE) == SlabType.BOTTOM ? Direction.DOWN : Direction.UP;

                        return new Action().setSides(requiredHalf);
                    } else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(ctx);
                }
                case SNOW -> {
                    int layers = ctx.currentState.getValue(SnowLayerBlock.LAYERS);
                    if (layers < ctx.requiredState.getValue(SnowLayerBlock.LAYERS)) {
                        Map<Direction, Vec3> sides = new HashMap<>() {{
                            put(Direction.UP, new Vec3(0, (layers / 8d) - 1, 0));
                        }};
                        return new ClickAction().setItem(Items.SNOW).setSides(sides);
                    } else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(ctx);

                }
                case DOOR, TRAPDOOR -> {
                    //判断门是不是铁制的，如果是就直接返回
                    if (ctx.requiredState.is(Blocks.IRON_DOOR) || ctx.requiredState.is(Blocks.IRON_TRAPDOOR)) break;
                    if (ctx.requiredState.getValue(BlockStateProperties.OPEN) != ctx.currentState.getValue(BlockStateProperties.OPEN)) {
                        return new ClickAction();
                    } else if (printerBreakWrongStateBlock && ctx.requiredState.getValue(DoorBlock.FACING) != ctx.currentState.getValue(DoorBlock.FACING))
                        BreakManager.addBlockToBreak(ctx);
                }
                case FENCE_GATE -> {
                    var facing = ctx.requiredState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    if (facing.getOpposite() == ctx.currentState.getValue(BlockStateProperties.HORIZONTAL_FACING)
                            || ctx.requiredState.getValue(BlockStateProperties.OPEN) != ctx.currentState.getValue(BlockStateProperties.OPEN)
                    ) {
                        return new ClickAction().setSides(facing.getOpposite()).setLookDirection(facing);
                    } else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(ctx);
                }
                case LEVER -> {
                    if (ctx.requiredState.getValue(LeverBlock.POWERED) != ctx.currentState.getValue(LeverBlock.POWERED))
                        return new ClickAction();
                    else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(ctx);
                }
                case CANDLES -> {
                    if (ctx.currentState.getValue(BlockStateProperties.CANDLES) < ctx.requiredState.getValue(BlockStateProperties.CANDLES))
                        return new ClickAction().setItem(ctx.requiredState.getBlock().asItem());
                    else if (!ctx.currentState.getValue(CandleBlock.LIT) && ctx.requiredState.getValue(CandleBlock.LIT))
                        return new ClickAction().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE);
                    else if (ctx.currentState.getValue(CandleBlock.LIT) && !ctx.requiredState.getValue(CandleBlock.LIT))
                        return new ClickAction();
                    else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(ctx);
                }
                case PICKLES -> {
                    if (ctx.currentState.getValue(SeaPickleBlock.PICKLES) < ctx.requiredState.getValue(SeaPickleBlock.PICKLES))
                        return new ClickAction().setItem(Items.SEA_PICKLE);
                    else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(ctx);
                }
                case REPEATER -> {
                    if (!ctx.requiredState.getValue(RepeaterBlock.DELAY).equals(ctx.currentState.getValue(RepeaterBlock.DELAY)))
                        return new ClickAction();
                    else if (
                            printerBreakWrongStateBlock &&
                                    ctx.requiredState.getValue(RepeaterBlock.POWERED) == ctx.currentState.getValue(RepeaterBlock.POWERED) &&
                                    ctx.requiredState.getValue(RepeaterBlock.LOCKED) == ctx.currentState.getValue(RepeaterBlock.LOCKED)
                    ) BreakManager.addBlockToBreak(ctx);
                }
                case COMPARATOR -> {
                    if (ctx.requiredState.getValue(ComparatorBlock.MODE) != ctx.currentState.getValue(ComparatorBlock.MODE))
                        return new ClickAction();
                    else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(ctx);
                }
                case NOTE_BLOCK -> {
                    if (Configs.Put.NOTE_BLOCK_TUNING.getBooleanValue() && !Objects.equals(ctx.requiredState.getValue(NoteBlock.NOTE), ctx.currentState.getValue(NoteBlock.NOTE)))
                        return new ClickAction();
                }
                case CAMPFIRE -> {
                    if (!ctx.requiredState.getValue(CampfireBlock.LIT) && ctx.currentState.getValue(CampfireBlock.LIT))
                        return new ClickAction().setItems(Implementation.SHOVELS).setSides(Direction.UP);
                    else if (ctx.requiredState.getValue(CampfireBlock.LIT) && !ctx.currentState.getValue(CampfireBlock.LIT))
                        return new ClickAction().setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE);
                    else if (printerBreakWrongStateBlock && ctx.requiredState.getValue(CampfireBlock.FACING) != ctx.currentState.getValue(CampfireBlock.FACING))
                        BreakManager.addBlockToBreak(ctx);
                }
                case END_PORTAL_FRAME -> {
                    if (ctx.requiredState.getValue(EndPortalFrameBlock.HAS_EYE) && !ctx.currentState.getValue(EndPortalFrameBlock.HAS_EYE))
                        return new ClickAction().setItem(Items.ENDER_EYE);
                    else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(ctx);
                }
                //#if MC >= 11904
                case FLOWERBED -> {
                    if (ctx.currentState.getValue(BlockStateProperties.FLOWER_AMOUNT) <= ctx.requiredState.getValue(BlockStateProperties.FLOWER_AMOUNT)) {
                        return new ClickAction().setItem(ctx.requiredState.getBlock().asItem());
                    } else if (printerBreakWrongStateBlock) BreakManager.addBlockToBreak(ctx);
                }
                //#endif
                case REDSTONE -> {
                    // 在Java版中，对于没有连接到任何红石元件的十字形的红石线，可以按使用键使其变为点状，从而不与任何方向连接，再按一次可以恢复。
                    boolean allNoneRequired = ctx.requiredState.getValue(RedStoneWireBlock.NORTH) == RedstoneSide.NONE &&
                            ctx.requiredState.getValue(RedStoneWireBlock.SOUTH) == RedstoneSide.NONE &&
                            ctx.requiredState.getValue(RedStoneWireBlock.EAST) == RedstoneSide.NONE &&
                            ctx.requiredState.getValue(RedStoneWireBlock.WEST) == RedstoneSide.NONE;

                    boolean allSideCurrent = ctx.currentState.getValue(RedStoneWireBlock.NORTH) == RedstoneSide.SIDE &&
                            ctx.currentState.getValue(RedStoneWireBlock.SOUTH) == RedstoneSide.SIDE &&
                            ctx.currentState.getValue(RedStoneWireBlock.EAST) == RedstoneSide.SIDE &&
                            ctx.currentState.getValue(RedStoneWireBlock.WEST) == RedstoneSide.SIDE;

                    if (allNoneRequired && allSideCurrent) {
                        return new ClickAction().setItem(Items.AIR);
                    }
                }
                case VINES, GLOW_LICHEN -> {
                    for (Direction direction : Direction.values()) {
                        if (direction == Direction.DOWN) continue;
                        if ((Boolean) getPropertyByName(ctx.requiredState, direction.name())) {
                            return new Action().setSides(direction).setLookDirection(direction);
                        }
                    }
                    BreakManager.addBlockToBreak(ctx);
                }
                case CAULDRON -> {
                    if (ctx.currentState.getValue(LayeredCauldronBlock.LEVEL) > ctx.requiredState.getValue(LayeredCauldronBlock.LEVEL)) {
                        if (InventoryUtils.playerHasAccessToItem(mc.player, Items.GLASS_BOTTLE))
                            return new ClickAction().setItem(Items.GLASS_BOTTLE);
                        else
                            mc.gui.setOverlayMessage(Component.nullToEmpty("降低炼药锅内水位需要 §l§6" + PreprocessUtils.getNameFromItem(Items.GLASS_BOTTLE)), false);
                    }
                    if (ctx.currentState.getValue(LayeredCauldronBlock.LEVEL) < ctx.requiredState.getValue(LayeredCauldronBlock.LEVEL))
                        if (InventoryUtils.playerHasAccessToItem(mc.player, Items.POTION))
                            return new ClickAction().setItem(Items.POTION);
                        else
                            mc.gui.setOverlayMessage(Component.nullToEmpty("增加炼药锅内水位需要 §l§6" + PreprocessUtils.getNameFromItem(Items.GLASS_BOTTLE)), false);
                }
                case DAYLIGHT_DETECTOR -> {
                    if (ctx.currentState.getValue(DaylightDetectorBlock.INVERTED) != ctx.requiredState.getValue(DaylightDetectorBlock.INVERTED))
                        return new ClickAction();
                }
                case FIRE -> {
                    if (!ctx.requiredState.getValue(FireBlock.AGE).equals(ctx.currentState.getValue(FireBlock.AGE)))
                        return null;
                    if (ctx.requiredState.getBlock() instanceof SoulFireBlock) return null;
                    for (Direction direction : Direction.values()) {
                        if (direction == Direction.DOWN) continue;
                        if ((Boolean) getPropertyByName(ctx.requiredState, direction.name())) {
                            return new Action().setSides(direction).setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                        }
                    }
                    return new Action().setSides(Direction.DOWN).setItems(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE).setRequiresSupport();
                }
                case COMPOSTER -> {
                    if (!Configs.Put.FILL_COMPOSTER.getBooleanValue()) return null;
                    if (ctx.currentState.getValue(ComposterBlock.LEVEL) < ctx.requiredState.getValue(ComposterBlock.LEVEL)) {
                        return new ClickAction().setItems(compostableItems);
                    }
                }
                case STAIR -> {
                    if (printerBreakWrongStateBlock &&
                            (ctx.requiredState.getValue(StairBlock.FACING) != ctx.currentState.getValue(StairBlock.FACING) ||
                                    ctx.requiredState.getValue(StairBlock.HALF) != ctx.currentState.getValue(StairBlock.HALF))) {
                        BreakManager.addBlockToBreak(ctx);
                    }
                }
                case DEFAULT -> {
                    Class<?>[] ignored = new Class<?>[]
                            {FenceBlock.class,
                                    WallBlock.class,
                                    IronBarsBlock.class,
                                    PressurePlateBlock.class,
                            };
                    if (printerBreakWrongStateBlock && !Arrays.asList(ignored).contains(ctx.requiredState.getBlock().getClass()))
                        BreakManager.addBlockToBreak(ctx);
                }
            }
        } else if (state == State.WRONG_BLOCK) switch (requiredType) {
            case FARMLAND -> {
                Block[] soilBlocks = new Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT_PATH, Blocks.COARSE_DIRT};

                for (Block soilBlock : soilBlocks) {
                    if (ctx.currentState.getBlock().equals(soilBlock))
                        return new ClickAction().setItems(Implementation.HOES);
                }

            }
            case DIRT_PATH -> {
                Block[] soilBlocks = new Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT,
                        Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.MYCELIUM, Blocks.PODZOL};

                for (Block soilBlock : soilBlocks) {
                    if (ctx.currentState.getBlock().equals(soilBlock))
                        return new ClickAction().setItems(Implementation.SHOVELS);
                }

            }
            case FLOWER_POT -> {
                if (ctx.requiredState.getBlock() instanceof FlowerPotBlock potBlock) {
                    Block content = potBlock.getPotted();
                    if (content != Blocks.AIR) {
                        return new ClickAction().setItem(content.asItem());
                    }
                }
            }
            case CAULDRON -> {
                if (Arrays.asList(requiredType.classes).contains(ctx.currentState.getBlock().getClass()))
                    return null;
                else if (Configs.Put.BREAK_WRONG_BLOCK.getBooleanValue() && BreakManager.canBreakBlock(ctx.blockPos))
                    BreakManager.addBlockToBreak(ctx);
            }
            case STRIP_LOG -> {
                Block stripped = STRIPPED_LOGS.get(ctx.currentState.getBlock());
                if (stripped != null && stripped == ctx.requiredState.getBlock())
                    return new ClickAction().setItems(Implementation.AXES);
            }
            // 新增：告示牌WrongBlock逻辑
            case SIGN -> {
                if (Configs.Put.BREAK_WRONG_BLOCK.getBooleanValue() && BreakManager.canBreakBlock(ctx.blockPos)) {
                    boolean isLegitimateSign = ctx.currentState.getBlock() instanceof StandingSignBlock
                            || ctx.currentState.getBlock() instanceof WallSignBlock
                            //#if MC >= 12002
                            || ctx.currentState.getBlock() instanceof WallHangingSignBlock
                            || ctx.currentState.getBlock() instanceof CeilingHangingSignBlock
                            //#endif
                            ;

                    if (!isLegitimateSign) {
                        BreakManager.addBlockToBreak(ctx);
                    }
                    BreakManager.addBlockToBreak(ctx);
                }
            }
            default -> {
                if (Configs.Put.REPLACE_CORAL.getBooleanValue() && ctx.requiredState.getBlock().getDescriptionId().contains("coral")) {
                    return null;
                }

                boolean printBreakWrongBlock = Configs.Put.BREAK_WRONG_BLOCK.getBooleanValue();
                boolean printerBreakExtraBlock = Configs.Put.BREAK_EXTRA_BLOCK.getBooleanValue();

                if (printBreakWrongBlock || printerBreakExtraBlock) {
                    if (BreakManager.canBreakBlock(ctx.blockPos)) {
                        if (printBreakWrongBlock && !ctx.requiredState.is(Blocks.AIR)) {
                            BreakManager.addBlockToBreak(ctx);
                        } else if (printerBreakExtraBlock && ctx.requiredState.is(Blocks.AIR)) {
                            BreakManager.addBlockToBreak(ctx);
                        }
                    }
                }
            }
        }

        return null;
    }

    // 水相关逻辑
    private @Nullable Action handleWaterLogic(BlockContext ctx, ClassHook requiredType, State state, AtomicReference<Boolean> skip) {
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
        ),                                      // 火把
        SLAB(SlabBlock.class),                  // 台阶
        STAIR(StairBlock.class),                // 楼梯
        TRAPDOOR(TrapDoorBlock.class),          // 活板门
        STRIP_LOG(RotatedPillarBlock.class),    // 去皮原木
        ANVIL(AnvilBlock.class),                // 铁砧
        HOPPER(HopperBlock.class),              // 漏斗
        CAMPFIRE(CampfireBlock.class),          // 营火
        BED(BedBlock.class),                    // 床
        BELL(BellBlock.class),                  // 钟
        AMETHYST(AmethystClusterBlock.class),   // 紫水晶
        DOOR(DoorBlock.class),                  // 门
        COCOA(CocoaBlock.class),                // 可可豆
        //#if MC >= 12003
        CRAFTER(CrafterBlock.class),            // 合成器
        //#endif
        CHEST(ChestBlock.class),                // 箱子
        OBSERVER(ObserverBlock.class),          // 侦测器
        LADDER(LadderBlock.class),              // 梯子
        LANTERN(LanternBlock.class),            // 灯笼
        ROD(RodBlock.class),                    // 末地烛 避雷针
        TRIPWIRE_HOOK(TripWireHookBlock.class), // 绊线钩
        RAIL(BaseRailBlock.class),              // 铁轨
        PISTON(PistonBaseBlock.class),          // 活塞 （为了避免被破坏错误状态破坏）
        SIGN(
                StandingSignBlock.class,
                WallSignBlock.class
                //#if MC >= 12002
                , WallHangingSignBlock.class
                , CeilingHangingSignBlock.class
                //#endif
        ),
        // 点击
        FLOWER_POT(FlowerPotBlock.class),               // 花盆
        BIG_DRIPLEAF_STEM(BigDripleafStemBlock.class),  // 大垂叶茎
        CAVE_VINES(CaveVinesBlock.class, CaveVinesPlantBlock.class),                // 洞穴藤蔓
        WEEPING_VINES(WeepingVinesBlock.class, WeepingVinesPlantBlock.class),       // 垂泪藤
        TWISTING_VINES(TwistingVinesBlock.class, TwistingVinesPlantBlock.class),    // 缠怨藤
        SNOW(SnowLayerBlock.class),                     // 雪
        CANDLES(CandleBlock.class),                     // 蜡烛
        REPEATER(RepeaterBlock.class),                  // 中继器
        COMPARATOR(ComparatorBlock.class),              // 比较器
        PICKLES(SeaPickleBlock.class),                  // 海泡菜
        NOTE_BLOCK(NoteBlock.class),                    // 音符盒
        END_PORTAL_FRAME(EndPortalFrameBlock.class),    // 末地传送门框架
        //#if MC >= 11904
        FLOWERBED(
                //#if MC >= 12105
                FlowerBedBlock.class
                //#else
                //$$ PinkPetalsBlock.class
                //#endif
        ), // 花簇（ojng你看看你这是什么抽象命名）
        //#endif
        VINES(VineBlock.class),                         // 藤蔓
        GLOW_LICHEN(GlowLichenBlock.class),             // 发光地衣
        FIRE(FireBlock.class, SoulFireBlock.class),     // 火，灵魂火
        REDSTONE(RedStoneWireBlock.class),              // 红石粉
        FENCE_GATE(FenceGateBlock.class),               // 栅栏门
        LEVER(LeverBlock.class),                        // 拉杆
        CAULDRON(CauldronBlock.class, LavaCauldronBlock.class, LayeredCauldronBlock.class), // 炼药锅
        DAYLIGHT_DETECTOR(DaylightDetectorBlock.class), // 阳光探测器
        COMPOSTER(ComposterBlock.class),                // 堆肥桶

        // 其他
        FARMLAND(FarmBlock.class),              // 耕地
        DIRT_PATH(DirtPathBlock.class),         // 土径
        DEAD_CORAL(Block.class),                // 死珊瑚
        NETHER_PORTAL(NetherPortalBlock.class), // 下界传送门
        SKIP(SkullBlock.class, LiquidBlock.class, BubbleColumnBlock.class, WaterlilyBlock.class), // 跳过
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
            for (Direction side : sides.keySet()) {
                BlockPos neighborPos = pos.relative(side);
                BlockState neighborState = world.getBlockState(neighborPos);
                if (Configs.Put.PRINT_IN_AIR.getBooleanValue() && !this.requiresSupport && !Implementation.isInteractive(neighborState.getBlock())) {
                    return side;
                }
                // 检查该侧面是否可以被点击且不可替换
                if (canBeClicked(world, neighborPos) && !BlockUtils.isReplaceable(neighborState)) {
                    validSides.add(side);
                }
            }
            if (validSides.isEmpty()) {
                return null;
            }
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

        public void queueAction(Printer.Queue queue, BlockPos blockPos, Direction side, boolean useShift) {
            if (Configs.Put.PRINT_IN_AIR.getBooleanValue() && !this.requiresSupport) {
                queue.queueClick(
                        blockPos,
                        side.getOpposite(),
                        getSides().get(side),
                        useShift
                );
            } else {
                queue.queueClick(
                        blockPos.relative(side),
                        side.getOpposite(),
                        getSides().get(side),
                        useShift
                );
            }
        }
    }

    public static class ClickAction extends Action {
        @Override
        public void queueAction(Printer.Queue queue, BlockPos blockPos, Direction side, boolean useShift) {
            queue.queueClick(blockPos, side, getSides().get(side), false);
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