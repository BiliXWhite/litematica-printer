package me.aleksilassila.litematica.printer.printer.zxy.memory;

//#if MC < 12001
//$$ import net.fabricmc.api.EnvType;
//$$ import net.fabricmc.api.Environment;
//$$ import net.minecraft.world.item.Item;
//$$ import net.minecraft.nbt.NbtCompound;
//$$ import org.jetbrains.annotations.Nullable;
//$$
//$$ import java.util.Objects;
//$$
//$$ @Environment(EnvType.CLIENT)
//$$ public class LightweightStack {
//$$     private final Item item;
//$$     private final @Nullable NbtCompound tag;
//$$
//$$     public LightweightStack(Item item, @Nullable NbtCompound tag) {
//$$         this.item = item;
//$$         this.tag = tag;
//$$     }
//$$
//$$     public Item getItem() {
//$$         return this.item;
//$$     }
//$$
//$$     public @Nullable NbtCompound getTag() {
//$$         return this.tag;
//$$     }
//$$
//$$     public boolean equals(Object o) {
//$$         if (this == o) {
//$$             return true;
//$$         } else if (o != null && this.getClass() == o.getClass()) {
//$$             LightweightStack that = (LightweightStack)o;
//$$             return this.item.equals(that.item) && Objects.equals(this.tag, that.tag);
//$$         } else {
//$$             return false;
//$$         }
//$$     }
//$$
//$$     public int hashCode() {
//$$         return Objects.hash(new Object[]{this.item, this.tag});
//$$     }
//$$
//$$     public String toString() {
//$$         return "LightweightStack{item=" + this.item + ", tag=" + this.tag + "}";
//$$     }
//$$ }
//#endif