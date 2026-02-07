package me.aleksilassila.litematica.printer.handler.handlers;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import lombok.Getter;
import lombok.Setter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.printer.*;
import me.aleksilassila.litematica.printer.utils.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class PrintHandler extends ClientPlayerTickHandler {
    public final static String NAME = "print";

    private final PlacementGuide guide;
    @Getter
    @Setter
    private boolean pistonNeedFix;
    @Getter
    @Setter
    private boolean printerMemorySync;

    private Action action;
    private SchematicBlockContext schematicBlockContext;
    private WorldSchematic schematic;

    public PrintHandler() {
        super(NAME, PrintModeType.PRINTER, Configs.Core.PRINT, Configs.Print.PRINT_SELECTION_TYPE, true);
        this.guide = new PlacementGuide(client);
    }

    @Override
    protected int getTickInterval() {
        return Configs.Placement.PLACE_INTERVAL.getIntegerValue();
    }

    @Override
    protected int getMaxEffectiveExecutionsPerTick() {
        return Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue();
    }

    @Override
    public boolean canIterationBlockPos(BlockPos blockPos) {
        if (!LitematicaUtils.isSchematicBlock(blockPos)) {
            return false;
        }
        if (isBlockPosOnCooldown(blockPos)) {
            return false;
        }
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return false;
        this.schematic=schematic;
        this.schematicBlockContext = new SchematicBlockContext(client, level, schematic, blockPos);
        if (Configs.Print.PRINT_SKIP.getBooleanValue()) {
            Set<String> skipSet = new HashSet<>(Configs.Print.PRINT_SKIP_LIST.getStrings()); // 转换为 HashSet
            if (skipSet.stream().anyMatch(s -> FilterUtils.matchName(s, schematicBlockContext.requiredState))) {
                return false;
            }
        }
        Action action = guide.getAction(schematicBlockContext);
        if (action == null) return false;
        this.action = action;
        return true;
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        if (Configs.Print.FALLING_CHECK.getBooleanValue() && schematicBlockContext.requiredState.getBlock() instanceof FallingBlock) {
            BlockPos downPos = blockPos.below();
            if (level.getBlockState(downPos) != schematic.getBlockState(downPos)) {
                MessageUtils.setOverlayMessage(Component.nullToEmpty("方块 " + schematicBlockContext.requiredState.getBlock().getName().getString() + " 下方方块不相符，跳过放置"), false);
                return;
            }
        }
        Direction side = action.getValidSide(level, blockPos);
        if (side == null) return;
        Item[] reqItems = action.getRequiredItems(schematicBlockContext.requiredState.getBlock());
        if (!InventoryUtils.switchToItems(player, reqItems)) return;
        boolean useShift = (Implementation.isInteractive(level.getBlockState(blockPos.relative(side)).getBlock()) && !(action instanceof PlacementGuide.ClickAction))
                || Configs.Print.PRINT_FORCED_SNEAK.getBooleanValue()
                || action.isShift();

        action.queueAction(blockPos, side, useShift, player);
        Vec3 hitModifier = LitematicaUtils.usePrecisionPlacement(blockPos, schematicBlockContext.requiredState);
        if (hitModifier != null) {
            ActionManager.INSTANCE.hitModifier = hitModifier;
            ActionManager.INSTANCE.useProtocol = true;
        }
        if (action.getPlayerLook() != null) {
            ActionManager.INSTANCE.sendLook(player, action.getPlayerLook());
        }
        Block block = schematicBlockContext.requiredState.getBlock();
        if (block instanceof PistonBaseBlock) {
            pistonNeedFix = true;
        }
        ActionManager.INSTANCE.sendQueue(player);
        setBlockPosCooldown(blockPos, ConfigUtils.getPlaceCooldown());
        if (ActionManager.INSTANCE.needWait) {
            skipIteration.set(true);
        }
    }
}

