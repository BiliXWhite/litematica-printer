package me.aleksilassila.litematica.printer.guide;

import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;

import java.util.concurrent.atomic.AtomicReference;

public class SkipGuide extends Guide {

    public SkipGuide(SchematicBlockContext context) {
        super(context);
    }

    @Override
    public boolean canExecute(AtomicReference<Boolean> skipOtherGuide) {
        skipOtherGuide.set(true);
        return false;
    }
}