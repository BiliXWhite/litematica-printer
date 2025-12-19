package me.aleksilassila.litematica.printer.guides.placement;

import me.aleksilassila.litematica.printer.Debug;
import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import me.aleksilassila.litematica.printer.utils.BlockUtils;
import me.aleksilassila.litematica.printer.utils.DirectionUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * An old school guide where there are defined specific conditions
 * for player state depending on the block being placed.
 */
public class GeneralPlacementGuide extends PlacementGuide {
    public GeneralPlacementGuide(SchematicBlockState state) {
        super(state);
    }

    protected List<Direction> getPossibleSides() {
        return Arrays.asList(Direction.values());
    }

    protected Optional<Direction> getLookDirection() {
        return Optional.empty();
    }

    protected boolean getRequiresSupport() {
        return false;
    }

    protected boolean getRequiresExplicitShift() {
        return false;
    }

    protected Vec3 getHitModifier(Direction validSide) {
        return new Vec3(0, 0, 0);
    }

    private Optional<Direction> getValidSide(SchematicBlockState state) {
        boolean printInAir = false; // LitematicaMixinMod.PRINT_IN_AIR.getBooleanValue();

        List<Direction> sides = getPossibleSides();

        if (sides.isEmpty()) {
            return Optional.empty();
        }

        List<Direction> validSides = new ArrayList<>();
        for (Direction side : sides) {
            if (printInAir && !getRequiresSupport()) {
                return Optional.of(side);
            } else {
                SchematicBlockState neighborState = state.offset(side);

                if (getProperty(neighborState.currentState, SlabBlock.TYPE).orElse(null) == SlabType.DOUBLE) {
                    validSides.add(side);
                    continue;
                }
                
                if (canBeClicked(neighborState.world, neighborState.blockPos) && // Handle unclickable grass for example
                        !BlockUtils.isReplaceable(neighborState.currentState))
                    validSides.add(side);
            }
        }

        for (Direction validSide : validSides) {
            if (!isInteractive(state.offset(validSide).currentState.getBlock())) {
                return Optional.of(validSide);
            }
        }

        return validSides.isEmpty() ? Optional.empty() : Optional.of(validSides.getFirst());
    }

    protected boolean getUseShift(SchematicBlockState state) {
        if (getRequiresExplicitShift())
            return true;
        Direction clickSide = getValidSide(state).orElse(null);
        if (clickSide == null)
            return false;
        return isInteractive(state.offset(clickSide).currentState.getBlock());
    }

    private Optional<Vec3> getHitVector(SchematicBlockState state) {
        return getValidSide(state).map(side -> Vec3.atCenterOf(state.blockPos)
                .add(Vec3.atLowerCornerOf(DirectionUtils.getVector(side)).scale(0.5))
                .add(getHitModifier(side)));
    }

    @Nullable
    public PrinterPlacementContext getPlacementContext(LocalPlayer player) {
        try {
            Optional<Direction> validSide = getValidSide(state);
            Optional<Vec3> hitVec = getHitVector(state);
            Optional<ItemStack> requiredItem = getRequiredItem(player);
            int requiredSlot = getRequiredItemStackSlot(player);

            if (validSide.isEmpty() || hitVec.isEmpty() || requiredItem.isEmpty() || requiredSlot == -1)
                return null;

            Optional<Direction> lookDirection = getLookDirection();
            boolean requiresShift = getUseShift(state);

            BlockHitResult blockHitResult = new BlockHitResult(hitVec.get(), validSide.get().getOpposite(),
                    state.blockPos.relative(validSide.get()), false);

            return new PrinterPlacementContext(player, blockHitResult, requiredItem.get(), requiredSlot,
                    lookDirection.orElse(null), requiresShift);
        } catch (Exception e) {
            Debug.write("getPlacementContext(): Exception caught: {}", e.getMessage());
            //e.printStackTrace();
            return null;
        }
    }
}
