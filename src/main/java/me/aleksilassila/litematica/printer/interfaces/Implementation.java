package me.aleksilassila.litematica.printer.interfaces;

import me.aleksilassila.litematica.printer.mixin.PlayerMoveC2SPacketAccessor;
import net.minecraft.block.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
//import net.minecraft.network.Packet;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Direction;

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

    public static PlayerAbilities getAbilities(ClientPlayerEntity playerEntity) {
        return playerEntity.getAbilities();
    }

    public static void sendLookPacket(ClientPlayerEntity playerEntity, Direction playerShouldBeFacingYaw, Direction playerShouldBeFacingPitch) {
        playerEntity.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                Implementation.getRequiredYaw(playerShouldBeFacingYaw),
                Implementation.getRequiredPitch(playerShouldBeFacingPitch),
                playerEntity.isOnGround()
                //#if MC > 12101
                ,playerEntity.horizontalCollision
                //#endif
        ));
    }

    public static boolean isLookOnlyPacket(Packet<?> packet) {
        return packet instanceof PlayerMoveC2SPacket.LookAndOnGround;
    }

    public static boolean isLookAndMovePacket(Packet<?> packet) {
        return packet instanceof PlayerMoveC2SPacket.Full;
    }

    public static Packet<?> getFixedLookPacket(ClientPlayerEntity playerEntity, Packet<?> packet, Direction directionYaw, Direction directionPitch) {
        if (directionYaw == null || directionPitch == null) return packet;

        float yaw = Implementation.getRequiredYaw(directionYaw);
        float pitch = Implementation.getRequiredPitch(directionPitch);

        double x = ((PlayerMoveC2SPacketAccessor) packet).getX();
        double y = ((PlayerMoveC2SPacketAccessor) packet).getY();
        double z = ((PlayerMoveC2SPacketAccessor) packet).getZ();
        boolean onGround = ((PlayerMoveC2SPacketAccessor) packet).getOnGround();
        return new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround
                //#if MC > 12101
                ,playerEntity.horizontalCollision
                //#endif
        );
    }

    public static float getRequiredYaw(Direction playerShouldBeFacing) {
        if (playerShouldBeFacing.getAxis().isHorizontal()) {
            return playerShouldBeFacing.getPositiveHorizontalDegrees();
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
            TrapdoorBlock.class, // 活板门
            BedBlock.class, // 床
            RedstoneWireBlock.class, // 红石线
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
