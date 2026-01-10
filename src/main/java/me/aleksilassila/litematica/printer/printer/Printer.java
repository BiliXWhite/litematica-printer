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
import me.aleksilassila.litematica.printer.config.enums.IterationModeType;
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
    public final PlacementGuide guide;
    public final Queue queue;
    public boolean printerMemorySync = false;
    public Map<BlockPos, Integer> placeCooldownList = new HashMap<>();
    public ItemStack orderlyStoreItem; //有序存放临时存储
    public int shulkerCooldown = 0;
    public long tickStartTime, tickEndTime;
    //强制循环半径
    public @Nullable BlockPos basePos = null;
    public MyBox workBox;
    public MyBox guiBox;
    public int printRange = Configs.General.PRINTER_RANGE.getIntegerValue();
    public boolean printerYAxisReverse = false;
    public int tickRate = Configs.General.PRINTER_SPEED.getIntegerValue();
    public int printerWorkingCountPerTick = Configs.General.BLOCKS_PER_TICK.getIntegerValue();
    public int waitTicks = 0;
    public int packetTick;
    public boolean updateChecked = false;
    public @Nullable BlockContext blockContext;
    // 活塞修复
    public boolean pistonNeedFix = false;
    public float workProgress = 0;

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

    public MyBox getBox(boolean isNewBox, LocalPlayer player, boolean gui) {
        MyBox box = gui ? guiBox : workBox;
        if (box == null || isNewBox) {
            MyBox newBox = new MyBox(player.getOnPos()).expand(printRange);
            if (gui) {
                guiBox = newBox;
            } else {
                workBox = newBox;
            }
            box = gui ? guiBox : workBox;
        }
        return box;
    }

    public BlockPos getBlockPos(boolean gui) {
        if (Configs.General.ITERATOR_USE_TIME.getIntegerValue() != 0 && System.currentTimeMillis() > tickEndTime) {
            return null;
        }
        LocalPlayer player = client.player;
        if (player == null) {
            return null;
        }
        MyBox box = getBox(false, player, gui);
        BlockPos playerOnPos = player.getOnPos();
        if (basePos == null || !basePos.equals(playerOnPos)) {
            basePos = playerOnPos;
            box = getBox(true, player, gui);
        }
        double threshold = printRange * 0.7;
        if (!basePos.closerThan(playerOnPos, threshold)) {
            basePos = null;
            return null;
        }
        box.setIterationOrderType((IterationOrderType) Configs.General.ITERATION_ORDER.getOptionListValue());
        box.setIterationModeType((IterationModeType) Configs.General.ITERATION_MODE.getOptionListValue());
        box.setXIncrement(!Configs.General.X_REVERSE.getBooleanValue());
        box.setYIncrement(!Configs.General.Y_REVERSE.getBooleanValue());
        box.setZIncrement(!Configs.General.Z_REVERSE.getBooleanValue());
        box.setCircleDirection(Configs.General.CIRCLE_DIRECTION.getBooleanValue());

        for (BlockPos pos : box) {
            if (Configs.General.ITERATOR_USE_TIME.getIntegerValue() != 0 && System.currentTimeMillis() > tickEndTime) {
                return null;
            }
            if (!canInteracted(pos) || !TempData.xuanQuFanWeiNei_p(pos)) {
                continue;
            }
            return pos;
        }
        box.resetIterations();
        basePos = null;
        return null;
    }

    public @Nullable BlockPos getGuiBlockPos() {
        return getBlockPos(true);
    }

    public @Nullable BlockPos getWorkerBlockPos() {
        return getBlockPos(false);
    }

    public void onGameTick(Minecraft client, ClientLevel level, LocalPlayer player) {
        cooldownTick(); // 冷却TICK放在前面, 不受开关与延迟影响
        if (!isEnable()) {
            return;
        }
        // 变量初始化, 通用部分(function调用了,getBlockPos, 也需要tickEndTime)
        printerYAxisReverse = false;
        printRange = Configs.General.PRINTER_RANGE.getIntegerValue();
        tickRate = Configs.General.PRINTER_SPEED.getIntegerValue();
        printerWorkingCountPerTick = Configs.General.BLOCKS_PER_TICK.getIntegerValue();
        tickStartTime = System.currentTimeMillis();
        tickEndTime = tickStartTime + Configs.General.ITERATOR_USE_TIME.getIntegerValue();
        if (tickRate != 0 && (tickStartTime / 50) % tickRate != 0) {
            return;
        }
        if (Configs.General.LAG_CHECK.getBooleanValue()) {
            if (packetTick > 20) {
                packetTick++;
                return;
            }
            packetTick++;
        }
        functionTick(client, level, player);    // 提取出来, 不受库存影响
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
            if (!function.canTick()) {
                continue;
            }
            function.tick(this, client, level, player);
        }
    }

    private void printerTick(Minecraft client, ClientLevel level, LocalPlayer player) {
        // 如果正在处理打开的容器/处理远程交互和快捷潜影盒/破坏方块列表有东西，则直接返回
        if (isOpenHandler || switchItem() || BreakManager.hasTargets()) {
            return;
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        queue.sendQueue(player);
        if (queue.needWait) {
            return;
        }
        // 单模, 非打印模式,
        if (Configs.General.MODE_SWITCH.getOptionListValue() instanceof ModeType modeType && modeType == ModeType.SINGLE) {
            if (Configs.General.PRINTER_MODE.getOptionListValue() instanceof PrintModeType printModeType && printModeType != PrintModeType.PRINTER) {
                return;
            }
        }
        // 遍历当前区域内所有符合条件的位置
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return;
        BlockPos targetPos;
        while ((targetPos = getWorkerBlockPos()) != null) {
            if (queue.needWait) {
                continue;
            }
            // 检查每刻放置方块是否超出限制
            if (Configs.General.BLOCKS_PER_TICK.getIntegerValue() != 0 && printerWorkingCountPerTick == 0) {
                return;
            }
            // 是否在渲染层内
            if (!PrinterUtils.isPositionInSelectionRange(player, targetPos, Configs.Put.PRINT_SELECTION_TYPE)) {
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
            BlockContext blockContext = new BlockContext(client, level, schematic, targetPos);
            // 检查放置跳过列表
            if (Configs.Put.PUT_SKIP.getBooleanValue()) {
                Set<String> skipSet = new HashSet<>(Configs.Put.PUT_SKIP_LIST.getStrings()); // 转换为 HashSet
                if (skipSet.stream().anyMatch(s -> FilterUtils.matchName(s, blockContext.requiredState))) {
                    continue;
                }
            }
            // 放置冷却
            placeCooldownList.put(targetPos, Configs.General.PLACE_COOLDOWN.getIntegerValue());
            PlacementGuide.Action action = guide.getAction(blockContext);
            if (action == null) {
                continue;
            }
            if (Configs.Put.FALLING_CHECK.getBooleanValue() && blockContext.requiredState.getBlock() instanceof FallingBlock) {
                //检查方块下面方块是否正确，否则跳过放置
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
            // 调试输出
            if (Configs.General.DEBUG_OUTPUT.getBooleanValue()) {
                Debug.write("方块名: {}", blockContext.requiredState.getBlock().getName().getString());
                Debug.write("方块位置: {}", targetPos.toShortString());
                Debug.write("方块类名: {}", blockContext.requiredState.getBlock().getClass().getName());
                Debug.write("方块ID: {}", BuiltInRegistries.BLOCK.getKey(blockContext.requiredState.getBlock()));
            }
            Item[] reqItems = action.getRequiredItems(blockContext.requiredState.getBlock());
            if (switchToItems(player, reqItems)) {
                boolean useShift = (Implementation.isInteractive(level.getBlockState(targetPos.relative(side)).getBlock()) && !(action instanceof PlacementGuide.ClickAction))
                        || Configs.Put.FORCED_SNEAK.getBooleanValue()
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

                if (tickRate == 0) {
                    queue.sendQueue(player);
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
                    if (Configs.General.BLOCKS_PER_TICK.getIntegerValue() != 0) {
                        printerWorkingCountPerTick--;
                    }
                    continue;
                }
                queue.sendQueue(player);
                return;
            }
        }
    }

    public float getProgress() {
        // 重置 basePos 以确保重新初始化迭代器
        basePos = null;
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return 0.0f;
        // 打印进度相关
        int totalCount = 0;
        int finishedCount = 0;
        BlockPos pos;
        while ((pos = getGuiBlockPos()) != null && client.level != null) {
            BlockState currentState = client.level.getBlockState(pos);
            totalCount++;
            if (isPrinterMode()) {
                BlockState requiredState = schematic.getBlockState(pos);
                if (requiredState.isAir()) {
                    totalCount--;
                    continue;
                }
                if (currentState.getBlock().defaultBlockState().equals(requiredState.getBlock().defaultBlockState())) {
                    finishedCount++;
                }
            }
            if (isFluidMode() && !(currentState.getBlock() instanceof LiquidBlock)) {
                finishedCount++;
            }
            if (isFillMode() && Arrays.asList(Functions.FILL.getFillItemsArray()).contains(currentState.getBlock().asItem())) {
                finishedCount++;
            }
            if (isMineMode() && currentState.isAir()) {
                finishedCount++;
            }
        }
        workProgress = totalCount < 1 ? workProgress : (float) finishedCount / totalCount;
        return workProgress;
    }

    public Vec3 usePrecisionPlacement(BlockPos pos, BlockState stateSchematic) {
        if (Configs.General.EASY_PLACE_PROTOCOL.getBooleanValue()) {
            EasyPlaceProtocol protocol = PlacementHandler.getEffectiveProtocolVersion();
            Vec3 hitPos = Vec3.atLowerCornerOf(pos);
            if (protocol == EasyPlaceProtocol.V3) {
                return applyPlacementProtocolV3(pos, stateSchematic, hitPos);
            } else if (protocol == EasyPlaceProtocol.V2) {
                // Carpet Accurate Block Placement protocol support, plus slab support
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
            MyBox myBox = new MyBox(box.getPos1(), box.getPos2());
            return myBox.contains(pos);
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
            if (Configs.General.PRINTER_SPEED.getIntegerValue() != 0) {
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


            if (Configs.Put.PLACE_USE_PACKET.getBooleanValue()) {
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
