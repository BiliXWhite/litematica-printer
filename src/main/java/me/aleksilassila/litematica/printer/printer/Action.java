package me.aleksilassila.litematica.printer.printer;

import lombok.Getter;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.utils.BlockUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnusedReturnValue")
public class Action {
    protected Map<Direction, Vec3> sides;
    @Nullable
    @Getter
    public Look look = null;
    @Nullable
    protected Item[] clickItems; // null == 空手
    protected boolean requiresSupport = false;
    protected boolean useShift = false;
    @Getter
    protected int waitTick = 0;     // 会占用其他任务

    public Action() {
        this.sides = new HashMap<>();
        for (Direction direction : Direction.values()) {
            sides.put(direction, new Vec3(0, 0, 0));
        }
    }

    public Action setLookYawPitch(float lookYaw, float lookPitch) {
        this.look = new Look(lookYaw, lookPitch);
        return this;
    }

    public Action setLookRotation(int lookRotation) {
        this.look = new Look(lookRotation);
        return this;
    }

    public Action setLookDirection(Direction lookDirection) {
        this.look = new Look(lookDirection);
        return this;
    }

    public Action setLookDirection(Direction lookDirectionYaw, Direction lookDirectionPitch) {
        this.look = new Look(lookDirectionYaw, lookDirectionPitch);
        return this;
    }

    public @Nullable Item[] getRequiredItems(Block backup) {
        return clickItems == null ? new Item[]{backup.asItem()} : clickItems;
    }

    public @NotNull Map<Direction, Vec3> getSides() {
        if (this.sides == null) {
            this.sides = new HashMap<>();
            for (Direction d : Direction.values()) {
                this.sides.put(d, new Vec3(0, 0, 0));
            }
        }
        return this.sides;
    }

    public Action setSides(Direction.Axis... axis) {
        Map<Direction, Vec3> sides = new HashMap<>();
        for (Direction.Axis a : axis) {
            for (Direction d : Direction.values()) {
                if (d.getAxis() == a) {
                    sides.put(d, new Vec3(0, 0, 0));
                }
            }
        }
        this.sides = sides;
        return this;
    }

    public Action setSides(Map<Direction, Vec3> sides) {
        this.sides = sides;
        return this;
    }

    public Action setSides(Direction... directions) {
        Map<Direction, Vec3> sides = new HashMap<>();
        for (Direction d : directions) {
            sides.put(d, new Vec3(0, 0, 0));
        }
        this.sides = sides;
        return this;
    }

    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    public @Nullable Direction getValidSide(ClientLevel world, BlockPos pos) {
        Map<Direction, Vec3> sides = getSides();
        List<Direction> validSides = new ArrayList<>();
        for (Direction side : sides.keySet()) {
            BlockPos neighborPos = pos.relative(side);
            BlockState neighborState = world.getBlockState(neighborPos);
            if (Configs.Placement.PLACE_IN_AIR.getBooleanValue() && !this.requiresSupport
                // TODO: 没理解, 都凭空放置了, 还检查相邻方块类型？所以注释掉了
                // && !Implementation.isInteractive(neighborState.getBlock())
            ) {
                return side;
            }
            if (PrinterUtils.canBeClicked(world, neighborPos) && !BlockUtils.isReplaceable(neighborState)) {
                validSides.add(side);
            }
        }
        if (validSides.isEmpty()) {
            return null;
        }
        // 选择一个不需要潜行放置的面
        for (Direction validSide : validSides) {
            BlockState requiredState = world.getBlockState(pos);
            BlockState sideBlockState = world.getBlockState(pos.relative(validSide));
            if (!Implementation.isInteractive(sideBlockState.getBlock()) && requiredState.canSurvive(world, pos)) {
                return validSide;
            }
        }
        return validSides.get(0);
    }

    public Action setItem(Item item) {
        return this.setItems(item);
    }

    public Action setItems(Item... items) {
        this.clickItems = items;
        return this;
    }

    public Action setRequiresSupport(boolean requiresSupport) {
        this.requiresSupport = requiresSupport;
        return this;
    }

    public Action setRequiresSupport() {
        return this.setRequiresSupport(true);
    }

    public Action setUseShift(boolean useShift) {
        this.useShift = useShift;
        return this;
    }

    public Action setUseShift() {
        return this.setUseShift(true);
    }

    public Action setWaitTick(int waitTick) {
        this.waitTick = waitTick;
        return this;
    }

    public void queueAction(ActionManager actionManager, BlockPos blockPos, Direction side, boolean useShift) {
        if (Configs.Placement.PLACE_IN_AIR.getBooleanValue() && !this.requiresSupport) {
            actionManager.queueClick(
                    blockPos,
                    side.getOpposite(),
                    getSides().get(side),
                    useShift
            );
        } else {
            actionManager.queueClick(
                    blockPos.relative(side),
                    side.getOpposite(),
                    getSides().get(side),
                    useShift
            );
        }
    }
}
