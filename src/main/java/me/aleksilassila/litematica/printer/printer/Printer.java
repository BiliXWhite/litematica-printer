package me.aleksilassila.litematica.printer.printer;

import com.google.common.collect.Lists;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.*;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.interfaces.IClientPlayerInteractionManager;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.mixin.masa.WorldUtilsAccessor;
import me.aleksilassila.litematica.printer.printer.bedrockUtils.BreakingFlowController;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.overwrite.MyBox;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.List;

import static fi.dy.masa.litematica.selection.SelectionMode.NORMAL;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST;
import static fi.dy.masa.tweakeroo.tweaks.PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION;
import static me.aleksilassila.litematica.printer.LitematicaMixinMod.*;
import static me.aleksilassila.litematica.printer.mixin.masa.MixinInventoryFix.getEmptyPickBlockableHotbarSlot;
import static me.aleksilassila.litematica.printer.mixin.masa.MixinInventoryFix.getPickBlockTargetSlot;
import static me.aleksilassila.litematica.printer.printer.Printer.TempData.*;
import static me.aleksilassila.litematica.printer.printer.State.PrintModeType.*;
import static me.aleksilassila.litematica.printer.printer.bedrockUtils.BreakingFlowController.cachedTargetBlockList;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsBlockName;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsItemName;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.*;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.openIng;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;

//#if MC >= 12001
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.SearchItem;
//#else
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.Memory;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase;
//$$ import net.minecraft.util.Identifier;
//#endif

//#if MC < 11904
//$$ import net.minecraft.util.Identifier;
//$$ import net.minecraft.command.argument.ItemStringReader;
//$$ import com.mojang.brigadier.StringReader;
//$$ import net.minecraft.util.registry.RegistryKey;
//$$ import net.minecraft.util.registry.Registry;
//#else if MC = 11904
//$$ import net.minecraft.registry.RegistryKey;
//$$ import net.minecraft.registry.RegistryKeys;
//#else
import net.minecraft.registry.Registries;
//#endif

//#if MC < 11900
//$$ import fi.dy.masa.malilib.util.SubChunkPos;
//#endif

//#if MC <= 12004
//$$import static fi.dy.masa.malilib.util.InventoryUtils.areStacksEqual;
//#else
import static fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreNbt;
//#endif

public class Printer extends PrinterUtils {
    public static HashSet<Item> remoteItem = new HashSet<>();
    public static HashSet<Item> fluidList = new HashSet<>();
    public static boolean printerMemorySync = false;
    public static BlockPos easyPos = null;
    public static boolean isOpenHandler = false;
    public static boolean updateChecked = false;
    static int tick = 0;
    static BlockPos breakTargetBlock = null;
    static int startTick = -1;
    static Map<BlockPos, Integer> skipPosMap = new HashMap<>();
    static int shulkerBoxSlot = -1;
    static ItemStack yxcfItem; //有序存放临时存储
    private static Printer INSTANCE = null;
    private int shulkerCooldown = 0;

    @NotNull
    public final MinecraftClient client;
    public final PlacementGuide guide;
    public final Queue queue;
    //强制循环半径
    public boolean resetRange = true;
    public boolean usingRange = true;
    public BlockPos basePos = null;
    public MyBox myBox;
    int printRange;
    boolean yDegression = false;
    BlockPos tempPos = null;
    int tickRate;
    boolean pendingItemSwitch = false;
    List<String> fluidBlocklist;
    long startTime;
    private boolean needDelay;
    private Item[] delayedItem;

    private Printer(@NotNull MinecraftClient client) {
        this.client = client;

        this.guide = new PlacementGuide(client);
        this.queue = new Queue(this);

        INSTANCE = this;
    }

    public static @NotNull Printer getPrinter() {
        if (INSTANCE == null) {
            INSTANCE = new Printer(ZxyUtils.client);
        }
        return INSTANCE;
    }

    public static boolean waJue(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        BlockState currentState = world.getBlockState(pos);
        Block block = currentState.getBlock();
        if (canBreakBlock(pos)) {
            client.interactionManager.updateBlockBreakingProgress(pos, Direction.DOWN);
            client.interactionManager.cancelBlockBreaking();
            return world.getBlockState(pos).isOf(block);
        }
        return false;
    }

