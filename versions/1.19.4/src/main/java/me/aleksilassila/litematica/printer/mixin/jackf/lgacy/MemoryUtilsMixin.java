package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;


 import me.aleksilassila.litematica.printer.LitematicaMixinMod;
 import net.minecraft.block.Block;
 import net.minecraft.block.BlockState;
 import net.minecraft.block.Blocks;
 import net.minecraft.block.entity.BlockEntity;
 import net.minecraft.client.MinecraftClient;
 import net.minecraft.client.gui.screen.ingame.HandledScreen;
 import net.minecraft.item.ItemStack;
 import net.minecraft.registry.Registries;
 import net.minecraft.screen.ScreenHandler;
 import net.minecraft.screen.slot.Slot;
 import net.minecraft.text.Text;
 import net.minecraft.util.math.BlockPos;
 import net.minecraft.util.math.Vec3d;
 import net.minecraft.world.World;
 import org.jetbrains.annotations.NotNull;
 import org.jetbrains.annotations.Nullable;
 import org.spongepowered.asm.mixin.Mixin;
 import org.spongepowered.asm.mixin.Overwrite;
 import org.spongepowered.asm.mixin.Shadow;
 import red.jackf.chesttracker.ChestTracker;
 import red.jackf.chesttracker.memory.MemoryDatabase;
 import red.jackf.chesttracker.memory.MemoryUtils;

 import java.util.Collection;
 import java.util.Collections;
 import java.util.List;
 import java.util.Objects;
 import java.util.stream.Collectors;

 import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.*;
 import static red.jackf.chesttracker.memory.MemoryUtils.checkValidCycle;

 @Mixin(MemoryUtils.class)
 public abstract class MemoryUtilsMixin {
     @Shadow
     private static <T extends ScreenHandler> boolean validScreenToTrack(HandledScreen<T> screen) {
         return false;
     }

     @Shadow private static @Nullable BlockPos latestPos;

     @Shadow(remap = false)
     public static List<ItemStack> condenseItems(List<ItemStack> list) {
         return null;
     }

     @Shadow
     @Nullable
     private static Text getTitleFromScreen(HandledScreen<?> screen, @Nullable BlockEntity blockEntity) {
         return null;
     }

     @Shadow
     public static Collection<BlockPos> getConnected(@NotNull World world, BlockPos pos) {
         return null;
     }

     @Shadow
     private static Vec3d getAveragePos(BlockPos basePos, Collection<BlockPos> connected) {
         return null;
     }


     @Shadow
     public static void setLatestPos(@Nullable BlockPos latestPos) {
     }

     /**
      * @author 2
      * @reason 2
      */
     @Overwrite
     public static boolean areStacksEquivalent(@NotNull ItemStack stack1, @NotNull ItemStack stack2, boolean ignoreNbt) {
         return stack1.getItem() == stack2.getItem() && (ignoreNbt || !stack1.hasNbt() && !stack2.hasNbt() || Objects.equals(stack1.getNbt(), stack2.getNbt()))
                 ||
                 fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack2, -1).stream().anyMatch((candidate) -> me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils.areStacksEquivalent(stack1, candidate, stack1.getNbt() == null));

     }


     /**
      * @author 2
      * @reason 2
      */
     @Overwrite
     public static <T extends ScreenHandler> void handleItemsFromScreen(@NotNull HandledScreen<T> screen) {
 //        if (!ignoreNextMerge) {
         if (validScreenToTrack(screen)) {
 //            System.out.println("========================1");
             MinecraftClient mc = MinecraftClient.getInstance();
             checkValidCycle(mc.world);
             MemoryDatabase database = MemoryDatabase.getCurrent();
             if (pos != null) latestPos = pos;
             if (latestPos == null) return;
             BlockState state = mc.world.getBlockState(latestPos);
             if (key == null) {
                 key = mc.world.getRegistryKey();
                 Block block = state.getBlock();
 //                System.out.println(state);
                 boolean k = true;
                 for (String string : LitematicaMixinMod.INVENTORY_LIST.getStrings()) {
                     if (Registries.BLOCK.getId(block).toString().contains(string)) {
                         k = false;
                         break;
                     }
                 }
                 if (k) return;
             }

 //                System.out.println("latestPos "+latestPos +"   "+ key);
             if (database != null && latestPos != null && key != null) {
                 List<ItemStack> stacks = condenseItems(screen.getScreenHandler().slots.stream().filter(me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils::isValidSlot).map(Slot::getStack).collect(Collectors.toList()));
                 if (state.getBlock() == Blocks.ENDER_CHEST) {
                     database.mergeItems(MemoryUtils.ENDER_CHEST_ID, red.jackf.chesttracker.memory.Memory.of(BlockPos.ORIGIN, stacks, null, null), Collections.emptyList());
                 } else {
                     Text title = getTitleFromScreen(screen, mc.world.getBlockEntity(latestPos));
                     Collection<BlockPos> connected = getConnected(mc.world, latestPos);
 //                    System.out.println(stacks);
 //                    System.out.println("Save" + key.getValue() + latestPos);
                     database.mergeItems(key.getValue(), red.jackf.chesttracker.memory.Memory.of(latestPos, stacks, title, connected.size() > 0 ? getAveragePos(latestPos, connected) : null), connected);
                 }
             }
             if (ChestTracker.CONFIG.miscOptions.printGuiClassNames)
                 ChestTracker.sendDebugMessage(Text.of(screen.getClass().getSimpleName()));
         }
 //    } else {
 //            ignoreNextMerge = false;
 //        }
         MemoryUtils.setLatestPos(null);
             key = null;
             pos = null;
     }
 }
