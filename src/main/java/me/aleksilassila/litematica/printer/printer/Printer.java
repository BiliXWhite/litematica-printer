package me.aleksilassila.litematica.printer.printer;

import com.mojang.brigadier.StringReader;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.interfaces.IClientPlayerInteractionManager;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.printer.memory.Memory;
import me.aleksilassila.litematica.printer.printer.memory.MemoryDatabase;
import me.aleksilassila.litematica.printer.printer.utils.BreakingFlowController;
import me.aleksilassila.litematica.printer.printer.zxy.OpenInventoryPacket;
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
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static fi.dy.masa.litematica.selection.SelectionMode.NORMAL;
import static me.aleksilassila.litematica.printer.printer.Printer.TempData.max;
import static me.aleksilassila.litematica.printer.printer.Printer.TempData.min;
import static me.aleksilassila.litematica.printer.printer.zxy.OpenInventoryPacket.openIng;

public class Printer extends PrinterUtils {
    public static boolean up = true;

    public class TempData {
        public static int[] min;
        public static int[] max;
        public static boolean xuanQuFanWeiNei(BlockPos pos) {
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
            int x=0,y=0,z=0;
            if(pos!=null){
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

    private static Printer INSTANCE;
    @NotNull
    private final MinecraftClient client;
    public final PlacementGuide guide;
    public final Queue queue;

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

    /*
    Fixme legit mode:
        - scaffoldings
    Fixme other:
        - signs
        - rotating blocks (signs, skulls)
     */
    void fluidMode(TempData data){
        int range = LitematicaMixinMod.PRINTING_RANGE.getIntegerValue();

        for (int y = range; y > -range - 1; y--) {
            for (int x = -range; x < range + 1; x++) {
                for (int z = -range; z < range + 1; z++) {
                    BlockPos pos = data.player.getBlockPos().north(x).west(z).up(y);
                    BlockState currentState = data.world.getBlockState(pos);
                    if (!TempData.xuanQuFanWeiNei(pos)) continue;
                    if (!DataManager.getRenderLayerRange().isPositionWithinRange(pos)) continue;
                    if (currentState.getFluidState().isOf(Fluids.LAVA) || currentState.getFluidState().isOf(Fluids.WATER)) {
                        blocklist = LitematicaMixinMod.FLUID_BLOCK_LIST.getStrings();
                        for(int i = 0;i<blocklist.size();i++){
                            try {
                                Item item = Registries.ITEM.get(new Identifier(blocklist.get(i)));
                                fluidList.add(item);
                            } catch (Exception e) {
                            }
                        }
                        switchToItems(data.player, fluidList.toArray(new Item[fluidList.size()]));
                        Item item = Implementation.getInventory(data.player).getMainHandStack().getItem();
                        String itemid = Registries.ITEM.getId(item).toString();
                        if(!blocklist.stream().anyMatch(b -> itemid.contains(b) || item.getName().toString().contains(b))) {
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
                }
            }
        }
    }

    void miningMode(TempData data) {
        for (int y = range; y > -range - 1; y--) {
            for (int x = -range; x < range + 1; x++) {
                for (int z = -range; z < range + 1; z++) {
                    BlockPos pos = data.player.getBlockPos().north(x).west(z).up(y);
//                    if (requiredState.isOf(Blocks.GLASS) && !currentState.isOf(Blocks.AIR) && !currentState.isOf(Blocks.BEDROCK)) {
                    if (TempData.xuanQuFanWeiNei(pos) && waJue(pos)) return;
                }
            }
        }
    }

    public static boolean waJue(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        BlockState currentState = world.getBlockState(pos);
//                    if (requiredState.isOf(Blocks.GLASS) && !currentState.isOf(Blocks.AIR) && !currentState.isOf(Blocks.BEDROCK)) {
        if (
                !currentState.isAir() &&
                !currentState.isOf(Blocks.AIR) &&
                !currentState.isOf(Blocks.CAVE_AIR) &&
                !currentState.isOf(Blocks.VOID_AIR) &&
                currentState.getBlock().getHardness() != -1 &&
                !(currentState.getBlock() instanceof FluidBlock) &&
                !client.player.isBlockBreakingRestricted(client.world, pos, client.interactionManager.getCurrentGameMode())
        ) {
            client.interactionManager.updateBlockBreakingProgress(pos, Direction.DOWN);
            client.interactionManager.cancelBlockBreaking();
            return !world.getBlockState(pos).isOf(Blocks.AIR);
        }
        return false;
    }


    public void jymod(TempData data) {
        int range = 3;
        BreakingFlowController.tick();
        int maxy = -9999;
        for (int y = range; y > -range - 1; y--) {
            for (int x = -range; x < range + 1; x++) {
                for (int z = -range; z < range + 1; z++) {
                    BlockPos pos = data.player.getBlockPos().north(x).west(z).up(y);
                    BlockState currentState = data.world.getBlockState(pos);
//                    BlockState requiredState = data.worldSchematic.getBlockState(pos);

//                    if (requiredState.isOf(Blocks.GLASS) && currentState.isOf(Blocks.BEDROCK)) {
                    if (currentState.isOf(Blocks.PISTON) && !data.world.getBlockState(pos.down()).isOf(Blocks.BEDROCK)) {
                        BreakingFlowController.poslist.add(pos);
                    }

                    if (TempData.xuanQuFanWeiNei(pos) && currentState.isOf(Blocks.BEDROCK) && pos.down().isWithinDistance(data.player.getPos(), 4f) && !client.world.getBlockState(pos.up()).isOf(Blocks.BEDROCK)) {
                        if (maxy == -9999) maxy = y;
                        if (y < maxy) return;
                        BreakingFlowController.addBlockPosToList(pos);
                    }
                }
            }
        }
    }
    public boolean verify() {
        if (client.isInSingleplayer()) return true;
        String address = null;
        try {
            address = client.getCurrentServerEntry().address.split(":")[0];
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
        if (Verify.getVerify() == null) {
            new Verify(address, client.player);
            return false;
        } else {
            return Verify.getVerify().tick(address);
        }
    }
    public int range;
    int tickRate;
    boolean isFacing = false;
    Item[] item2 = null;
    List<String> blocklist;
    public static HashSet<Item> items2 = new HashSet<>();
    public static HashSet<Item> fluidList = new HashSet<>();
    static LinkedList<TempPos> tempList = new LinkedList<>();
    class TempPos{
        public TempPos(BlockPos pos, int tick) {
            this.pos = pos;
            this.tick = tick;
        }

        public BlockPos pos;
        public int tick;
    }
    public void tick() {
        if (!verify()) return;
        TempData data = new TempData(client.player, client.world, SchematicWorldHandler.getSchematicWorld());
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        ClientPlayerEntity pEntity = client.player;
        ClientWorld world = client.world;

        tickRate = LitematicaMixinMod.PRINT_INTERVAL.getIntegerValue();
        range = LitematicaMixinMod.PRINTING_RANGE.getIntegerValue();
        tick = tick == 0x7fffffff ? 0 : tick + 1;

        if (tickRate != 0) {
            if (tick % tickRate != 0) {
                return;
            }
            queue.sendQueue(data.player);
        }

        if (isFacing) {
            switchToItems(pEntity, item2);
            queue.sendQueue(data.player);
            isFacing = false;
        }

        if(items2.size() != 0 && !isOpenHandler && !openIng){
            if(LitematicaMixinMod.QUICKSHULKER.getBooleanValue() && openShulker(items2)){
                return;
            }else if(LitematicaMixinMod.INVENTORY.getBooleanValue()){
                for (Item item : items2) {
                    MemoryDatabase database = MemoryDatabase.getCurrent();
                    if (database != null) {
                        for (Identifier dimension : database.getDimensions()) {
                            for (Memory memory : database.findItems(item.getDefaultStack(), dimension)) {
                                OpenInventoryPacket.sendOpenInventory(memory.getPosition(), RegistryKey.of(RegistryKeys.WORLD, dimension));
                                isOpenHandler = true;
                                return;
                            }
                        }
                    }
                }
                items2 = new HashSet<>();
                isOpenHandler = false;
            }
        }


        if(isOpenHandler)return;

        if (LitematicaMixinMod.BEDROCK.getBooleanValue()) {
            jymod(data);
            return;
        }
        if (LitematicaMixinMod.EXCAVATE.getBooleanValue()) {
            miningMode(data);
            return;
        }
        if (LitematicaMixinMod.FLUID.getBooleanValue()) {
            fluidMode(data);
            return;
        }
        for (TempPos tempPos : tempList) tempPos.tick++;
        LitematicaMixinMod.shouldPrintInAir = LitematicaMixinMod.PRINT_IN_AIR.getBooleanValue();
        LitematicaMixinMod.shouldReplaceFluids = LitematicaMixinMod.REPLACE_FLUIDS.getBooleanValue();
        // forEachBlockInRadius:
        for (int y = -range; y < range + 1; y++) {
            for (int x = -range; x < range + 1; x++) {
                z:
                for (int z = -range; z < range + 1; z++) {
                    BlockPos pos = pEntity.getBlockPos().north(x).west(z).up(y);
                    BlockState requiredState = worldSchematic.getBlockState(pos);
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
                    if (playerHasAccessToItems(pEntity, requiredItems)){
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
                                if(tempList.get(i).tick>3){
                                    tempList.remove(i);
                                    i--;
                                    continue;
                                }
                                if(this.queue.target.equals(tempList.get(i).pos) && tempList.get(i).tick < 3){
                                    this.queue.clearQueue();
                                    continue z;
                                }

                            }
                            tempList.add(new TempPos(this.queue.target,0));
                            queue.sendQueue(pEntity);
                            continue;
                        }
                        return;
                    }
                }
            }
        }
    }
    public ArrayList<BlockPos> siftBlock(String blockName) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        AreaSelection i = DataManager.getSelectionManager().getCurrentSelection();
        List<Box> box;
        if (i == null) return null;
        box = i.getAllSubRegionBoxes();
        for (int index = 0; index < box.size(); index++) {
            TempData.comparePos(box.get(index), null);
            for (int x = min[0]; x <= max[0]; x++) {
                for (int y = min[1]; y <= max[1]; y++) {
                    for (int z = min[2]; z <= max[2]; z++) {
                        BlockPos pos = new BlockPos(new BlockPos(x, y, z));
                        BlockState state = client.world.getBlockState(pos);
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
    public void switchInv(){
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ScreenHandler sc = player.currentScreenHandler;
        if(sc.equals(player.playerScreenHandler)){
            return;
        }
        for(Item item : items2) {
//            System.out.println(Arrays.toString(items2));
            for (int y = 0; y < sc.slots.size(); y++) {
                if (sc.slots.get(y).getStack().getItem().equals(item)) {
                    String[] str = Configs.Generic.PICK_BLOCKABLE_SLOTS.getStringValue().split(",");
                    if(str.length==0) return;
                    for (String s : str) {
                        if (s == null) break;
                        try {
                            int c = Integer.parseInt(s) - 1;
                            if (Registries.ITEM.getId(player.getInventory().getStack(c).getItem()).toString().contains("shulker_box") && LitematicaMixinMod.QUICKSHULKER.getBooleanValue()) {
                                MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.of("无可替换的槽位，请将预选位的濳影盒换个位置"),false);
                                continue;
                            }
//                            System.out.println(y);
//                            System.out.println(c);
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
        items2 = new HashSet<>();
        isOpenHandler = false;
        player.closeHandledScreen();

    }

    boolean openShulker(HashSet<Item> items){
        for (Item item : items) {
            ScreenHandler sc = MinecraftClient.getInstance().player.playerScreenHandler;
//            if(!MinecraftClient.getInstance().player.currentScreenHandler.equals(sc))return false;
            for (int i = 0; i < sc.slots.size(); i++) {
                ItemStack stack = sc.slots.get(i).getStack();
                if(i<9){
                    int i2 = i+36;
                    stack = sc.slots.get(i2).getStack();
                }
                Item item2 = sc.slots.get(i).getStack().getItem();
                String itemid = Registries.ITEM.getId(item2).toString();
                if(itemid.contains("shulker_box")){
                    DefaultedList<ItemStack> items1 = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack, -1);
                    if(items1.stream().anyMatch(s1 -> s1.getItem().equals(item))){
                        try {
                            Class quickShulker = Class.forName("net.kyrptonaught.quickshulker.client.ClientUtil");
                            Method checkAndSend = quickShulker.getDeclaredMethod("CheckAndSend",ItemStack.class,int.class);
                            if(i<9) i+=36;
                            checkAndSend.invoke(checkAndSend,stack,i);
                            isOpenHandler = true;
                            //                                    System.out.println("open "+b);
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

            ((IClientPlayerInteractionManager) printerInstance.client.interactionManager)
                    .rightClickBlock(target, side, hitVec);

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