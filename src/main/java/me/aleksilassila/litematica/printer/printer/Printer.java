package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EasyPlaceProtocol;
import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.Debug;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PreprocessUtils;
import me.aleksilassila.litematica.printer.config.enums.IterationOrderType;
import me.aleksilassila.litematica.printer.config.enums.ModeType;
import me.aleksilassila.litematica.printer.config.enums.PrintModeType;
import me.aleksilassila.litematica.printer.function.FunctionExtension;
import me.aleksilassila.litematica.printer.function.FunctionModeBase;
import me.aleksilassila.litematica.printer.function.Functions;
import me.aleksilassila.litematica.printer.interfaces.IMultiPlayerGameMode;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.overwrite.MyBox;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import me.aleksilassila.litematica.printer.utils.DirectionUtils;
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
import static me.aleksilassila.litematica.printer.InitHandler.*;
import static me.aleksilassila.litematica.printer.config.enums.PrintModeType.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.*;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.*;

import org.jetbrains.annotations.Nullable;

//#if MC > 12105
import net.minecraft.world.entity.player.Input;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
//#else
//$$ import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
//#endif

public class Printer extends PrinterUtils {
    @NotNull
    public static final Minecraft client = Minecraft.getInstance();
    private static Printer INSTANCE = null;
    public final PlacementGuide guide;
    public final Queue queue;
    public boolean printerMemorySync = false;
    public BlockPos easyPos = null;
    public Map<BlockPos, Integer> placeCooldownList = new HashMap<>();
    public ItemStack orderlyStoreItem; //有序存放临时存储
    public int shulkerCooldown = 0;
    public long tickStartTime;
    public long tickEndTime;
    //强制循环半径
    public BlockPos basePos = null;
    public MyBox myBox;
    public int printRange = PRINTER_RANGE.getIntegerValue();
    public boolean printerYAxisReverse = false;
    public int tickRate = PRINTER_SPEED.getIntegerValue();
    public int printerWorkingCountPerTick = BLOCKS_PER_TICK.getIntegerValue();
    public int waitTicks = 0;
    public int packetTick;
    public boolean updateChecked = false;
    public BlockState requiredState;
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

    public BlockPos getBlockPos() {
        if (ITERATOR_USE_TIME.getIntegerValue() != 0 && System.currentTimeMillis() > tickEndTime)
            return null;
        LocalPlayer player = client.player;
        if (player == null)
            return null;
        BlockPos playerPos = player.getOnPos();
        // 如果 basePos 为空，则初始化为玩家当前位置，并扩展 myBox 范围
        if (basePos == null) {
            basePos = playerPos;
            myBox = new MyBox(basePos).inflate(printRange);
        }
        double threshold = printRange * 0.7;
        if (!basePos.closerThan(playerPos, threshold)) {
            basePos = null;
            return null;
        }
        if (ITERATION_ORDER.getOptionListValue() instanceof IterationOrderType iterationOrderType) {
            myBox.initIterator();
            myBox.setIterationMode(iterationOrderType);
            myBox.xIncrement = !X_REVERSE.getBooleanValue();
            myBox.yIncrement = Y_REVERSE.getBooleanValue() == printerYAxisReverse;
            myBox.zIncrement = !Z_REVERSE.getBooleanValue();
            Iterator<BlockPos> iterator = myBox.iterator;
            while (iterator.hasNext()) {
                if (ITERATOR_USE_TIME.getIntegerValue() != 0 && System.currentTimeMillis() > tickEndTime) return null;
                BlockPos pos = iterator.next();
                // 只有在形状为球体的时候才判断在不在距离内
                if (
                        ((isPrinterMode() && isSchematicBlock(pos)) ||
                                TempData.xuanQuFanWeiNei_p(pos)) &&
                                PlaceUtils.canInteracted(pos)
                ) {
                    return pos;
                }
            }
        }
        // 如果没有找到符合条件的位置，重置 basePos 并返回 null
        basePos = null;
        return null;
    }

