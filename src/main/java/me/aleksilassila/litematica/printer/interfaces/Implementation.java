package me.aleksilassila.litematica.printer.interfaces;

import me.aleksilassila.litematica.printer.mixin.ServerboundMovePlayerPacketAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;

/**
 * Dirty class that contains anything and everything that is
 * required to access variables and functions that are inconsistent
 * across different minecraft versions. In other words, this should
 * be the only file that has to be changed in every printer branch.
 */
public class Implementation {
    public static final Item[] HOES = {Items.DIAMOND_HOE, Items.IRON_HOE, Items.GOLDEN_HOE,
            Items.NETHERITE_HOE, Items.STONE_HOE, Items.WOODEN_HOE};

    public static final Item[] SHOVELS = {Items.DIAMOND_SHOVEL, Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL,
            Items.NETHERITE_SHOVEL, Items.STONE_SHOVEL, Items.WOODEN_SHOVEL};

    public static final Item[] AXES = {Items.DIAMOND_AXE, Items.IRON_AXE, Items.GOLDEN_AXE,
            Items.NETHERITE_AXE, Items.STONE_AXE, Items.WOODEN_AXE};

    public static Abilities getAbilities(LocalPlayer playerEntity) {
        return playerEntity.getAbilities();
    }

    public static void sendLookPacket(LocalPlayer playerEntity, Direction playerShouldBeFacingYaw, Direction playerShouldBeFacingPitch) {
        playerEntity.connection.send(new ServerboundMovePlayerPacket.Rot(
                Implementation.getRequiredYaw(playerShouldBeFacingYaw),
                Implementation.getRequiredPitch(playerShouldBeFacingPitch),
                playerEntity.onGround()
                //#if MC > 12101
                ,playerEntity.horizontalCollision
                //#endif
        ));
    }

    public static boolean isLookOnlyPacket(Packet<?> packet) {
        return packet instanceof ServerboundMovePlayerPacket.Rot;
    }

    public static boolean isLookAndMovePacket(Packet<?> packet) {
        return packet instanceof ServerboundMovePlayerPacket.PosRot;
    }

    public static Packet<?> getFixedLookPacket(LocalPlayer playerEntity, Packet<?> packet, Direction directionYaw, Direction directionPitch) {
        if (directionYaw == null || directionPitch == null) return packet;

        float yaw = Implementation.getRequiredYaw(directionYaw);
        float pitch = Implementation.getRequiredPitch(directionPitch);

        double x = ((ServerboundMovePlayerPacketAccessor) packet).getX();
        double y = ((ServerboundMovePlayerPacketAccessor) packet).getY();
        double z = ((ServerboundMovePlayerPacketAccessor) packet).getZ();
        boolean onGround = ((ServerboundMovePlayerPacketAccessor) packet).getOnGround();
        return new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, onGround
                //#if MC > 12101
                ,playerEntity.horizontalCollision
                //#endif
        );
    }

    public static float getRequiredYaw(Direction playerShouldBeFacing) {
        if (playerShouldBeFacing.getAxis().isHorizontal()) {
            return playerShouldBeFacing.toYRot();
        } else {
            return 0;
        }
    }

    public static float getRequiredPitch(Direction playerShouldBeFacing) {
        if (playerShouldBeFacing.getAxis().isVertical()) {
            return playerShouldBeFacing == Direction.DOWN ? 90 : -90;
        } else {
            return 0;
        }
    }

    public static boolean isInteractive(Block block) {
        for (Class<?> clazz : interactiveBlocks) {
            if (clazz.isInstance(block)) {
                return true;
            }
        }

        return false;
    }

    public static Class<?>[] interactiveBlocks = {
            AbstractFurnaceBlock.class, // 炉子
            CraftingTableBlock.class, // 工作台
            LeverBlock.class, // 拉杆
            DoorBlock.class, // 门
            TrapDoorBlock.class, // 活板门
            BedBlock.class, // 床
            RedStoneWireBlock.class, // 红石线
            ScaffoldingBlock.class, // 脚手架
            HopperBlock.class, // 漏斗
            EnchantingTableBlock.class, // 附魔台
            NoteBlock.class, // 音符盒
            JukeboxBlock.class, // 唱片机
            CakeBlock.class, // 蛋糕
            FenceGateBlock.class, // 栅栏门
            BrewingStandBlock.class, // 酿造台
            DragonEggBlock.class, // 龙蛋
            CommandBlock.class, // 命令方块
            BeaconBlock.class, // 信标
            AnvilBlock.class, // 铁砧
            ComparatorBlock.class, // 比较器
            RepeaterBlock.class, // 中继器
            DropperBlock.class, // 投掷器
            DispenserBlock.class, // 发射器
            ShulkerBoxBlock.class, // 潜影盒
            LecternBlock.class, // 讲台
            FlowerPotBlock.class, // 花盆
            BarrelBlock.class, // 桶
            BellBlock.class, // 钟
            SmithingTableBlock.class, // 锻造台
            LoomBlock.class, // 织布机
            CartographyTableBlock.class, // 制图台
            GrindstoneBlock.class, // 砂轮
            StonecutterBlock.class, // 切石机
            //#if MC < 12109
            //$$ FletchingTableBlock.class, // 制箭台
            //#endif
            SmokerBlock.class, // 烟熏炉
            BlastFurnaceBlock.class, // 高炉
            //#if MC >= 12003
            CrafterBlock.class //合成器
            //#endif

    };


}
