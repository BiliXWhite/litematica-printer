package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.data.DataManager;

import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EasyPlaceProtocol;
import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.bilixwhite.utils.BedrockUtils;
import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.bilixwhite.utils.StringUtils;
import me.aleksilassila.litematica.printer.interfaces.IClientPlayerInteractionManager;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.bilixwhite.BreakManager;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.overwrite.MyBox;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import net.minecraft.world.level.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
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
import static me.aleksilassila.litematica.printer.LitematicaPrinterMod.*;
import static me.aleksilassila.litematica.printer.printer.State.PrintModeType.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.*;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.*;

import net.minecraft.core.registries.BuiltInRegistries;

//#if MC > 12105
import net.minecraft.world.entity.player.Input;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
//#else
//$$ import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
//#endif

public class Printer extends PrinterUtils {
    public static HashSet<Item> fluidModeItemList = new HashSet<>();
    public static HashSet<Fluid> fluidModeList = new HashSet<>();
    public static HashSet<Item> fillModeItemList = new HashSet<>();
    public static Item[] fluidItemsArray = new Item[0];
    public static Fluid[] fluidArray = new Fluid[0];
    public static Item[] fillItemsArray = new Item[0];
    public static boolean printerMemorySync = false;
    public static BlockPos easyPos = null;
    public static Map<BlockPos, Integer> placeCooldownList = new HashMap<>();
    static ItemStack orderlyStoreItem; //有序存放临时存储
    private static Printer INSTANCE = null;
    public int shulkerCooldown = 0;
    public static long tickStartTime;
    public static long tickEndTime;

    @NotNull
    public static final Minecraft client = Minecraft.getInstance();
    public final PlacementGuide guide;
    public final Queue queue;
    public final BreakManager breakManager = BreakManager.instance();
    //强制循环半径
    public BlockPos basePos = null;
    public MyBox myBox;
    int printRange = PRINTER_RANGE.getIntegerValue();
    boolean printerYAxisReverse = false;
    int tickRate = PRINTER_SPEED.getIntegerValue();
    List<String> fluidBlocklist = new ArrayList<>();
    List<String> fluidList = new ArrayList<>();
    List<String> fillBlocklist = new ArrayList<>();
    private boolean needDelay;
    private int printerWorkingCountPerTick = BLOCKS_PER_TICK.getIntegerValue();
    private int waitTicks = 0;

    public static int packetTick;
    public static boolean updateChecked = false;
    public static BlockState requiredState;


    // 活塞修复
    public static boolean pistonNeedFix = false;

    private float workProgress = 0;

    private Printer(@NotNull Minecraft client) {

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
        if (ITERATOR_USE_TIME.getIntegerValue() != 0 && System.currentTimeMillis() > tickEndTime) return null;
        LocalPlayer player = client.player;
        if (player == null) return null;

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
        myBox.initIterator();
        myBox.setIterationMode(ITERATION_ORDER.getOptionListValue());
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

        // 如果没有找到符合条件的位置，重置 basePos 并返回 null
        basePos = null;
        return null;
    }

