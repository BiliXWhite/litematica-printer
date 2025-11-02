package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters;
import net.minecraft.block.BlockState;

import java.util.HashSet;
import java.util.Set;

import static me.aleksilassila.litematica.printer.LitematicaMixinMod.I18N_PREFIX;

public enum State {
    MISSING_BLOCK,
    WRONG_STATE,
    WRONG_BLOCK,
    CORRECT;

    public static State get(BlockState schematicBlockState, BlockState currentBlockState) {

        Set<String> replaceSet = new HashSet<>(LitematicaMixinMod.REPLACEABLE_LIST.getStrings());

        // 如果两个方块状态完全相同，则返回正确状态
        if (schematicBlockState == currentBlockState)
            return CORRECT;
        // 如果方块类型相同但状态不同，则返回错误状态
        else if (schematicBlockState.getBlock().getDefaultState() == currentBlockState.getBlock().getDefaultState())
            return WRONG_STATE;
        // 如果原理图中方块不为空，且实际方块为空，则返回缺失方块状态
        else if (!schematicBlockState.isAir() && currentBlockState.isAir())
            return MISSING_BLOCK;
        // 如果启用了替换功能，且当前方块在可替换列表中，则返回缺失方块状态（实际上这会和破坏额外方块打架）
        else if (LitematicaMixinMod.REPLACE.getBooleanValue() &&
                replaceSet.stream().anyMatch(string -> !Filters.equalsName(string,schematicBlockState) &&
                        Filters.equalsName(string,currentBlockState)) && !schematicBlockState.isAir()
        ) return MISSING_BLOCK;
        // 其他情况返回错误方块状态
        else return WRONG_BLOCK;
    }



    public enum PrintModeType implements IConfigOptionListEntry {
        PRINTER("printer", "打印"),
        MINE("mine", "挖掘"),
        FLUID("fluid", "流体"),
        FILL("fill", "填充"),
        BEDROCK("bedrock", "破基岩");

        private final String configString;
        private final String translationKey;

        PrintModeType(String configString, String translationKey) {
            this.configString = configString;
            this.translationKey = translationKey;
        }
        @Override
        public String getStringValue() {
            return this.configString;
        }

        @Override
        public String getDisplayName() {
            return StringUtils.translate(this.translationKey);
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
                if (mode.configString.equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            return PrintModeType.PRINTER;
        }
    }
    public enum ExcavateListMode implements IConfigOptionListEntry {
        TWEAKEROO("tweakeroo", "Tweakeroo预设"),
        CUSTOM("custom", "自定义");

        private final String configString;
        private final String translationKey;

        ExcavateListMode(String configString, String translationKey) {
            this.configString = configString;
            this.translationKey = translationKey;
        }
        @Override
        public String getStringValue() {
            return this.configString;
        }

        @Override
        public String getDisplayName() {
            return StringUtils.translate(this.translationKey);
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
                if (mode.configString.equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            return ExcavateListMode.CUSTOM;
        }
    }
    public enum ModeType implements IConfigOptionListEntry {
        MULTI("multi", "多模"),
        SINGLE("single", "单模");

        private final String configString;
        private final String translationKey;

        ModeType(String configString, String translationKey) {
            this.configString = configString;
            this.translationKey = translationKey;
        }
        @Override
        public String getStringValue() {
            return this.configString;
        }

        @Override
        public String getDisplayName() {
            return StringUtils.translate(this.translationKey);
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
                if (mode.configString.equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            return ModeType.SINGLE;
        }
    }

    public enum RadiusShapeType implements IConfigOptionListEntry {
        SPHERE("sphere", I18N_PREFIX + ".list.iteratorShapeType.sphere"),
        CUBE("cube", I18N_PREFIX + ".list.iteratorShapeType.cube");

        private final String configString;
        private final String translationKey;

        RadiusShapeType(String configString, String translationKey) {
            this.configString = configString;
            this.translationKey = translationKey;
        }

        @Override
        public String getStringValue() {
            return this.configString;
        }

        @Override
        public String getDisplayName() {
            return StringUtils.translate(this.translationKey);
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
                if (mode.configString.equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            return RadiusShapeType.SPHERE;
        }
    }


    public enum IterationOrderType implements IConfigOptionListEntry {
        XYZ("xyz", "X→Y→Z"),
        XZY("xzy", "X→Z→Y"),
        YXZ("yxz", "Y→X→Z"),
        YZX("yzx", "Y→Z→X"),
        ZXY("zxy", "Z→X→Y"),
        ZYX("zyx", "Z→Y→X");

        private final String configString;
        private final String displayName;

        IterationOrderType(String configString, String displayName) {
            this.configString = configString;
            this.displayName = displayName;
        }

        @Override
        public String getStringValue() {
            return this.configString;
        }

        @Override
        public String getDisplayName() {
            return StringUtils.translate(this.displayName);
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
                if (type.configString.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return IterationOrderType.XYZ;
        }
    }

    public enum QuickShulkerModeType implements IConfigOptionListEntry {
        CLICK_SLOT("click_slot"),
        INVOKE("invoke");

        private final String configString;
        QuickShulkerModeType(String configString) {
            this.configString = configString;
        }

        @Override
        public String getStringValue() {
            return this.configString;
        }

        @Override
        public String getDisplayName() {
            return StringUtils.translate(I18N_PREFIX + ".quickShulkerMode." + this.configString);
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
                if (type.configString.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return QuickShulkerModeType.CLICK_SLOT;
        }
    }
    public enum FillModeFacingType implements IConfigOptionListEntry {
        DOWN("down"),
        UP("up"),
        WEST("west"),
        EAST("east"),
        NORTH("north"),
        SOUTH("south");

        private final String configString;

        FillModeFacingType(String configString) {
            this.configString = configString;
        }

        @Override
        public String getStringValue() {
            return this.configString;
        }

        public String getDisplayName() {
            return StringUtils.translate(I18N_PREFIX + ".fillModeFacing." + this.configString);
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
                if (type.configString.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return FillModeFacingType.DOWN;
        }
    }
}