    public static boolean canBreakBlock(BlockPos pos) {
        MinecraftClient client = ZxyUtils.client;
        ClientWorld world = client.world;
        BlockState currentState = world.getBlockState(pos);
        return !currentState.isAir() &&
                !currentState.isOf(Blocks.AIR) &&
                !currentState.isOf(Blocks.CAVE_AIR) &&
                !currentState.isOf(Blocks.VOID_AIR) &&
                !(currentState.getBlock().getHardness() == -1) &&
                !(currentState.getBlock() instanceof FluidBlock) &&
                !client.player.isBlockBreakingRestricted(client.world, pos, client.interactionManager.getCurrentGameMode());
    }

    public static BlockPos excavateBlock(BlockPos pos) {
        if (!canInteracted(pos)) {
            breakTargetBlock = null;
            return null;
        }
        //一个游戏刻挖一次就好
        if (startTick == tick) {
            return null;
        } else if (breakTargetBlock != null) {
            if (!Printer.waJue(breakTargetBlock)) {
                BlockPos breakTargetBlock1 = breakTargetBlock;
                breakTargetBlock = null;
                return breakTargetBlock1;
            } else return null;
        }
        startTick = tick;
        breakTargetBlock = pos;
        return null;
    }

    static boolean breakRestriction(BlockState blockState, BlockPos pos) {
        if (EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.TWEAKEROO)) {
            if (!FabricLoader.getInstance().isModLoaded("tweakeroo")) return true;
//            return isPositionAllowedByBreakingRestriction(pos,Direction.UP);
            UsageRestriction.ListType listType = BLOCK_TYPE_BREAK_RESTRICTION.getListType();
            if (listType == UsageRestriction.ListType.BLACKLIST) {
                return BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> equalsBlockName(string, blockState, pos));
            } else if (listType == UsageRestriction.ListType.WHITELIST) {
                return BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST.getStrings().stream()
                        .anyMatch(string -> equalsBlockName(string, blockState, pos));
            } else {
                return true;
            }
        } else {
            IConfigOptionListEntry optionListValue = EXCAVATE_LIMIT.getOptionListValue();
            if (optionListValue == UsageRestriction.ListType.BLACKLIST) {
                return EXCAVATE_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> equalsBlockName(string, blockState, pos));
            } else if (optionListValue == UsageRestriction.ListType.WHITELIST) {
                return EXCAVATE_WHITELIST.getStrings().stream()
                        .anyMatch(string -> equalsBlockName(string, blockState, pos));
            } else {
                return true;
            }
        }
    }

    static boolean isLimitedByTheNumberOfLayers(BlockPos pos) {
        return LitematicaMixinMod.RENDER_LAYER_LIMIT.getBooleanValue() && !DataManager.getRenderLayerRange().isPositionWithinRange(pos);
    }

    public static int bedrockModeRange() {
        return LitematicaMixinMod.RANGE_MODE.getOptionListValue() == State.ListType.SPHERE ? getRage() : 6;
    }

    public static boolean bedrockModeTarget(BlockState block) {
//        return LitematicaMixinMod.BEDROCK_LIST.getStrings().stream().anyMatch(string -> Registries.BLOCK.getId(block.getBlock()).toString().contains(string));
        return LitematicaMixinMod.BEDROCK_LIST.getStrings().stream().anyMatch(string -> Filters.equalsName(string, block));
    }

    /**
     * 判断给定的位置是否属于当前加载的图纸结构范围内。
     *
     * <p>
     * 该方法通过从数据管理器中获取结构放置管理器，然后查找与给定位置相交的所有图纸结构部分，
     * 如果其中任一部分包含该位置，则返回 <code>true</code>，否则返回 <code>false</code>。
     * </p>
     *
     * @param offset 要检测的方块位置
     * @return 如果位置属于图纸结构的一部分，则返回 true，否则返回 false
     */
    public static boolean isSchematicBlock(BlockPos offset) {
        SchematicPlacementManager schematicPlacementManager = DataManager.getSchematicPlacementManager();
        //#if MC < 11900
        //$$ List<SchematicPlacementManager.PlacementPart> allPlacementsTouchingChunk = schematicPlacementManager.getAllPlacementsTouchingSubChunk(new SubChunkPos(offset));
        //#else
        List<SchematicPlacementManager.PlacementPart> allPlacementsTouchingChunk = schematicPlacementManager.getAllPlacementsTouchingChunk(offset);
        //#endif

        for (SchematicPlacementManager.PlacementPart placementPart : allPlacementsTouchingChunk) {
            if (placementPart.getBox().containsPos(offset)) {
                return true;
            }
        }
        return false;
    }

    BlockPos getBlockPos() {
        // 获取当前玩家实例
        ClientPlayerEntity player = client.player;
        if (player == null) return null;

        // 获取玩家当前位置
        BlockPos playerPos = player.getBlockPos();

        // 如果 basePos 为空，则初始化为玩家当前位置，并扩展 myBox 范围
        if (basePos == null) {
            basePos = playerPos;
            myBox = new MyBox(basePos).expand(printRange);
        }

        // 检查玩家位置是否在 basePos 的一定范围内，如果不在则重置 basePos 并返回 null
        double threshold = printRange * 0.7;
        if (!basePos.isWithinDistance(playerPos, threshold)) {
            basePos = null;
            return null;
        }

        // 设置 myBox 的 y 轴增量方向，并初始化迭代器
        myBox.yIncrement = !yDegression;
        myBox.initIterator();

        // 根据玩家面向来选择迭代器的顺序
        myBox.setIterateZFirst(player.getHorizontalFacing().getAxis() == Direction.Axis.X);

        // 缓存配置值，减少循环中重复调用
        IConfigOptionListEntry rangeMode = LitematicaMixinMod.RANGE_MODE.getOptionListValue();
        boolean isSphere = rangeMode == State.ListType.SPHERE;
        Iterator<BlockPos> iterator = myBox.iterator;

        // 遍历 myBox 中的所有位置，找到符合条件的位置并返回
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (isSphere && !basePos.isWithinDistance(pos, printRange)) {
                continue;
            }
            return pos;
        }

        // 如果没有找到符合条件的位置，重置 basePos 并返回 null
        basePos = null;
        return null;
    }

    void fluidMode() {
        fluidBlocklist = LitematicaMixinMod.FLUID_BLOCK_LIST.getStrings();
        if (fluidBlocklist.isEmpty()) return;
        if (fluidList.isEmpty()) {
            for (String itemName : fluidBlocklist) {
                List<Item> list = Registries.ITEM.stream().filter(item -> equalsItemName(itemName, new ItemStack(item))).toList();
                fluidList.addAll(list);
            }
        }
        Item[] array = fluidList.toArray(new Item[0]);
        BlockPos pos;
        while ((pos = getBlockPos()) != null && client.world != null && client.player != null) {
            BlockState currentState = client.world.getBlockState(pos);
            if (client.player != null && !canInteracted(pos)) continue;
            if (!TempData.xuanQuFanWeiNei_p(pos)) continue;
            if (isLimitedByTheNumberOfLayers(pos)) continue;
            if (currentState.getFluidState().isOf(Fluids.LAVA) || currentState.getFluidState().isOf(Fluids.WATER)) {
                if (!switchToItems(client.player, array)) {
                    remoteItem.addAll(fluidList);
                    return;
                }

                ((IClientPlayerInteractionManager) client.interactionManager).rightClickBlock(pos, Direction.UP, Vec3d.ofCenter(pos));
                if (tickRate == 0) {
                    continue;
                }
                return;
            }
        }
    }

    void miningMode() {
        BlockPos pos;
        while ((pos = tempPos == null ? getBlockPos() : tempPos) != null) {
            if (client.player != null && (!canInteracted(pos) || isLimitedByTheNumberOfLayers(pos))) {
                if (tempPos == null) continue;
                tempPos = null;
                continue;
            }
            if (client.world != null &&
                    TempData.xuanQuFanWeiNei_p(pos) &&
                    breakRestriction(client.world.getBlockState(pos), pos) &&
                    waJue(pos)) {
                tempPos = pos;
                return;
            }
            tempPos = null;
        }
    }

    //此模式依赖bug运行 请勿随意修改
    public void bedrockMode() {

        BreakingFlowController.tick();
        int maxy = -9999;
        BlockPos pos;
        while ((pos = getBlockPos()) != null && client.world != null) {
            if (!ZxyUtils.bedrockCanInteracted(pos, getRage())) continue;
            if (isLimitedByTheNumberOfLayers(pos)) continue;
            BlockState currentState = client.world.getBlockState(pos);
            BlockPos finalPos = pos;
            if ((currentState.isOf(Blocks.PISTON) || (currentState.isOf(Blocks.SLIME_BLOCK) &&
                    cachedTargetBlockList.stream().allMatch(
                            targetBlock -> targetBlock.temppos.stream().noneMatch(
                                    blockPos -> blockPos.equals(finalPos)))))
                    && !bedrockModeTarget(client.world.getBlockState(pos.down())) && xuanQuFanWeiNei_p(pos, 3)) {
                BreakingFlowController.addPosList(pos);
            } else if (currentState.isOf(Blocks.PISTON_HEAD)) {
                switchToItems(client.player, new Item[]{Items.AIR, Items.DIAMOND_PICKAXE});
                ((IClientPlayerInteractionManager) client.interactionManager)
                        .rightClickBlock(pos, Direction.UP, Vec3d.ofCenter(pos));
            }

            if (TempData.xuanQuFanWeiNei_p(pos) &&
                    bedrockModeTarget(currentState) &&
                    ZxyUtils.bedrockCanInteracted(pos, getRage() - 1.5) &&
                    !bedrockModeTarget(client.world.getBlockState(pos.up()))) {
                if (maxy == -9999) maxy = pos.getY();
                if (pos.getY() < maxy) {
                    //重置迭代器 如果不重置 继续根据上次结束的y轴递减会出事
                    myBox.resetIterator();
                    return;
                }
                BreakingFlowController.addBlockPosToList(pos);
            }
        }
    }

    public boolean switchItem() {
        if (!remoteItem.isEmpty() && !isOpenHandler && !openIng && OpenInventoryPacket.key == null) {
            ClientPlayerEntity player = client.player;
            ScreenHandler sc = player.currentScreenHandler;
            if (!player.currentScreenHandler.equals(player.playerScreenHandler)) return false;
            //排除合成栏 装备栏 副手
            if (PRINT_CHECK.getBooleanValue() && sc.slots.stream().skip(9).limit(sc.slots.size() - 10).noneMatch(slot -> slot.getStack().isEmpty())
                    && (LitematicaMixinMod.QUICK_SHULKER.getBooleanValue() || LitematicaMixinMod.INVENTORY.getBooleanValue())) {
                SwitchItem.checkItems();
                return true;
            }
            if (LitematicaMixinMod.QUICK_SHULKER.getBooleanValue() && openShulker(remoteItem)) {
                return true;
            } else if (LitematicaMixinMod.INVENTORY.getBooleanValue()) {
                for (Item item : remoteItem) {
                    //#if MC >= 12001
                    //#if MC > 12004
                    MemoryUtils.currentMemoryKey = client.world.getRegistryKey().getValue();
                    //#else
                    //$$ MemoryUtils.currentMemoryKey = client.world.getDimensionKey().getValue();
                    //#endif
                    MemoryUtils.itemStack = new ItemStack(item);
                    if (SearchItem.search(true)) {
                        closeScreen++;
                        isOpenHandler = true;
                        printerMemorySync = true;
                        return true;
                    }
                    //#else
                    //$$
                    //$$    MemoryDatabase database = MemoryDatabase.getCurrent();
                    //$$    if (database != null) {
                    //$$        for (Identifier dimension : database.getDimensions()) {
                    //$$            for (Memory memory : database.findItems(item.getDefaultStack(), dimension)) {
                    //$$                MemoryUtils.setLatestPos(memory.getPosition());
                    //#if MC < 11904
                    //$$ OpenInventoryPacket.sendOpenInventory(memory.getPosition(), RegistryKey.of(Registry.WORLD_KEY, dimension));
                    //#else
                    //$$ OpenInventoryPacket.sendOpenInventory(memory.getPosition(), RegistryKey.of(RegistryKeys.WORLD, dimension));
                    //#endif
                    //$$                if(closeScreen == 0)closeScreen++;
                    //$$                syncPrinterInventory = true;
                    //$$                isOpenHandler = true;
                    //$$                return true;
                    //$$            }
                    //$$        }
                    //$$    }
                    //#endif
                }
                remoteItem = new HashSet<>();
                isOpenHandler = false;
            }
        }
        return false;
    }

    public void myTick() {
        ArrayList<BlockPos> deletePosList = new ArrayList<>();
        skipPosMap.forEach((k, v) -> {
            skipPosMap.put(k, v + 1);
            if (v >= PUT_COOLING.getIntegerValue()) {
                deletePosList.add(k);
            }
        });
        for (BlockPos blockPos : deletePosList) {
            skipPosMap.remove(blockPos);
        }
    }

    public void tick() {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        ClientWorld world = client.world;
        // 预载常用配置值
        int compulsionRange = COMPULSION_RANGE.getIntegerValue();
        int interval = LitematicaMixinMod.PRINT_INTERVAL.getIntegerValue();
        boolean useEasyMode = LitematicaMixinMod.EASY_MODE.getBooleanValue();

        // 更新环境参数
        resetRange = true;
        printRange = compulsionRange;
        usingRange = true;
        yDegression = false;
        startTime = System.currentTimeMillis();
        // 更新 tick 计数（避免溢出）
        tick = (tick == Integer.MAX_VALUE) ? 0 : tick + 1;
        if (shulkerCooldown > 0) {
            shulkerCooldown--;
        }

        // 优先执行队列中的点击操作
        if (interval != 0) {
            queue.sendQueue(player);
            if (tick % interval != 0) {
                return;
            }
        }

        // 当需要调整装备时，切换物品并执行队列点击操作
        if (needDelay) {
            switchToItems(player, delayedItem);
            queue.sendQueue(player);
            needDelay = false;
        }

        // 如果正在处理打开的容器或切换物品，则直接返回
        if (isOpenHandler || switchItem()) {
            return;
        }

        // 模式判断（减少重复调用，提高分支执行效率）
        Object modeOption = LitematicaMixinMod.MODE_SWITCH.getOptionListValue();
        if (modeOption.equals(State.ModeType.MULTI)) {
            boolean multiBreak = MULTI_BREAK.getBooleanValue();
            if (LitematicaMixinMod.BEDROCK_SWITCH.getBooleanValue()) {
                yDegression = true;
                bedrockMode();
                if (multiBreak) return;
            }
            if (LitematicaMixinMod.EXCAVATE.getBooleanValue()) {
                yDegression = true;
                miningMode();
                if (multiBreak) return;
            }
            if (LitematicaMixinMod.FLUID.getBooleanValue()) {
                fluidMode();
                if (multiBreak) return;
            }
        } else if (LitematicaMixinMod.PRINTER_MODE.getOptionListValue() instanceof State.PrintModeType modeType
                && modeType != PRINTER) {
            switch (modeType) {
                case BEDROCK -> { yDegression = true; bedrockMode(); }
                case EXCAVATE -> { yDegression = true; miningMode(); }
                case FLUID -> fluidMode();
            }
            return;
        }

        // 遍历当前区域内所有符合条件的位置
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return;
        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            if (!canInteracted(pos)) continue;
            BlockState state = schematic.getBlockState(pos);
            PlacementGuide.Action action = guide.getAction(world, schematic, pos);
            if (action == null) continue;



            // 检查放置跳过列表，使用简单 for 循环替代 stream 提升性能
            if (LitematicaMixinMod.PUT_SKIP.getBooleanValue()) {
                boolean skip = false;
                for (String s : PUT_SKIP_LIST.getStrings()) {
                    if (Filters.equalsName(s, state)) {
                        skip = true;
                        break;
                    }
                }
                if (skip) continue;
            }

            // 渲染层范围判断
            if (!DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;
            // 跳过冷却中的位置
            if (skipPosMap.containsKey(pos)) continue;
            skipPosMap.put(pos, 0);

            if (useEasyMode) {
                easyPos = pos;
                WorldUtilsAccessor.doEasyPlaceAction(client);
                easyPos = null;
                if (interval != 0)
                    return;
                else
                    continue;
            }

            Direction side = action.getValidSide(world, pos);
            if (side == null) continue;

            Item[] reqItems = action.getRequiredItems(state.getBlock());
            if (playerHasAccessToItems(player, reqItems)) {
                boolean useShift = LitematicaMixinMod.FORCED_PLACEMENT.getBooleanValue() ||
                        Implementation.isInteractive(world.getBlockState(pos).getBlock()) ||
                        action.useShift;
                if (pendingItemSwitch) continue;
                switchToItems(player, reqItems);
                action.queueAction(queue, pos, side, useShift, action.getLookHorizontalDirection() != null);
                sendLook(player, action.getLookHorizontalDirection(), action.getLookDirectionPitch());
                if (interval == 0) {
                    var block = schematic.getBlockState(pos).getBlock();
                    if (block instanceof PistonBlock ||
                            block instanceof ObserverBlock ||
                            block instanceof DispenserBlock
                    ) {
                        delayedItem = reqItems;
                        needDelay = true;
                        continue;
                    }
                    queue.sendQueue(player);
                    continue;
                }
                return;
            }
        }
    }

    public LinkedList<BlockPos> siftBlock(String blockName) {
        LinkedList<BlockPos> blocks = new LinkedList<>();
        AreaSelection i = DataManager.getSelectionManager().getCurrentSelection();
        List<Box> boxes;
        if (i == null) return blocks;
        boxes = i.getAllSubRegionBoxes();
        for (Box box : boxes) {
            MyBox myBox = new MyBox(box);
            for (BlockPos pos : myBox) {
                BlockState state = null;
                if (client.world != null) {
                    state = client.world.getBlockState(pos);
                }
                if (Filters.equalsName(blockName, state)) {
                    blocks.add(pos);
                }
            }
        }
        return blocks;
    }

    //todo)) 没修好呢（
    public float getPrintProgress() {
        // 重置 basePos 以确保重新初始化迭代器
        basePos = null;
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return 0.0f;

        int printedCount = 0;
        int totalCount = 0;
        BlockPos pos;
        while ((pos = getBlockPos()) != null) {
            if (!isSchematicBlock(pos)) continue;
            totalCount++;
            BlockState requiredState = schematic.getBlockState(pos);
            if (requiredState == null || requiredState.getBlock() instanceof AirBlock) continue;
            BlockState currentState = client.world.getBlockState(pos);
            if (currentState.getBlock().getDefaultState().equals(requiredState.getBlock().getDefaultState())) {
                printedCount++;
            }
        }
        //client.inGameHud.getChatHud().addMessage(Text.of("已放置: " + printedCount + " / " + totalCount));
        return totalCount == 0 ? 0.0f : ((float) printedCount / totalCount);
    }

    public void switchInv() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ScreenHandler sc = player.currentScreenHandler;
        if (sc.equals(player.playerScreenHandler)) {
            return;
        }
        DefaultedList<Slot> slots = sc.slots;
        String[] pickSlots = Configs.Generic.PICK_BLOCKABLE_SLOTS.getStringValue().split(",");
        if (pickSlots.length == 0) return;
        PlayerInventory inv = player.getInventory();

        for (Item remote : remoteItem) {
            for (int y = 0, invSize = slots.get(0).inventory.size(); y < invSize; y++) {
                if (!slots.get(y).getStack().getItem().equals(remote)) continue;
                try {
                    for (String s : pickSlots) {
                        if (s == null) break;
                        int c = Integer.parseInt(s.trim()) - 1;
                        ItemStack slotStack = inv.getStack(c);
                        if (Registries.ITEM.getId(slotStack.getItem()).toString().contains("shulker_box") &&
                                LitematicaMixinMod.QUICK_SHULKER.getBooleanValue()) {
                            client.inGameHud.setOverlayMessage(Text.of("潜影盒占用了预选栏"), false);
                        }
                        if (OpenInventoryPacket.key != null) {
                            SwitchItem.newItem(slots.get(y).getStack(), OpenInventoryPacket.pos, OpenInventoryPacket.key, y, -1);
                        } else {
                            SwitchItem.newItem(slots.get(y).getStack(), null, null, y, shulkerBoxSlot);
                        }
                        int emptySlot = getEmptyPickBlockableHotbarSlot(inv);
                        int targetSlot = emptySlot == -1 ? getPickBlockTargetSlot(player) : emptySlot;
                        c = (targetSlot == -1 ? c : targetSlot);
                        ZxyUtils.switchPlayerInvToHotbarAir(c);
                        fi.dy.masa.malilib.util.InventoryUtils.swapSlots(sc, y, c);
                        inv.selectedSlot = c;
                        player.closeHandledScreen();
                        if (shulkerBoxSlot != -1) {
                            client.interactionManager.clickSlot(sc.syncId, shulkerBoxSlot, 1, SlotActionType.PICKUP, client.player);
                        }
                        shulkerBoxSlot = -1;
                        isOpenHandler = false;
                        remoteItem.clear();
                        return;
                    }
                } catch (Exception e) {
                    System.out.println("切换物品异常");
                }
            }
        }
        shulkerBoxSlot = -1;
        remoteItem.clear();
        isOpenHandler = false;
        if (!player.currentScreenHandler.equals(player.playerScreenHandler)) {
            player.closeHandledScreen();
        }
    }

    boolean openShulker(HashSet<Item> items) {
        if (shulkerCooldown > 0) {
            return false;
        }
        for (Item item : items) {
            ScreenHandler sc = MinecraftClient.getInstance().player.playerScreenHandler;
            for (int i = 9; i < sc.slots.size(); i++) {
                ItemStack stack = sc.slots.get(i).getStack();
                String itemid = Registries.ITEM.getId(stack.getItem()).toString();
                if (itemid.contains("shulker_box") && stack.getCount() == 1) {
                    DefaultedList<ItemStack> items1 = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack, -1);
                    if (items1.stream().anyMatch(s1 -> s1.getItem().equals(item))) {
                        try {
                            shulkerBoxSlot = i;
                            client.interactionManager.clickSlot(sc.syncId, i, 1, SlotActionType.PICKUP, client.player);
                            closeScreen++;
                            isOpenHandler = true;
                            shulkerCooldown = 20; // AxShulkers的潜影盒延迟
                            return true;
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean switchToItems(ClientPlayerEntity player, Item[] items) {
        if (items == null) return false;
        PlayerInventory inv = Implementation.getInventory(player);
        for (Item item : items) {
            if (Implementation.getAbilities(player).creativeMode) {
                InventoryUtils.setPickedItemToHand(new ItemStack(item), client);
                client.interactionManager.clickCreativeStack(client.player.getStackInHand(Hand.MAIN_HAND), 36 + inv.selectedSlot);
                return true;
            } else {
                int slot = -1;
                for (int i = 0; i < inv.size(); i++) {
                    if (inv.getStack(i).getItem() == item && inv.getStack(i).getCount() > 0)
                        slot = i;
                }
                if (slot != -1) {
                    yxcfItem = inv.getStack(slot);
                    swapHandWithSlot(player, slot);
                    return true;
                }
            }
        }
        return false;
    }

    public void swapHandWithSlot(ClientPlayerEntity player, int slot) {
        ItemStack stack = Implementation.getInventory(player).getStack(slot);
        int sourceSlot = client.player.getInventory().getSlotWithStack(stack);
        PlayerInventory inventory = player.getInventory();

        if (PlayerInventory.isValidHotbarIndex(sourceSlot)) {
            if (PLACE_USE_PACKET.getBooleanValue()) {
                // 通过数据包切换热键槽
                client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(sourceSlot));
                inventory.selectedSlot = sourceSlot;
            } else {
                inventory.selectedSlot = sourceSlot;
            }
        } else {
            int hotbarSlot = sourceSlot;
            if (sourceSlot == -1 || !PlayerInventory.isValidHotbarIndex(sourceSlot)) {
                hotbarSlot = getEmptyPickBlockableHotbarSlot(inventory);
            }
            if (hotbarSlot == -1) {
                hotbarSlot = getPickBlockTargetSlot(player);
            }
            if (hotbarSlot != -1) {
                if (PLACE_USE_PACKET.getBooleanValue()) {
                    client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
                    inventory.selectedSlot = hotbarSlot;
                } else {
                    inventory.selectedSlot = hotbarSlot;
                }
                if (EntityUtils.isCreativeMode(player)) {
                    inventory.main.set(hotbarSlot, stack.copy());
                } else {
                    if (
                        //#if MC <= 12004
                        //$$areStacksEqual(stack.copy(), player.getMainHandStack())
                        //#else
                            areStacksEqualIgnoreNbt(stack.copy(), player.getMainHandStack())
                        //#endif
                    )
                    {
                        return;
                    }

                    if (player.isCreative())
                    {
                        //#if MC <= 12101
                        //$$player.getInventory().addPickBlock(stack.copy());
                        //#else
                        player.getInventory().swapStackWithHotbar(stack.copy());
                        //#endif
                        client.interactionManager.clickCreativeStack(player.getMainHandStack(), 36 + player.getInventory().selectedSlot);
                        return;
                    } else {
                        int slot1 = fi.dy.masa.malilib.util.InventoryUtils.findSlotWithItem(player.playerScreenHandler, stack.copy(), true);
                        if (slot1 != -1) {
                            // 使用数据包或普通点击方式交换槽位中的物品
                            if (PLACE_USE_PACKET.getBooleanValue()) {
                                Int2ObjectMap<ItemStack> snapshot = new Int2ObjectOpenHashMap<>();
                                DefaultedList<Slot> slots = player.currentScreenHandler.slots;
                                int totalSlots = slots.size();
                                List<ItemStack> copies = Lists.newArrayListWithCapacity(totalSlots);
                                for (Slot slotItem : slots) {
                                    copies.add(slotItem.getStack().copy());
                                }
                                for (int j = 0; j < totalSlots; j++) {
                                    ItemStack original = copies.get(j);
                                    ItemStack current = slots.get(j).getStack();
                                    if (!ItemStack.areEqual(original, current)) {
                                        snapshot.put(j, current.copy());
                                    }
                                }
                                client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                                        player.playerScreenHandler.syncId,
                                        player.currentScreenHandler.getRevision(),
                                        slot1,
                                        hotbarSlot,
                                        SlotActionType.SWAP,
                                        stack.copy(),
                                        snapshot));
                            } else {
                                client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot1, hotbarSlot, SlotActionType.SWAP, player);
                            }
                        }
                    }
                }
                WorldUtils.setEasyPlaceLastPickBlockTime();
            }
        }
    }

    public void sendLook(ClientPlayerEntity player, Direction directionYaw, Direction directionPitch) {
        if (directionYaw != null || directionPitch != null) {
            Implementation.sendLookPacket(player, directionYaw, directionPitch);
        }
        queue.lookDirYaw = directionYaw;
        queue.lookDirPitch = directionPitch;
    }

    public static class TempData {
        public static boolean xuanQuFanWeiNei_p(BlockPos pos) {
            return xuanQuFanWeiNei_p(pos, 0);
        }

        public static boolean xuanQuFanWeiNei_p(BlockPos pos, int p) {
            AreaSelection i = DataManager.getSelectionManager().getCurrentSelection();
            if (i == null) return false;
            if (DataManager.getSelectionManager().getSelectionMode() == NORMAL) {
                List<Box> arr = i.getAllSubRegionBoxes();
                for (Box box : arr) {
                    if (comparePos(box, pos, p)) {
                        return true;
                    }
                }
                return false;
            } else {
                Box box = i.getSubRegionBox(DataManager.getSimpleArea().getName());
                return comparePos(box, pos, p);
            }
        }

        static boolean comparePos(Box box, BlockPos pos, int p) {
            if (box == null || box.getPos1() == null || box.getPos2() == null || pos == null) return false;
            net.minecraft.util.math.Box box1 = new MyBox(box);
            box1 = box1.expand(p);
            //因为麻将的Box.contains方法内部用的 x >= this.minX && x < this.maxX ... 使得最小边界能被覆盖，但是最大边界不行
            //因此 我重写了该方法
            return box1.contains(Vec3d.of(pos));
        }
    }

    public static class Queue {
        final Printer printerInstance;
        public BlockPos target;
        public Direction side;
        public Vec3d hitModifier;
        public boolean shift = false;
        public boolean didSendLook = true;
        public boolean termsOfUse = false;
        public Direction lookDirYaw = null;
        public Direction lookDirPitch = null;

        public Queue(Printer printerInstance) {
            this.printerInstance = printerInstance;
        }

        public void queueClick(@NotNull BlockPos target, @NotNull Direction side, @NotNull Vec3d hitModifier, boolean shift, boolean didSendLook) {
            if (LitematicaMixinMod.PRINT_INTERVAL.getIntegerValue() != 0) {
                if (this.target != null) {
                    System.out.println("Was not ready yet.");
                    return;
                }
            }

            this.didSendLook = didSendLook;
            this.target = target;
            this.side = side;
            this.hitModifier = hitModifier;
            this.shift = shift;

        }

        public void sendQueue(ClientPlayerEntity player) {
            if (target == null || side == null || hitModifier == null) return;

            boolean wasSneaking = player.isSneaking();
            Direction direction = side.getAxis() == Direction.Axis.Y
                    ? (lookDirYaw != null && lookDirYaw.getAxis().isHorizontal()
                    ? lookDirYaw
                    : (lookDirPitch != null && lookDirPitch.getAxis().isHorizontal()
                    ? lookDirPitch
                    : Direction.NORTH))
                    : side;

            Vec3d hitVec = !termsOfUse
                    ? Vec3d.ofCenter(target)
                    .add(Vec3d.of(side.getVector()).multiply(0.5))
                    .add(hitModifier.rotateY((direction.getPositiveHorizontalDegrees() + 90) % 360).multiply(0.5))
                    : hitModifier;

            // 切换 SHIFT 状态（仅当当前状态与目标状态不同时处理）
            if (shift != wasSneaking) {
                ClientCommandC2SPacket.Mode preMode = shift
                        ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY
                        : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY;
                player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, preMode));
                player.setSneaking(shift);
            }

            if (yxcfItem != null) {
                if (yxcfItem.isEmpty()) {
                    SwitchItem.removeItem(yxcfItem);
                } else {
                    SwitchItem.syncUseTime(yxcfItem);
                }
            }

            printerInstance.sendLook(player, lookDirYaw, lookDirPitch);

            if (PLACE_USE_PACKET.getBooleanValue()) {
                //#if MC >= 11904
                // 1.19.4 及以上版本
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
                ((IClientPlayerInteractionManager) printerInstance.client.interactionManager)
                        .rightClickBlock(target, side, hitVec);
            }

            // 恢复原有的 SHIFT 状态
            if (shift != wasSneaking) {
                ClientCommandC2SPacket.Mode postMode = wasSneaking
                        ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY
                        : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY;
                player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, postMode));
                player.setSneaking(wasSneaking);
            }

            clearQueue();
        }

        public void clearQueue() {
            this.target = null;
            this.side = null;
            this.hitModifier = null;
            this.lookDirYaw = null;
            this.shift = false;
            this.didSendLook = true;
        }
    }
}