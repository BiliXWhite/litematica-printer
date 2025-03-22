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
import static fi.dy.masa.litematica.util.WorldUtils.applyCarpetProtocolHitVec;
import static fi.dy.masa.litematica.util.WorldUtils.applyPlacementProtocolV3;
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
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem.reSwitchItem;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;
import net.minecraft.util.Identifier;

//#if MC >= 12001
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.SearchItem;
//#else
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.Memory;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase;
//#endif

//#if MC < 11904
//$$ import net.minecraft.command.argument.ItemStringReader;
//$$ import com.mojang.brigadier.StringReader;
//$$ import net.minecraft.util.registry.RegistryKey;
//$$ import net.minecraft.util.registry.Registry;
//#else if MC = 11904
//$$ import net.minecraft.util.Identifier;
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
    public static Vec3d itemPos = null;
    public static HashSet<Item> remoteItem = new HashSet<>();
    public static HashSet<Item> fluidList = new HashSet<>();
    public static boolean printerMemorySync = false;
    public static BlockPos easyPos = null;
    public static boolean isOpenHandler = false;
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
    public int range2;
    //强制循环半径
    public boolean reSetRange1 = true;
    //正常循环
    public boolean reSetRange2 = true;
    public boolean usingRange1 = true;
    public BlockPos basePos = null;
    public MyBox myBox;
    int x1, y1, z1, x2, y2, z2;
    int range1;
    boolean yDegression = false;
    BlockPos tempPos = null;
    int tickRate;
    boolean isFacing = false;
    Item[] item2 = null;
    List<String> fluidBlocklist;
    long startTime;

    private Printer(@NotNull MinecraftClient client) {
        this.client = client;

        this.guide = new PlacementGuide(client);
        this.queue = new Queue(this);

        INSTANCE = this;
    }

    public static void init(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        INSTANCE = new Printer(client);

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
        if (EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.TW)) {
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

    // 执行一次获取一个pos
    @Deprecated
    BlockPos getBlockPos() {
        if (!usingRange1 && timedOut()) return null;
        ClientPlayerEntity player = client.player;
        if (player == null) return null;
        if (reSetRange1) {
            x1 = -range1;
            z1 = -range1;
            y1 = yDegression ? range1 : -range1;
            reSetRange1 = false;
        }
        if (reSetRange2) {
            x2 = -range2;
            z2 = -range2;
            y2 = yDegression ? range2 : -range2;
            reSetRange2 = false;
        }

        BlockPos pos;
        if (usingRange1) {
            pos = player.getBlockPos().north(x1).west(z1).up(y1);
        } else {
            pos = player.getBlockPos().north(x2).west(z2).up(y2);
        }

        if ((usingRange1 && x1 >= range1 && z1 >= range1 && (yDegression ? y1 < -range1 : y1 > range1)) ||
                (!usingRange1 && x2 >= range2 && z2 >= range2 && (yDegression ? y2 < -range2 : y2 > range2))) {
            // 当前范围迭代完成
            if (usingRange1) {
                usingRange1 = false; // 切换到使用 range2
                if (range2 <= range1) return null;
                return pos;
            } else {
                reSetRange2 = true;
                return null;
            }
        }

        if (usingRange1) {
            x1++;
            if (x1 > range1) {
                x1 = -range1;
                z1++;
            }
            if (z1 > range1) {
                z1 = -range1;
                if (yDegression) {
                    y1--;
                } else {
                    y1++;
                }
            }
        } else {
            x2++;
            if (x2 > range2) {
                x2 = -range2;
                z2++;
            }
            if (z2 > range2) {
                z2 = -range2;
                if (yDegression) {
                    y2--;
                } else {
                    y2++;
                }
            }
        }
        return pos;
    }

    BlockPos getBlockPos2() {
        if (timedOut()) return null;
        ClientPlayerEntity player = client.player;
        if (player == null) return null;
        if (basePos == null) {
            BlockPos blockPos = player.getBlockPos();
            basePos = blockPos;
            myBox = new MyBox(blockPos).expand(range1);
        }
        //移动后会触发，频繁重置pos会浪费性能
        double num = range1 * 0.7;
        if (!basePos.isWithinDistance(player.getBlockPos(), num)) {
            basePos = null;
            return null;
        }
        myBox.yIncrement = !yDegression;
        myBox.initIterator();
        Iterator<BlockPos> iterator = myBox.iterator;
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            IConfigOptionListEntry optionListValue = LitematicaMixinMod.RANGE_MODE.getOptionListValue();
            if (optionListValue == State.ListType.SPHERE && !basePos.isWithinDistance(pos, range1)) {
                continue;
            }
            return pos;
        }
        basePos = null;
        return null;
    }

    //根据当前毫秒值判断是否超出了屏幕刷新率
    boolean timedOut() {
        if (frameGenerationTime == 0) return System.currentTimeMillis() > 15 + startTime;
        return System.currentTimeMillis() > frameGenerationTime + startTime;
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
        Item[] array = fluidList.toArray(new Item[fluidList.size()]);
        BlockPos pos;
        while ((pos = getBlockPos2()) != null && client.world != null && client.player != null) {
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
        while ((pos = tempPos == null ? getBlockPos2() : tempPos) != null) {
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
        range2 = bedrockModeRange();
        BlockPos pos;
        while ((pos = getBlockPos2()) != null && client.world != null) {
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
                    && (LitematicaMixinMod.QUICKSHULKER.getBooleanValue() || LitematicaMixinMod.INVENTORY.getBooleanValue())) {
                SwitchItem.checkItems();
                return true;
            }
            if (LitematicaMixinMod.QUICKSHULKER.getBooleanValue() && openShulker(remoteItem)) {
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
        if (PlacementGuide.createPortalTick != 1) {
            PlacementGuide.createPortalTick = 1;
        }
    }

    public void tick() {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        ClientPlayerEntity pEntity = client.player;
        ClientWorld world = client.world;

        reSetRange1 = true;
        range1 = COMPULSION_RANGE.getIntegerValue();
        range2 = getPrinterRange();
        usingRange1 = true;
        yDegression = false;
        startTime = System.currentTimeMillis();
        tickRate = LitematicaMixinMod.PRINT_INTERVAL.getIntegerValue();

        tick = tick == 0x7fffffff ? 0 : tick + 1;
        boolean easyModeBooleanValue = LitematicaMixinMod.EASY_MODE.getBooleanValue();
        boolean forcedPlacementBooleanValue = LitematicaMixinMod.FORCED_PLACEMENT.getBooleanValue();

        //神tm潜影盒延迟
        if (shulkerCooldown > 0) {
            shulkerCooldown--;
        }

        if (tickRate != 0) {
            queue.sendQueue(client.player);
            if (tick % tickRate != 0) {
                return;
            }
        }
        if (isFacing) {
            switchToItems(pEntity, item2);
            queue.sendQueue(client.player);
            isFacing = false;
        }

        if (isOpenHandler) return;
        if (switchItem()) return;

        if (LitematicaMixinMod.MODE_SWITCH.getOptionListValue().equals(State.ModeType.MULTI)) {
            boolean multiBreakBooleanValue = MULTI_BREAK.getBooleanValue();
            if (LitematicaMixinMod.BEDROCK_SWITCH.getBooleanValue()) {
                yDegression = true;
                bedrockMode();
                if (multiBreakBooleanValue) return;
            }
            if (LitematicaMixinMod.EXCAVATE.getBooleanValue()) {
                yDegression = true;
                miningMode();
                if (multiBreakBooleanValue) return;
            }
            if (LitematicaMixinMod.FLUID.getBooleanValue()) {
                fluidMode();
                if (multiBreakBooleanValue) return;
            }
        } else if (LitematicaMixinMod.PRINTER_MODE.getOptionListValue() instanceof State.PrintModeType modeType && modeType != PRINTER) {
            switch (modeType) {
                case BEDROCK -> {
                    yDegression = true;
                    bedrockMode();
                }
                case EXCAVATE -> {
                    yDegression = true;
                    miningMode();
                }
                case FLUID -> fluidMode();
            }
            return;
        }

        LitematicaMixinMod.shouldPrintInAir = LitematicaMixinMod.PRINT_IN_AIR.getBooleanValue();

        // forEachBlockInRadius:
        BlockPos pos;
        while ((pos = getBlockPos2()) != null) {
            if (client.player != null && !canInteracted(pos)) continue;
            BlockState requiredState = worldSchematic.getBlockState(pos);
            PlacementGuide.Action action = guide.getAction(world, worldSchematic, pos);
            //跳过放置
            if (LitematicaMixinMod.PUT_SKIP.getBooleanValue() &&
//                    PUT_SKIP_LIST.getStrings().stream().anyMatch(block -> Registries.BLOCK.getId(requiredState.getBlock()).toString().contains(block))
                    PUT_SKIP_LIST.getStrings().stream().anyMatch(block -> Filters.equalsName(block, requiredState))
//                   && PUT_SKIP_LIST.getStrings().contains(Registries.BLOCK.getId(requiredState.getBlock()).toString())
            ) {
                continue;
            }
            if (!DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;
            //放置冷却
            if (skipPosMap.containsKey(pos)) {
                continue;
            } else {
                skipPosMap.put(pos, 0);
            }

            if (USE_EASY_MODE.getBooleanValue() && action != null) {
                easyPos = pos;
                WorldUtilsAccessor.doEasyPlaceAction(client);
                easyPos = null;
                if (tickRate != 0) return;
                else continue;
            }

            if (action == null) continue;

            Direction side = action.getValidSide(world, pos);
            if (side == null) continue;

            Item[] requiredItems = action.getRequiredItems(requiredState.getBlock());
            if (playerHasAccessToItems(pEntity, requiredItems)) {
                // Handle shift and chest placement
                // Won't be required if clickAction
                boolean useShift = false;
                if (requiredState.contains(ChestBlock.CHEST_TYPE)) {
                    // Left neighbor from player's perspective
//                    BlockPos leftNeighbor = pos.offset(requiredState.get(ChestBlock.FACING).rotateYClockwise());
//                    BlockState leftState = world.getBlockState(leftNeighbor);
                    switch (requiredState.get(ChestBlock.CHEST_TYPE)) {
                        case SINGLE:
                        case RIGHT: {
//                            side = requiredState.get(ChestBlock.FACING).rotateYCounterclockwise();
                            useShift = true;
                            break;
                        }
                        case LEFT: {
                            if (world.getBlockState(pos.offset(requiredState.get(ChestBlock.FACING).rotateYClockwise())).isAir())
                                continue;
                            side = requiredState.get(ChestBlock.FACING).rotateYClockwise();
                            useShift = true;
                            break;
                        }
//                        case RIGHT: {
//                            useShift = true;
//                            break;
//                        }
//                        case LEFT: { // Actually right
//                            if (leftState.contains(ChestBlock.CHEST_TYPE) && leftState.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) {
//                                useShift = false;
//
//                                // Check if it is possible to place without shift
//                                if (Implementation.isInteractive(world.getBlockState(pos.offset(side)).getBlock())) {
//                                    continue;
//                                }
//                            } else {
//                                continue;
//                            }
//                            break;
//                        }
                    }
                } else if (Implementation.isInteractive(world.getBlockState(pos.offset(side)).getBlock())) {
                    useShift = true;
                }

                Direction lookDir = action.getLookDirection();

                if (!easyModeBooleanValue &&
                        (requiredState.isOf(Blocks.PISTON) || //活塞
                                requiredState.isOf(Blocks.STICKY_PISTON) || //粘性活塞
                                requiredState.isOf(Blocks.OBSERVER) || //观察者
                                requiredState.isOf(Blocks.DROPPER) || //投掷器
                                requiredState.isOf(Blocks.DISPENSER) //发射器
                                //#if MC >= 12003
                                || requiredState.isOf(Blocks.CRAFTER) //合成器
                                //#endif
                        ) && isFacing
                ) {
                    continue;
                }

                //确认侦测器朝向方块是否正确
                if (requiredState.isOf(Blocks.OBSERVER) && PUT_TESTING.getBooleanValue()) {
                    BlockPos offset = pos.offset(lookDir);
                    BlockState state1 = world.getBlockState(offset);
                    BlockState state2 = worldSchematic.getBlockState(offset);

                    if (isSchematicBlock(offset)) {
                        State state = State.get(state1, state2);
                        if (!(state == State.CORRECT)) continue;
                    }
                }
                if (forcedPlacementBooleanValue) useShift = true;
                //发送放置准备
                sendPlacementPreparation(pEntity, requiredItems, lookDir);
                action.queueAction(queue, pos, side, useShift, lookDir != null);

                Vec3d hitModifier = usePrecisionPlacement(pos, requiredState);
                if (hitModifier != null) {
                    queue.hitModifier = hitModifier;
                    queue.termsOfUse = true;
                }

                if (requiredState.isOf(Blocks.NOTE_BLOCK)) {
                    queue.sendQueue(pEntity);
                    continue;
                }

                if (tickRate == 0) {
                    //处理不能快速放置的方块
//                    if(hitModifier != null){
//                        useBlock(hitModifier,action.lookDirection,pos,false);
//                        continue;
//                    }
                    if (hitModifier == null &&
                            (requiredState.isOf(Blocks.PISTON) || //活塞
                                    requiredState.isOf(Blocks.STICKY_PISTON) || //粘性活塞
                                    requiredState.isOf(Blocks.OBSERVER) || //观察者
                                    requiredState.isOf(Blocks.DROPPER) || //投掷器
                                    requiredState.isOf(Blocks.DISPENSER) //发射器
                                    //#if MC >= 12003
                                    || requiredState.isOf(Blocks.CRAFTER) //合成器
                                    //#endif
                            )
                    ) {
                        item2 = requiredItems;
                        isFacing = true;
                        continue;
                    }

                    queue.sendQueue(pEntity);
                    continue;
                }
                return;
            }
        }
    }

    public Vec3d usePrecisionPlacement(BlockPos pos, BlockState stateSchematic) {
        if (LitematicaMixinMod.EASY_MODE.getBooleanValue()) {
            EasyPlaceProtocol protocol = PlacementHandler.getEffectiveProtocolVersion();
            Vec3d hitPos = Vec3d.of(pos);
            if (protocol == EasyPlaceProtocol.V3) {
                return applyPlacementProtocolV3(pos, stateSchematic, hitPos);
            } else if (protocol == EasyPlaceProtocol.V2) {
                // Carpet Accurate Block Placement protocol support, plus slab support
                return applyCarpetProtocolHitVec(pos, stateSchematic, hitPos);
            }
        }
        return null;
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
//                        Block block = state.getBlock();
//                        if (Registries.BLOCK.getId(block).toString().contains(blockName)) {
                if (Filters.equalsName(blockName, state)) {
                    blocks.add(pos);
                }
            }
        }
        return blocks;
    }

    private void sendPlacementPreparation(ClientPlayerEntity player, Item[] requiredItems, Direction lookDir) {
        switchToItems(player, requiredItems);
        sendLook(player, lookDir);
    }

    public void switchInv() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ScreenHandler sc = player.currentScreenHandler;
        if (sc.equals(player.playerScreenHandler)) {
            return;
        }
        DefaultedList<Slot> slots = sc.slots;
        for (Item item : remoteItem) {
            for (int y = 0; y < slots.get(0).inventory.size(); y++) {
                if (slots.get(y).getStack().getItem().equals(item)) {
                    String[] str = Configs.Generic.PICK_BLOCKABLE_SLOTS.getStringValue().split(",");
                    if (str.length == 0) return;
                    for (String s : str) {
                        if (s == null) break;
                        try {
                            int c = Integer.parseInt(s) - 1;
                            if (Registries.ITEM.getId(player.getInventory().getStack(c).getItem()).toString().contains("shulker_box") &&
                                    LitematicaMixinMod.QUICKSHULKER.getBooleanValue()) {
                                client.inGameHud.setOverlayMessage(Text.of("潜影盒占用了预选栏"), false);
                                //continue;
                            }

                            if (OpenInventoryPacket.key != null) {
                                SwitchItem.newItem(slots.get(y).getStack(), OpenInventoryPacket.pos, OpenInventoryPacket.key, y, -1);
                            } else SwitchItem.newItem(slots.get(y).getStack(), null, null, y, shulkerBoxSlot);
                            int a = getEmptyPickBlockableHotbarSlot(player.getInventory()) == -1 ?
                                    getPickBlockTargetSlot(player) :
                                    getEmptyPickBlockableHotbarSlot(player.getInventory());
                            c = a == -1 ? c : a;
                            ZxyUtils.switchPlayerInvToHotbarAir(c);
                            fi.dy.masa.malilib.util.InventoryUtils.swapSlots(sc, y, c);
                            player.getInventory().selectedSlot = c;
                            player.closeHandledScreen();
                            if (shulkerBoxSlot != -1) {
                                client.interactionManager.clickSlot(sc.syncId, shulkerBoxSlot, 1, SlotActionType.PICKUP, client.player);
                            }
                            shulkerBoxSlot = -1;
                            isOpenHandler = false;
                            remoteItem = new HashSet<>();
                            return;
                        } catch (Exception e) {
                            System.out.println("切换物品异常");
                        }
                    }
                }
            }
        }
        shulkerBoxSlot = -1;
        remoteItem = new HashSet<>();
        isOpenHandler = false;
        ScreenHandler sc2 = player.currentScreenHandler;
        if (!sc2.equals(player.playerScreenHandler)) {
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
                            shulkerCooldown = 20; // Set cooldown to 20 ticks
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
            if (SWITCH_ITEM_USE_PACKET.getBooleanValue()) {
                client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(sourceSlot));
                //同步物品栏
                if (AFTER_SWITCH_ITEM_SYNC.getBooleanValue()) {
                    inventory.selectedSlot = sourceSlot;
                    MinecraftClient.getInstance().player.getInventory().selectedSlot = sourceSlot;
                }
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
                if (SWITCH_ITEM_USE_PACKET.getBooleanValue()) {
                    client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
                    //同步物品栏
                    if (AFTER_SWITCH_ITEM_SYNC.getBooleanValue()) {
                        inventory.selectedSlot = hotbarSlot;
                    }
                } else {
                    inventory.selectedSlot = hotbarSlot;
                }

                if (EntityUtils.isCreativeMode(player)) {
                    inventory.main.set(hotbarSlot, stack.copy());
                } else {

                    // Already holding the requested item
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
                        client.interactionManager.clickCreativeStack(player.getMainHandStack(), 36 + player.getInventory().selectedSlot); // sendSlotPacket
                        return;
                    }
                    else
                    {
                        int slot1 = fi.dy.masa.malilib.util.InventoryUtils.findSlotWithItem(player.playerScreenHandler, stack.copy(), true);

                        if (slot1 != -1)
                        {
                            //client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot1, currentHotbarSlot, SlotActionType.SWAP, player);
                            //改用数据包发送
                            if (SWITCH_ITEM_USE_PACKET.getBooleanValue()) {
                                Int2ObjectMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap();
                                DefaultedList<Slot> defaultedList = player.currentScreenHandler.slots;
                                int i = defaultedList.size();
                                List<ItemStack> list = Lists.newArrayListWithCapacity(i);

                                for (Slot slot2 : defaultedList) {
                                    list.add(slot2.getStack().copy());
                                }

                                for (int j = 0; j < i; ++j) {
                                    ItemStack itemStack = list.get(j);
                                    ItemStack itemStack2 = defaultedList.get(j).getStack();
                                    if (!ItemStack.areEqual(itemStack, itemStack2)) {
                                        int2ObjectMap.put(j, itemStack2.copy());
                                    }
                                }
                                client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(player.playerScreenHandler.syncId, player.currentScreenHandler.getRevision(), slot1, hotbarSlot, SlotActionType.SWAP, stack.copy(), int2ObjectMap));
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

    public void sendLook(ClientPlayerEntity player, Direction direction) {
        if (direction != null) {
            Implementation.sendLookPacket(player, direction);
        }
        queue.lookDir = direction;
    }

    public static class TempData {
        public static boolean xuanQuFanWeiNei_p(BlockPos pos) {
            return xuanQuFanWeiNei_p(pos, 0);
        }

        public static boolean xuanQuFanWeiNei_p(BlockPos pos, int p) {
            AreaSelection i = DataManager.getSelectionManager().getCurrentSelection();
            if (i == null) return false;
            if (DataManager.getSelectionManager().getSelectionMode() == NORMAL) {
                boolean fw = false;
                List<Box> arr = i.getAllSubRegionBoxes();
                for (int j = 0; j < arr.size(); j++) {
                    if (comparePos(arr.get(j), pos, p)) {
                        return true;
                    } else {
                        fw = false;
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
        public Direction lookDir = null;

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

            Direction direction = side.getAxis() == Direction.Axis.Y ?
                    ((lookDir == null || !lookDir.getAxis().isHorizontal())
                            ? Direction.NORTH : lookDir) : side;

//            hitModifier = new Vec3d(hitModifier.x, hitModifier.y, hitModifier.z);
            Vec3d hitVec = hitModifier;
            if (!termsOfUse) {
                hitModifier = hitModifier.rotateY((direction.getPositiveHorizontalDegrees() + 90) % 360);
                hitVec = Vec3d.ofCenter(target)
                        .add(Vec3d.of(side.getVector()).multiply(0.5))
                        .add(hitModifier.multiply(0.5));
            }

            if (shift && !wasSneaking) {
                player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            }
            else if (!shift && wasSneaking) {
                player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            }

            ItemStack mainHandStack1 = yxcfItem;
            if (mainHandStack1 != null) {
                if (mainHandStack1.isEmpty()) {
                    SwitchItem.removeItem(mainHandStack1);
                } else SwitchItem.syncUseTime(mainHandStack1);
            }

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
                ((IClientPlayerInteractionManager) printerInstance.client.interactionManager).rightClickBlock(target, side, hitVec);
                //其他方法(interactBlock)可能会导致一些问题
                //player.interactionManager.interactBlock(player, player.world, Hand.MAIN_HAND, new BlockHitResult(hitVec, side, target, false));
                //其他方法(clickBlock)可能会导致一些问题
                //player.interactionManager.clickBlock(target, side);
            }

//            System.out.println("Printed at " + (target.toString()) + ", " + side + ", modifier: " + hitVec);

            if (shift && !wasSneaking)
                player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            else if (!shift && wasSneaking)
                player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));

            clearQueue();
        }

        public void clearQueue() {
            this.target = null;
            this.side = null;
            this.hitModifier = null;
            this.lookDir = null;
            this.shift = false;
            this.didSendLook = true;
        }
    }
}