    void fluidMode() {
        requiredState = null;
        if (!FLUID_BLOCK_LIST.getStrings().equals(fluidBlocklist)) {
            fluidBlocklist.clear();
            fluidBlocklist.addAll(FLUID_BLOCK_LIST.getStrings());
            if (FLUID_BLOCK_LIST.getStrings().isEmpty()) return;
            fluidModeItemList.clear();
            for (String itemName : fluidBlocklist) {
                List<Item> list = BuiltInRegistries.ITEM.stream().filter(item -> equalsItemName(itemName, new ItemStack(item))).toList();
                fluidModeItemList.addAll(list);
            }
            fluidItemsArray = fluidModeItemList.toArray(new Item[0]);
        }

        if (!FLUID_LIST.getStrings().equals(fluidList)) {
            fluidList.clear();
            fluidList.addAll(FLUID_LIST.getStrings());
            if (FLUID_LIST.getStrings().isEmpty()) return;
            fluidModeList.clear();
            for (String itemName : fluidList) {
                List<Fluid> list = BuiltInRegistries.FLUID.stream().filter(item -> equalsBlockName(itemName, item.defaultFluidState().createLegacyBlock())).toList();
                fluidModeList.addAll(list);
            }
            fluidArray = fluidModeList.toArray(new Fluid[0]);
        }

        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            if (BLOCKS_PER_TICK.getIntegerValue() != 0 && printerWorkingCountPerTick == 0) return;

            if (!TempData.xuanQuFanWeiNei_p(pos)) continue;
            // 跳过冷却中的位置
            if (placeCooldownList.containsKey(pos)) continue;
            placeCooldownList.put(pos, PLACE_COOLDOWN.getIntegerValue());

            FluidState fluidState = client.level.getBlockState(pos).getFluidState();
            if (Arrays.asList(fluidArray).contains(fluidState.getType())) {
                if (!FILL_FLOWING_FLUID.getBooleanValue() && !fluidState.isSource()) continue;
                if (switchToItems(client.player, fluidItemsArray)) {
                    new PlacementGuide.Action().queueAction(queue, pos, Direction.UP, false);
                    if (tickRate == 0) {
                        queue.sendQueue(client.player);
                        if (BLOCKS_PER_TICK.getIntegerValue() != 0) printerWorkingCountPerTick--;
                        continue;
                    }
                    return;
                }
            }
        }
    }

    void fillMode() {
        requiredState = null;
        if (!FILL_BLOCK_LIST.getStrings().equals(fillBlocklist)) {
            fillBlocklist.clear();
            fillBlocklist.addAll(FILL_BLOCK_LIST.getStrings());
            if (FILL_BLOCK_LIST.getStrings().isEmpty()) return;
            fillModeItemList.clear();
            for (String itemName : fillBlocklist) {
                List<Item> list = BuiltInRegistries.ITEM.stream().filter(item -> equalsItemName(itemName, new ItemStack(item))).toList();
                fillModeItemList.addAll(list);
            }
            fillItemsArray = fillModeItemList.toArray(new Item[0]);
        }

        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            if (BLOCKS_PER_TICK.getIntegerValue() != 0 && printerWorkingCountPerTick == 0) return;

            if (!TempData.xuanQuFanWeiNei_p(pos)) continue;
            // 跳过冷却中的位置
            if (placeCooldownList.containsKey(pos)) continue;
            placeCooldownList.put(pos, PLACE_COOLDOWN.getIntegerValue());

            var currentState = client.level.getBlockState(pos);
            if (currentState.isAir() || (currentState.getBlock() instanceof LiquidBlock) || REPLACEABLE_LIST.getStrings().stream().anyMatch(s -> equalsBlockName(s, currentState))) {
                if (switchToItems(client.player, fillItemsArray)) {
                    new PlacementGuide.Action().setLookDirection(PlaceUtils.getFillModeFacing().getOpposite()).queueAction(queue, pos, PlaceUtils.getFillModeFacing(), false);
                    if (tickRate == 0) {
                        queue.sendQueue(client.player);
                        if (BLOCKS_PER_TICK.getIntegerValue() != 0) printerWorkingCountPerTick--;
                        continue;
                    }
                    return;
                }
            }
        }
    }


    BlockPos breakPos = null;

    void mineMode() {
        BlockPos pos;
        while ((pos = breakPos == null ? getBlockPos() : breakPos) != null) {
            if (BreakManager.breakRestriction(client.level.getBlockState(pos)) &&
                    breakManager.breakBlock(pos)) {
                requiredState = client.level.getBlockState(pos);
                breakPos = pos;
                return;
            }
            // 清空临时位置
            breakPos = null;
        }
    }

    public void bedrockMode() {
        if (client.player.isCreative()) {
            ZxyUtils.actionBar("创造模式无法使用破基岩模式！");
            return;
        }
        if (!Statistics.loadBedrockMiner) {
            ZxyUtils.actionBar("未安装Bedrock Miner模组/游戏版本小于1.19，无法破基岩！");
            return;
        }
        if (!BedrockUtils.isWorking()) {
            BedrockUtils.setWorking(true);
        }
        if (BedrockUtils.isBedrockMinerFeatureEnable()) {   // 限制原功能(手动点击或使用方块：添加、开关)
            BedrockUtils.setBedrockMinerFeatureEnable(false);
        }
        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            if (client.player != null &&
                    (!PlaceUtils.canInteracted(pos) || isLimitedByTheNumberOfLayers(pos) || !TempData.xuanQuFanWeiNei_p(pos))) {
                continue;
            }
            BedrockUtils.addToBreakList(pos, client.level);
            // 原谅我使用硬编码plz 我真的不想写太多的优化了555
            placeCooldownList.put(pos, 100);
        }
    }

    public void tick() {
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

    public void printerTick() {
        if (LAG_CHECK.getBooleanValue()) {
            if (packetTick > 20)
                return;
            packetTick++;
        }
        LocalPlayer player = client.player;
        if (player == null) return;
        ClientLevel world = client.level;
        // 预载常用配置值
        printRange = PRINTER_RANGE.getIntegerValue();
        tickRate = PRINTER_SPEED.getIntegerValue();
        printerWorkingCountPerTick = BLOCKS_PER_TICK.getIntegerValue();
        printerYAxisReverse = false;

        // 如果正在处理打开的容器/处理远程交互和快捷潜影盒/破坏方块列表有东西，则直接返回
        if (isOpenHandler || switchItem() || BreakManager.hasTargets()) {
            return;
        }

        //从这里才算作开始
        tickStartTime = System.currentTimeMillis();
        tickEndTime = tickStartTime + LitematicaPrinterMod.ITERATOR_USE_TIME.getIntegerValue();

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

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        if (MODE_SWITCH.getOptionListValue().equals(State.ModeType.MULTI)) {
            boolean multiBreakBooleanValue = MULTI_BREAK.getBooleanValue();
            if (LitematicaPrinterMod.MINE.getBooleanValue()) {
                printerYAxisReverse = true;
                mineMode();
                if (multiBreakBooleanValue) return;
            }
            if (LitematicaPrinterMod.FLUID.getBooleanValue()) {
                fluidMode();
                if (multiBreakBooleanValue) return;
            }
            if (LitematicaPrinterMod.FILL.getBooleanValue()) {
                fillMode();
                if (multiBreakBooleanValue) return;
            }
            if (LitematicaPrinterMod.BEDROCK.getBooleanValue()) {
                printerYAxisReverse = true;
                bedrockMode();
                if (multiBreakBooleanValue) return;
            }
        } else if (PRINTER_MODE.getOptionListValue() instanceof State.PrintModeType modeType && modeType != PRINTER) {
            switch (modeType) {
                case MINE -> {
                    printerYAxisReverse = true;
                    mineMode();
                }
                case FLUID -> fluidMode();
                case FILL -> fillMode();
                case BEDROCK -> {
                    printerYAxisReverse = true;
                    bedrockMode();
                }
            }
            return;
        }

        // 遍历当前区域内所有符合条件的位置
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return;
        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            // 检查每刻放置方块是否超出限制
            if (BLOCKS_PER_TICK.getIntegerValue() != 0 && printerWorkingCountPerTick == 0) return;
            // 是否在渲染层内
            if (!DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;
            if (!isSchematicBlock(pos)) continue;

            requiredState = schematic.getBlockState(pos);

            // 检查放置跳过列表
            if (PUT_SKIP.getBooleanValue()) {
                Set<String> skipSet = new HashSet<>(PUT_SKIP_LIST.getStrings()); // 转换为 HashSet
                if (skipSet.stream().anyMatch(s -> Filters.equalsName(s, requiredState))) {
                    continue;
                }
            }

            // 跳过冷却中的位置
            if (placeCooldownList.containsKey(pos)) continue;
            // 放置冷却
            placeCooldownList.put(pos, PLACE_COOLDOWN.getIntegerValue());

            // 检查放置条件
            PlacementGuide.Action action = guide.getAction(world, schematic, pos);
            if (action == null) continue;

            if (LitematicaPrinterMod.FALLING_CHECK.getBooleanValue() && requiredState.getBlock() instanceof FallingBlock) {
                //检查方块下面方块是否正确，否则跳过放置
                BlockPos downPos = pos.below();
                if (world.getBlockState(downPos) != schematic.getBlockState(downPos)) {
                    client.gui.setOverlayMessage(Component.nullToEmpty("方块 " + requiredState.getBlock().getName().getString() + " 下方方块不相符，跳过放置"), false);
                    continue;
                }
            }

            Direction side = action.getValidSide(world, pos);
            if (side == null) continue;

            // 调试输出
            if (DEBUG_OUTPUT.getBooleanValue()) {
                StringUtils.info("方块名: " + requiredState.getBlock().getName().getString());
                StringUtils.info("方块类名: " + requiredState.getBlock().getClass().getName());
                StringUtils.info("方块ID: " + requiredState.getBlock().getClass().getName());
            }

            if (needDelay) continue;

            Item[] reqItems = action.getRequiredItems(requiredState.getBlock());
            if (switchToItems(player, reqItems)) {
                boolean useShift = (Implementation.isInteractive(world.getBlockState(pos.relative(side)).getBlock()) && !(action instanceof PlacementGuide.ClickAction))
                        || FORCED_SNEAK.getBooleanValue()
                        || action.useShift;


                action.queueAction(queue, pos, side, useShift);

                Vec3 hitModifier = usePrecisionPlacement(pos, requiredState);
                if (hitModifier != null) {
                    queue.hitModifier = hitModifier;
                    queue.termsOfUse = true;
                }

                if (action.getLookDirection() != null)
                    sendLook(player, action.getLookDirection(), action.getLookDirectionPitch());

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
                        needDelay = true;
                        return;
                    }

                    queue.sendQueue(player);
                    if (BLOCKS_PER_TICK.getIntegerValue() != 0) printerWorkingCountPerTick--;
                    continue;
                }
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
            if (isFillMode() && Arrays.asList(fillItemsArray).contains(currentState.getBlock().asItem())) {
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
                if (inv.getItem(i).getItem() == item && inv.getItem(i).getCount() > 0) {
                    slot = i;
                    break;
                }
            }
            if (slot != -1) {
                orderlyStoreItem = inv.getItem(slot);
                var stack = new ItemStack(item);
                return PlaceUtils.setPickedItemToHand(stack, client);
            }
            lastNeedItemList.add(item);
        }
        return false;
    }


    public void swapHandWithSlot(LocalPlayer player, int slot) {
        ItemStack stack = player.getInventory().getItem(slot);
        PlaceUtils.setPickedItemToHand(stack, client);
    }

    public void sendLook(LocalPlayer player, Direction directionYaw, Direction directionPitch) {
        if (directionYaw != null || directionPitch != null) {
            Implementation.sendLookPacket(player, directionYaw, directionPitch);
        }
        queue.lookDirYaw = directionYaw;
        queue.lookDirPitch = directionPitch;
    }

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

    public static class Queue {
        final Printer printerInstance;
        public BlockPos target;
        public Direction side;
        public Vec3 hitModifier;
        public boolean needSneak = false;
        public boolean termsOfUse = false;
        public Direction lookDirYaw = null;
        public Direction lookDirPitch = null;

        public Queue(Printer printerInstance) {
            this.printerInstance = printerInstance;
        }

        public void queueClick(@NotNull BlockPos target, @NotNull Direction side, @NotNull Vec3 hitModifier, boolean needSneak) {
            if (PRINTER_SPEED.getIntegerValue() != 0) {
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

        public void sendQueue(LocalPlayer player) {
            if (target == null || side == null || hitModifier == null) return;

            Direction direction = side.getAxis() == Direction.Axis.Y
                    ? (lookDirYaw != null && lookDirYaw.getAxis().isHorizontal()
                    ? lookDirYaw
                    : (lookDirPitch != null && lookDirPitch.getAxis().isHorizontal()
                    ? lookDirPitch
                    : Direction.UP))
                    : side;

            Vec3 hitVec = !termsOfUse
                    ? Vec3.atCenterOf(target)
                    .add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.5))
                    .add(hitModifier.yRot((direction.toYRot() + 90) % 360).scale(0.5))
                    : hitModifier;


            if (orderlyStoreItem != null) {
                if (orderlyStoreItem.isEmpty()) {
                    SwitchItem.removeItem(orderlyStoreItem);
                } else {
                    SwitchItem.syncUseTime(orderlyStoreItem);
                }
            }

            boolean wasSneak = player.isShiftKeyDown();

            if (needSneak && !wasSneak) setShift(player, true);
            else if (!needSneak && wasSneak) setShift(player, false);

            if (PRINTER_SPEED.getIntegerValue() >= 1 && lookDirYaw != null) {
                Implementation.sendLookPacket(player, lookDirYaw, lookDirPitch);
            }


            if (PLACE_USE_PACKET.getBooleanValue()) {
                //#if MC >= 11904
                player.connection.send(new ServerboundUseItemOnPacket(
                        net.minecraft.world.InteractionHand.MAIN_HAND,
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
                ((IClientPlayerInteractionManager) client.getConnection())
                        .rightClickBlock(target, side, hitVec);
            }

            if (needSneak && !wasSneak) setShift(player, false);
            else if (!needSneak && wasSneak) setShift(player, true);

            clearQueue();
        }

        public void setShift(LocalPlayer player, boolean shift) {
            //#if MC > 12105
            Input input = new Input(player.input.keyPresses.forward(), player.input.keyPresses.backward(), player.input.keyPresses.left(), player.input.keyPresses.right(), player.input.keyPresses.jump(), shift, player.input.keyPresses.sprint());
            ServerboundPlayerInputPacket packet = new ServerboundPlayerInputPacket(input);
            //#else
            //$$ ServerboundPlayerCommandPacket packet = new ServerboundPlayerCommandPacket(player, shift ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY);
            //#endif
            player.setShowDeathScreen(shift);
            player.connection.send(packet);
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
