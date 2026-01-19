package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EasyPlaceProtocol;
import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.Debug;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.IterationOrderType;
import me.aleksilassila.litematica.printer.config.enums.ModeType;
import me.aleksilassila.litematica.printer.config.enums.PrintModeType;
import me.aleksilassila.litematica.printer.function.Function;
import me.aleksilassila.litematica.printer.function.Functions;
import me.aleksilassila.litematica.printer.interfaces.IMultiPlayerGameMode;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import me.aleksilassila.litematica.printer.utils.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static fi.dy.masa.litematica.selection.SelectionMode.NORMAL;
import static fi.dy.masa.litematica.util.WorldUtils.applyCarpetProtocolHitVec;
import static fi.dy.masa.litematica.util.WorldUtils.applyPlacementProtocolV3;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.*;

import org.jetbrains.annotations.Nullable;

//#if MC > 12105
import net.minecraft.world.entity.player.Input;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
//#else
//$$ import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
//#endif

public class Printer extends PrinterUtils {
    private static Printer INSTANCE = null;

    public boolean printerMemorySync = false;
    public Map<BlockPos, Integer> placeCooldownList = new HashMap<>();
    public ItemStack orderlyStoreItem; //有序存放临时存储
    public int shulkerCooldown = 0;

    public long tickStartTime, tickEndTime;
    public int packetTick;

    @NotNull
    public final PlacementGuide guide;
    public final Queue queue;

    //强制循环半径
    public BlockPos basePos = null;
    public AtomicReference<MyBox> commonBox = new AtomicReference<>();
    public AtomicReference<MyBox> guiBox = new AtomicReference<>();
    public AtomicReference<MyBox> placeBox = new AtomicReference<>();
    public int printRange = Configs.General.WORK__RANGE.getIntegerValue();
    public int placeSpeed = Configs.Placement.PLACE_SPEED.getIntegerValue();
    public int waitTicks = 0;

    public boolean updateChecked = false;
    public @Nullable BlockContext blockContext;
    // 活塞修复
    public boolean pistonNeedFix = false;
    public float workProgress = 0;

    public long workProgressTotalCount = 0, workProgressFinishedCount = 0;

    private Printer(@NotNull Minecraft client) {
        this.guide = new PlacementGuide(client);
        this.queue = new Queue(this);
        INSTANCE = this;
    }

