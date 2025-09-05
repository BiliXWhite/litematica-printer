package me.aleksilassila.litematica.printer.printer;

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
import me.aleksilassila.litematica.printer.mixin.masa.WorldUtilsAccessor;
import me.aleksilassila.litematica.printer.printer.bedrockUtils.BreakingFlowController;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.PlayerAction;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Verify;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.overwrite.MyBox;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static fi.dy.masa.litematica.selection.SelectionMode.NORMAL;
import static fi.dy.masa.litematica.util.WorldUtils.applyCarpetProtocolHitVec;
import static fi.dy.masa.litematica.util.WorldUtils.applyPlacementProtocolV3;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST;
import static fi.dy.masa.tweakeroo.tweaks.PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION;
import static me.aleksilassila.litematica.printer.LitematicaMixinMod.*;
import static me.aleksilassila.litematica.printer.printer.Printer.TempData.*;
import static me.aleksilassila.litematica.printer.printer.State.PrintModeType.*;
import static me.aleksilassila.litematica.printer.printer.bedrockUtils.BreakingFlowController.cachedTargetBlockList;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsBlockName;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Filters.equalsItemName;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.PlayerAction.excavateBlock;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//#if MC >= 12001
    //#if MC > 12105
    import net.minecraft.util.PlayerInput;
    //#endif
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
            net.minecraft.util.math.Box box1 = new MyBox(box);
            box1 = box1.expand(p);
            //因为麻将的Box.contains方法内部用的 x >= this.minX && x < this.maxX ... 最小边界能被覆盖，但是最大边界不行
            //因此 重写了该方法
            return box1.contains(Vec3d.of(pos));
        }
    }
    public static boolean isEnablePrinter(){
        return TOGGLE_PRINTING_MODE.getBooleanValue() || PRINT.getKeybind().isPressed();
    }

    private static Printer INSTANCE = null;
    @NotNull
    public final MinecraftClient client;
    public final PlacementGuide guide;
    public static PlacementGuide.Action currentAction;

    public static int tick = 0;

    public static @NotNull Printer getPrinter() {
        if (INSTANCE == null) {
            INSTANCE = new Printer(ZxyUtils.client);
        }
        return INSTANCE;
    }

    private Printer(@NotNull MinecraftClient client) {
        this.client = client;

        this.guide = new PlacementGuide(client);

        INSTANCE = this;
    }


    int range1;
    boolean yDegression = false;
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
        //离中心点一段距离后会触发，频繁重置pos会浪费性能
        double num = range1 * 0.7;
        if (!basePos.isWithinDistance(player.getBlockPos(), num)) {
            basePos = null;
            return null;
        }
        myBox.yIncrement = !yDegression;
        myBox.initIterator();
        Iterator<BlockPos> iterator = myBox.iterator;
        while (!timedOut() && iterator.hasNext()) {
            BlockPos pos = iterator.next();
            IConfigOptionListEntry optionListValue = RANGE_MODE.getOptionListValue();
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
        return System.currentTimeMillis() > printTimedOut + startTime;
    }
    public class ItemConfig {
        public ItemConfig(List<Item> itemList, boolean holdRequired) {
            this.itemList = itemList;
            this.holdRequired = holdRequired;
        }

        public List<Item> itemList;
        public boolean holdRequired;
    }

    public HashMap<List<String>, ItemConfig> replaceTaskMap = new HashMap<>();
    public BlockPos replacePos = null;

    public void initReplaceTask(){
        List<String> replaceTaskList = FLUID_BLOCK_LIST.getStrings();
        if (replaceTaskList.isEmpty()) return;
        // replaceTaskList 元素格式案例 minecraft:stone|minecraft:dirt => minecraft:grass_block
        for (String replace : replaceTaskList) {
            if (replace.length() < 2 || "//".equals(replace.substring(0,2))) continue;
            boolean holdRequired = false;
            if (replace.length() > 2 && "&&".equals(replace.substring(0,2))) {
                holdRequired = true;
                replace = replace.substring(2);
            }
            String[] split = replace.replaceAll("\\s","").split("=>|->");
            if(split.length > 2) continue;
            String[] originBlock = split[0].split("\\|");
            String[] newBlock = split[1].split("\\|");

            replaceTaskMap.put(new ArrayList<>(List.of(originBlock)),new ItemConfig(Registries.ITEM.stream().filter(item ->
                    Arrays.stream(newBlock).anyMatch(targetBlockName -> equalsItemName(targetBlockName, new ItemStack(item)))).toList(),holdRequired));
        }

    }

    public void replaceMode() {
        if (replaceTaskMap.isEmpty()) initReplaceTask();

        BlockPos pos;
        while ((pos = replacePos != null ? replacePos : getBlockPos2()) != null && client.world != null && client.player != null) {
            BlockState currentState = client.world.getBlockState(pos);
            if (client.player != null && !canInteracted(pos) || !xuanQuFanWeiNei_p(pos) || isLimitedByTheNumberOfLayers(pos)) {
                replacePos = null;
                continue;
            }
//            if (currentState.getFluidState().isOf(Fluids.LAVA) || currentState.getFluidState().isOf(Fluids.WATER)) {
            BlockPos finalPos = pos;
            AtomicReference<PlacementGuide.Action> action = new AtomicReference<>();
            final boolean[] skip = {false}; // 跳过方块名不符合的方块
            // map中的k是源方块名，v是目标方块
            boolean b = replaceTaskMap.entrySet().stream().anyMatch(entry -> {
                for (String blockName : entry.getKey()) {
                    // 为排流体破坏多余方块特殊处理
                    if (canBreakBlock(finalPos) && equalsBlockName(blockName, currentState, finalPos) && entry.getKey().stream().filter(name -> !blockName.equals(name)).anyMatch(name -> checkFluid(finalPos,name))) {
                        if (excavateBlock(finalPos) == null) {
                            replacePos = finalPos;
                            return true;
                        }
                        replacePos = null;
                        return false;
                    }

                    if ((!entry.getValue().holdRequired || entry.getValue().itemList.stream().anyMatch(item -> hasItem(item))) &&
                            ("all".equals(blockName) || equalsBlockName(blockName, currentState, finalPos)) &&
                            entry.getValue().itemList.stream().noneMatch(item -> equalsBlockName(item.getName().getString(), currentState, finalPos))) {
                        if (((Predicate<List<Item>>) items -> {
                            action.set(guide.buildAction(client.world, Blocks.AIR.getDefaultState(), currentState, finalPos, guide.getClassHook(currentState)));
                            if (excavateBlock(finalPos) == null) {
                                replacePos = finalPos;
                                return true;
                            }
                            replacePos = null;

                            if (items.stream().noneMatch(item -> item.equals(Items.AIR))) {
                                // 要么不是流体，要么是源
                                if ((currentState.getFluidState().isEmpty() || currentState.getFluidState().isStill())) {
                                    if (!switchToItems(client.player, items.toArray(new Item[0]))) {
                                        remoteItem.addAll(items);
                                        return true;
                                    }
                                    if (action.get() != null) {
                                        action.get().queueAction(finalPos, false);
                                        action.get().sendQueue(client.player);
                                    } else if (action.get() == null) {
                                        ((IClientPlayerInteractionManager) client.interactionManager).rightClickBlock(finalPos, Direction.UP, Vec3d.ofCenter(finalPos));
                                    }
                                    return false;
                                }
                            }
                            return false;
                        }).test(entry.getValue().itemList)) return true;
                        return false;
                    }
                    replacePos = null;
                }
                skip[0] = true;
                return false;
            });
            if (skip[0]) continue;
            if (b) return;
            if (tickRate == 0) continue;
            return;
        }
    }

    public boolean checkFluid(BlockPos pos, String blockName) {
        {
            if (equalsBlockName(blockName, Blocks.WATER) || equalsBlockName(blockName, Blocks.LAVA)) {
                if (client.world.getBlockState(pos.up()).getFluidState().isEmpty() &&
                        client.world.getBlockState(pos.down()).getFluidState().isEmpty() &&
                        client.world.getBlockState(pos.north()).getFluidState().isEmpty() &&
                        client.world.getBlockState(pos.south()).getFluidState().isEmpty() &&
                        client.world.getBlockState(pos.east()).getFluidState().isEmpty() &&
                        client.world.getBlockState(pos.west()).getFluidState().isEmpty()) {
                    return true;
                }
            }
            return false;
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
                    xuanQuFanWeiNei_p(pos) &&
                    breakRestriction(client.world.getBlockState(pos),pos) &&
                    PlayerAction.waJue(pos)) {
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
    public static Vec3d itemPos = null;
    //此模式依赖bug运行 请勿随意修改
    public void bedrockMode() {

        BreakingFlowController.tick();
        int maxy = -9999;
        BlockPos pos;
        while ((pos = getBlockPos2()) != null && client.world != null) {
            if (!bedrockCanInteracted(pos,getRage())) continue;
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
            if (xuanQuFanWeiNei_p(pos) &&
                    bedrockModeTarget(currentState) &&
                    bedrockCanInteracted(pos, getRage() - 1.5) &&
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
    }

    static boolean isLimitedByTheNumberOfLayers(BlockPos pos){
        return RENDER_LAYER_LIMIT.getBooleanValue() && !DataManager.getRenderLayerRange().isPositionWithinRange(pos);
    }

    public static int bedrockModeRange() {
        return RANGE_MODE.getOptionListValue() == State.ListType.SPHERE ? getRage() : 6;
    }

    public static boolean bedrockModeTarget(BlockState block) {
//        return LitematicaMixinMod.BEDROCK_LIST.getStrings().stream().anyMatch(string -> Registries.BLOCK.getId(block.getBlock()).toString().contains(string));
        return BEDROCK_LIST.getStrings().stream().anyMatch(string -> equalsBlockName(string,block.getBlock()));
    }
    
    public boolean verify() {
        if (client.isInSingleplayer()) return true;
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

    public static int tickRate;
    boolean isFacing = false;
    Item[] item2 = null;

    static Map<BlockPos,Integer> skipPosMap = new HashMap<>();
    public static boolean printerMemorySync = false;


    long startTime;
    public static BlockPos easyPos = null;
    public void myTick(){
        ArrayList<BlockPos> deletePosList = new ArrayList<>();
        skipPosMap.forEach((k,v) -> {
            skipPosMap.put(k,v+1);
            if(v >= PUT_COOLING.getIntegerValue()){
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
        if (!verify()) return;
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        ClientPlayerEntity pEntity = client.player;
        ClientWorld world = client.world;

        range1 = PRINTER_RANGE.getIntegerValue();
        yDegression = false;
        startTime = System.currentTimeMillis();
        tickRate = PRINT_INTERVAL.getIntegerValue();

        tick = tick == 0x7fffffff ? 0 : tick + 1;
        boolean easyModeBooleanValue = EASY_MODE.getBooleanValue();
        boolean forcedPlacementBooleanValue = FORCED_PLACEMENT.getBooleanValue();

        if (tickRate != 0) {
            if (currentAction != null){
                switchToItems(pEntity, currentAction.clickItems);
                currentAction.sendQueue(client.player);
            }

            if (tick % tickRate != 0) {
                return;
            }
        }
        if (isFacing) {
            switchToItems(pEntity, item2);
            if (currentAction != null) currentAction.sendQueue(client.player);
            isFacing = false;
        }

        if (isOpenHandler) return;
        if (switchItem()) return;

        if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.MULTI)){
            boolean multiBreakBooleanValue = MULTI_BREAK.getBooleanValue();
            if (BEDROCK_SWITCH.getBooleanValue()) {
                yDegression = true;
                bedrockMode();
                if(multiBreakBooleanValue) return;
            }
            if (LitematicaMixinMod.EXCAVATE.getBooleanValue()) {
                yDegression = true;
                miningMode();
                if(multiBreakBooleanValue) return;
            }
            if (LitematicaMixinMod.REPLACE_BLOCK.getBooleanValue()) {
                replaceMode();
                if(multiBreakBooleanValue) return;
            }
        }else if (PRINTER_MODE.getOptionListValue() instanceof State.PrintModeType modeType && modeType != PRINTER) {
            switch (modeType){
                case BEDROCK -> {
                    yDegression = true;
                    bedrockMode();
                }
                case EXCAVATE -> {
                    yDegression = true;
                    miningMode();
                }
                case REPLACE_BLOCK -> replaceMode();
            }
            return;
        }

        shouldPrintInAir = PRINT_IN_AIR.getBooleanValue();

        // forEachBlockInRadius:
        BlockPos pos;
        while ((pos = getBlockPos2()) != null) {
            if (client.player != null && !canInteracted(pos)) continue;
            if (!DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;
            BlockState requiredState = worldSchematic.getBlockState(pos);
            //跳过放置
            if (PUT_SKIP.getBooleanValue() &&
                    PUT_SKIP_LIST.getStrings().stream().anyMatch(block -> equalsBlockName(block,requiredState.getBlock())))
            {
                continue;
            }
            //放置冷却
            if (skipPosMap.containsKey(pos)) {
                continue;
            }else {
                skipPosMap.put(pos,0);
            }


            if(USE_EASY_MODE.getBooleanValue()) {
                easyPos = pos;
                WorldUtilsAccessor.doEasyPlaceAction(client);
                easyPos = null;
                if(tickRate != 0) return;
                else continue;
            }
            PlacementGuide.Action action = guide.getAction(world, worldSchematic, pos);
            if (action == null || action.side == null) continue;
            if (playerHasAccessToItems(pEntity, action.clickItems)) {
                // Handle shift and chest placement
                // Won't be required if clickAction
                boolean useShift = false;
                if (requiredState.contains(ChestBlock.CHEST_TYPE)) {
                    switch (requiredState.get(ChestBlock.CHEST_TYPE)) {
                        case SINGLE:
                        case RIGHT: {
                            useShift = true;
                            break ;
                        }
                        case LEFT: {
                            if(world.getBlockState(pos.offset(requiredState.get(ChestBlock.FACING).rotateYClockwise())).isAir()) continue;
                            action.side = requiredState.get(ChestBlock.FACING).rotateYClockwise().getOpposite();
                            useShift = true;
                            break ;
                        }
                    }
                } else if (Implementation.isInteractive(world.getBlockState(pos.offset(action.side)).getBlock())) {
                    useShift = true;
                }

                if (!easyModeBooleanValue && isFacingBlock(requiredState) && isFacing) {
                    continue;
                }
                Direction lookDir = action.getLookDirection();
                //确认侦测器看向方块是否正确
                if (requiredState.isOf(Blocks.OBSERVER) && PUT_TESTING.getBooleanValue()) {
                    BlockPos offset = pos.offset(lookDir);
                    if (isSchematicBlock(offset)) {
                        BlockState state1 = world.getBlockState(offset);
                        State state = State.get(state1, requiredState);
                        if (state != State.CORRECT) continue;
                    }
                }
                if(forcedPlacementBooleanValue) useShift = true;
                //发送放置准备
                action.sendPlacementPreparation(pEntity);
                action.queueAction(pos, useShift);

                Vec3d hitModifier = usePrecisionPlacement(pos, requiredState);
                if(hitModifier != null) {
                    action.hitModifier = hitModifier;
                    action.usePrecisionPlacement = true;
                }
//                if (requiredState.isOf(Blocks.NOTE_BLOCK)) {
//                    action.sendQueue(pEntity);
//                    continue;
//                }

                if (tickRate == 0) {
                    //处理不能快速放置的方块
                    if (hitModifier == null && isFacingBlock(requiredState)
                    ) {
                        item2 = action.clickItems;
                        isFacing = true;
                        currentAction = action;
                        continue;
                    }

                    action.sendQueue(pEntity);
                    continue;
                }
                currentAction = action;
                return;
            }
        }
    }
    public boolean isFacingBlock(BlockState state){
        return state.isOf(Blocks.PISTON) ||
                state.isOf(Blocks.STICKY_PISTON) ||
                //#if MC > 12002
                state.isOf(Blocks.CRAFTER) ||
                //#endif
                state.isOf(Blocks.OBSERVER) ||
                state.isOf(Blocks.DROPPER) ||
                state.isOf(Blocks.DISPENSER);
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
        if (EASY_MODE.getBooleanValue() && stateSchematic != null && pos != null) {
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
                if (state != null && equalsBlockName(blockName, state.getBlock())) {
                    blocks.add(pos);
                }
            }
        }
        return blocks;
    }

    public static ItemStack yxcfItem; //有序存放临时存储

    public void swapHandWithSlot(ClientPlayerEntity player, int slot) {
        ItemStack stack = Implementation.getInventory(player).getStack(slot);
        InventoryUtils.setPickedItemToHand(stack, client);
    }
}