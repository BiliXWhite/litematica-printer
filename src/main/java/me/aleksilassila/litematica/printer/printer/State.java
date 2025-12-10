package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public enum State {
    MISSING_BLOCK,
    WRONG_STATE,
    WRONG_BLOCK,
    CORRECT;

    public static State get(BlockState schematicBlockState, BlockState currentBlockState) {
        Set<String> replaceSet = new HashSet<>(InitHandler.REPLACEABLE_LIST.getStrings());
        // 如果两个方块状态完全相同，则返回正确状态
        if (schematicBlockState == currentBlockState)
            return CORRECT;
            // 如果方块类型相同但状态不同，则返回错误状态
        else if (schematicBlockState.getBlock().defaultBlockState() == currentBlockState.getBlock().defaultBlockState())
            return WRONG_STATE;
            // 如果原理图中方块不为空，且实际方块为空，则返回缺失方块状态
        else if (!schematicBlockState.isAir() && currentBlockState.isAir())
            return MISSING_BLOCK;
            // 如果启用了替换功能，且当前方块在可替换列表中，则返回缺失方块状态（实际上这会和破坏额外方块打架）
        else if (InitHandler.REPLACE.getBooleanValue() &&
                replaceSet.stream().anyMatch(string -> !Filters.equalsName(string, schematicBlockState) &&
                        Filters.equalsName(string, currentBlockState)) && !schematicBlockState.isAir()
        ) return MISSING_BLOCK;
            // 其他情况返回错误方块状态
        else return WRONG_BLOCK;
    }

    public static State get(BlockPos pos) {
        BlockState schematicBlockState = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
        BlockState currentBlockState = Minecraft.getInstance().level.getBlockState(pos);
        return get(schematicBlockState, currentBlockState);
    }


    public enum PrintModeType implements IConfigOptionListEntry {
        PRINTER(I18n.PRINT_MODE_PRINTER),
        MINE(I18n.PRINT_MODE_MINE),
        FLUID(I18n.PRINT_MODE_FLUID),
        FILL(I18n.PRINT_MODE_FILL),
        REPLACE(I18n.PRINT_MODE_REPLACE),
        BEDROCK(I18n.PRINT_MODE_BEDROCK);

        private final I18n i18n;

        PrintModeType(I18n i18n) {
            this.i18n = i18n;
        }

        @Override
        public String getStringValue() {
            return this.i18n.getSimpleKey();
        }

        @Override
        public String getDisplayName() {
            return this.i18n.getConfigListKeyComponent().getString();
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            int id = this.ordinal();
            if (forward) {
                if (++id >= values().length) {
                    id = 0;
                }
            } else {
                if (--id < 0) {
                    id = values().length - 1;
                }
            }
            return values()[id % values().length];
        }

        @Override
        public PrintModeType fromString(String name) {
            return fromStringStatic(name);
        }

        public static PrintModeType fromStringStatic(String name) {
            for (PrintModeType mode : PrintModeType.values()) {
                if (mode.getStringValue().equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            return PrintModeType.PRINTER;
        }
    }

    public enum ExcavateListMode implements IConfigOptionListEntry {
        TWEAKEROO(I18n.EXCAVATE_LIST_MODE_TWEAKEROO),
        CUSTOM(I18n.EXCAVATE_LIST_MODE_CUSTOM);

        private final I18n i18n;

        ExcavateListMode(I18n i18n) {
            this.i18n = i18n;
        }

        @Override
        public String getStringValue() {
            return this.i18n.getSimpleKey();
        }

        @Override
        public String getDisplayName() {
            return this.i18n.getConfigListKeyComponent().getString();
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            int id = this.ordinal();
            if (forward) {
                if (++id >= values().length) {
                    id = 0;
                }
            } else {
                if (--id < 0) {
                    id = values().length - 1;
                }
            }
            return values()[id % values().length];
        }

        @Override
        public ExcavateListMode fromString(String name) {
            return fromStringStatic(name);
        }

        public static ExcavateListMode fromStringStatic(String name) {
            for (ExcavateListMode mode : ExcavateListMode.values()) {
                if (mode.getStringValue().equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            return ExcavateListMode.CUSTOM;
        }
    }

    public enum ModeType implements IConfigOptionListEntry {
        MULTI(I18n.MODE_TYPE_MULTI),
        SINGLE(I18n.MODE_TYPE_SINGLE);

        private final I18n i18n;

        ModeType(I18n i18n) {
            this.i18n = i18n;
        }


        @Override
        public String getStringValue() {
            return this.i18n.getSimpleKey();
        }

        @Override
        public String getDisplayName() {
            return this.i18n.getConfigListKeyComponent().getString();
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            int id = this.ordinal();

            if (forward) {
                if (++id >= values().length) {
                    id = 0;
                }
            } else {
                if (--id < 0) {
                    id = values().length - 1;
                }
            }

            return values()[id % values().length];
        }

        @Override
        public ModeType fromString(String name) {
            return fromStringStatic(name);
        }

        public static ModeType fromStringStatic(String name) {
            for (ModeType mode : ModeType.values()) {
                if (mode.getStringValue().equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            return ModeType.SINGLE;
        }
    }
    public enum FileBlockModeType implements IConfigOptionListEntry {
        WHITELIST(I18n.FILE_BLOCK_MODE_TYPE_WHITELIST),
        HANDHELD(I18n.FILE_BLOCK_MODE_TYPE_HANDHELD);

        private final I18n i18n;

        FileBlockModeType(I18n i18n) {
            this.i18n = i18n;
        }

        @Override
        public String getStringValue() {
            return this.i18n.getSimpleKey();
        }

        @Override
        public String getDisplayName() {
            return this.i18n.getConfigListKeyComponent().getString();
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            int id = this.ordinal();
            if (forward) {
                if (++id >= values().length) {
                    id = 0;
                }
            } else {
                if (--id < 0) {
                    id = values().length - 1;
                }
            }
            return values()[id % values().length];
        }

        @Override
        public FileBlockModeType fromString(String name) {
            return fromStringStatic(name);
        }

        public static FileBlockModeType fromStringStatic(String name) {
            for (FileBlockModeType mode : FileBlockModeType.values()) {
                if (mode.getStringValue().equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return FileBlockModeType.WHITELIST;
        }
    }

    public enum RadiusShapeType implements IConfigOptionListEntry {
        SPHERE(I18n.ITERATOR_SHAPE_TYPE_SPHERE),
        OCTAHEDRON(I18n.ITERATOR_SHAPE_TYPE_OCTAHEDRON),
        CUBE(I18n.ITERATOR_SHAPE_TYPE_CUBE);

        private final I18n i18n;

        RadiusShapeType(I18n i18n) {
            this.i18n = i18n;
        }

        @Override
        public String getStringValue() {
            return this.i18n.getSimpleKey();
        }

        @Override
        public String getDisplayName() {
            return this.i18n.getConfigListKeyComponent().getString();
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            int id = this.ordinal();
            if (forward) {
                if (++id >= values().length) {
                    id = 0;
                }
            } else {
                if (--id < 0) {
                    id = values().length - 1;
                }
            }
            return values()[id % values().length];
        }

        @Override
        public RadiusShapeType fromString(String name) {
            return fromStringStatic(name);
        }

        public static RadiusShapeType fromStringStatic(String name) {
            for (RadiusShapeType mode : RadiusShapeType.values()) {
                if (mode.getStringValue().equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return RadiusShapeType.SPHERE;
        }
    }


    public enum IterationOrderType implements IConfigOptionListEntry {
        XYZ(I18n.ITERATION_ORDER_XYZ),
        XZY(I18n.ITERATION_ORDER_XZY),
        YXZ(I18n.ITERATION_ORDER_YXZ),
        YZX(I18n.ITERATION_ORDER_YZX),
        ZXY(I18n.ITERATION_ORDER_ZXY),
        ZYX(I18n.ITERATION_ORDER_ZYX);

        private final I18n i18n;

        IterationOrderType(I18n i18n) {
            this.i18n = i18n;
        }

        @Override
        public String getStringValue() {
            return this.i18n.getSimpleKey();
        }

        @Override
        public String getDisplayName() {
            return this.i18n.getConfigListKeyComponent().getString();
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            int id = this.ordinal();
            if (forward) {
                if (++id >= values().length) {
                    id = 0;
                }
            } else {
                if (--id < 0) {
                    id = values().length - 1;
                }
            }
            return values()[id];
        }

        @Override
        public IterationOrderType fromString(String name) {
            return fromStringStatic(name);
        }

        public static IterationOrderType fromStringStatic(String name) {
            for (IterationOrderType type : IterationOrderType.values()) {
                if (type.getStringValue().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return IterationOrderType.XYZ;
        }
    }

    public enum QuickShulkerModeType implements IConfigOptionListEntry {
        CLICK_SLOT(I18n.PRINTER_QUICK_SHULKER_MODE_CLICK_SLOT),
        INVOKE(I18n.PRINTER_QUICK_SHULKER_MODE_INVOKE);

        private final I18n i18n;

        QuickShulkerModeType(I18n i18n) {
            this.i18n = i18n;
        }

        @Override
        public String getStringValue() {
            return this.i18n.getSimpleKey();
        }

        @Override
        public String getDisplayName() {
            return this.i18n.getConfigListKeyComponent().getString();
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            int id = this.ordinal();
            if (forward) {
                if (++id >= values().length) {
                    id = 0;
                }
            } else {
                if (--id < 0) {
                    id = values().length - 1;
                }
            }
            return values()[id];
        }

        @Override
        public QuickShulkerModeType fromString(String name) {
            return fromStringStatic(name);
        }

        public static QuickShulkerModeType fromStringStatic(String name) {
            for (QuickShulkerModeType type : QuickShulkerModeType.values()) {
                if (type.getStringValue().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return QuickShulkerModeType.CLICK_SLOT;
        }
    }

    public enum FillModeFacingType implements IConfigOptionListEntry {
        DOWN(I18n.FILL_MODE_FACING_DOWN),
        UP(I18n.FILL_MODE_FACING_UP),
        WEST(I18n.FILL_MODE_FACING_WEST),
        EAST(I18n.FILL_MODE_FACING_EAST),
        NORTH(I18n.FILL_MODE_FACING_NORTH),
        SOUTH(I18n.FILL_MODE_FACING_SOUTH);

        private final I18n i18n;

        FillModeFacingType(I18n i18n) {
            this.i18n = i18n;
        }

        @Override
        public String getStringValue() {
            return this.i18n.getSimpleKey();
        }

        public String getDisplayName() {
            return this.i18n.getConfigListKeyComponent().getString();
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            int id = this.ordinal();
            if (forward) {
                if (++id >= values().length) {
                    id = 0;
                }
            } else {
                if (--id < 0) {
                    id = values().length - 1;
                }
            }
            return values()[id];
        }

        @Override
        public FillModeFacingType fromString(String name) {
            return fromStringStatic(name);
        }

        public static FillModeFacingType fromStringStatic(String name) {
            for (FillModeFacingType type : FillModeFacingType.values()) {
                if (type.getStringValue().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return FillModeFacingType.DOWN;
        }
    }
}

