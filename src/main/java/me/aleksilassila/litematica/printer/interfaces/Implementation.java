package me.aleksilassila.litematica.printer.interfaces;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;

/**
 * 【版本兼容适配类】
 * 这个类是为了兼容不同Minecraft版本的"脏类"（Dirty Class），核心作用是：
 * 1. 集中处理不同MC版本中变量/方法/类结构不一致的问题；
 * 2. 封装所有版本相关的适配逻辑，使得打印机模组的其他代码无需感知版本差异；
 * 3. 这是每个打印机分支中唯一需要根据MC版本修改的文件。
 *
 * 注："脏类"是行业术语，指为了兼容而集中存放版本适配代码的类，结构可能不优雅但能减少版本适配的工作量。
 */
public class Implementation {
    /**
     * 所有材质的锄头物品数组（打印机判断是否手持锄头工具时使用）
     * 包含：钻石、铁、金、下界合金、石、木质锄头
     */
    public static final Item[] HOES = {Items.DIAMOND_HOE, Items.IRON_HOE, Items.GOLDEN_HOE,
            Items.NETHERITE_HOE, Items.STONE_HOE, Items.WOODEN_HOE};

    /**
     * 所有材质的铲子物品数组（打印机判断是否手持铲子工具时使用）
     * 包含：钻石、铁、金、下界合金、石、木质铲子
     */
    public static final Item[] SHOVELS = {Items.DIAMOND_SHOVEL, Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL,
            Items.NETHERITE_SHOVEL, Items.STONE_SHOVEL, Items.WOODEN_SHOVEL};

    /**
     * 所有材质的斧头物品数组（打印机判断是否手持斧头工具时使用）
     * 包含：钻石、铁、金、下界合金、石、木质斧头
     */
    public static final Item[] AXES = {Items.DIAMOND_AXE, Items.IRON_AXE, Items.GOLDEN_AXE,
            Items.NETHERITE_AXE, Items.STONE_AXE, Items.WOODEN_AXE};

    /**
     * 获取本地玩家的能力属性对象
     * @param playerEntity 本地玩家实例（客户端玩家）
     * @return 玩家的能力属性（包含飞行、创造模式、无敌等状态）
     */
    public static Abilities getAbilities(LocalPlayer playerEntity) {
        return playerEntity.getAbilities();
    }

    /**
     * 判断方块是否为「可交互方块」（打印机需要特殊处理这类方块的放置/交互逻辑）
     * @param block 待判断的方块实例
     * @return true=可交互方块，false=普通方块
     */
    public static boolean isInteractive(Block block) {
        // 遍历所有可交互方块的类，判断当前方块是否属于其中一种
        for (Class<?> clazz : interactiveBlocks) {
            if (clazz.isInstance(block)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 可交互方块的类数组（打印机需要特殊处理的方块类型）
     * 注：通过类判断而非方块实例，避免版本更新导致的方块ID/实例变化问题
     */
    public static Class<?>[] interactiveBlocks = {
            AbstractFurnaceBlock.class,     // 熔炉（所有熔炉的基类：普通熔炉/烟熏炉/高炉）
            CraftingTableBlock.class,       // 工作台
            LeverBlock.class,               // 拉杆
            DoorBlock.class,                // 门（所有门的基类：木门/铁门等）
            TrapDoorBlock.class,            // 活板门
            BedBlock.class,                 // 床
            RedStoneWireBlock.class,        // 红石线
            ScaffoldingBlock.class,         // 脚手架
            HopperBlock.class,              // 漏斗
            EnchantingTableBlock.class,     // 附魔台
            NoteBlock.class,                // 音符盒
            JukeboxBlock.class,             // 唱片机
            CakeBlock.class,                // 蛋糕
            FenceGateBlock.class,           // 栅栏门
            BrewingStandBlock.class,        // 酿造台
            DragonEggBlock.class,           // 龙蛋
            CommandBlock.class,             // 命令方块
            BeaconBlock.class,              // 信标
            AnvilBlock.class,               // 铁砧
            ComparatorBlock.class,          // 红石比较器
            RepeaterBlock.class,            // 红石中继器
            DropperBlock.class,             // 投掷器
            DispenserBlock.class,           // 发射器
            ShulkerBoxBlock.class,          // 潜影盒（所有颜色的潜影盒）
            LecternBlock.class,             // 讲台
            FlowerPotBlock.class,           // 花盆
            BarrelBlock.class,              // 木桶
            BellBlock.class,                // 钟
            SmithingTableBlock.class,       // 锻造台
            LoomBlock.class,                // 织布机
            CartographyTableBlock.class,    // 制图台
            GrindstoneBlock.class,          // 砂轮
            StonecutterBlock.class,         // 切石机
            // 条件编译：MC版本 < 1.21.9（12109）时包含制箭台（1.21.9+可能移除/重构）
            //#if MC < 12109
            //$$ FletchingTableBlock.class, // 制箭台
            //#endif
            SmokerBlock.class,              // 烟熏炉
            BlastFurnaceBlock.class,        // 高炉
            // 条件编译：MC版本 >= 1.20.3（12003）时包含合成器（1.20.3新增的Crafter方块）
            //#if MC >= 12003
            CrafterBlock.class              // 合成器（自动合成台）
            //#endif
    };
}