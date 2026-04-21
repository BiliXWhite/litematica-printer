package me.aleksilassila.litematica.printer.guide;

import me.aleksilassila.litematica.printer.enums.BlockMatchResult;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.printer.action.Action;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Guide {
    protected final SchematicBlockContext context;

    public Guide(SchematicBlockContext context) {
        this.context = context;
    }

    public boolean canExecute(AtomicReference<Boolean> skipOtherGuide) {
        return true;
    }

    public final Optional<Action> buildAction(BlockMatchResult state, AtomicReference<Boolean> skipOtherGuide) {
        return this.onBuildAction(state, skipOtherGuide)
                .or(() -> switch (state) {
                    case MISSING -> this.onBuildActionMissingBlock(state, skipOtherGuide);
                    case WRONG_BLOCK -> this.onBuildActionWrongBlock(state, skipOtherGuide);
                    case WRONG_STATE -> this.onBuildActionWrongState(state, skipOtherGuide);
                    case CORRECT -> this.onBuildActionCorrect(state, skipOtherGuide);
                });
    }

    protected Optional<Action> onBuildAction(BlockMatchResult state, AtomicReference<Boolean> skipOtherGuide) {
        return Optional.empty();
    }

    protected Optional<Action> onBuildActionMissingBlock(BlockMatchResult state, AtomicReference<Boolean> skipOtherGuide) {
        return Optional.empty();
    }

    protected Optional<Action> onBuildActionWrongBlock(BlockMatchResult state, AtomicReference<Boolean> skipOtherGuide) {
        return Optional.empty();
    }

    protected Optional<Action> onBuildActionWrongState(BlockMatchResult state, AtomicReference<Boolean> skipOtherGuide) {
        return Optional.empty();
    }

    protected Optional<Action> onBuildActionCorrect(BlockMatchResult state, AtomicReference<Boolean> skipOtherGuide) {
        return Optional.empty();
    }
}