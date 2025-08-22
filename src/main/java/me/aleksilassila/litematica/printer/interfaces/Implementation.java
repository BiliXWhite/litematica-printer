package me.aleksilassila.litematica.printer.interfaces;

import me.aleksilassila.litematica.printer.mixin.PlayerMoveC2SPacketAccessor;
import me.aleksilassila.litematica.printer.printer.PlacementGuide;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.block.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
//import net.minecraft.network.Packet;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import static me.aleksilassila.litematica.printer.printer.Printer.itemPos;

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

    public static PlayerInventory getInventory(ClientPlayerEntity playerEntity) {
        return playerEntity.getInventory();
    }

    public static PlayerAbilities getAbilities(ClientPlayerEntity playerEntity) {
        return playerEntity.getAbilities();
    }

    public static float getYaw(ClientPlayerEntity playerEntity) {
        return playerEntity.getYaw();
    }

    public static float getPitch(ClientPlayerEntity playerEntity) {
        return playerEntity.getPitch();
    }

    public static float[] getRequiredAngles(ClientPlayerEntity player, Direction direction1, Direction direction2) {
        float[] angles = new float[]{player.getYaw(), player.getPitch()};
        if(direction1 == null) return angles;
        if(direction2 == null) {
            angles[0] = Implementation.getRequiredYaw(player, direction1);
            angles[1] = Implementation.getRequiredPitch(player, direction1);
            return angles;
        }
        Direction yaw;
        Direction pitch;
        if (direction2 == Direction.UP || direction2 == Direction.DOWN) {
            yaw = direction1;
            pitch = direction2;
        }else {
            yaw = direction2;
            pitch = direction1;
        }

        angles[0] = Implementation.getRequiredYaw(player, yaw);
        angles[1] = Implementation.getRequiredPitch(player, pitch);
        return angles;
    }
    public static void sendLookPacket(ClientPlayerEntity playerEntity, Direction direction1, Direction direction2) {
        if(direction1 == null) return;
        if(direction2 == null) {
            sendLookPacket(playerEntity, direction1);
            return;
        }
        float[] requiredAngles = getRequiredAngles(playerEntity, direction1, direction2);
        sendLookPacket(playerEntity, requiredAngles[0], requiredAngles[1]);
    }
    public static void sendLookPacket(ClientPlayerEntity playerEntity, Direction playerShouldBeFacing) {
        float requiredYaw = Implementation.getRequiredYaw(playerEntity, playerShouldBeFacing);
        float requiredPitch = Implementation.getRequiredPitch(playerEntity, playerShouldBeFacing);
        sendLookPacket(playerEntity, requiredYaw ,requiredPitch);
    }

    public static void sendLookPacket(ClientPlayerEntity playerEntity, float yaw, float pitch){
        playerEntity.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                yaw,
                pitch,
                playerEntity.isOnGround()
                //#if MC > 12101
                //$$ ,playerEntity.horizontalCollision
                //#endif
        ));
    }

    public static boolean isLookOnlyPacket(Packet<?> packet) {
        return packet instanceof PlayerMoveC2SPacket.LookAndOnGround;
    }

    public static boolean isLookAndMovePacket(Packet<?> packet) {
        return packet instanceof PlayerMoveC2SPacket.Full;
    }

    public static Packet<?> getFixedLookPacket(ClientPlayerEntity playerEntity, Packet<?> packet, PlacementGuide.Action action) {
        if (action.lookDirection == null) return packet;

        float[] angles = getRequiredAngles(playerEntity, action.lookDirection, action.lookDirection2);

        double x = ((PlayerMoveC2SPacketAccessor) packet).getX();
        double y = ((PlayerMoveC2SPacketAccessor) packet).getY();
        double z = ((PlayerMoveC2SPacketAccessor) packet).getZ();
        boolean onGround = ((PlayerMoveC2SPacketAccessor) packet).getOnGround();
        return new PlayerMoveC2SPacket.Full(x, y, z, angles[0], angles[1], onGround
                //#if MC > 12101
                //$$ ,playerEntity.horizontalCollision
                //#endif
        );
    }

    protected static float getRequiredYaw(ClientPlayerEntity playerEntity, Direction playerShouldBeFacing) {
        if (playerShouldBeFacing.getAxis().isHorizontal()) {
            return playerShouldBeFacing.asRotation();
        } else {
            return Implementation.getYaw(playerEntity);
        }
    }

    protected static float getRequiredPitch(ClientPlayerEntity playerEntity, Direction playerShouldBeFacing) {
        if (playerShouldBeFacing.getAxis().isVertical()) {
            return playerShouldBeFacing == Direction.DOWN ? 90 : -90;
        } else {
            float pitch = Implementation.getPitch(playerEntity);
            return Math.abs(pitch) < 40 ? pitch : pitch / Math.abs(pitch) * 40;
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

    public enum NewBlocks {
//        LICHEN(AbstractLichenBlock.class),
        ROD(RodBlock.class),
        CANDLES(CandleBlock.class),
        AMETHYST(AmethystClusterBlock.class);

        public final Class<?> clazz;

        NewBlocks(Class<?> clazz) {
            this.clazz = clazz;
        }
    }

    public static Class<?>[] interactiveBlocks = {
            ChestBlock.class, AbstractFurnaceBlock.class, CraftingTableBlock.class,
            LeverBlock.class, DoorBlock.class, TrapdoorBlock.class,
            BedBlock.class, RedstoneWireBlock.class, ScaffoldingBlock.class, HopperBlock.class,
            EnchantingTableBlock.class, NoteBlock.class, JukeboxBlock.class, CakeBlock.class,
            FenceGateBlock.class, BrewingStandBlock.class, DragonEggBlock.class, CommandBlock.class,
            BeaconBlock.class, AnvilBlock.class, ComparatorBlock.class, RepeaterBlock.class,
            DropperBlock.class, DispenserBlock.class, ShulkerBoxBlock.class, LecternBlock.class,
            FlowerPotBlock.class, BarrelBlock.class, BellBlock.class, SmithingTableBlock.class,
            LoomBlock.class, CartographyTableBlock.class, GrindstoneBlock.class,
            StonecutterBlock.class,
            //#if MC > 12002
            CrafterBlock.class
            //#endif

    };


}
