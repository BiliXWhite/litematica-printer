package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EasyPlaceProtocol;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.interfaces.IClientPlayerInteractionManager;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.mixin.masa.Litematica_InventoryUtilsMixin;
import me.aleksilassila.litematica.printer.mixin.masa.WorldUtilsAccessor;
import me.aleksilassila.litematica.printer.printer.bedrockUtils.BreakingFlowController;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.PinYinSearch;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.BlockFilters;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Verify;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import me.aleksilassila.litematica.printer.printer.zxy.overwrite.MyBox;
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
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static fi.dy.masa.litematica.selection.SelectionMode.NORMAL;
import static fi.dy.masa.litematica.util.WorldUtils.applyCarpetProtocolHitVec;
import static fi.dy.masa.litematica.util.WorldUtils.applyPlacementProtocolV3;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST;
import static fi.dy.masa.tweakeroo.tweaks.PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION;
import static me.aleksilassila.litematica.printer.LitematicaMixinMod.*;
import static me.aleksilassila.litematica.printer.printer.Printer.TempData.*;
import static me.aleksilassila.litematica.printer.printer.bedrockUtils.BreakingFlowController.cachedTargetBlockList;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.BlockFilters.equalsBlockName;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.openIng;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.closeScreen;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.pos;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem.reSwitchItem;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//#if MC >= 12001
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.SearchItem;
import red.jackf.chesttracker.api.providers.InteractionTracker;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.loadChestTracker;
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
//#else
import net.minecraft.registry.Registries;
    //#if MC < 12002
    //$$ import net.minecraft.registry.RegistryKey;
    //$$ import net.minecraft.registry.RegistryKeys;
    //#endif
//#endif

//#if MC < 11900
//$$ import fi.dy.masa.malilib.util.SubChunkPos;
//#endif

public class Printer extends PrinterUtils {
    private static final Logger log = LoggerFactory.getLogger(Printer.class);
    public static boolean up = true;

    public static class TempData {
        public static boolean xuanQuFanWeiNei_p(BlockPos pos) {
          return  xuanQuFanWeiNei_p(pos,0);
        }
        public static boolean xuanQuFanWeiNei_p(BlockPos pos,int p) {
            AreaSelection i = DataManager.getSelectionManager().getCurrentSelection();
            if (i == null) return false;
            if (DataManager.getSelectionManager().getSelectionMode() == NORMAL) {
                boolean fw = false;
                List<Box> arr = i.getAllSubRegionBoxes();
                for (int j = 0; j < arr.size(); j++) {
                    if (comparePos(arr.get(j), pos,p)) {
                        return true;
                    } else {
                        fw = false;
                    }
                }
                return fw;
            } else {
                Box box = i.getSubRegionBox(DataManager.getSimpleArea().getName());
                return comparePos(box, pos,p);
            }
        }

        static boolean comparePos(Box box, BlockPos pos,int p) {
            if(box == null || box.getPos1() == null || box.getPos2() == null || pos == null) return false;
            net.minecraft.util.math.Box box1 = new MyBox(Vec3d.of(box.getPos1()),Vec3d.of(box.getPos2()));
            box1 = box1.expand(p);
            //因为麻将的Box.contains方法内部用的 x >= this.minX && x < this.maxX ... 使得最小边界能被覆盖，但是最大边界不行
            //因此 我重写了该方法
            return box1.contains(Vec3d.of(pos));
        }
    }

    private static Printer INSTANCE = null;
    @NotNull
    public final MinecraftClient client;
    public final PlacementGuide guide;
    public final Queue queue;
    public int range2;

    static int tick = 0;

    public static void init(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        INSTANCE = new Printer(client);

    }

    public static @Nullable Printer getPrinter() {
//        if (INSTANCE == null) {
//            INSTANCE = new Printer(client);
//        }

        return INSTANCE;
    }

    private Printer(@NotNull MinecraftClient client) {
        this.client = client;

        this.guide = new PlacementGuide(client);
        this.queue = new Queue(this);

        INSTANCE = this;
    }

    int x1, y1, z1, x2, y2, z2;
    int range1;

