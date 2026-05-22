package me.aleksilassila.litematica.printer.config.builder;

import fi.dy.masa.malilib.config.options.ConfigDouble;
import me.aleksilassila.litematica.printer.I18n;

public class FloatConfigBuilder extends BaseConfigBuilder<ConfigDouble, FloatConfigBuilder> {
    private double defaultValue = 0.0;
    private double minValue = Double.NEGATIVE_INFINITY;
    private double maxValue = Double.POSITIVE_INFINITY;
    private boolean useSlider = false;

    public FloatConfigBuilder(I18n i18n) {
        super(i18n);
    }

    public FloatConfigBuilder(String translateKey) {
        this(I18n.of(translateKey));
    }

    public FloatConfigBuilder defaultValue(double value) {
        this.defaultValue = value;
        return this;
    }

    public FloatConfigBuilder range(double min, double max) {
        return this.min(min).max(max);
    }

    public FloatConfigBuilder min(double min) {
        this.minValue = min;
        return this;
    }

    public FloatConfigBuilder max(double max) {
        this.maxValue = max;
        return this;
    }

    public FloatConfigBuilder useSlider(boolean useSlider) {
        this.useSlider = useSlider;
        return this;
    }

    @Override
    public ConfigDouble build() {
        ConfigDouble config = new ConfigDouble(i18n.getNameKey(), defaultValue, minValue, maxValue, useSlider, descKey);
        return buildExtension(config);
    }
}
