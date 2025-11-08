package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.data.DataManager;

import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EasyPlaceProtocol;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
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
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.text.Text;
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
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.*;
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
    public static final MinecraftClient client = MinecraftClient.getInstance();
    public final PlacementGuide guide;
    public final Queue queue;
    public final BreakManager breakManager = BreakManager.instance();
    //强制循环半径
    public BlockPos basePos = null;
    public MyBox myBox;
    int printRange = PRINTER_RANGE.getIntegerValue();
    boolean yReverse = false;
    int tickRate = PRINTER_SPEED.getIntegerValue();
    List<String> fluidBlocklist =  new ArrayList<>();
    List<String> fluidList =  new ArrayList<>();
    List<String> fillBlocklist =  new ArrayList<>();
    private boolean needDelay;
    private int printPerTick = BLOCKS_PER_TICK.getIntegerValue();

    public static int packetTick;
    public static boolean updateChecked = false;
    public static BlockState requiredState;
    public static BlockState currentState;
    private int waitTicks;

    // 活塞修复
    public static boolean pistonNeedFix = false;

    private float workProgress = 0;

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
        if (ITERATOR_USE_TIME.getIntegerValue() != 0 && System.currentTimeMillis() > tickEndTime) return null;
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
                List<Item> list = Registries.ITEM.stream().filter(item -> equalsItemName(itemName, new ItemStack(item))).toList();
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
                List<Fluid> list = Registries.FLUID.stream().filter(item -> equalsBlockName(itemName, item.getDefaultState().getBlockState())).toList();
                fluidModeList.addAll(list);
            }
            fluidArray = fluidModeList.toArray(new Fluid[0]);
        }

        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            if (BLOCKS_PER_TICK.getIntegerValue() != 0 && printPerTick == 0) return;

            if (!TempData.xuanQuFanWeiNei_p(pos)) continue;
            // 跳过冷却中的位置
            if (placeCooldownList.containsKey(pos)) continue;
            placeCooldownList.put(pos, 0);

            FluidState fluidState = client.world.getBlockState(pos).getFluidState();
            if (Arrays.asList(fluidArray).contains(fluidState.getFluid())) {
                if (!FILL_FLOWING_FLUID.getBooleanValue() && !fluidState.isStill()) continue;
                if (playerHasAccessToItems(client.player, fluidItemsArray)) {
                    switchToItems(client.player, fluidItemsArray);
                    new PlacementGuide.Action().queueAction(queue, pos, Direction.UP, false);
                    if (tickRate == 0) {
                        queue.sendQueue(client.player);
                        if (BLOCKS_PER_TICK.getIntegerValue() != 0) printPerTick--;
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
                List<Item> list = Registries.ITEM.stream().filter(item -> equalsItemName(itemName, new ItemStack(item))).toList();
                fillModeItemList.addAll(list);
            }
            fillItemsArray = fillModeItemList.toArray(new Item[0]);
        }

        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            if (BLOCKS_PER_TICK.getIntegerValue() != 0 && printPerTick == 0) return;

            if (!TempData.xuanQuFanWeiNei_p(pos)) continue;
            // 跳过冷却中的位置
            if (placeCooldownList.containsKey(pos)) continue;
            placeCooldownList.put(pos, 0);

            var currentState = client.world.getBlockState(pos);
            if (currentState.isAir() || (currentState.getBlock() instanceof FluidBlock) || REPLACEABLE_LIST.getStrings().stream().anyMatch(s -> equalsBlockName(s, currentState))) {
                if (playerHasAccessToItems(client.player, fillItemsArray)) {
                    switchToItems(client.player, fillItemsArray);
                    new PlacementGuide.Action().setLookDirection(PlaceUtils.getFillModeFacing().getOpposite()).queueAction(queue, pos, PlaceUtils.getFillModeFacing(), false);
                    if (tickRate == 0) {
                        queue.sendQueue(client.player);
                        if (BLOCKS_PER_TICK.getIntegerValue() != 0) printPerTick--;
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
            if (BreakManager.breakRestriction(client.world.getBlockState(pos)) &&
                    breakManager.breakBlock(pos)) {
                requiredState = client.world.getBlockState(pos);
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
            ZxyUtils.actionBar("未安装Bedrock Miner模组/游戏版本小于1.21.6，无法破基岩！");
            return;
        }
        if (!BedrockUtils.isWorking()) BedrockUtils.toggle();
        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            if (client.player != null &&
                    (!PlaceUtils.canInteracted(pos) || isLimitedByTheNumberOfLayers(pos) || !TempData.xuanQuFanWeiNei_p(pos))) {
                continue;
            }
            BedrockUtils.addToBreakList(pos, client.world);
            // 原谅我使用硬编码plz 我真的不想写太多的优化了555
            placeCooldownList.put(pos, -400);
        }
    }

    public void tick() {
        if (shulkerCooldown > 0) {
            shulkerCooldown--;
        }

        if (waitTicks > 0) {
            waitTicks--;
        }

        if (Statistics.loadBedrockMiner) {
            if (isBedrockMode() || !PRINT_SWITCH.getBooleanValue()) {
                if (BedrockUtils.isWorking()) BedrockUtils.toggle();
            }
        }

        Iterator<Map.Entry<BlockPos, Integer>> iterator = placeCooldownList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            int newValue = entry.getValue() + 1;
            if (newValue >= PLACE_COOLDOWN.getIntegerValue()) {
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
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        ClientWorld world = client.world;
        // 预载常用配置值
        printRange = PRINTER_RANGE.getIntegerValue();
        tickRate = PRINTER_SPEED.getIntegerValue();
        printPerTick = BLOCKS_PER_TICK.getIntegerValue();
        yReverse = false;

        // 如果正在处理打开的容器/处理远程交互和快捷潜影盒/破坏方块列表有东西，则直接返回
        if (isOpenHandler || switchItem() || BreakManager.hasTargets()) {
            return;
        }

        //从这里才算作开始
        tickStartTime = System.currentTimeMillis();
        tickEndTime = tickStartTime + LitematicaMixinMod.ITERATOR_USE_TIME.getIntegerValue();

        // 优先执行队列中的点击操作
        if (tickRate != 0) {
            queue.sendQueue(player);
            if ((tickStartTime / 50) % tickRate != 0) {
                return;
            }
        }

        if (waitTicks > 0) return;

        // 0tick修复
        if (needDelay) {
            queue.sendQueue(player);
            needDelay = false;
        }

        if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.MULTI)) {
            boolean multiBreakBooleanValue = MULTI_BREAK.getBooleanValue();
            if (LitematicaMixinMod.MINE.getBooleanValue()) {
                yReverse = true;
                mineMode();
                if(multiBreakBooleanValue) return;
            }
            if (LitematicaMixinMod.FLUID.getBooleanValue()) {
                fluidMode();
                if(multiBreakBooleanValue) return;
            }
            if (LitematicaMixinMod.FILL.getBooleanValue()) {
                fillMode();
                if(multiBreakBooleanValue) return;
            }
            if (LitematicaMixinMod.BEDROCK.getBooleanValue()) {
                yReverse = true;
                bedrockMode();
                if(multiBreakBooleanValue) return;
            }
        } else if (PRINTER_MODE.getOptionListValue() instanceof State.PrintModeType modeType && modeType != PRINTER) {
            switch (modeType){
                case MINE -> {
                    yReverse = true;
                    mineMode();
                }
                case FLUID -> fluidMode();
                case FILL -> fillMode();
                case BEDROCK -> {
                    yReverse = true;
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
            if (BLOCKS_PER_TICK.getIntegerValue() != 0 && printPerTick == 0) return;
            // 是否在渲染层内
            if (!DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;
            if (!isSchematicBlock(pos)) continue;

            requiredState = schematic.getBlockState(pos);
            currentState = world.getBlockState(pos);

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
            placeCooldownList.put(pos, 0);

            // 检查放置条件
            PlacementGuide.Action action = guide.getAction(world, schematic, pos);
            if (action == null) continue;

            if (LitematicaMixinMod.FALLING_CHECK.getBooleanValue() && requiredState.getBlock() instanceof FallingBlock) {
                //检查方块下面方块是否正确，否则跳过放置
                BlockPos downPos = pos.down();
                if (world.getBlockState(downPos) != schematic.getBlockState(downPos)) {
                    client.inGameHud.setOverlayMessage(Text.of("方块 " + requiredState.getBlock().getName().getString() + " 下方方块不相符，跳过放置"), false);
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

            Item[] reqItems = action.getRequiredItems(requiredState.getBlock());
            if (playerHasAccessToItems(player, reqItems)) {
                boolean useShift = (Implementation.isInteractive(world.getBlockState(pos.offset(side)).getBlock()) && !(action instanceof PlacementGuide.ClickAction))
                        || FORCED_SNEAK.getBooleanValue()
                        || action.useShift;

                if (needDelay) continue;
                if (!switchToItems(player, reqItems)) return;

                action.queueAction(queue, pos, side, useShift);

                Vec3d hitModifier = usePrecisionPlacement(pos, requiredState);
                if(hitModifier != null){
                    queue.hitModifier = hitModifier;
                    queue.termsOfUse = true;
                }

                if (action.getLookDirection() != null)
                    sendLook(player, action.getLookDirection(), action.getLookDirectionPitch());

                var block = requiredState.getBlock();
                if (block instanceof PistonBlock) {
                    pistonNeedFix = true;
                }
                waitTicks = action.getWaitTick();

                if (tickRate == 0) {
                    if (block instanceof PistonBlock ||
                            block instanceof ObserverBlock ||
                            block instanceof DispenserBlock ||
                            block instanceof BarrelBlock ||
                            block instanceof WallBannerBlock
                            //#if MC >= 12101
                            || block instanceof CrafterBlock
                        //#endif
                            || block instanceof WallSignBlock
                            || block instanceof GrindstoneBlock
                    ) {
                        needDelay = true;
                        return;
                    }

                    queue.sendQueue(player);
                    if (BLOCKS_PER_TICK.getIntegerValue() != 0) printPerTick--;
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
            BlockState currentState = client.world.getBlockState(pos);
            totalCount++;

            if (isPrinterMode()) {
                BlockState requiredState = schematic.getBlockState(pos);
                if (requiredState.isAir()) {
                    totalCount--;
                    continue;
                }
                if (currentState.getBlock().getDefaultState().equals(requiredState.getBlock().getDefaultState())) {
                    finishedCount++;
                }
            }
            if (isFluidMode() && !(currentState.getBlock() instanceof FluidBlock)) {
                finishedCount++;
            }
            if (isFillMode() && Arrays.asList(fillItemsArray).contains(currentState.getBlock().asItem())) {
                finishedCount++;
            }
            if (isMineMode() && currentState.isAir()) {
                finishedCount++;
            }
        }
        workProgress = totalCount == 0 ? workProgress : (float) finishedCount / totalCount;
        return workProgress;
    }

    public Vec3d usePrecisionPlacement(BlockPos pos,BlockState stateSchematic) {
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
        if (items == null) items = List.of(Items.AIR).toArray(Item[]::new);
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
                InventoryUtils.setPickedItemToHand(stack, client);
//                ZxyUtils.setPickedItemToHand(
//                        player.getInventory().getSlotWithStack(stack),
//                        stack);
                client.interactionManager.clickCreativeStack(client.player.getStackInHand(Hand.MAIN_HAND), 36 + inv.selectedSlot);
                return true;
            }
        }
        return true;
    }


    public boolean swapHandWithSlot(ClientPlayerEntity player, int slot) {
        ItemStack stack = player.getInventory().getStack(slot);
        int slotNum = client.player.getInventory().getSlotWithStack(stack);
        InventoryUtils.setPickedItemToHand(stack, client);
        return true;
        //return ZxyUtils.setPickedItemToHand(slotNum, stack);
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

        public void sendQueue(ClientPlayerEntity player) {
            if (target == null || side == null || hitModifier == null) return;

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

            boolean wasSneak = player.isSneaking();

            if (needSneak && !wasSneak) setShift(player, true);
            else if (!needSneak && wasSneak) setShift(player, false);

            if (PRINTER_SPEED.getIntegerValue() >= 1 && lookDirYaw != null) {
                Implementation.sendLookPacket(player, lookDirYaw, lookDirPitch);
            }


            if (PLACE_USE_PACKET.getBooleanValue()) {
                //#if MC >= 11904
                player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(
                        Hand.MAIN_HAND,
                        new BlockHitResult(hitVec, side, target, false),
                        ZxyUtils.getSequence()
                ));
                //#else
                //$$ player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(
                //$$         Hand.MAIN_HAND,
                //$$         new BlockHitResult(hitVec, side, target, false)
                //$$ ));
                //#endif
            } else {
                ((IClientPlayerInteractionManager) client.interactionManager)
                        .rightClickBlock(target, side, hitVec);
            }

            if (needSneak && !wasSneak) setShift(player, false);
            else if (!needSneak && wasSneak) setShift(player, true);

            clearQueue();
        }

        public void setShift(ClientPlayerEntity player , boolean shift) {
            //#if MC > 12105
            //$$ PlayerInput input = new PlayerInput(player.input.playerInput.forward(), player.input.playerInput.backward(), player.input.playerInput.left(), player.input.playerInput.right(), player.input.playerInput.jump(), shift, player.input.playerInput.sprint());
            //$$ PlayerInputC2SPacket packet = new PlayerInputC2SPacket(input);
            //#else
            ClientCommandC2SPacket packet = new ClientCommandC2SPacket(player, shift ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY);
            //#endif
            player.setSneaking(shift);
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
