package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;

import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EasyPlaceProtocol;
import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.interfaces.IClientPlayerInteractionManager;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.overwrite.MyBox;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static fi.dy.masa.litematica.selection.SelectionMode.NORMAL;
import static fi.dy.masa.litematica.util.WorldUtils.applyCarpetProtocolHitVec;
import static fi.dy.masa.litematica.util.WorldUtils.applyPlacementProtocolV3;
import static me.aleksilassila.litematica.printer.LitematicaMixinMod.*;
import static me.aleksilassila.litematica.printer.printer.State.PrintModeType.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsBlockName;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsItemName;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.switchItem;

//#if MC < 11904
//$$ import net.minecraft.util.Identifier;
//$$ import net.minecraft.command.argument.ItemStringReader;
//$$ import com.mojang.brigadier.StringReader;
//$$ import net.minecraft.util.registry.RegistryKey;
//$$ import net.minecraft.util.registry.Registry;
//#else
//#if MC == 11904
//$$ import net.minecraft.registry.RegistryKeys;
//$$ import net.minecraft.registry.RegistryKey;
//#endif
import net.minecraft.registry.Registries;
//#endif

//#if MC <= 12004
//$$import static fi.dy.masa.malilib.util.InventoryUtils.areStacksEqual;
//#endif

//#if MC > 12105
//$$import net.minecraft.util.PlayerInput;
//$$import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
//#else
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
//#endif

public class Printer extends PrinterUtils {
    public static HashSet<Item> fluidModeItemList = new HashSet<>();
    public static HashSet<Item> fillModeItemList = new HashSet<>();
    public static boolean printerMemorySync = false;
    public static BlockPos easyPos = null;
    public static Map<BlockPos, Integer> placeCooldownList = new HashMap<>();
    static ItemStack orderlyStoreItem; //有序存放临时存储
    private static Printer INSTANCE = null;
    public int shulkerCooldown = 0;
    public static long tickStartTime;

    @NotNull
    public static final MinecraftClient client = MinecraftClient.getInstance();
    public final PlacementGuide guide;
    public final Queue queue;
    public final BreakManager breakManager = BreakManager.instance();
    //强制循环半径
    public BlockPos basePos = null;
    public MyBox myBox;
    int printRange;
    boolean yReverse = false;
    int tickRate;
    List<String> fluidBlocklist;
    List<String> fillBlocklist;
    private boolean needDelay;
    private int printPerTick;

    public static int packetTick;
    public static boolean updateChecked = false;
    public static BlockState requiredState;

    private Printer(@NotNull MinecraftClient client) {

        this.guide = new PlacementGuide(client);
        this.queue = new Queue(this);

        INSTANCE = this;
    }

    public static @NotNull Printer getPrinter() {
        if (INSTANCE == null) {
            INSTANCE = new Printer(client);
        }
        return INSTANCE;
    }


    BlockPos getBlockPos() {
        if (LitematicaMixinMod.ITERATOR_USE_TIME.getIntegerValue() != 0 && PlaceUtils.isTimeOut()) return null;
        ClientPlayerEntity player = client.player;
        if (player == null) return null;

        BlockPos playerPos = player.getBlockPos();

        // 如果 basePos 为空，则初始化为玩家当前位置，并扩展 myBox 范围
        if (basePos == null) {
            basePos = playerPos;
            myBox = new MyBox(basePos).expand(printRange);
        }

        double threshold = printRange * 0.7;
        if (!basePos.isWithinDistance(playerPos, threshold)) {
            basePos = null;
            return null;
        }
        myBox.initIterator();
        myBox.setIterationMode(ITERATION_ORDER.getOptionListValue());
        myBox.xIncrement = !X_REVERSE.getBooleanValue();
        myBox.yIncrement = Y_REVERSE.getBooleanValue() == yReverse;
        myBox.zIncrement = !Z_REVERSE.getBooleanValue();

        Iterator<BlockPos> iterator = myBox.iterator;

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (!basePos.isWithinDistance(pos, printRange)) {
                continue;
            }
            return pos;
        }

