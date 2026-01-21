package me.aleksilassila.litematica.printer.config.builder;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.utils.BlockUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.stream.Stream;

public class StringListConfigBuilder extends BaseConfigBuilder<ConfigStringList, StringListConfigBuilder> {
    private ImmutableList<String> defaultValue = ImmutableList.of();

    public StringListConfigBuilder(I18n i18n) {
        super(i18n);
    }

    public StringListConfigBuilder(String translateKey) {
        this(I18n.of(translateKey));
    }

    public StringListConfigBuilder defaultValue(ImmutableList<String> value) {
        this.defaultValue = value;
        return this;
    }

    public StringListConfigBuilder defaultValue(List<?> value) {
        this.defaultValue = convertToImmutableStringList(value.stream());
        return this;
    }

    public StringListConfigBuilder defaultValue(Stream<?> value) {
        this.defaultValue = convertToImmutableStringList(value);
        return this;
    }

    public StringListConfigBuilder defaultValue(Object... value) {
        this.defaultValue = convertToImmutableStringList(Stream.of(value));
        return this;
    }

    private ImmutableList<String> convertToImmutableStringList(Stream<?> stream) {
        return stream
                .map(this::convertObjectToString)
                .filter(s -> s != null && !s.isEmpty()) // 过滤空值
                .collect(ImmutableList.toImmutableList());
    }

    @SuppressWarnings("all")
    private String convertObjectToString(Object obj) {
        if (obj == null) {
            return "";
        }
        if (obj instanceof BlockState blockState) {
            return BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
        }
        if (obj instanceof Block block) {
            return BuiltInRegistries.BLOCK.getKey(block).toString();
        }
        if (obj instanceof ItemStack itemStack) {
            return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        }
        if (obj instanceof ItemLike itemLike) {
            Item item = itemLike.asItem();
            return BuiltInRegistries.ITEM.getKey(item).toString();
        }
        return obj.toString();
    }

    @Override
    public ConfigStringList build() {
        ConfigStringList config = new ConfigStringList(i18n.getId(), defaultValue, commentKey);
        return buildExtension(config);
    }
}