    public void onGameTick() {
        cooldownTick(); // 冷却TICK放在前面,不受开关影响
        if (!(InitHandler.PRINT_SWITCH.getBooleanValue() || InitHandler.PRINT.getKeybind().isPressed())) {
            return;
        }
        printerTick();
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

    private void printerTick() {
        if (LAG_CHECK.getBooleanValue()) {
            if (packetTick > 20) {
                packetTick++;
                return;
            }
            packetTick++;
        }
        LocalPlayer player = client.player;
        ClientLevel world = client.level;
        if (world == null || player == null)
            return;

        // 预载常用配置值
        printRange = PRINTER_RANGE.getIntegerValue();
        tickRate = PRINTER_SPEED.getIntegerValue();
        printerWorkingCountPerTick = BLOCKS_PER_TICK.getIntegerValue();
        printerYAxisReverse = false;

        // 如果正在处理打开的容器/处理远程交互和快捷潜影盒/破坏方块列表有东西，则直接返回
        if (isOpenHandler || switchItem() || BreakManager.hasTargets()) {
            return;
        }

        // 打印机工作开始
        tickStartTime = System.currentTimeMillis();
        tickEndTime = tickStartTime + ITERATOR_USE_TIME.getIntegerValue();

        if (tickRate != 0 && (tickStartTime / 50) % tickRate != 0) {
            return;
        }

        queue.sendQueue(player);

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        for (FunctionExtension function : Functions.LIST) {
            if (function instanceof FunctionModeBase functionModeBase) {
                if (!functionModeBase.canTick()) {
                    continue;
                }
            }
            function.tick(this, client, world, player);
        }

        // 单模, 非打印模式,
        if (MODE_SWITCH.getOptionListValue() instanceof ModeType modeType && modeType == ModeType.SINGLE) {
            if (PRINTER_MODE.getOptionListValue() instanceof PrintModeType printModeType && printModeType != PRINTER) {
                return;
            }
        }

        // 遍历当前区域内所有符合条件的位置
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return;
        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            // 检查每刻放置方块是否超出限制
            if (BLOCKS_PER_TICK.getIntegerValue() != 0 && printerWorkingCountPerTick == 0) {
                return;
            }
            // 是否在渲染层内
            if (PrinterUtils.isLimitedByTheNumberOfLayers(pos)) {
                continue;
            }
            if (!isSchematicBlock(pos)) {
                continue;
            }

            requiredState = schematic.getBlockState(pos);

            // 检查放置跳过列表
            if (PUT_SKIP.getBooleanValue()) {
                Set<String> skipSet = new HashSet<>(PUT_SKIP_LIST.getStrings()); // 转换为 HashSet
                if (skipSet.stream().anyMatch(s -> equalsName(s, requiredState))) {
                    continue;
                }
            }

            // 跳过冷却中的位置
            if (placeCooldownList.containsKey(pos)) {
                continue;
            }
            // 放置冷却
            placeCooldownList.put(pos, PLACE_COOLDOWN.getIntegerValue());

            // 检查放置条件
            PlacementGuide.Action action = guide.getAction(world, schematic, pos);
            if (action == null) {
                continue;
            }

            if (FALLING_CHECK.getBooleanValue() && requiredState.getBlock() instanceof FallingBlock) {
                //检查方块下面方块是否正确，否则跳过放置
                BlockPos downPos = pos.below();
                if (world.getBlockState(downPos) != schematic.getBlockState(downPos)) {
                    client.gui.setOverlayMessage(Component.nullToEmpty("方块 " + requiredState.getBlock().getName().getString() + " 下方方块不相符，跳过放置"), false);
                    continue;
                }
            }

            Direction side = action.getValidSide(world, pos);
            if (side == null) {
                continue;
            }

            // 调试输出
            if (DEBUG_OUTPUT.getBooleanValue()) {
                Debug.write("方块名: {}", requiredState.getBlock().getName().getString());
                Debug.write("方块位置: {}", pos.toShortString());
                Debug.write("方块类名: {}", requiredState.getBlock().getClass().getName());
                Debug.write("方块ID: {}", BuiltInRegistries.BLOCK.getKey(requiredState.getBlock()));
            }

            if (queue.needWait) {
                continue;
            }

            Item[] reqItems = action.getRequiredItems(requiredState.getBlock());
            if (switchToItems(player, reqItems)) {
                boolean useShift = (Implementation.isInteractive(world.getBlockState(pos.relative(side)).getBlock()) && !(action instanceof PlacementGuide.ClickAction))
                        || FORCED_SNEAK.getBooleanValue()
                        || action.useShift;


                action.queueAction(queue, pos, side, useShift);

                Vec3 hitModifier = usePrecisionPlacement(pos, requiredState);
                if (hitModifier != null) {
                    queue.hitModifier = hitModifier;
                    queue.useProtocol = true;
                }

                if (action.getLookYaw() != null && action.getLookPitch() != null) {
                    sendLook(action.getLookYaw(), action.getLookPitch());
                }

                var block = requiredState.getBlock();
                if (block instanceof PistonBaseBlock) {
                    pistonNeedFix = true;
                }
                waitTicks = action.getWaitTick();
                if (tickRate == 0) {
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
                    queue.sendQueue(player);
                    if (queue.needWait) {
                        return;
                    }
                    if (BLOCKS_PER_TICK.getIntegerValue() != 0) {
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
        while ((pos = getBlockPos()) != null) {
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
            if (isFillMode() && Arrays.asList(Functions.FUNCTION_FILL.getFillItemsArray()).contains(currentState.getBlock().asItem())) {
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
        if (EASYPLACE_PROTOCOL.getBooleanValue()) {
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
        boolean isCreativeMode = Implementation.getAbilities(player).instabuild;
        // 创造模式
        if (isCreativeMode) {
            var stack = new ItemStack(items[0]);
            return PlaceUtils.setPickedItemToHand(stack, client);
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
                return PlaceUtils.setPickedItemToHand(slot, orderlyStoreItem, client);
            }
            lastNeedItemList.add(item);
        }
        return false;
    }

    public void sendLook(float directionYaw, float directionPitch) {
        queue.lookYaw = directionYaw;
        queue.lookPitch = directionPitch;
        if (client.player != null) {
            Implementation.sendLookPacket(client.player, directionYaw, directionPitch);
        }
    }

    //region 单例

    public void clearQueue() {
        queue.clearQueue();
    }

    public static class TempData {
        public static boolean xuanQuFanWeiNei_p(BlockPos pos) {
            AreaSelection i = DataManager.getSelectionManager().getCurrentSelection();
            if (i == null) return false;
            if (DataManager.getSelectionManager().getSelectionMode() == NORMAL) {
                List<Box> arr = i.getAllSubRegionBoxes();
                for (Box box : arr) {
                    if (comparePos(box, pos)) {
                        return true;
                    }
                }
                return false;
            } else {
                Box box = i.getSubRegionBox(DataManager.getSimpleArea().getName());
                return comparePos(box, pos);
            }
        }

        static boolean comparePos(Box box, BlockPos pos) {
            if (box == null || box.getPos1() == null || box.getPos2() == null || pos == null) return false;
            MyBox myBox = new MyBox(box);
            //因为麻将的Box.contains方法内部用的 x >= this.minX && x < this.maxX ... 使得最小边界能被覆盖，但是最大边界不行
            //因此 我重写了该方法
            return myBox.contains(Vec3.atLowerCornerOf(pos));
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
            if (PRINTER_SPEED.getIntegerValue() != 0) {
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

            Direction direction;
            if (lookYaw == null) {
                direction = side;
            } else {
                direction = DirectionUtils.getHorizontalDirection(lookYaw);
            }

            Vec3 hitVec;
            if (!useProtocol) {
                Vec3 targetCenter = Vec3.atCenterOf(target);
                Vec3 sideOffset = Vec3.atLowerCornerOf(PreprocessUtils.getVec3iFromDirection(side)).scale(0.5);
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


            if (PLACE_USE_PACKET.getBooleanValue()) {
                //#if MC >= 11904
                player.connection.send(new ServerboundUseItemOnPacket(
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(hitVec, side, target, false),
                        ZxyUtils.getSequence()
                ));
                //#else
                //$$ player.connection.send(new ServerboundUseItemOnPacket(
                //$$         net.minecraft.world.InteractionHand.MAIN_HAND,
                //$$         new BlockHitResult(hitVec, side, target, false)
                //$$ ));
                //#endif
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
            player.connection.send(packet);
        }

        public void clearQueue() {
            this.target = null;
            this.side = null;
            this.hitModifier = null;
            this.useShift = false;
            this.needWait = false;
            this.lookYaw = null;
            this.lookPitch = null;
        }
    }

    //endregion
}
