package me.aleksilassila.litematica.printer.guides;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.actions.Action;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.utils.PlayerInventoryUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;


public abstract class Guide extends Implementation {
    protected static final float BREAKING_PROGRESS_MAX = 0.7F;

    protected final SchematicBlockState state;
    protected final BlockState currentState;
    protected final BlockState targetState;

    public Guide(SchematicBlockState state) {
        this.state = state;
        this.currentState = state.currentState;
        this.targetState = state.targetState;
    }

    protected boolean playerHasRightItem(LocalPlayer player) {
        return getRequiredItemStackSlot(player) != -1;
    }

    protected int getSlotWithItem(LocalPlayer player, ItemStack itemStack) {
        NonNullList<ItemStack> items = PlayerInventoryUtils.getNonEquipmentItems(player);
        for (int i = 0; i < items.size(); ++i) {
            if (itemStack.isEmpty() && items.get(i).is(itemStack.getItem())) {
                return i;
            }
            if (!items.get(i).isEmpty() && ItemStack.isSameItem(items.get(i), itemStack)) {
                return i;
            }
        }

        return -1;
    }

    protected int getRequiredItemStackSlot(LocalPlayer player) {
        if (player.getAbilities().instabuild) {
            return PlayerInventoryUtils.getSelectedSlot(player);
        }

        Optional<ItemStack> requiredItem = getRequiredItem(player);
        return requiredItem.map(itemStack -> getSlotWithItem(player, itemStack)).orElse(-1);
    }

    public boolean canExecute(LocalPlayer player) {
        if (!playerHasRightItem(player)) {
            return false;
        }

        BlockState targetState = state.targetState;
        BlockState currentState = state.currentState;

        return !statesEqual(targetState, currentState);
    }

    abstract public @NotNull List<Action> execute(LocalPlayer player);

    abstract protected @NotNull List<ItemStack> getRequiredItems();

    /**
     * Returns the first required item that the player has access to,
     * or empty if the items are inaccessible.
     */
    protected Optional<ItemStack> getRequiredItem(LocalPlayer player) {
        List<ItemStack> requiredItems = getRequiredItems();

        for (ItemStack requiredItem : requiredItems) {
            if (player.getAbilities().instabuild) {
                return Optional.of(requiredItem);
            }

            int slot = getSlotWithItem(player, requiredItem);
            if (slot > -1) {
                return Optional.of(requiredItem);
            }
        }

        return Optional.empty();
    }

    protected boolean statesEqualIgnoreProperties(BlockState state1, BlockState state2,
                                                  Property<?>... propertiesToIgnore) {
        if (state1.getBlock() != state2.getBlock()) {
            return false;
        }

        loop:
        for (Property<?> property : state1.getProperties()) {
            if (property == BlockStateProperties.WATERLOGGED && !(state1.getBlock() instanceof CoralBlock)) {
                continue;
            }

            for (Property<?> ignoredProperty : propertiesToIgnore) {
                if (property == ignoredProperty) {
                    continue loop;
                }
            }

            try {
                if (state1.getValue(property) != state2.getValue(property)) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    protected static <T extends Comparable<T>> Optional<T> getProperty(BlockState blockState, Property<T> property) {
        if (blockState.hasProperty(property)) {
            return Optional.of(blockState.getValue(property));
        }
        return Optional.empty();
    }

    /**
     * Returns true if
     */
    protected boolean statesEqual(BlockState state1, BlockState state2) {
        return statesEqualIgnoreProperties(state1, state2);
    }

    public boolean skipOtherGuides() {
        return false;
    }
}