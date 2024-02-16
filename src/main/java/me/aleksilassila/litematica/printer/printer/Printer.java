package me.aleksilassila.litematica.printer.printer;

import com.mojang.brigadier.StringReader;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.interfaces.IClientPlayerInteractionManager;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.mixin.masa.Litematica_InventoryUtilsMixin;
import me.aleksilassila.litematica.printer.printer.bedrockUtils.BreakingFlowController;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.*;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.SearchItem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static fi.dy.masa.litematica.selection.SelectionMode.NORMAL;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST;
import static fi.dy.masa.tweakeroo.tweaks.PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION;
import static me.aleksilassila.litematica.printer.printer.Printer.TempData.max;
import static me.aleksilassila.litematica.printer.printer.Printer.TempData.min;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket.openIng;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.closeScreen;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.SwitchItem.reSwitchItem;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;

;

public class Printer extends PrinterUtils {
    public static boolean up = true;

    public static class TempData {
        public static int[] min;
        public static int[] max;

        public static boolean xuanQuFanWeiNei_p(BlockPos pos) {
            AreaSelection i = DataManager.getSelectionManager().getCurrentSelection();
            if (i == null) return false;
            if (DataManager.getSelectionManager().getSelectionMode() == NORMAL) {
                boolean fw = false;
                List<Box> arr = i.getAllSubRegionBoxes();
                for (int j = 0; j < arr.size(); j++) {
                    if (comparePos(arr.get(j), pos)) {
                        return true;
                    } else {
                        fw = false;
                    }
                }
                return fw;
            } else {
                Box box = i.getSubRegionBox(DataManager.getSimpleArea().getName());
                return comparePos(box, pos);
            }
        }

        static boolean comparePos(Box box, BlockPos pos) {
            int x = 0, y = 0, z = 0;
            if (pos != null) {
                x = pos.getX();
                y = pos.getY();
                z = pos.getZ();
            }
            if (box == null) return false;
            BlockPos kpos1 = box.getPos1();
            BlockPos kpos2 = box.getPos2();
            min = new int[]{
                    kpos1.getX() < kpos2.getX() ? kpos1.getX() : kpos2.getX(),
                    kpos1.getY() < kpos2.getY() ? kpos1.getY() : kpos2.getY(),
                    kpos1.getZ() < kpos2.getZ() ? kpos1.getZ() : kpos2.getZ()
            };
            max = new int[]{
                    kpos1.getX() > kpos2.getX() ? kpos1.getX() : kpos2.getX(),
                    kpos1.getY() > kpos2.getY() ? kpos1.getY() : kpos2.getY(),
                    kpos1.getZ() > kpos2.getZ() ? kpos1.getZ() : kpos2.getZ()
            };
            if (
                    x < min[0] || x > max[0] ||
                            y < min[1] || y > max[1] ||
                            z < min[2] || z > max[2]
            ) {
                return false;
            } else {
                return true;
            }
        }

        public ClientPlayerEntity player;
        public ClientWorld world;
        public WorldSchematic worldSchematic;

        public TempData(ClientPlayerEntity player, ClientWorld world, WorldSchematic worldSchematic) {
            this.player = player;
            this.world = world;
            this.worldSchematic = worldSchematic;
        }
    }

    private static Printer INSTANCE = null;
    @NotNull
    private final MinecraftClient client;
    public final PlacementGuide guide;
    public final Queue queue;
    public int range;

    int tick = 0;

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

    private Printer(MinecraftClient client) {
        this.client = client;

        this.guide = new PlacementGuide(client);
        this.queue = new Queue(this);

        INSTANCE = this;
    }

    int x;
    int y;
    int z;
    boolean getPosIng = false;
    boolean yDegression = false;

    //执行一次获取一个pos
    //方法使用时一定要放在if语句最后 不然会出现获取了pos但是被后面的条件跳过了，这个pos就被跳过了
    BlockPos getBlockPos() {
        ClientPlayerEntity player = client.player;
        if (player == null) return null;
        if (!getPosIng) {
            x = -range;
            z = -range;
            y = yDegression ? range : -range;
            getPosIng = true;
        }
        if (x < range + 1) {
            BlockPos up1 = player.getBlockPos().north(x).west(z).up(y);
            x++;
            return up1;
        }

        if (z < range + 1) {
            x = -range;
            BlockPos up1 = player.getBlockPos().north(x).west(z).up(y);
            z++;
            x++;
            return up1;
        }
        if (yDegression ? y > -range - 1 : y < range + 1) {
            x = -range;
            z = -range;
            BlockPos up1 = player.getBlockPos().north(x).west(z).up(y);
            x++;
            z++;
            if (yDegression) y--;
            else y++;
            return up1;
        }
        getPosIng = false;
        return null;
    }

