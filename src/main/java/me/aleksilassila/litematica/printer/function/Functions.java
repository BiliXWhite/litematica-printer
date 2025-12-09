package me.aleksilassila.litematica.printer.function;

import com.google.common.collect.ImmutableList;

public class Functions {
    public static final FunctionBedrockMode FUNCTION_BEDROCK = new FunctionBedrockMode();
    public static final FunctionFillMode FUNCTION_FILL = new FunctionFillMode();
    public static final FunctionFluidMode FUNCTION_FLUID = new FunctionFluidMode();
    public static final FunctionMineMode FUNCTION_MINE_MODE = new FunctionMineMode();

    public static final ImmutableList<FunctionExtension> LIST = ImmutableList.of(
            FUNCTION_BEDROCK,
            FUNCTION_FILL,
            FUNCTION_FLUID,
            FUNCTION_MINE_MODE
    );
}
