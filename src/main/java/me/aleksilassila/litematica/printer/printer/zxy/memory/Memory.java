package me.aleksilassila.litematica.printer.printer.zxy.memory;

import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
public class Memory {
    private final @Nullable BlockPos position;
    @Getter
    private final List<ItemStack> items;
    private final @Nullable Vec3 nameOffset;
    private @Nullable Component title;
    @Setter
    private Boolean manualTitle = false;

    private Memory(@Nullable BlockPos position, List<ItemStack> items, @Nullable Component title, @Nullable Vec3 nameOffset) {
        this.position = position;
        this.items = items;
        this.title = title;
        this.nameOffset = nameOffset;
    }

    public static Memory of(@Nullable BlockPos pos, List<ItemStack> items, @Nullable Component title, @Nullable Vec3 nameOffset) {
        return new Memory(pos == null ? null : pos.immutable(), items, title, nameOffset);
    }

    public @Nullable BlockPos getPosition() {
        return this.position;
    }

    public @Nullable Component getTitle() {
        return this.title;
    }

    public void setTitle(@Nullable Component title) {
        this.title = title;
    }

    public String toString() {
        return "Memory{position=" + this.position + ", items=" + this.items + ", title=" + this.title + "}";
    }

    public @Nullable Vec3 getNameOffset() {
        return this.nameOffset;
    }

    public Boolean isManualTitle() {
        return this.manualTitle;
    }
}