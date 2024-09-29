
package me.aleksilassila.litematica.printer.printer.zxy.memory;
//#if MC < 12001
//$$ import net.fabricmc.api.EnvType;
//$$ import net.fabricmc.api.Environment;
//$$ import net.minecraft.item.ItemStack;
//$$ import net.minecraft.text.Text;
//$$ import net.minecraft.util.math.BlockPos;
//$$ import net.minecraft.util.math.Vec3d;
//$$ import org.jetbrains.annotations.Nullable;
//$$
//$$ import java.util.List;
//$$
//$$ @Environment(EnvType.CLIENT)
//$$ public class Memory {
//$$     private final @Nullable BlockPos position;
//$$     private final List<ItemStack> items;
//$$     private final @Nullable Vec3d nameOffset;
//$$     private @Nullable Text title;
//$$     private Boolean manualTitle = false;
//$$
//$$     private Memory(@Nullable BlockPos position, List<ItemStack> items, @Nullable Text title, @Nullable Vec3d nameOffset) {
//$$         this.position = position;
//$$         this.items = items;
//$$         this.title = title;
//$$         this.nameOffset = nameOffset;
//$$     }
//$$
//$$     public static Memory of(@Nullable BlockPos pos, List<ItemStack> items, @Nullable Text title, @Nullable Vec3d nameOffset) {
//$$ //        System.out.println("of" + items);
//$$         return new Memory(pos == null ? null : pos.toImmutable(), items, title, nameOffset);
//$$     }
//$$
//$$     public @Nullable BlockPos getPosition() {
//$$         return this.position;
//$$     }
//$$
//$$     public List<ItemStack> getItems() {
//$$         return this.items;
//$$     }
//$$
//$$     public @Nullable Text getTitle() {
//$$         return this.title;
//$$     }
//$$
//$$     public void setTitle(@Nullable Text title) {
//$$         this.title = title;
//$$     }
//$$
//$$     public String toString() {
//$$         return "Memory{position=" + this.position + ", items=" + this.items + ", title=" + this.title + "}";
//$$     }
//$$
//$$     public @Nullable Vec3d getNameOffset() {
//$$         return this.nameOffset;
//$$     }
//$$
//$$     public Boolean isManualTitle() {
//$$         return this.manualTitle;
//$$     }
//$$
//$$     public void setManualTitle(Boolean manualTitle) {
//$$         this.manualTitle = manualTitle;
//$$     }
//$$ }
//#endif