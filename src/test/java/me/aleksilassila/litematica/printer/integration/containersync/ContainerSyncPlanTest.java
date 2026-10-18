package me.aleksilassila.litematica.printer.integration.containersync;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ContainerSyncPlanTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        //#if MC >= 260100
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                //#if MC >= 260300
                //$$ .build(net.minecraft.data.registries.VanillaRegistries.createWorldLookup()).forEach(p -> p.apply());
                //#else
                .build(net.minecraft.data.registries.VanillaRegistries.createLookup()).forEach(p -> p.apply());
                //#endif
        //#endif
    }

    static ItemStack stone(int count) { return new ItemStack(Items.STONE, count); }
    static final ContainerSyncPlan.Rules RULES = new ContainerSyncPlan.Rules() {
        public boolean movable(int slot) { return true; }
        public int capacity(int slot, ItemStack stack) { return stack.getMaxStackSize(); }
    };

    @Test void surplusGoesToInventoryAndNeverDrops() {
        var plan = ContainerSyncPlan.create(List.of(stone(12)), List.of(stone(20), stone(60), ItemStack.EMPTY), RULES);
        assertNull(plan.problem());
        var end = plan.steps().get(plan.steps().size() - 1);
        assertEquals(12, end.items().get(0).getCount());
        assertEquals(64, end.items().get(1).getCount());
        assertEquals(4, end.items().get(2).getCount());
        assertTrue(end.cursor().isEmpty());
        assertTrue(plan.steps().stream().allMatch(step -> step.slot() >= 0));
    }

    @Test void missingMaterialsOrCapacityProducesNoPartialPlan() {
        var missing = ContainerSyncPlan.create(List.of(stone(64)), List.of(stone(60), stone(3)), RULES);
        assertEquals(ContainerSyncPlan.Problem.MISSING_ITEMS, missing.problem());
        assertTrue(missing.steps().isEmpty());
        var full = ContainerSyncPlan.create(List.of(stone(12)), List.of(stone(20), stone(64)), RULES);
        assertEquals(ContainerSyncPlan.Problem.NO_SPACE, full.problem());
        assertTrue(full.steps().isEmpty());
    }

    @Test void countsExistingTargetAndReusesMisplacedItems() {
        var plan = ContainerSyncPlan.create(List.of(stone(64), ItemStack.EMPTY), List.of(stone(60), stone(4), ItemStack.EMPTY), RULES);
        assertNull(plan.problem());
        var end = plan.steps().get(plan.steps().size() - 1);
        assertEquals(64, end.items().get(0).getCount());
        assertTrue(end.items().get(1).isEmpty());
        assertTrue(end.items().get(2).isEmpty());
    }

    @Test void protectedSlotsCannotSupplyOrReceiveItems() {
        var protectedSlots = new ContainerSyncPlan.Rules() {
            public boolean movable(int slot) { return slot == 0; }
            public int capacity(int slot, ItemStack stack) { return slot == 0 ? 64 : 0; }
        };
        assertEquals(ContainerSyncPlan.Problem.MISSING_ITEMS,
                ContainerSyncPlan.create(List.of(stone(64)), List.of(stone(60), stone(64)), protectedSlots).problem());
        assertEquals(ContainerSyncPlan.Problem.NO_SPACE,
                ContainerSyncPlan.create(List.of(stone(12)), List.of(stone(20), ItemStack.EMPTY), protectedSlots).problem());
    }

    @Test void emptyTemplateRecoversWrongItemsInsteadOfThrowingThemAway() {
        var plan = ContainerSyncPlan.create(List.of(ItemStack.EMPTY), List.of(stone(64), ItemStack.EMPTY), RULES);
        assertNull(plan.problem());
        var end = plan.steps().get(plan.steps().size() - 1);
        assertTrue(end.items().get(0).isEmpty());
        assertEquals(64, end.items().get(1).getCount());
    }

    @Test void nonStackableItemsNeedSeparateInventorySlots() {
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        var plan = ContainerSyncPlan.create(List.of(ItemStack.EMPTY), List.of(sword, sword.copy()), RULES);
        assertEquals(ContainerSyncPlan.Problem.NO_SPACE, plan.problem());
        assertTrue(plan.steps().isEmpty());
    }

    @Test void differentItemDataDoesNotSatisfyTemplate() {
        ItemStack named = stone(1);
        //#if MC >= 12006
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                //#if MC > 11802
                net.minecraft.network.chat.Component.literal("template"));
                //#else
                //$$ new net.minecraft.network.chat.TextComponent("template"));
                //#endif
        //#else
        //$$ named.setHoverName(
        //#if MC > 11802
        //$$         net.minecraft.network.chat.Component.literal("template"));
        //#else
        //$$         new net.minecraft.network.chat.TextComponent("template"));
        //#endif
        //#endif
        var plan = ContainerSyncPlan.create(List.of(named), List.of(ItemStack.EMPTY, stone(64)), RULES);
        assertEquals(ContainerSyncPlan.Problem.MISSING_ITEMS, plan.problem());
    }

    @Test void alreadyMatchingTargetNeedsNoExtraCopyInPlayerInventory() {
        var plan = ContainerSyncPlan.create(List.of(stone(64)), List.of(stone(64), ItemStack.EMPTY), RULES);
        assertNull(plan.problem());
        assertTrue(plan.steps().isEmpty());
    }
}
