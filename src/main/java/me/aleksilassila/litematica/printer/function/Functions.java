package me.aleksilassila.litematica.printer.function;

import com.google.common.collect.ImmutableList;
import me.aleksilassila.litematica.printer.function.breaks.FunctionMine;
import me.aleksilassila.litematica.printer.function.placements.FunctionFill;
import me.aleksilassila.litematica.printer.function.placements.FunctionFluid;

public class Functions {
    public static final FunctionBedrock BEDROCK = new FunctionBedrock();
    public static final FunctionFill FILL = new FunctionFill();
    public static final FunctionFluid FLUID = new FunctionFluid();
    public static final FunctionMine MINE = new FunctionMine();

    public static final ImmutableList<Function> VALUES = ImmutableList.of(BEDROCK, FILL, FLUID, MINE);
}
