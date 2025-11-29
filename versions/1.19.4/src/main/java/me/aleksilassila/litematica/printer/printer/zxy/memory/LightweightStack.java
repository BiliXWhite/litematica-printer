package me.aleksilassila.litematica.printer.printer.zxy.memory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public record LightweightStack(Item item, @Nullable CompoundTag tag) {

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            LightweightStack that = (LightweightStack) o;
            return this.item.equals(that.item) && Objects.equals(this.tag, that.tag);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(this.item, this.tag);
    }

    public @NotNull String toString() {
        return "LightweightStack{item=" + this.item + ", tag=" + this.tag + "}";
    }
}