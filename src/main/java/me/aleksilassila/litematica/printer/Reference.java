package me.aleksilassila.litematica.printer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.ComposterBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * 模组核心常量引用类
 * 集中管理模组的全局固定值，避免硬编码和拼写错误
 */
public class Reference {
    public final static Minecraft MINECRAFT = Minecraft.getInstance();
    public final static String MOD_ID = "litematica_printer";
    public final static String MOD_NAME = "Litematica Printer";
    public final static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public final static Item[] COMPOSTABLE_ITEMS = Arrays.stream(ComposterBlock.COMPOSTABLES.keySet().toArray(ItemLike[]::new)).map(ItemLike::asItem).toArray(Item[]::new);
    public static final Item[] HOE_ITEMS = {Items.DIAMOND_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.NETHERITE_HOE, Items.STONE_HOE, Items.WOODEN_HOE};
    public static final Item[] SHOVEL_ITEMS = {Items.DIAMOND_SHOVEL, Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL, Items.NETHERITE_SHOVEL, Items.STONE_SHOVEL, Items.WOODEN_SHOVEL};
    public static final Item[] AXE_ITEMS = {Items.DIAMOND_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.NETHERITE_AXE, Items.STONE_AXE, Items.WOODEN_AXE};
}