        // 如果没有找到符合条件的位置，重置 basePos 并返回 null
        basePos = null;
        return null;
    }

    void fluidMode() {
        requiredState = null;
        fluidBlocklist = LitematicaMixinMod.FLUID_BLOCK_LIST.getStrings();
        if (fluidBlocklist.isEmpty()) return;
        fluidModeItemList.clear();
        for (String itemName : fluidBlocklist) {
            List<Item> list = Registries.ITEM.stream().filter(item -> equalsItemName(itemName, new ItemStack(item))).toList();
            fluidModeItemList.addAll(list);
        }
        Item[] array = fluidModeItemList.toArray(new Item[0]);

        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            if (PRINT_PER_TICK.getIntegerValue() != 0 && printPerTick == 0) return;
            if (!canInteracted(pos) || !TempData.xuanQuFanWeiNei_p(pos) || isLimitedByTheNumberOfLayers(pos)) {
                continue;
            }

            // 跳过冷却中的位置
            if (placeCooldownList.containsKey(pos)) continue;
            placeCooldownList.put(pos, 0);

            BlockState currentState = client.world.getBlockState(pos);
            if (currentState.getFluidState().isOf(Fluids.LAVA) || currentState.getFluidState().isOf(Fluids.WATER)) {
                if (playerHasAccessToItems(client.player, array)) {
                    switchToItems(client.player, array);
                    new PlacementGuide.Action().queueAction(queue, pos, Direction.UP, false);
                    if (tickRate == 0) {
                        queue.sendQueue(client.player);
                        if (PRINT_PER_TICK.getIntegerValue() != 0) printPerTick--;
                        continue;
                    }
                    return;
                }
            }
        }
    }

    void fillMode() {
        requiredState = null;
        fillBlocklist = LitematicaMixinMod.FILL_BLOCK_LIST.getStrings();
        if (fillBlocklist.isEmpty()) return;
        fillModeItemList.clear();
        for (String itemName : fillBlocklist) {
            List<Item> list = Registries.ITEM.stream().filter(item -> equalsItemName(itemName, new ItemStack(item))).toList();
            fillModeItemList.addAll(list);
        }
        Item[] array = fillModeItemList.toArray(new Item[0]);

        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            if (PRINT_PER_TICK.getIntegerValue() != 0 && printPerTick == 0) return;

            if (!canInteracted(pos) || !TempData.xuanQuFanWeiNei_p(pos) || isLimitedByTheNumberOfLayers(pos)) {
                continue;
            }

            // 跳过冷却中的位置
            if (placeCooldownList.containsKey(pos)) continue;
            placeCooldownList.put(pos, 0);

            var currentState = client.world.getBlockState(pos);
            if (currentState.isAir() || (currentState.getBlock() instanceof FluidBlock) || REPLACEABLE_LIST.getStrings().stream().anyMatch(s -> equalsBlockName(s, currentState))) {
                if (playerHasAccessToItems(client.player, array)) {
                    switchToItems(client.player, array);
                    new PlacementGuide.Action().queueAction(queue, pos, Direction.UP, false);
                    if (tickRate == 0) {
                        queue.sendQueue(client.player);
                        if (PRINT_PER_TICK.getIntegerValue() != 0) printPerTick--;
                        continue;
                    }
                    return;
                }
            }

        }
    }

    BlockPos breakPos = null;
    void miningMode() {
        BlockPos pos;
        // 循环处理方块位置，直到找到可挖掘的目标或遍历完成
        while ((pos = breakPos == null ? getBlockPos() : breakPos) != null) {
            // 检查玩家状态和位置限制条件
            if (client.player != null && (!canInteracted(pos) || isLimitedByTheNumberOfLayers(pos))) {
                // 重置临时位置并继续循环
                if (breakPos == null) continue;
                breakPos = null;
                continue;
            }
            if (TempData.xuanQuFanWeiNei_p(pos) &&
                    BreakManager.breakRestriction(client.world.getBlockState(pos)) &&
                    breakManager.breakBlock(pos)) {
                requiredState = client.world.getBlockState(pos);
                breakPos = pos;
                return;
            }
            // 清空临时位置
            breakPos = null;
        }
    }

    public void myTick() {
        if (shulkerCooldown > 0) {
            shulkerCooldown--;
        }

        Iterator<Map.Entry<BlockPos, Integer>> iterator = placeCooldownList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            int newValue = entry.getValue() + 1;
            if (newValue >= PUT_COOLING.getIntegerValue()) {
                iterator.remove();
            } else {
                entry.setValue(newValue);
            }
        }
    }

    public void tick() {
        if (LitematicaMixinMod.LAG_CHECK.getBooleanValue()) {
            if (packetTick > 20)
                return;
            packetTick++;
        }
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        ClientWorld world = client.world;
        // 预载常用配置值
        int compulsionRange = COMPULSION_RANGE.getIntegerValue();
        tickStartTime = System.currentTimeMillis();
        tickRate = PRINT_INTERVAL.getIntegerValue();
        printPerTick = LitematicaMixinMod.PRINT_PER_TICK.getIntegerValue();

        // 更新环境参数
        if (compulsionRange != printRange) {
            printRange = compulsionRange;
        }

        // 如果正在处理打开的容器或切换物品，则直接返回
        if (isOpenHandler || switchItem()) {
            return;
        }

        // 优先执行队列中的点击操作
        if (tickRate != 0) {
            queue.sendQueue(player);
            if ((tickStartTime / 50) % tickRate != 0) {
                return;
            }
        }

        // 0tick修复
        if (needDelay) {
            queue.sendQueue(player);
            needDelay = false;
        }

        // 模式判断（减少重复调用，提高分支执行效率）
        Object modeOption = LitematicaMixinMod.MODE_SWITCH.getOptionListValue();
        if (modeOption.equals(State.ModeType.MULTI)) {
            boolean multiBreak = MULTI_BREAK.getBooleanValue();
            if (LitematicaMixinMod.EXCAVATE.getBooleanValue()) {
                yReverse = true;
                miningMode();
                if (multiBreak) return;
            }
            if (LitematicaMixinMod.FLUID.getBooleanValue()) {
                fluidMode();
                if (multiBreak) return;
            }
            if (LitematicaMixinMod.FILL.getBooleanValue()) {
                fillMode();
                if (multiBreak) return;
            }
        } else if (LitematicaMixinMod.PRINTER_MODE.getOptionListValue() instanceof State.PrintModeType modeType
                && modeType != PRINTER) {
            switch (modeType) {
                case MINING -> {
                    yReverse = true;
                    miningMode();
                }
                case FLUID -> fluidMode();
                case FILL -> fillMode();
            }
            return;
        }

        if (!yReverse) yReverse = true;
        // 遍历当前区域内所有符合条件的位置
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return;
        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            // 检查每刻放置方块是否超出限制
            if (PRINT_PER_TICK.getIntegerValue() != 0 && printPerTick == 0) return;
            // 是否是投影方块
            if (!isSchematicBlock(pos)) continue;
            // 是否在破坏列表内
            if (BreakManager.inBreakTargets(pos)) continue;
            // 是否在渲染层内
            if (!DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;
            // 是否可接触到
            if (!canInteracted(pos)) continue;

            requiredState = schematic.getBlockState(pos);

            // 检查放置跳过列表
            if (LitematicaMixinMod.PUT_SKIP.getBooleanValue()) {
                Set<String> skipSet = new HashSet<>(PUT_SKIP_LIST.getStrings()); // 转换为 HashSet
                if (skipSet.stream().anyMatch(s -> Filters.equalsName(s, requiredState))) {
                    continue;
                }
            }

            // 跳过冷却中的位置
            if (placeCooldownList.containsKey(pos)) continue;
            // 放置冷却
            placeCooldownList.put(pos, 0);

            // 检查放置条件
            PlacementGuide.Action action = guide.getAction(world, schematic, pos);
            if (action == null) continue;


            Direction side = action.getValidSide(world, pos);
            if (side == null) continue;

            // 调试输出
            if (DEBUG_OUTPUT.getBooleanValue()) {
                //#if MC < 12104 && MC != 12101
                //$$ Litematica.logger.info("[Printer] 方块名: {}", requiredState.getBlock().getName().getString());
                //$$ Litematica.logger.info("[Printer] 方块类名: {}", requiredState.getBlock().getClass().getName());
                //$$ Litematica.logger.info("[Printer] 方块ID: {}", Registries.BLOCK.getId(requiredState.getBlock()));
                //#else
                Litematica.LOGGER.info("[Printer] 方块名: {}", requiredState.getBlock().getName().getString());
                Litematica.LOGGER.info("[Printer] 方块类名: {}", requiredState.getBlock().getClass().getName());
                Litematica.LOGGER.info("[Printer] 方块ID: {}", Registries.BLOCK.getId(requiredState.getBlock()));
                //#endif
            }

            Item[] reqItems = action.getRequiredItems(requiredState.getBlock());
            if (playerHasAccessToItems(player, reqItems)) {
                boolean useShift = (Implementation.isInteractive(world.getBlockState(pos.offset(side)).getBlock()) && !(action instanceof PlacementGuide.ClickAction))
                        || LitematicaMixinMod.FORCED_SNEAK.getBooleanValue()
                        || action.useShift;

                if (needDelay) continue;
                if (!switchToItems(player, reqItems)) return;

                action.queueAction(queue, pos, side, useShift);

                Vec3d hitModifier = usePrecisionPlacement(pos, requiredState);
                if(hitModifier != null){
                    queue.hitModifier = hitModifier;
                    queue.termsOfUse = true;
                }

                if (action.getLookHorizontalDirection() != null)
                    sendLook(player, action.getLookHorizontalDirection(), action.getLookDirectionPitch());

                if (tickRate == 0) {
                    var block = schematic.getBlockState(pos).getBlock();

                    if (block instanceof PistonBlock ||
                            block instanceof ObserverBlock ||
                            block instanceof DispenserBlock ||
                            block instanceof BarrelBlock ||
                            block instanceof WallBannerBlock
                            //#if MC >= 12101
                            || block instanceof CrafterBlock
                        //#endif
                    ) {
                        needDelay = true;
                        return;
                    }

                    queue.sendQueue(player);
                    if (PRINT_PER_TICK.getIntegerValue() != 0) printPerTick--;
                    continue;
                }
                return;
            }
        }
    }

    public float getPrintProgress() {
        // 重置 basePos 以确保重新初始化迭代器
        basePos = null;
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return 0.0f;

        int printedCount = 0;
        int totalCount = 0;
        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            BlockState requiredState = schematic.getBlockState(pos);
            BlockState currentState = client.world.getBlockState(pos);
            if (!isSchematicBlock(pos) || requiredState == null || requiredState.getBlock() instanceof AirBlock)
                continue;
            totalCount++;
            if (currentState.getBlock().getDefaultState().equals(requiredState.getBlock().getDefaultState())) {
                printedCount++;
            }
        }
        return totalCount == 0 ? 0.0f : ((float) printedCount / totalCount);
    }

    public Vec3d usePrecisionPlacement(BlockPos pos,BlockState stateSchematic){
        if (EASYPLACE_PROTOCOL.getBooleanValue()) {
            EasyPlaceProtocol protocol = PlacementHandler.getEffectiveProtocolVersion();
            Vec3d hitPos = Vec3d.of(pos);
            if (protocol == EasyPlaceProtocol.V3)
            {
                return applyPlacementProtocolV3(pos, stateSchematic, hitPos);
            }
            else if (protocol == EasyPlaceProtocol.V2)
            {
                // Carpet Accurate Block Placement protocol support, plus slab support
                return applyCarpetProtocolHitVec(pos, stateSchematic, hitPos);
            }
        }
        return null;
    }

    public boolean switchToItems(ClientPlayerEntity player, Item[] items) {
        if (items == null) return true;
        PlayerInventory inv = player.getInventory();

        // 遍历物品列表，查找玩家背包中可用的物品
        for (Item item : items) {
            int slot = -1;
            // 在玩家背包中查找指定物品的槽位
            for (int i = 0; i < inv.size(); i++) {
                if (inv.getStack(i).getItem() == item && inv.getStack(i).getCount() > 0)
                    slot = i;
            }
            // 如果找到物品槽位，则交换手持物品与该槽位物品
            if (slot != -1) {
                orderlyStoreItem = inv.getStack(slot);
                return swapHandWithSlot(player, slot);
            }
            // 如果玩家处于创造模式，则直接设置选中的物品到手中
            if (Implementation.getAbilities(player).creativeMode) {
                var stack = new ItemStack(item);
                player.getInventory().getSlotWithStack(stack);
                ZxyUtils.setPickedItemToHand(
                        player.getInventory().getSlotWithStack(stack),
                        stack);
                client.interactionManager.clickCreativeStack(client.player.getStackInHand(Hand.MAIN_HAND), 36 + inv.selectedSlot);
                return true;
            }
        }
        return true;
    }


    public boolean swapHandWithSlot(ClientPlayerEntity player, int slot) {
        ItemStack stack = player.getInventory().getStack(slot);
        int slotNum = client.player.getInventory().getSlotWithStack(stack);
        return ZxyUtils.setPickedItemToHand(slotNum, stack);
    }

    public void sendLook(ClientPlayerEntity player, Direction directionYaw, Direction directionPitch) {
        if (directionYaw != null || directionPitch != null) {
            Implementation.sendLookPacket(player, directionYaw, directionPitch);
        }
        queue.lookDirYaw = directionYaw;
        queue.lookDirPitch = directionPitch;
    }

    public void clearQueue() { queue.clearQueue();}

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
            return myBox.contains(Vec3d.of(pos));
        }
    }

    public static class Queue {
        final Printer printerInstance;
        public BlockPos target;
        public Direction side;
        public Vec3d hitModifier;
        public boolean needSneak = false;
        public boolean termsOfUse = false;
        public Direction lookDirYaw = null;
        public Direction lookDirPitch = null;

        public Queue(Printer printerInstance) {
            this.printerInstance = printerInstance;
        }

        public void queueClick(@NotNull BlockPos target, @NotNull Direction side, @NotNull Vec3d hitModifier, boolean needSneak) {
            if (LitematicaMixinMod.PRINT_INTERVAL.getIntegerValue() != 0) {
                if (this.target != null) {
                    System.out.println("Was not ready yet.");
                    return;
                }
            }

            this.target = target;
            this.side = side;
            this.hitModifier = hitModifier;
            this.needSneak = needSneak;

        }

        public void sendQueue(ClientPlayerEntity player) {
            if (target == null || side == null || hitModifier == null) return;

            boolean wasSneaking = player.isSneaking();
            Direction direction = side.getAxis() == Direction.Axis.Y
                    ? (lookDirYaw != null && lookDirYaw.getAxis().isHorizontal()
                    ? lookDirYaw
                    : (lookDirPitch != null && lookDirPitch.getAxis().isHorizontal()
                    ? lookDirPitch
                    : Direction.UP))
                    : side;

            Vec3d hitVec = !termsOfUse
                    ? Vec3d.ofCenter(target)
                    .add(Vec3d.of(side.getVector()).multiply(0.5))
                    .add(hitModifier.rotateY((direction.getPositiveHorizontalDegrees() + 90) % 360).multiply(0.5))
                    : hitModifier;


            if (orderlyStoreItem != null) {
                if (orderlyStoreItem.isEmpty()) {
                    SwitchItem.removeItem(orderlyStoreItem);
                } else {
                    SwitchItem.syncUseTime(orderlyStoreItem);
                }
            }

            if (PRINT_INTERVAL.getIntegerValue() >= 1 && lookDirYaw != null)
                Implementation.sendLookPacket(player, lookDirYaw, lookDirPitch);

            if (needSneak && !wasSneaking) {
                setShift(player, true);
            }
            else if (!needSneak && wasSneaking) {
                setShift(player, false);
            }

            if (PLACE_USE_PACKET.getBooleanValue()) {
                //#if MC >= 11904
                player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(
                        Hand.MAIN_HAND,
                        new BlockHitResult(hitVec, side, target, false),
                        ZxyUtils.getSequence()
                ));
                //#else
                //$$player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(
                //$$        Hand.MAIN_HAND,
                //$$        new BlockHitResult(hitVec, side, target, false)
                //$$));
                //#endif
            } else {
                ((IClientPlayerInteractionManager) client.interactionManager)
                        .rightClickBlock(target, side, hitVec);
            }

            if (needSneak && !wasSneaking)
                setShift(player, true);
            else if (!needSneak && wasSneaking)
                setShift(player, false);

            clearQueue();
        }

        public void setShift(ClientPlayerEntity player , boolean shift) {
            //#if MC > 12105
            //$$ PlayerInput input = new PlayerInput(player.input.playerInput.forward(), player.input.playerInput.backward(), player.input.playerInput.left(), player.input.playerInput.right(), player.input.playerInput.jump(), shift, player.input.playerInput.sprint());
            //$$ PlayerInputC2SPacket packet = new PlayerInputC2SPacket(input);
            //#else
            ClientCommandC2SPacket packet = new ClientCommandC2SPacket(player, shift ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY);
            //#endif
            player.networkHandler.sendPacket(packet);
        }

        public void clearQueue() {
            this.target = null;
            this.side = null;
            this.hitModifier = null;
            this.lookDirYaw = null;
            this.needSneak = false;
        }
    }
}
