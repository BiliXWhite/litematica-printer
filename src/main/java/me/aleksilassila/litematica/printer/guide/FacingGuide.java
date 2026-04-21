//package me.aleksilassila.litematica.printer.guide;
//
//import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
//import me.aleksilassila.litematica.printer.utils.minecraft.BlockStateUtils;
//import net.minecraft.core.Direction;
//import net.minecraft.core.FrontAndTop;
//import net.minecraft.world.level.block.state.properties.*;
//import org.jspecify.annotations.Nullable;
//
//public abstract class FacingGuide {
//    protected final SchematicBlockContext context;
//
//    /**
//     * 核心朝向
//     */
//    protected final @Nullable Direction facing;
//    /**
//     * 轴 (X/Y/Z) - 原木、锁链
//     */
//    protected final Direction.@Nullable Axis axis;
//    /**
//     * 仅水平轴 (X/Z)
//     */
//    protected final Direction.@Nullable Axis horizontalAxis;
//    /**
//     * 方块朝向 (前+上) - 命令方块、结构方块
//     */
//    protected final @Nullable FrontAndTop orientation;
//
//    /**
//     * 楼梯/台阶的上下半部分
//     */
//    protected final @Nullable Half half;
//    /**
//     * 双格方块 (门/床) 的上下段
//     */
//    protected final @Nullable DoubleBlockHalf doubleBlockHalf;
//    /**
//     * 台阶类型
//     */
//    protected final @Nullable SlabType slabType;
//    /**
//     * 床的部分 (床头/床尾)
//     */
//    protected final @Nullable BedPart bedPart;
//
//    /**
//     * 附着面 (按钮/拉杆/火把)
//     */
//    protected final @Nullable AttachFace attachFace;
//    /**
//     * 钟的附着类型
//     */
//    protected final @Nullable BellAttachType bellAttachment;
//
//    /**
//     * 栅栏/玻璃板等：是否向北连接
//     */
//    protected final @Nullable Boolean north;
//    /**
//     * 栅栏/玻璃板等：是否向东连接
//     */
//    protected final @Nullable Boolean east;
//    /**
//     * 栅栏/玻璃板等：是否向南连接
//     */
//    protected final @Nullable Boolean south;
//    /**
//     * 栅栏/玻璃板等：是否向西连接
//     */
//    protected final @Nullable Boolean west;
//    /**
//     * 栅栏/玻璃板等：是否向上连接
//     */
//    protected final @Nullable Boolean up;
//    /**
//     * 栅栏/玻璃板等：是否向下连接
//     */
//    protected final @Nullable Boolean down;
//    /**
//     * 栅栏是否在墙内
//     */
//    protected final @Nullable Boolean inWall;
//
//    /**
//     * 墙方块：北侧连接状态
//     */
//    protected final @Nullable WallSide northWall;
//    /**
//     * 墙方块：东侧连接状态
//     */
//    protected final @Nullable WallSide eastWall;
//    /**
//     * 墙方块：南侧连接状态
//     */
//    protected final @Nullable WallSide southWall;
//    /**
//     * 墙方块：西侧连接状态
//     */
//    protected final @Nullable WallSide westWall;
//
//    /**
//     * 红石线：北侧连接状态
//     */
//    protected final @Nullable RedstoneSide northRedstone;
//    /**
//     * 红石线：东侧连接状态
//     */
//    protected final @Nullable RedstoneSide eastRedstone;
//    /**
//     * 红石线：南侧连接状态
//     */
//    protected final @Nullable RedstoneSide southRedstone;
//    /**
//     * 红石线：西侧连接状态
//     */
//    protected final @Nullable RedstoneSide westRedstone;
//
//    /**
//     * 门的铰链侧
//     */
//    protected final @Nullable DoorHingeSide doorHinge;
//    /**
//     * 楼梯形状
//     */
//    protected final @Nullable StairsShape stairsShape;
//    /**
//     * 铁轨形状
//     */
//    protected final @Nullable RailShape railShape;
//    /**
//     * 是否含水
//     */
//    protected final @Nullable Boolean waterlogged;
//
//    public FacingGuide(SchematicBlockContext context) {
//        this.context = context;
//
//        // 1. 初始化朝向与旋转
//        this.facing = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.FACING)
//                .or(() -> BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.HORIZONTAL_FACING))
//                .or(() -> BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.VERTICAL_DIRECTION))
//                .or(() -> BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.FACING_HOPPER))
//                .orElse(null);
//        this.axis = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.AXIS).orElse(null);
//        this.horizontalAxis = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.HORIZONTAL_AXIS).orElse(null);
//        this.orientation = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.ORIENTATION).orElse(null);
//
//        // 2. 初始化分层/分段
//        this.half = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.HALF).orElse(null);
//        this.doubleBlockHalf = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.DOUBLE_BLOCK_HALF).orElse(null);
//        this.slabType = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.SLAB_TYPE).orElse(null);
//        this.bedPart = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.BED_PART).orElse(null);
//
//        // 3. 初始化附着面
//        this.attachFace = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.ATTACH_FACE).orElse(null);
//        this.bellAttachment = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.BELL_ATTACHMENT).orElse(null);
//
//        // 4. 初始化连接状态 (你指出的缺失部分)
//        this.north = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.NORTH).orElse(null);
//        this.east = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.EAST).orElse(null);
//        this.south = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.SOUTH).orElse(null);
//        this.west = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.WEST).orElse(null);
//        this.up = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.UP).orElse(null);
//        this.down = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.DOWN).orElse(null);
//        this.inWall = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.IN_WALL).orElse(null);
//
//        this.northWall = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.NORTH_WALL).orElse(null);
//        this.eastWall = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.EAST_WALL).orElse(null);
//        this.southWall = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.SOUTH_WALL).orElse(null);
//        this.westWall = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.WEST_WALL).orElse(null);
//
//        this.northRedstone = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.NORTH_REDSTONE).orElse(null);
//        this.eastRedstone = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.EAST_REDSTONE).orElse(null);
//        this.southRedstone = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.SOUTH_REDSTONE).orElse(null);
//        this.westRedstone = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.WEST_REDSTONE).orElse(null);
//
//        // 5. 初始化其他细节
//        this.doorHinge = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.DOOR_HINGE).orElse(null);
//        this.stairsShape = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.STAIRS_SHAPE).orElse(null);
//        this.railShape = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.RAIL_SHAPE)
//                .or(() -> BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.RAIL_SHAPE_STRAIGHT))
//                .orElse(null);
//        this.waterlogged = BlockStateUtils.getProperty(context.requiredState, BlockStateProperties.WATERLOGGED).orElse(null);
//    }
//
//
//}