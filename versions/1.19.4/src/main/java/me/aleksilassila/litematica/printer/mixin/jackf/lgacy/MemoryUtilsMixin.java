package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;

import me.aleksilassila.litematica.printer.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
    private static <T extends AbstractContainerMenu> boolean validScreenToTrack(AbstractContainerScreen<T> screen) {
        return false;
    }

    @Shadow private static @Nullable BlockPos latestPos;

    @Shadow(remap = false)
    public static List<ItemStack> condenseItems(List<ItemStack> list) {
        return null;
    }

    @Shadow
    @Nullable
    private static Component getTitleFromScreen(AbstractContainerScreen<?> screen, @Nullable BlockEntity blockEntity) {
        return null;
    }

    @Shadow
    public static Collection<BlockPos> getConnected(@NotNull Level world, BlockPos pos) {
        return null;
    }

    @Shadow
    private static Vec3 getAveragePos(BlockPos basePos, Collection<BlockPos> connected) {
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
        return stack1.getItem() == stack2.getItem() && (ignoreNbt || !stack1.hasTag() && !stack2.hasTag() || Objects.equals(stack1.getTag(), stack2.getTag()))
                ||
                fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack2, -1).stream().anyMatch((candidate) -> me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils.areStacksEquivalent(stack1, candidate, stack1.getTag() == null));

    }


    /**
     * @author 2
     * @reason 2
     */
    @Overwrite
    public static <T extends AbstractContainerMenu> void handleItemsFromScreen(@NotNull AbstractContainerScreen<T> screen) {
        //        if (!ignoreNextMerge) {
        if (validScreenToTrack(screen)) {
            //            System.out.println("========================1");
            Minecraft mc = Minecraft.getInstance();
            checkValidCycle(mc.level);
            MemoryDatabase database = MemoryDatabase.getCurrent();
            if (pos != null) latestPos = pos;
            if (latestPos == null) return;
            BlockState state = mc.level.getBlockState(latestPos);
            if (key == null) {
                key = mc.level.dimension();
                Block block = state.getBlock();
                //                System.out.println(state);
                boolean k = true;
                for (String string : Configs.INVENTORY_LIST.getStrings()) {
                    if (BuiltInRegistries.BLOCK.getKey(block).toString().contains(string)) {
                        k = false;
                        break;
                    }
                }
                if (k) return;
            }

            //                System.out.println("latestPos "+latestPos +"   "+ key);
            if (database != null && latestPos != null && key != null) {
                List<ItemStack> stacks = condenseItems(screen.getMenu().slots.stream().filter(me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils::isValidSlot).map(Slot::getItem).collect(Collectors.toList()));
                if (state.getBlock() == Blocks.ENDER_CHEST) {
                    database.mergeItems(MemoryUtils.ENDER_CHEST_ID, red.jackf.chesttracker.memory.Memory.of(BlockPos.ZERO, stacks, null, null), Collections.emptyList());
                } else {
                    Component title = getTitleFromScreen(screen, mc.level.getBlockEntity(latestPos));
                    Collection<BlockPos> connected = getConnected(mc.level, latestPos);
                    //                    System.out.println(stacks);
                    //                    System.out.println("Save" + key.getValue() + latestPos);
                    database.mergeItems(key.location(), red.jackf.chesttracker.memory.Memory.of(latestPos, stacks, title, !connected.isEmpty() ? getAveragePos(latestPos, connected) : null), connected);
                }
            }
            if (ChestTracker.CONFIG.miscOptions.printGuiClassNames)
                ChestTracker.sendDebugMessage(Component.nullToEmpty(screen.getClass().getSimpleName()));
        }
        //    } else {
        //            ignoreNextMerge = false;
        //        }
        MemoryUtils.setLatestPos(null);
        key = null;
        pos = null;
    }
}
