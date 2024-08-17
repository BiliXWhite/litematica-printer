package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
//import net.minecraft.util.registry.Registry;

public enum State {
    MISSING_BLOCK,
    WRONG_STATE,
    WRONG_BLOCK,
    CORRECT;

    public static State get(BlockState schematicBlockState, BlockState currentBlockState) {
//        if(currentBlockState.getBlock() instanceof FluidBlock){
//            System.out.println(Registries.BLOCK.getId(currentBlockState.getBlock()));
//        }
        if (!schematicBlockState.isAir() && (currentBlockState.isAir() ||
                (LitematicaMixinMod.REPLACEABLE_LIST.getStrings().stream()
                        .anyMatch(string -> !Registries.BLOCK.getId(schematicBlockState.getBlock()).toString().contains(string) &&
                                Registries.BLOCK.getId(currentBlockState.getBlock()).toString().contains(string)) &&
                        LitematicaMixinMod.REPLACE.getBooleanValue())))
//        if (!schematicBlockState.isAir() && (currentBlockState.isAir() || currentBlockState.getBlock() instanceof FluidBlock || currentBlockState.isOf(Blocks.SNOW) || currentBlockState.isOf(Blocks.BUBBLE_COLUMN)))
//        if (!schematicBlockState.isAir() && (currentBlockState.isAir())
            return State.MISSING_BLOCK;
        else if (schematicBlockState.getBlock().equals(currentBlockState.getBlock())
                && !schematicBlockState.equals(currentBlockState))
            return State.WRONG_STATE;
        else if (!schematicBlockState.getBlock().equals(currentBlockState.getBlock()))
            return WRONG_BLOCK;

        return State.CORRECT;
    }


    public enum PrintModeType implements IConfigOptionListEntry {
        PRINTER("printer", "打印"),
        BEDROCK("bedrock", "基岩"),
        EXCAVATE("excavate", "挖掘"),
        FLUID("fluid", "流体");

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
        TW("tw", "tw"),
        ME("me", "自带");

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

            return ExcavateListMode.ME;
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
    public enum ListType implements IConfigOptionListEntry {
        SPHERE("sphere", "球体"),
        CUBE("cube", "立方体");

        private final String configString;
        private final String translationKey;

        ListType(String configString, String translationKey) {
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
        public ListType fromString(String name) {
            return fromStringStatic(name);
        }

        public static ListType fromStringStatic(String name) {
            for (ListType mode : ListType.values()) {
                if (mode.configString.equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            return ListType.SPHERE;
        }
    }

}