    boolean yDegression = false;
    //强制循环半径
    public boolean reSetRange1 = true;
    //正常循环
    public boolean reSetRange2 = true;
    public boolean usingRange1 = true;
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
        if(reSetRange2){
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
                if(range2 <= range1) return null;
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
    public BlockPos basePos = null;
    public MyBox myBox;
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
            if (optionListValue == State.ListType.SPHERE && !basePos.isWithinDistance(pos,range1)) {
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

//        for (int y = range; y > -range - 1; y--) {
//            for (int x = -range; x < range + 1; x++) {
//                for (int z = -range; z < range + 1; z++) {
        BlockPos pos;
        while ((pos = getBlockPos2()) != null && client.world != null && client.player != null) {
            BlockState currentState = client.world.getBlockState(pos);
            if (client.player != null && !canInteracted(pos)) continue;
            if (!TempData.xuanQuFanWeiNei_p(pos)) continue;
            if (isLimitedByTheNumberOfLayers(pos)) continue;
            if (currentState.getFluidState().isOf(Fluids.LAVA) || currentState.getFluidState().isOf(Fluids.WATER)) {
                blocklist = LitematicaMixinMod.FLUID_BLOCK_LIST.getStrings();
                for (int i = 0; i < blocklist.size(); i++) {
                    try {
                        //#if MC < 11904
                        //$$ ItemStringReader read = new ItemStringReader(new StringReader(blocklist.get(i)), true);
                        //$$ read.consume();
                        //$$ Item item = read.getItem();
                        //$$ ////#elseif MC < 12005
                        //$$ ////$$ ItemStringReader.ItemResult itemResult = ItemStringReader.item(Registries.ITEM.getReadOnlyWrapper(), new StringReader(blocklist.get(i)));
                        //$$ ////$$ Item item = itemResult.item().value();
                        //#else
                        Item item = Registries.ITEM.get(Identifier.tryParse(blocklist.get(i).toString()));
                        //#endif
                        if (item != null) fluidList.add(item);
                    } catch (Exception e) {
                    }
                }
                switchToItems(client.player, fluidList.toArray(new Item[fluidList.size()]));
                Item item = Implementation.getInventory(client.player).getMainHandStack().getItem();
                String itemid = Registries.ITEM.getId(item).toString();
                if (!blocklist.stream().anyMatch(b -> itemid.contains(b) || item.getName().toString().contains(b))) {
                    items2.addAll(fluidList);
                    return;
                }
//                        sendClick(pos, Vec3d.ofCenter(pos));
                ((IClientPlayerInteractionManager) client.interactionManager).rightClickBlock(pos, Direction.UP, Vec3d.ofCenter(pos));
                if (tickRate == 0) {
                    continue;
                }
                return;
            }
//                }
//            }
//        }
        }
    }

    BlockPos tempPos = null;

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
                    breakRestriction(client.world.getBlockState(pos),pos) &&
                    waJue(pos)) {
                tempPos = pos;
                return;
            }
            tempPos = null;
        }
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

    static BlockPos breakTargetBlock = null;
    static int startTick = -1;
    public static BlockPos excavateBlock(BlockPos pos){
        if (!canInteracted(pos)) {
            breakTargetBlock = null;
            return null;
        }
        //一个游戏刻挖一次就好
        if(startTick == tick){
            return null;
        }else if(breakTargetBlock != null){
            if (!Printer.waJue(breakTargetBlock)) {
                BlockPos breakTargetBlock1 = breakTargetBlock;
                breakTargetBlock = null;
                return breakTargetBlock1;
            }
            else return null;
        }
        startTick = tick;
        breakTargetBlock = pos;
        return null;
    }

    static boolean breakRestriction(BlockState blockState,BlockPos pos) {
        if(EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.TW)){
            if (!FabricLoader.getInstance().isModLoaded("tweakeroo")) return true;
//            return isPositionAllowedByBreakingRestriction(pos,Direction.UP);
            UsageRestriction.ListType listType = BLOCK_TYPE_BREAK_RESTRICTION.getListType();
            if (listType == UsageRestriction.ListType.BLACKLIST) {
                return BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> equalsBlockName(string,blockState,pos));
            } else if (listType == UsageRestriction.ListType.WHITELIST) {
                return BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST.getStrings().stream()
                        .anyMatch(string -> equalsBlockName(string,blockState,pos));
            } else {
                return true;
            }
        }else {
            IConfigOptionListEntry optionListValue = EXCAVATE_LIMIT.getOptionListValue();
            if (optionListValue == UsageRestriction.ListType.BLACKLIST) {
                return EXCAVATE_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> equalsBlockName(string,blockState,pos));
            } else if (optionListValue == UsageRestriction.ListType.WHITELIST) {
                return EXCAVATE_WHITELIST.getStrings().stream()
                        .anyMatch(string -> equalsBlockName(string,blockState,pos));
            } else {
                return true;
            }
        }
    }
    public static int moveTick = 0;
    public static Vec3d itemPos = null;
    public static ItemStack offHandItem = null;
    //此模式依赖bug运行 请勿随意修改
    public void bedrockMode() {

//        if (!client.player.getOffHandStack().isEmpty()){
//            offHandItem = client.player.getOffHandStack();
//        }else offHandItem = null;

        BreakingFlowController.tick();
        int maxy = -9999;
        range2 = bedrockModeRange();
        BlockPos pos;
        while ((pos = getBlockPos2()) != null && client.world != null) {
            if (!ZxyUtils.bedrockCanInteracted(pos,getRage())) continue;
            if (isLimitedByTheNumberOfLayers(pos)) continue;
            BlockState currentState = client.world.getBlockState(pos);
//                    if (currentState.isOf(Blocks.PISTON) && !data.world.getBlockState(pos.down()).isOf(Blocks.BEDROCK)) {
            BlockPos finalPos = pos;
            if ((currentState.isOf(Blocks.PISTON) || (currentState.isOf(Blocks.SLIME_BLOCK) &&
                    cachedTargetBlockList.stream().allMatch(
                            targetBlock -> targetBlock.temppos.stream().noneMatch(
                                    blockPos -> blockPos.equals(finalPos)))))
                    && !bedrockModeTarget(client.world.getBlockState(pos.down())) && xuanQuFanWeiNei_p(pos,3)) {
                BreakingFlowController.addPosList(pos);
            } else if (currentState.isOf(Blocks.PISTON_HEAD)) {
                switchToItems(client.player, new Item[]{Items.AIR, Items.DIAMOND_PICKAXE});
                ((IClientPlayerInteractionManager) client.interactionManager)
                        .rightClickBlock(pos, Direction.UP, Vec3d.ofCenter(pos));
            }

//                    if (TempData.xuanQuFanWeiNei_p(pos) && currentState.isOf(Blocks.BEDROCK)  && ZxyUtils.canInteracted(pos,range-1.5) && !client.world.getBlockState(pos.up()).isOf(Blocks.BEDROCK)) {
            if (TempData.xuanQuFanWeiNei_p(pos) &&
                    bedrockModeTarget(currentState) &&
                    ZxyUtils.bedrockCanInteracted(pos, getRage() - 1.5) &&
                    !bedrockModeTarget(client.world.getBlockState(pos.up()))) {
                if (maxy == -9999) maxy = pos.getY();
                if (pos.getY() < maxy){
                    //重置迭代器 如果不重置 继续根据上次结束的y轴递减会出事
                    myBox.resetIterator();
                    return;
                }
                BreakingFlowController.addBlockPosToList(pos);
            }
        }
//        if (offHandItem != null){
//            PlayerScreenHandler sc = client.player.playerScreenHandler;
//            int i1 = -1;
//            for (int i = 0; i < sc.slots.size(); i++) {
//                if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqual(sc.slots.get(i).getStack(),offHandItem)) i1 = i;
//            }
//            if (i1 != -1) client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, i1, 40, SlotActionType.SWAP, client.player);
//        }


        //尝试移动到掉落物位置。。。
//        if (moveTick < 20) return;
//        moveTick = 0;
//        List<Item> items = List.of(Items.PISTON, Items.SLIME_BLOCK, Items.REDSTONE_TORCH);
//        ClientPlayerEntity player = client.player;
//        Vec3d playerPos = player.getPos();
//
//        net.minecraft.util.math.Box area = new net.minecraft.util.math.Box(playerPos.subtract(4,4,4), playerPos.add(4, 4, 4));
//        List<ItemEntity> entitiesByClass = client.world.getEntitiesByClass(ItemEntity.class, area, entity -> items.contains(entity.getStack().getItem()));
//        Vec3d playerPos1 = new Vec3d(playerPos.getX(),playerPos.getY(),playerPos.getZ());
//        for (ItemEntity byClass : entitiesByClass) {
//            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
//                    byClass.getX(),byClass.getY(),byClass.getZ(),player.isOnGround()));
////            itemPos = new Vec3d(byClass.getX(),byClass.getY(),byClass.getZ());
////            player.setPosition(byClass.getX(),byClass.getY(),byClass.getZ());
////            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(player.getYaw(),player.getPitch(),player.isOnGround()));
//            break;
//        }
//        player.setPosition(playerPos1.getX(),playerPos1.getY(),playerPos1.getZ());
//        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(playerPos1.getX(),playerPos1.getY(),playerPos1.getZ(),player.isOnGround()));
    }

    static boolean isLimitedByTheNumberOfLayers(BlockPos pos){
        return LitematicaMixinMod.RENDER_LAYER_LIMIT.getBooleanValue() && !DataManager.getRenderLayerRange().isPositionWithinRange(pos);
    }

    public static int bedrockModeRange() {
        return LitematicaMixinMod.RANGE_MODE.getOptionListValue() == State.ListType.SPHERE ? getRage() : 6;
    }

    public static boolean bedrockModeTarget(BlockState block) {
//        return LitematicaMixinMod.BEDROCK_LIST.getStrings().stream().anyMatch(string -> Registries.BLOCK.getId(block.getBlock()).toString().contains(string));
        return LitematicaMixinMod.BEDROCK_LIST.getStrings().stream().anyMatch(string -> equalsBlockName(string,block));
    }

    public boolean verify() {
        if (client.isInSingleplayer() && client.isRealmsEnabled()) return true;
        String address = null;
        try {
            address = Objects.requireNonNull(client.getCurrentServerEntry()).address.split(":")[0];
        } catch (Exception e) {
            return true;
        }
        if (Verify.getVerify() == null) {
            return new Verify(address, client.player).tick(address);
        } else {
            return Verify.getVerify().tick(address);
        }
    }

    int tickRate;
    boolean isFacing = false;
    Item[] item2 = null;
    List<String> blocklist;
    public static HashSet<Item> items2 = new HashSet<>();
    public static HashSet<Item> fluidList = new HashSet<>();
    static Map<BlockPos,Integer> skipPosMap = new HashMap<>();
    public static boolean printerMemorySync = false;


    public boolean switchItem() {
        if (!items2.isEmpty() && !isOpenHandler && !openIng && OpenInventoryPacket.key == null) {
            ClientPlayerEntity player = client.player;
            ScreenHandler sc = player.currentScreenHandler;
            if (!player.currentScreenHandler.equals(player.playerScreenHandler)) return false;
            //排除合成栏 装备栏 副手
            if (PRINT_CHECK.getBooleanValue() && sc.slots.stream().skip(9).limit(sc.slots.size() - 10).noneMatch(slot -> slot.getStack().isEmpty())
                    && (LitematicaMixinMod.QUICKSHULKER.getBooleanValue() || LitematicaMixinMod.INVENTORY.getBooleanValue())) {
                SwitchItem.checkItems();
                return true;
            }
            if (LitematicaMixinMod.QUICKSHULKER.getBooleanValue() && openShulker(items2)) {
                return true;
            } else if (LitematicaMixinMod.INVENTORY.getBooleanValue()) {
                for (Item item : items2) {
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
                items2 = new HashSet<>();
                isOpenHandler = false;
            }
        }
        return false;
    }
    long startTime;
    public static Item[] testItem = null;

    public static BlockPos easyPos = null;
    public void myTick(){
        ArrayList<BlockPos> deletePosList = new ArrayList<>();
        skipPosMap.forEach((k,v) -> {
            skipPosMap.put(k,v+1);
            if(v > PUT_COOLING.getIntegerValue()){
                deletePosList.add(k);
            }
        });
        for (BlockPos blockPos : deletePosList) {
            skipPosMap.remove(blockPos);
        }
        //破基岩移动包冷却
        moveTick++;
    }
    public void tick() {
        if (!verify()) return;
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

        if(LitematicaMixinMod.MODE_SWITCH.getOptionListValue().equals(State.ModeType.MULTI)){
            boolean multiBreakBooleanValue = MULTI_BREAK.getBooleanValue();
            if (LitematicaMixinMod.BEDROCK_SWITCH.getBooleanValue()) {
                yDegression = true;
                bedrockMode();
                if(multiBreakBooleanValue) return;
            }
            if (LitematicaMixinMod.EXCAVATE.getBooleanValue()) {
                yDegression = true;
                miningMode();
                if(multiBreakBooleanValue) return;
            }
            if (LitematicaMixinMod.FLUID.getBooleanValue()) {
                fluidMode();
                if(multiBreakBooleanValue) return;
            }
        }else {
            IConfigOptionListEntry mode = LitematicaMixinMod.PRINTER_MODE.getOptionListValue();
            if (mode.equals(State.PrintModeType.BEDROCK)) {
                yDegression = true;
                bedrockMode();
                return;
            } else if (mode.equals(State.PrintModeType.EXCAVATE)) {
                yDegression = true;
                miningMode();
                return;
            }else if(mode.equals(State.PrintModeType.FLUID)){
                fluidMode();
                return;
            }
        }

        LitematicaMixinMod.shouldPrintInAir = LitematicaMixinMod.PRINT_IN_AIR.getBooleanValue();

        // forEachBlockInRadius:
        BlockPos pos;
        z:
        while ((pos = getBlockPos2()) != null) {
            if (client.player != null && !canInteracted(pos)) continue;
            BlockState requiredState = worldSchematic.getBlockState(pos);
            PlacementGuide.Action action = guide.getAction(world, worldSchematic, pos);
            if (requiredState.isOf(Blocks.NETHER_PORTAL) || requiredState.isOf(Blocks.END_PORTAL)) continue;

            //跳过放置
            if (LitematicaMixinMod.PUT_SKIP.getBooleanValue() &&
//                    PUT_SKIP_LIST.getStrings().stream().anyMatch(block -> Registries.BLOCK.getId(requiredState.getBlock()).toString().contains(block))
                    PUT_SKIP_LIST.getStrings().stream().anyMatch(block -> equalsBlockName(block,requiredState))
//                   && PUT_SKIP_LIST.getStrings().contains(Registries.BLOCK.getId(requiredState.getBlock()).toString())
                   ) {
                continue;
            }
            if (!DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;
            //放置冷却
            if (skipPosMap.containsKey(pos)) {
                queue.clearQueue();
                continue;
            }else {
                skipPosMap.put(pos,0);
            }

            if(USE_EASY_MODE.getBooleanValue() && action != null) {
                easyPos = pos;
                WorldUtilsAccessor.doEasyPlaceAction(client);
                easyPos = null;
                if(tickRate != 0) return;
                else continue;
            }

            if (action == null) continue;

            Direction side = action.getValidSide(world, pos);
            if (side == null) continue;

            Item[] requiredItems = action.getRequiredItems(requiredState.getBlock());
            if (playerHasAccessToItems(pEntity, requiredItems)) {
                testItem = requiredItems;
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
                            break ;
                        }
                        case LEFT: {
                            if(world.getBlockState(pos.offset(requiredState.get(ChestBlock.FACING).rotateYClockwise())).isAir()) continue;
                            side = requiredState.get(ChestBlock.FACING).rotateYClockwise();
                            useShift = true;
                            break ;
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
                        (requiredState.isOf(Blocks.PISTON) ||
                        requiredState.isOf(Blocks.STICKY_PISTON) ||
                        requiredState.isOf(Blocks.OBSERVER) ||
                        requiredState.isOf(Blocks.DROPPER) ||
                        requiredState.isOf(Blocks.DISPENSER)) && isFacing
                ) {
                    continue;
                }

                //确认侦测器朝向方块是否正确
                if(requiredState.isOf(Blocks.OBSERVER) && PUT_TESTING.getBooleanValue()){
                    BlockPos offset = pos.offset(lookDir);
                    BlockState state1 = world.getBlockState(offset);
                    BlockState state2 = worldSchematic.getBlockState(offset);

                    if (isSchematicBlock(offset)) {
                        State state = State.get(state1,state2);
                        if (!(state == State.CORRECT)) continue z;
                    }
                }
                if(forcedPlacementBooleanValue) useShift = true;
                //发送放置准备
                sendPlacementPreparation(pEntity, requiredItems, lookDir);
                action.queueAction(queue, pos, side, useShift, lookDir != null);

                Vec3d hitModifier = usePrecisionPlacement(pos, requiredState);
                if(hitModifier != null){
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
                            (requiredState.isOf(Blocks.PISTON) ||
                            requiredState.isOf(Blocks.STICKY_PISTON) ||
                            requiredState.isOf(Blocks.OBSERVER) ||
                            requiredState.isOf(Blocks.DROPPER) ||
                            requiredState.isOf(Blocks.DISPENSER))
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

    public Vec3d usePrecisionPlacement(BlockPos pos,BlockState stateSchematic){
        if (LitematicaMixinMod.EASY_MODE.getBooleanValue()) {
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
                if (equalsBlockName(blockName, state)) {
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

    public static boolean isOpenHandler = false;

    public void switchInv() {
//        if(true) return;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ScreenHandler sc = player.currentScreenHandler;
        if (sc.equals(player.playerScreenHandler)) {
            return;
        }
        DefaultedList<Slot> slots = sc.slots;
        for (Item item : items2) {
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
                                MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.of("濳影盒占用了预选栏"), false);
                                continue;
                            }
//                            System.out.println(y);
//                            System.out.println(c);
//                            int shulkerSlot = -1;
//                            for (int i = slots.get(0).inventory.size(); i < slots.size(); i++) {
//                                if(!(slots.get(i).inventory instanceof PlayerInventory)) continue;
//                                ItemStack stack = slots.get(i).getStack();
////                                if (SwitchItem.shulkerBoxCompare(stack,shulkerBox,-1)){
//                                if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqual(stack,shulkerBox)){
//                                    shulkerSlot = i;
//                                    break;
//                                }
//                            }
//                            shulkerBox = shulkerSlot == -1? null : slots.get(shulkerSlot).getStack();
                            if (OpenInventoryPacket.key != null) {
                                SwitchItem.newItem(slots.get(y).getStack(), OpenInventoryPacket.pos, OpenInventoryPacket.key, y, -1);
                            } else SwitchItem.newItem(slots.get(y).getStack(), null, null, y, shulkerBoxSlot);
                            int boxSlot = shulkerBoxSlot;
                            shulkerBoxSlot = -1;
                            int a = Litematica_InventoryUtilsMixin.getEmptyPickBlockableHotbarSlot(player.getInventory()) == -1 ?
                                    Litematica_InventoryUtilsMixin.getPickBlockTargetSlot(player) :
                                    Litematica_InventoryUtilsMixin.getEmptyPickBlockableHotbarSlot(player.getInventory());
                            c = a == -1 ? c : a;
                            ZxyUtils.switchPlayerInvToHotbarAir(c);
                            fi.dy.masa.malilib.util.InventoryUtils.swapSlots(sc, y, c);
                            player.getInventory().selectedSlot = c;
                            player.closeHandledScreen();
                            isOpenHandler = false;
                            items2 = new HashSet<>();
                            return;
                        } catch (Exception e) {
                            System.out.println("切换物品异常");
                        }
                    }
                }
            }
        }
        shulkerBoxSlot = -1;
        items2 = new HashSet<>();
        isOpenHandler = false;
        ScreenHandler sc2 = player.currentScreenHandler;
        if (!sc2.equals(player.playerScreenHandler)) {
            player.closeHandledScreen();
        }
    }

    static int shulkerBoxSlot = -1;
    private static Method method;

    static {
        try {
            method = Class.forName("net.kyrptonaught.quickshulker.client.ClientUtil").getDeclaredMethod("CheckAndSend", ItemStack.class, int.class);
        } catch (Exception ignored) {
            method = null;
        }
    }

    boolean openShulker(HashSet<Item> items) {
        for (Item item : items) {
            ScreenHandler sc = MinecraftClient.getInstance().player.playerScreenHandler;
//            if(!MinecraftClient.getInstance().player.currentScreenHandler.equals(sc))return false;
            for (int i = 9; i < sc.slots.size(); i++) {
                ItemStack stack = sc.slots.get(i).getStack();
                String itemid = Registries.ITEM.getId(stack.getItem()).toString();
                if (itemid.contains("shulker_box") && stack.getCount() == 1) {
                    DefaultedList<ItemStack> items1 = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack, -1);
                    if (items1.stream().anyMatch(s1 -> s1.getItem().equals(item))) {
                        try {
                            if (reSwitchItem == null) shulkerBoxSlot = i;
//                            ClientUtil.CheckAndSend(stack,i);
                            //#if MC >= 12001
                            if(loadChestTracker) InteractionTracker.INSTANCE.clear();
                            //#endif
                            method.invoke(method, stack, i);
                            closeScreen++;
                            isOpenHandler = true;
                            //System.out.println("open "+b);
                            return true;
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        return false;
    }
    static ItemStack yxcfItem; //有序存放临时存储
    public void switchToItems(ClientPlayerEntity player, Item[] items) {
        if (items == null) return;
        PlayerInventory inv = Implementation.getInventory(player);
        //inv.getMainHandStack()  信息滞后 如果服务器有延迟这个获取的信息可能是错误的
//        for (Item item : items) {
//            if (inv.getMainHandStack().getItem() == item) {
//                return;
//            }
//        }
        for (Item item : items) {
            if (Implementation.getAbilities(player).creativeMode) {
                InventoryUtils.setPickedItemToHand(new ItemStack(item), client);
                client.interactionManager.clickCreativeStack(client.player.getStackInHand(Hand.MAIN_HAND), 36 + inv.selectedSlot);
                return;
            } else {
                int slot = -1;
                for (int i = 0; i < inv.size(); i++) {
                    if (inv.getStack(i).getItem() == item && inv.getStack(i).getCount() > 0)
                        slot = i;
                }
                if (slot != -1) {
                    yxcfItem = inv.getStack(slot);
                    swapHandWithSlot(player, slot);
                    return;
                }
            }
        }
    }

    public void swapHandWithSlot(ClientPlayerEntity player, int slot) {
        ItemStack stack = Implementation.getInventory(player).getStack(slot);
        InventoryUtils.setPickedItemToHand(stack, client);
    }

    public void sendLook(ClientPlayerEntity player, Direction direction) {
        if (direction != null) {
            Implementation.sendLookPacket(player, direction);
        }
        queue.lookDir = direction;
    }

    public static class Queue {
        public BlockPos target;
        public Direction side;
        public Vec3d hitModifier;
        public boolean shift = false;
        public boolean didSendLook = true;
        public boolean termsOfUse = false;

        public Direction lookDir = null;

        final Printer printerInstance;

        public Queue(Printer printerInstance) {
            this.printerInstance = printerInstance;
        }

        public void queueClick(@NotNull BlockPos target, @NotNull Direction side, @NotNull Vec3d hitModifier) {
            queueClick(target, side, hitModifier, true, true);
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
            if(!termsOfUse){
                hitModifier = hitModifier.rotateY((direction.asRotation() + 90) % 360);
                 hitVec = Vec3d.ofCenter(target)
                        .add(Vec3d.of(side.getVector()).multiply(0.5))
                        .add(hitModifier.multiply(0.5));
            }

            if (shift && !wasSneaking)
                player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            else if (!shift && wasSneaking)
                player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));

            ItemStack mainHandStack1 = yxcfItem;

            ((IClientPlayerInteractionManager) printerInstance.client.interactionManager)
                        .rightClickBlock(target, side, hitVec);


            if (mainHandStack1 != null) {
                if ( mainHandStack1.isEmpty()) {
                    SwitchItem.removeItem(mainHandStack1);
                } else SwitchItem.syncUseTime(mainHandStack1);
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