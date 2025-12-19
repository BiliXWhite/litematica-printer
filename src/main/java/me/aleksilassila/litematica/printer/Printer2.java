package me.aleksilassila.litematica.printer;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.actions.Action;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.guides.Guide;
import me.aleksilassila.litematica.printer.guides.Guides;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.overwrite.MyBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static me.aleksilassila.litematica.printer.InitHandler.*;
import static me.aleksilassila.litematica.printer.InitHandler.ITERATOR_USE_TIME;

public class Printer2 {
    @NotNull
    private final Minecraft client;
    @NotNull
    public final LocalPlayer player;
    public final ActionHandler actionHandler;
    private final Guides interactionGuides = new Guides();

    private long gameTickCounter, packetTickCounter;
    public Map<BlockPos, Integer> placeCooldownList = new HashMap<>();

    public Printer2(@NotNull Minecraft client, @NotNull LocalPlayer player) {
        this.client = client;
        this.player = player;
        this.actionHandler = new ActionHandler(client, player);
    }

    public void onGameTick() {
        gameTickCounter++;
        if (LAG_CHECK.getBooleanValue()) {  // 延迟检测
            if (packetTickCounter > 20) {
                return;
            }
            packetTickCounter++;
        }
        boolean didFindPlacement = true;
        for (int i = 0; i < 10; i++) {
            if (didFindPlacement) {
                didFindPlacement = printerTick();
            }
            actionHandler.onGameTick(); // 处理动作
        }

    }

    public boolean printerTick() {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        if (worldSchematic == null) {
            return false;
        }
        if (!actionHandler.acceptsActions()) {
            return false;
        }
        if (!InitHandler.PRINT_SWITCH.getBooleanValue() && !InitHandler.PRINT.getKeybind().isPressed()) {   // 开关检查
            return false;
        }
        Abilities abilities = player.getAbilities();
        if (!abilities.mayBuild) {
            return false;
        }
        findBlock:
        for (BlockPos position : getReachablePositions()) {
            SchematicBlockState state = new SchematicBlockState(player.level(), worldSchematic, position);
            if (state.targetState.equals(state.currentState) || state.targetState.isAir()) {
                continue;
            }
            Guide[] guides = interactionGuides.getInteractionGuides(state);
            for (Guide guide : guides) {
                // Add INTERACT_BLOCKS pull by DarkReaper231
                if (guide.canExecute(player)) {
                    Debug.write("Executing {} for {}", guide, state);
                    List<Action> actions = guide.execute(player);
                    actionHandler.addActions(actions.toArray(Action[]::new));
                    return true;
                }
                if (guide.skipOtherGuides()) {
                    continue findBlock;
                }
            }
        }
        return false;
    }

    private List<BlockPos> getReachablePositions() {
        int maxReach = (int) Math.ceil(PlaceUtils.getPlayerBlockInteractionRange());
        double maxReachSquared = Mth.square(InitHandler.PRINTER_RANGE.getIntegerValue());
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int y = -maxReach; y < maxReach + 1; y++) {
            for (int x = -maxReach; x < maxReach + 1; x++) {
                for (int z = -maxReach; z < maxReach + 1; z++) {
                    BlockPos blockPos = player.blockPosition().north(x).west(z).above(y);
                    if (!DataManager.getRenderLayerRange().isPositionWithinRange(blockPos)) {
                        continue;
                    }
                    if (this.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(blockPos)) > maxReachSquared) {
                        continue;
                    }
                    positions.add(blockPos);
                }
            }
        }
        return positions.stream()
                .filter(p ->
                {
                    Vec3 vec = Vec3.atCenterOf(p);
                    return this.player.position().distanceToSqr(vec) > 1
                            && this.player.getEyePosition().distanceToSqr(vec) > 1;
                })
                .sorted((a, b) ->
                {
                    double aDistance = this.player.position().distanceToSqr(Vec3.atCenterOf(a));
                    double bDistance = this.player.position().distanceToSqr(Vec3.atCenterOf(b));
                    return Double.compare(aDistance, bDistance);
                }).toList();
    }

    public void resetPacketTickCounter() {
        this.packetTickCounter = 0;
    }
}