    public static @NotNull Printer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Printer(client);
        }
        return INSTANCE;
    }

    private BlockPos getBlockPos(boolean gui, AtomicReference<MyBox> box, @Nullable Function function) {
        long tickEndTime = gui ? this.tickEndTime + 5 : this.tickEndTime;
        if (Configs.General.ITERATOR_USE_TIME.getIntegerValue() != 0 && System.currentTimeMillis() > tickEndTime) {
            return null;
        }
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            return null;
        }
        BlockPos playerPos = player.getOnPos();
        double threshold = printRange * 0.7;
        if (box.get() == null || basePos == null || !basePos.closerThan(playerPos, threshold)) {
            basePos = playerPos;
            box.set(new MyBox(basePos).expand(printRange));
            workProgressTotalCount = 0;
            workProgressFinishedCount = 0;
        }
        IterationOrderType iterationOrderType = (IterationOrderType) Configs.General.ITERATION_ORDER.getOptionListValue();
        box.get().setIterationMode(iterationOrderType);
        box.get().xIncrement = !Configs.General.X_REVERSE.getBooleanValue();
        box.get().yIncrement = !Configs.General.Y_REVERSE.getBooleanValue();
        box.get().zIncrement = !Configs.General.Z_REVERSE.getBooleanValue();
        Iterator<BlockPos> iterator = box.get().iterator();
        boolean loop = true;
        while (loop && iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (Configs.General.ITERATOR_USE_TIME.getIntegerValue() != 0 && System.currentTimeMillis() > tickEndTime) {
                loop = false;   // 退出循环, 但是不浪费本次获取的位置
            }
            if (!PrinterUtils.canInteracted(pos)) {
                continue;
            }
            if (!gui && function != null) { // 仅非 GUI 进行预筛选, 因为需要计算进度
                if (!function.isConfigAllowExecute(this) || !function.canIterationTest(this, level, player, pos)) {
                    continue;
                }
            }
            if (function == null) {
                return pos;
            }
            if (TempData.xuanQuFanWeiNei_p(pos)) {
                return pos;
            }
        }
        // 是强制被退出, 不重置位置
        if (!loop) {
            return null;
        }
        basePos = null;
        return null;
    }

    public @Nullable BlockPos getBlockPos(AtomicReference<MyBox> box, @Nullable Function function) {
        return getBlockPos(false, box, function);
    }

    public @Nullable BlockPos getBlockPos() {
        return getBlockPos(false, commonBox, null);
    }

    public float getProgress() {
        if (client.level == null) return 0.0f;
        BlockPos pos;
        while ((pos = getBlockPos(true, guiBox, null)) != null) {
            if (isPrinterMode()) {
                if (!PrinterUtils.isPositionInSelectionRange(client.player, pos, Configs.Print.PRINT_SELECTION_TYPE)) {
                    continue;
                }
                WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
                if (schematic == null) return 0.0f;
                BlockContext context = new BlockContext(client, client.level, schematic, pos);
                if (context.requiredState.isAir()) {
                    continue;
                }
                if (State.get(context) == State.CORRECT) {
                    workProgressFinishedCount++;
                }
            }

            if (isFluidMode()) {
                if (!PrinterUtils.isPositionInSelectionRange(client.player, pos, Configs.FLUID.FLUID_SELECTION_TYPE)) {
                    continue;
                }
                if (!(client.level.getBlockState(pos).getBlock() instanceof LiquidBlock)) {
                    workProgressFinishedCount++;
                }
            }

            if (isFillMode()) {
                if (!PrinterUtils.isPositionInSelectionRange(client.player, pos, Configs.Fill.FILL_SELECTION_TYPE)) {
                    continue;
                }
                if (Arrays.asList(Functions.FILL.getFillItemsArray()).contains(client.level.getBlockState(pos).getBlock().asItem())) {
                    workProgressFinishedCount++;
                }
            }

            if (isMineMode()) {
                if (!PrinterUtils.isPositionInSelectionRange(client.player, pos, Configs.Excavate.MINE_SELECTION_TYPE)) {
                    continue;
                }
                if (client.level.getBlockState(pos).isAir()) {
                    workProgressFinishedCount++;
                }
            }
            workProgressTotalCount++;
        }
        workProgress = workProgressTotalCount < 1 ? workProgress : (float) workProgressFinishedCount / workProgressTotalCount;
        return workProgress;
    }

    public void onGameTick(Minecraft client, ClientLevel level, LocalPlayer player) {
        cooldownTick(); // 冷却TICK放在前面, 不受开关与延迟影响
        if (!isEnable()) {
            return;
        }
        printRange = Configs.General.WORK__RANGE.getIntegerValue();
        placeSpeed = Configs.Placement.PLACE_SPEED.getIntegerValue();
        tickStartTime = System.currentTimeMillis();
        tickEndTime = tickStartTime + Configs.General.ITERATOR_USE_TIME.getIntegerValue();
        if (Configs.General.LAG_CHECK.getBooleanValue()) {
            if (packetTick > 20) {
                packetTick++;
                return;
            }
            packetTick++;
        }
        // 清理上一次队列（正常情况下, 在打印等功能内已经被调用, 此时应该是空的, 避免特殊情况, 所以这边进行先处理）
        queue.sendQueue(player);
        if (queue.needWait) {
            return;
        }
        functionTick(client, level, player);
        printerTick(client, level, player);
    }

    private void cooldownTick() {
        if (shulkerCooldown > 0) {
            shulkerCooldown--;
        }
        Iterator<Map.Entry<BlockPos, Integer>> iterator = placeCooldownList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            int newValue = entry.getValue() - 1;
            if (newValue <= 0) {
                iterator.remove();
            } else {
                entry.setValue(newValue);
            }
        }
    }

    private void functionTick(Minecraft client, ClientLevel level, LocalPlayer player) {
        for (Function function : Functions.VALUES) {
            function.cooldownTick();
            if (!function.isConfigAllowExecute(this)) {
                continue;
            }
            function.tick(this, client, level, player);
        }
    }

    private void printerTick(Minecraft client, ClientLevel level, LocalPlayer player) {
        if (placeSpeed != 0 && (tickStartTime / 50) % placeSpeed != 0) {
            return;
        }
        // 如果正在处理打开的容器/处理远程交互和快捷潜影盒/破坏方块列表有东西，则直接返回
        if (isOpenHandler || switchItem() || BreakManager.hasTargets()) {
            return;
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        // 单模, 非打印模式,
        if (!isPrinterMode()) {
            return;
        }
        // 遍历当前区域内所有符合条件的位置
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return;
        int printerWorkingCountPerTick = Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue();
        boolean loop = true;
        BlockPos targetPos;
        while (loop && (targetPos = getBlockPos(placeBox, null)) != null) {
            // 检查每刻放置方块是否超出限制
            if (Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue() != 0 && printerWorkingCountPerTick == 0) {
                loop = false;
            }
            // 是否在渲染层内
            if (!PrinterUtils.isPositionInSelectionRange(player, targetPos, Configs.Print.PRINT_SELECTION_TYPE)) {
                continue;
            }
            // 是否是投影方块
            if (!isSchematicBlock(targetPos)) {
                continue;
            }
            // 跳过冷却中的位置
            if (placeCooldownList.containsKey(targetPos)) {
                continue;
            }
            blockContext = new BlockContext(client, level, schematic, targetPos);

            if (Configs.Print.PUT_SKIP.getBooleanValue()) {
                Set<String> skipSet = new HashSet<>(Configs.Print.PUT_SKIP_LIST.getStrings()); // 转换为 HashSet
                if (skipSet.stream().anyMatch(s -> FilterUtils.matchName(s, blockContext.requiredState))) {
                    continue;
                }
            }

            PlacementGuide.Action action = guide.getAction(blockContext);
            if (action == null) {
                continue;
            }
            if (Configs.Print.FALLING_CHECK.getBooleanValue() && blockContext.requiredState.getBlock() instanceof FallingBlock) {
                BlockPos downPos = targetPos.below();
                if (level.getBlockState(downPos) != schematic.getBlockState(downPos)) {
                    client.gui.setOverlayMessage(Component.nullToEmpty("方块 " + blockContext.requiredState.getBlock().getName().getString() + " 下方方块不相符，跳过放置"), false);
                    continue;
                }
            }
            Direction side = action.getValidSide(level, targetPos);
            if (side == null) {
                continue;
            }
            waitTicks = action.getWaitTick();

            Debug.write("方块名: {}", blockContext.requiredState.getBlock().getName().getString());
            Debug.write("方块位置: {}", targetPos.toShortString());
            Debug.write("方块类名: {}", blockContext.requiredState.getBlock().getClass().getName());
            Debug.write("方块ID: {}", BuiltInRegistries.BLOCK.getKey(blockContext.requiredState.getBlock()));

            Item[] reqItems = action.getRequiredItems(blockContext.requiredState.getBlock());
            if (switchToItems(player, reqItems)) {
                boolean useShift = (Implementation.isInteractive(level.getBlockState(targetPos.relative(side)).getBlock()) && !(action instanceof PlacementGuide.ClickAction))
                        || Configs.Print.FORCED_SNEAK.getBooleanValue()
                        || action.useShift;

                action.queueAction(queue, targetPos, side, useShift);

                Vec3 hitModifier = usePrecisionPlacement(targetPos, blockContext.requiredState);
                if (hitModifier != null) {
                    queue.hitModifier = hitModifier;
                    queue.useProtocol = true;
                }

                if (action.getLookYaw() != null && action.getLookPitch() != null) {
                    sendLook(player, action.getLookYaw(), action.getLookPitch());
                }
                var block = blockContext.requiredState.getBlock();
                if (block instanceof PistonBaseBlock) {
                    pistonNeedFix = true;
                }
                queue.sendQueue(player);
                placeCooldownList.put(blockContext.blockPos, Configs.Placement.PLACE_COOLDOWN.getIntegerValue());

                if (block instanceof PistonBaseBlock ||
                        block instanceof ObserverBlock ||
                        block instanceof DispenserBlock ||
                        block instanceof BarrelBlock ||
                        block instanceof WallBannerBlock
                        //#if MC >= 12101
                        || block instanceof CrafterBlock
                        //#endif
                        || block instanceof WallSignBlock
                        || block instanceof GrindstoneBlock
                        || block instanceof LadderBlock
                ) {
                    return;
                }
                if (waitTicks > 0) {
                    return;
                }
                if (queue.needWait) {
                    return;
                }
                if (Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue() != 0) {
                    printerWorkingCountPerTick--;
                }
            }
        }
    }

    public Vec3 usePrecisionPlacement(BlockPos pos, BlockState stateSchematic) {
        if (Configs.Placement.EASY_PLACE_PROTOCOL.getBooleanValue()) {
            EasyPlaceProtocol protocol = PlacementHandler.getEffectiveProtocolVersion();
            Vec3 hitPos = Vec3.atLowerCornerOf(pos);
            if (protocol == EasyPlaceProtocol.V3) {
                return applyPlacementProtocolV3(pos, stateSchematic, hitPos);
            } else if (protocol == EasyPlaceProtocol.V2) {
                // Carpet Accurate Block placements protocol support, plus slab support
                return applyCarpetProtocolHitVec(pos, stateSchematic, hitPos);
            }
        }
        return null;
    }

    public boolean switchToItems(LocalPlayer player, Item[] items) {
        if (items == null || items.length == 0) {
            items = new Item[]{Items.AIR};
        }
        Inventory inv = player.getInventory();
        boolean isCreativeMode = PlayerUtils.getAbilities(player).instabuild;
        // 创造模式
        if (isCreativeMode) {
            var stack = new ItemStack(items[0]);
            return InventoryUtils.setPickedItemToHand(stack, client);
        }
        // 找到背包中可用的物品
        for (Item item : items) {
            int slot = -1;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack itemStack = inv.getItem(i);
                if (itemStack.getItem().equals(item)) {
                    slot = i;
                    break;
                }
            }
            if (slot != -1) {
                orderlyStoreItem = inv.getItem(slot);
                return InventoryUtils.setPickedItemToHand(slot, orderlyStoreItem, client);
            }
            lastNeedItemList.add(item);
        }
        return false;
    }

    public void sendLook(LocalPlayer player, float directionYaw, float directionPitch) {
        queue.lookYaw = directionYaw;
        queue.lookPitch = directionPitch;
        Implementation.sendLookPacket(player, directionYaw, directionPitch);
    }

    public void clearQueue() {
        queue.clearQueue();
    }

    public static class TempData {
        public static boolean xuanQuFanWeiNei_p(BlockPos pos) {
            AreaSelection selection = DataManager.getSelectionManager().getCurrentSelection();
            if (selection == null) return false;
            if (DataManager.getSelectionManager().getSelectionMode() == NORMAL) {
                List<Box> arr = selection.getAllSubRegionBoxes();
                for (Box box : arr) {
                    if (comparePos(box, pos)) {
                        return true;
                    }
                }
                return false;
            } else {
                Box box = selection.getSubRegionBox(DataManager.getSimpleArea().getName());
                return comparePos(box, pos);
            }
        }

        static boolean comparePos(Box box, BlockPos pos) {
            if (box == null || box.getPos1() == null || box.getPos2() == null || pos == null) return false;
            MyBox printerBox = new MyBox(box.getPos1(), box.getPos2());
            return printerBox.contains(pos);
        }
    }

    public class Queue {
        final Printer printerInstance;
        public BlockPos target;
        public Direction side;
        public Vec3 hitModifier;
        public boolean useShift = false;
        public boolean useProtocol = false;
        public @Nullable Float lookYaw = null;
        public @Nullable Float lookPitch = null;
        public boolean needWait = false;

        public Queue(Printer printerInstance) {
            this.printerInstance = printerInstance;
        }

        public void queueClick(@NotNull BlockPos target, @NotNull Direction side, @NotNull Vec3 hitModifier, boolean useShift) {
            if (Configs.Placement.PLACE_SPEED.getIntegerValue() != 0) {
                if (this.target != null) {
                    System.out.println("Was not ready yet.");
                    return;
                }
            }
            this.target = target;
            this.side = side;
            this.hitModifier = hitModifier;
            this.useShift = useShift;
        }


        public void sendQueue(LocalPlayer player) {
            if (target == null || side == null || hitModifier == null) {
                // 会刷屏污染日志
                // Debug.write("放置所需信息缺少！ Target:" + (target == null) + " Side:" + (side == null) + " HitModifier:" + (hitModifier == null));
                clearQueue();
                return;
            }
            if (!useProtocol && !needWait) {
                if (lookYaw != null && lookPitch != null) {
                    if (DirectionUtils.orderedByNearest(lookYaw, lookPitch)[0].getAxis().isHorizontal()) {
                        needWait = true;
                        return;
                    }
                }
            }
            if (needWait) {
                needWait = false;
            }
            Direction direction;
            if (lookYaw == null) {
                direction = side;
            } else {
                direction = DirectionUtils.getHorizontalDirection(lookYaw);
            }

            Vec3 hitVec;
            if (!useProtocol) {
                Vec3 targetCenter = Vec3.atCenterOf(target);
                Vec3 sideOffset = Vec3.atLowerCornerOf(DirectionUtils.getVector(side)).scale(0.5);
                Vec3 rotatedHitModifier = hitModifier.yRot((direction.toYRot() + 90) % 360).scale(0.5);
                hitVec = targetCenter.add(sideOffset).add(rotatedHitModifier);
            } else {
                hitVec = hitModifier;
            }

            if (orderlyStoreItem != null) {
                if (orderlyStoreItem.isEmpty()) {
                    SwitchItem.removeItem(orderlyStoreItem);
                } else {
                    SwitchItem.syncUseTime(orderlyStoreItem);
                }
            }

            boolean wasSneak = player.isShiftKeyDown();

            if (useShift && !wasSneak) {
                setShift(player, true);
            } else if (!useShift && wasSneak) {
                setShift(player, false);
            }


            if (Configs.Placement.PLACE_USE_PACKET.getBooleanValue()) {
                NetworkUtils.sendSequencedPacket(sequence -> new ServerboundUseItemOnPacket(
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(hitVec, side, target, false)
                        //#if MC > 11802
                        , sequence
                        //#endif
                ));
            } else {
                if (client.gameMode != null) {
                    ((IMultiPlayerGameMode) client.gameMode).litematica_printer$rightClickBlock(target, side, hitVec);
                }
            }

            if (useShift && !wasSneak) {
                setShift(player, false);
            } else if (!useShift && wasSneak) {
                setShift(player, true);
            }

            clearQueue();
        }

        public void setShift(LocalPlayer player, boolean shift) {
            //#if MC > 12105
            Input input = new Input(player.input.keyPresses.forward(), player.input.keyPresses.backward(), player.input.keyPresses.left(), player.input.keyPresses.right(), player.input.keyPresses.jump(), shift, player.input.keyPresses.sprint());
            ServerboundPlayerInputPacket packet = new ServerboundPlayerInputPacket(input);
            //#else
            //$$ ServerboundPlayerCommandPacket packet = new ServerboundPlayerCommandPacket(player, shift ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY);
            //#endif

            player.setShiftKeyDown(shift);
            NetworkUtils.sendPacket(packet);
        }

        public void clearQueue() {
            this.target = null;
            this.side = null;
            this.hitModifier = null;
            this.useShift = false;
            this.useProtocol = false;
            this.needWait = false;
            this.lookYaw = null;
            this.lookPitch = null;
        }
    }

    //endregion
}
