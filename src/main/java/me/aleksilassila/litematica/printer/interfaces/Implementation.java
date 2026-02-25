package me.aleksilassila.litematica.printer.interfaces;

import net.minecraft.world.level.block.*;

public class Implementation {

    /**
     * 可以交互的方块类
     */
    public static Class<?>[] interactiveBlocks = {
            AbstractFurnaceBlock.class,     // 熔炉/烟熏炉/高炉
            CraftingTableBlock.class,       // 工作台
            LeverBlock.class,               // 拉杆
            DoorBlock.class,                // 门
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
            ShulkerBoxBlock.class,          // 潜影盒
            LecternBlock.class,             // 讲台
            FlowerPotBlock.class,           // 花盆
            BarrelBlock.class,              // 木桶
            BellBlock.class,                // 钟
            SmithingTableBlock.class,       // 锻造台
            LoomBlock.class,                // 织布机
            CartographyTableBlock.class,    // 制图台
            GrindstoneBlock.class,          // 砂轮
            StonecutterBlock.class,         // 切石机
            //#if MC < 12109
            //$$ FletchingTableBlock.class, // 制箭台
            //#endif
            SmokerBlock.class,              // 烟熏炉
            BlastFurnaceBlock.class,        // 高炉
            //#if MC >= 12003
            CrafterBlock.class              // 合成器（自动合成台）
            //#endif
    };

    /**
     * 检查方块是否可以交互
     *
     * @param block 你传入的方块类
     * @return 是否可以交互
     */
    public static boolean isInteractive(Block block) {
        for (Class<?> clazz : interactiveBlocks) {
            if (clazz.isInstance(block)) {
                return true;
            }
        }
        return false;
    }
}