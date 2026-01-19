package me.aleksilassila.litematica.printer.config.builder;

import fi.dy.masa.malilib.config.options.ConfigInteger;
import me.aleksilassila.litematica.printer.I18n;

public class IntegerConfigBuilder extends BaseConfigBuilder<ConfigInteger, IntegerConfigBuilder> {
    private int defaultValue = 0;
    private int minValue = Integer.MIN_VALUE;
    private int maxValue = Integer.MAX_VALUE;
    private boolean useSlider = false;

    public IntegerConfigBuilder(I18n i18n) {
        super(i18n);
    }

    public IntegerConfigBuilder(String translateKey) {
        this(I18n.of(translateKey));
    }

    public IntegerConfigBuilder defaultValue(int value) {
        this.defaultValue = value;
        return this;
    }

    public IntegerConfigBuilder range(int min, int max) {
        return this.min(min).max(max);
    }

    public IntegerConfigBuilder min(int min) {
        this.minValue = min;
        return this;
    }

    public IntegerConfigBuilder max(int max) {
        this.maxValue = max;
        return this;
    }

    public IntegerConfigBuilder useSlider(boolean useSlider) {
        this.useSlider = useSlider;
        return this;
    }

    @Override
    public ConfigInteger build() {
        ConfigInteger config = new ConfigInteger(i18n.getId(), defaultValue, minValue, maxValue, useSlider, commentKey);
        return buildExtension(config);
    }
}