    /*
    Fixme legit mode:
        - scaffoldings
    Fixme other:
        - signs
        - rotating blocks (signs, skulls)
     */
    void fluidMode() {

//        for (int y = range; y > -range - 1; y--) {
//            for (int x = -range; x < range + 1; x++) {
//                for (int z = -range; z < range + 1; z++) {
        BlockPos pos;
        while (!timedOut() && (pos = getBlockPos()) != null && client.world != null && client.player != null) {
            BlockState currentState = client.world.getBlockState(pos);
            if (client.player != null && !canInteracted(pos, range)) continue;
            if (!TempData.xuanQuFanWeiNei_p(pos)) continue;
            if (!DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;
            if (currentState.getFluidState().isOf(Fluids.LAVA) || currentState.getFluidState().isOf(Fluids.WATER)) {
                blocklist = LitematicaMixinMod.FLUID_BLOCK_LIST.getStrings();
                for (int i = 0; i < blocklist.size(); i++) {
                    try {
                        ItemStringReader.ItemResult itemResult = ItemStringReader.item(Registries.ITEM.getReadOnlyWrapper(), new StringReader(blocklist.get(i)));
                        Item item = itemResult.item().value();
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
        while (!timedOut() && (pos = tempPos == null ? getBlockPos() : tempPos) != null) {

//        for (int y = range; y > -range - 1; y--) {
//            for (int x = -range; x < range + 1; x++) {
//                for (int z = -range; z < range + 1; z++) {
//                    BlockPos pos = data.player.getBlockPos().north(x).west(z).up(y);

            if (client.player != null && !canInteracted(pos, range)) {
                if (tempPos == null) continue;
                tempPos = null;
                continue;
            }
            if (!DataManager.getRenderLayerRange().isPositionWithinRange(pos)) {
                if (tempPos == null) continue;
                tempPos = null;
                continue;
            }
            if (TempData.xuanQuFanWeiNei_p(pos) && waJue(pos)) {
                tempPos = pos;
                return;
            }
            tempPos = null;
        }
//            }
//        }
    }

    public static boolean waJue(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        BlockState currentState = world.getBlockState(pos);
        if (
                !currentState.isAir() &&
                        !currentState.isOf(Blocks.AIR) &&
                        !currentState.isOf(Blocks.CAVE_AIR) &&
                        !currentState.isOf(Blocks.VOID_AIR) &&
                        !(currentState.getBlock().getHardness() == -1) &&
                        !(currentState.getBlock() instanceof FluidBlock) &&
                        !client.player.isBlockBreakingRestricted(client.world, pos, client.interactionManager.getCurrentGameMode()) &&

                        twBreakRestriction(currentState)
        ) {
            client.interactionManager.updateBlockBreakingProgress(pos, Direction.DOWN);
            client.interactionManager.cancelBlockBreaking();
            return !world.getBlockState(pos).isOf(Blocks.AIR);
        }
        return false;
    }

    static boolean twBreakRestriction(BlockState blockState) {
        if (!FabricLoader.getInstance().isModLoaded("tweakeroo")) return true;
        UsageRestriction.ListType listType = BLOCK_TYPE_BREAK_RESTRICTION.getListType();
        if (listType == UsageRestriction.ListType.BLACKLIST) {
            return BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST.getStrings().stream()
                    .noneMatch(string -> Registries.BLOCK.getId(blockState.getBlock()).toString().contains(string));
        } else if (listType == UsageRestriction.ListType.WHITELIST) {
            return BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST.getStrings().stream()
                    .anyMatch(string -> Registries.BLOCK.getId(blockState.getBlock()).toString().contains(string));
        } else {
            return true;
        }
    }


    //此模式依赖bug运行 请勿随意修改
    public void jymod() {
        BreakingFlowController.tick();
        int maxy = -9999;
        range = bedrockModeRange();
        BlockPos pos;
        while (!timedOut() && (pos = getBlockPos()) != null && client.world != null) {
//        for (int y = range; y > -range - 1; y--) {
//            for (int x = -range; x < range + 1; x++) {
//                for (int z = -range; z < range + 1; z++) {
//                    BlockPos pos = data.player.getBlockPos().north(x).west(z).up(y);
            if (!ZxyUtils.bedrockCanInteracted(pos, range)) continue;
            BlockState currentState = client.world.getBlockState(pos);
//                    if (currentState.isOf(Blocks.PISTON) && !data.world.getBlockState(pos.down()).isOf(Blocks.BEDROCK)) {
            if (currentState.isOf(Blocks.PISTON) && !bedrockModeTarget(client.world.getBlockState(pos.down()).getBlock())) {
                BreakingFlowController.addPosList(pos);
            } else if (currentState.isOf(Blocks.PISTON_HEAD)) {
                switchToItems(client.player, new Item[]{Items.AIR, Items.DIAMOND_PICKAXE});
                ((IClientPlayerInteractionManager) client.interactionManager)
                        .rightClickBlock(pos, Direction.UP, Vec3d.ofCenter(pos));
            }

//                    if (TempData.xuanQuFanWeiNei_p(pos) && currentState.isOf(Blocks.BEDROCK)  && ZxyUtils.canInteracted(pos,range-1.5) && !client.world.getBlockState(pos.up()).isOf(Blocks.BEDROCK)) {
            if (TempData.xuanQuFanWeiNei_p(pos) && bedrockModeTarget(currentState.getBlock()) && ZxyUtils.bedrockCanInteracted(pos, range - 1.5) && !bedrockModeTarget(client.world.getBlockState(pos.up()).getBlock())) {
                if (maxy == -9999) maxy = y;
                if (y < maxy) return;
                BreakingFlowController.addBlockPosToList(pos);
            }
        }
//                }
//            }
//        }
    }

    public static int bedrockModeRange() {
        return LitematicaMixinMod.RANGE_MODE.getOptionListValue() == State.ListType.SPHERE ? getPrinterRange() : 6;
    }

    public static boolean bedrockModeTarget(Block block) {
        return LitematicaMixinMod.BEDROCK_LIST.getStrings().stream().anyMatch(string -> Registries.BLOCK.getId(block).toString().contains(string));
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
            new Verify(address, client.player);
            return false;
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
    static LinkedList<TempPos> tempList = new LinkedList<>();
    public static boolean printerMemorySync = false;

    static class TempPos {
        public TempPos(BlockPos pos, int tick) {
            this.pos = pos;
            this.tick = tick;
        }

        public BlockPos pos;
        public int tick;
    }

    public boolean switchItem() {
        if (!items2.isEmpty() && !isOpenHandler && !openIng) {
            ClientPlayerEntity player = client.player;
            ScreenHandler sc = player.currentScreenHandler;
            if (!player.currentScreenHandler.equals(player.playerScreenHandler)) return false;
            //排除合成栏 装备栏 副手
            if (sc.slots.stream().skip(9).limit(sc.slots.size() - 10).noneMatch(slot -> slot.getStack().isEmpty())
                    && (LitematicaMixinMod.QUICKSHULKER.getBooleanValue() || LitematicaMixinMod.INVENTORY.getBooleanValue())) {
                SwitchItem.checkItems();
                return true;
            }
            if (LitematicaMixinMod.QUICKSHULKER.getBooleanValue() && openShulker(items2)) {
                return true;
            } else if (LitematicaMixinMod.INVENTORY.getBooleanValue()) {
                for (Item item : items2) {
                    MemoryUtils.currentMemoryKey = client.world.getDimensionKey().getValue();
                    MemoryUtils.itemStack = new ItemStack(item);
                    if (SearchItem.search(true)) {
                        Statistics.closeScreen++;
                        isOpenHandler = true;
                        printerMemorySync = true;
                        return true;
                    }
//                    MemoryDatabase database = MemoryDatabase.getCurrent();
//                    if (database != null) {
//                        for (Identifier dimension : database.getDimensions()) {
//                            for (Memory memory : database.findItems(item.getDefaultStack(), dimension)) {
//                                OpenInventoryPacket.sendOpenInventory(memory.getPosition(), RegistryKey.of(RegistryKeys.WORLD, dimension));
//                                isOpenHandler = true;
//                                return;
//                            }
//                        }
//                    }
                }
                items2 = new HashSet<>();
                isOpenHandler = false;
            }
        }
        return false;
    }

    //根据当前毫秒值判断是否超出了屏幕刷新率
    boolean timedOut() {
        if (frameGenerationTime == 0) return System.currentTimeMillis() > 15 + startTime;
        return System.currentTimeMillis() > frameGenerationTime + startTime;
    }

    long startTime;

    public void tick() {
        if (!verify()) return;
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        ClientPlayerEntity pEntity = client.player;
        ClientWorld world = client.world;

        if (range != getPrinterRange()) getPosIng = false;
        yDegression = false;
        startTime = System.currentTimeMillis();
        tickRate = LitematicaMixinMod.PRINT_INTERVAL.getIntegerValue();
        range = getPrinterRange();
        tick = tick == 0x7fffffff ? 0 : tick + 1;

        if (tickRate != 0) {
            if (tick % tickRate != 0) {
                return;
            }
            queue.sendQueue(client.player);
        }
        if (isFacing) {
            switchToItems(pEntity, item2);
            queue.sendQueue(client.player);
            isFacing = false;
        }

        if (isOpenHandler) return;
        if (switchItem()) return;

        if (LitematicaMixinMod.BEDROCK_SWITCH.getBooleanValue()) {
            yDegression = true;
            jymod();
            return;
        }
        if (LitematicaMixinMod.EXCAVATE.getBooleanValue()) {
            yDegression = true;
            miningMode();
            return;
        }
        if (LitematicaMixinMod.FLUID.getBooleanValue()) {
            fluidMode();
            return;
        }
        for (TempPos tempPos : tempList) tempPos.tick++;
        LitematicaMixinMod.shouldPrintInAir = LitematicaMixinMod.PRINT_IN_AIR.getBooleanValue();
        // forEachBlockInRadius:
        BlockPos pos;
        z:
        while (!timedOut() && (pos = getBlockPos()) != null) {
//        for (int y = -range; y < range + 1; y++) {
//            for (int x = -range; x < range + 1; x++) {
//                z:
//                for (int z = -range; z < range + 1; z++) {
            BlockState requiredState = worldSchematic.getBlockState(pos);
            if (client.player != null && !canInteracted(pos, range)) continue;
            PlacementGuide.Action action = guide.getAction(world, worldSchematic, pos);
            if (requiredState.isOf(Blocks.NETHER_PORTAL) ||
                    requiredState.isOf(Blocks.END_PORTAL)
            ) continue;
            //跳过侦测器和红石块的放置
            if ((requiredState.isOf(Blocks.OBSERVER) || requiredState.isOf(Blocks.REDSTONE_BLOCK)) && !LitematicaMixinMod.SKIP.getBooleanValue()) {
                continue;
            }

            if (!DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;
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
                    BlockPos leftNeighbor = pos.offset(requiredState.get(ChestBlock.FACING).rotateYClockwise());
                    BlockState leftState = world.getBlockState(leftNeighbor);

                    switch (requiredState.get(ChestBlock.CHEST_TYPE)) {
                        case SINGLE:
                        case RIGHT: {
                            useShift = true;
                            break;
                        }
                        case LEFT: { // Actually right
                            if (leftState.contains(ChestBlock.CHEST_TYPE) && leftState.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) {
                                useShift = false;

                                // Check if it is possible to place without shift
                                if (Implementation.isInteractive(world.getBlockState(pos.offset(side)).getBlock())) {
                                    continue;
                                }
                            } else {
                                continue;
                            }
                            break;
                        }
                    }
                } else if (Implementation.isInteractive(world.getBlockState(pos.offset(side)).getBlock())) {
                    useShift = true;
                }

                Direction lookDir = action.getLookDirection();

                if ((requiredState.isOf(Blocks.PISTON) ||
                        requiredState.isOf(Blocks.STICKY_PISTON) ||
                        requiredState.isOf(Blocks.OBSERVER) ||
                        requiredState.isOf(Blocks.DROPPER) ||
                        requiredState.isOf(Blocks.DISPENSER)) && isFacing
                ) {
                    continue;
                }
                //发送放置准备
                sendPlacementPreparation(pEntity, requiredItems, lookDir);
                action.queueAction(queue, pos, side, useShift, lookDir != null);


//                        if(lookDir != null){
//                            requiredItems2 = action.getRequiredItems(requiredState.getBlock());
//                            isFacing = true;
//                            continue;
//                        }

                if (requiredState.isOf(Blocks.NOTE_BLOCK)) {
                    queue.sendQueue(pEntity);
                    continue;
                }
                if (tickRate == 0) {
                    //处理不能快速放置的方块
                    if (requiredState.isOf(Blocks.PISTON) ||
                            requiredState.isOf(Blocks.STICKY_PISTON) ||
                            requiredState.isOf(Blocks.OBSERVER) ||
                            requiredState.isOf(Blocks.DROPPER) ||
                            requiredState.isOf(Blocks.DISPENSER)
                    ) {
                        item2 = requiredItems;
                        isFacing = true;
                        continue;
                    }
                    for (int i = 0; i < tempList.size(); i++) {
                        if (tempList.get(i).tick > 3) {
                            tempList.remove(i);
                            i--;
                            continue;
                        }
                        if (this.queue.target.equals(tempList.get(i).pos) && tempList.get(i).tick < 3) {
                            this.queue.clearQueue();
                            continue z;
                        }

                    }
                    tempList.add(new TempPos(this.queue.target, 0));
                    queue.sendQueue(pEntity);
                    continue;
                }
                return;
            }
//                }
//            }
//        }
        }
    }

    public LinkedList<BlockPos> siftBlock(String blockName) {
        LinkedList<BlockPos> blocks = new LinkedList<>();
        AreaSelection i = DataManager.getSelectionManager().getCurrentSelection();
        List<Box> box;
        if (i == null) return blocks;
        box = i.getAllSubRegionBoxes();
        for (int index = 0; index < box.size(); index++) {
            TempData.comparePos(box.get(index), null);
            for (int x = min[0]; x <= max[0]; x++) {
                for (int y = min[1]; y <= max[1]; y++) {
                    for (int z = min[2]; z <= max[2]; z++) {
                        BlockPos pos = new BlockPos(new BlockPos(x, y, z));
                        BlockState state = null;
                        if (client.world != null) {
                            state = client.world.getBlockState(pos);
                        }
                        Block block = state.getBlock();
                        if (Registries.BLOCK.getId(block).toString().contains(blockName)) {
                            blocks.add(pos);
                        }
                    }
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
                                MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.of("没有可替换的槽位，请将预选位的濳影盒换个位置"), false);
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
                                SwitchItem.newItem(slots.get(y).getStack(), OpenInventoryPacket.pos, OpenInventoryPacket.key, y, shulkerBoxSlot);
                            } else SwitchItem.newItem(slots.get(y).getStack(), null, null, y, shulkerBoxSlot);
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
        player.closeHandledScreen();
    }

    static int shulkerBoxSlot = -1;

    boolean openShulker(HashSet<Item> items) {
        for (Item item : items) {
            ScreenHandler sc = MinecraftClient.getInstance().player.playerScreenHandler;
//            if(!MinecraftClient.getInstance().player.currentScreenHandler.equals(sc))return false;
            for (int i = 9; i < sc.slots.size(); i++) {
                ItemStack stack = sc.slots.get(i).getStack();
                String itemid = Registries.ITEM.getId(stack.getItem()).toString();
                if (itemid.contains("shulker_box")) {
                    DefaultedList<ItemStack> items1 = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack, -1);
                    if (items1.stream().anyMatch(s1 -> s1.getItem().equals(item))) {
                        try {
                            if (reSwitchItem == null) shulkerBoxSlot = i;
//                            ClientUtil.CheckAndSend(stack,i);
                            Class quickShulker = Class.forName("net.kyrptonaught.quickshulker.client.ClientUtil");
                            Method checkAndSend = quickShulker.getDeclaredMethod("CheckAndSend", ItemStack.class, int.class);
                            checkAndSend.invoke(checkAndSend, stack, i);
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

    public void switchToItems(ClientPlayerEntity player, Item[] items) {
        if (items == null) return;
        PlayerInventory inv = Implementation.getInventory(player);
        for (Item item : items) {
            if (inv.getMainHandStack().getItem() == item) {
                return;
            }
        }
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

            hitModifier = new Vec3d(hitModifier.z, hitModifier.y, hitModifier.x);
            hitModifier = hitModifier.rotateY((direction.asRotation() + 90) % 360);

            Vec3d hitVec = Vec3d.ofCenter(target)
                    .add(Vec3d.of(side.getVector()).multiply(0.5))
                    .add(hitModifier.multiply(0.5));

            if (shift && !wasSneaking)
                player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            else if (!shift && wasSneaking)
                player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));

            ItemStack mainHandStack1 = printerInstance.client.player.getMainHandStack();
            ItemStack mainHandStack2 = printerInstance.client.player.getMainHandStack().copy();
            ((IClientPlayerInteractionManager) printerInstance.client.interactionManager)
                    .rightClickBlock(target, side, hitVec);
            if (mainHandStack1.isEmpty()) {
                SwitchItem.removeItem(mainHandStack2);
            } else SwitchItem.syncUseTime(mainHandStack1);
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