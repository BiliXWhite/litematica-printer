package me.aleksilassila.litematica.printer.enums;

/**
 * 方块冷却类型(使用枚举进行统一名称, 方块冷却管理器是以字符串设计的, 以下是打印机平时使用的类型)
 */
public enum BlockCooldownType {
    /*** 通用(单模式共用, 或者其他有共用需求的) ***/
    COMMON("common"),

    /*** 打印含水方块(用于破冰后可能存在空气, 导致重复放置冰块无法生成水) ***/
    PRINT_WATER("print_water"),

    /*** 打印(多模式) ***/
    PRINT("print"),

    /*** 挖掘(多模式) ***/
    MINE("mine"),

    /*** 排流体(多模式) ***/
    FLUID("fluid"),

    /*** 填充(多模式) ***/
    FILL("fill"),

    /*** 替换(多模式) ***/
    REPLACE("replace"),

    /*** 破基岩(多模式) ***/
    BEDROCK("bedrock");

    public final String type;

    BlockCooldownType(String type) {
        this.type = type;
    }
}