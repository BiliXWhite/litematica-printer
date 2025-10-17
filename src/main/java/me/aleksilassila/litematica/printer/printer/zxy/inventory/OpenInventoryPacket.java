package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import fi.dy.masa.malilib.util.StringUtils;
import io.netty.buffer.Unpooled;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.PacketByteBuf;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;


//#if MC >= 12001
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import red.jackf.chesttracker.api.providers.InteractionTracker;
//#endif

//#if MC < 11904
//$$ import net.minecraft.util.registry.Registry;
//#else
import net.minecraft.registry.RegistryKeys;
//#endif

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.Printer.printerMemorySync;
import static net.minecraft.block.ShulkerBoxBlock.FACING;
//#if MC > 12004
import net.minecraft.block.Block;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.HelloPackage.HELLO_REMOTE_INTERACTIONS_ID;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.OpenPackage.OPEN_INVENTORY_ID;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.ReturnPackage.OPEN_RETURN_ID;
//#else
//$$import net.minecraft.util.Hand;
//#endif
public class OpenInventoryPacket {

    @NotNull
    static MinecraftClient client = MinecraftClient.getInstance();

    //#if MC > 12104
    //$$ private static final ChunkTicketType OPEN_TICKET = ChunkTicketType.UNKNOWN;
    //#else
    private static final ChunkTicketType<ChunkPos> OPEN_TICKET = ChunkTicketType.create("openInv", Comparator.comparingLong(ChunkPos::toLong), 2);
    //#endif
    public static HashMap<ServerPlayerEntity, TickList> tickMap = new HashMap<>();
    public static boolean openIng = false;
    public static RegistryKey<World> key = null;
    public static BlockPos pos = null;
    public static boolean isRemote = false;
    public static boolean clientTry = false;
    public static long clientTryTime = 0;
    public static long remoteTime = 0;
    //#if MC > 12006
    private static final Identifier OPEN_INVENTORY = Identifier.of("remoteinventory", "open_inventory");
    private static final Identifier OPEN_RETURN = Identifier.of("openreturn", "open_return");
    private static final Identifier HELLO_REMOTE_INTERACTIONS = Identifier.of("hello", "hello_remote_interactions");
    //#else
    //$$ private static final Identifier OPEN_INVENTORY = new Identifier("remoteinventory", "open_inventory");
    //$$ private static final Identifier OPEN_RETURN = new Identifier("openreturn", "open_return");
    //$$ private static final Identifier HELLO_REMOTE_INTERACTIONS = new Identifier("hello", "hello_remote_interactions");
    //#endif
    public static ArrayList<ServerPlayerEntity> playerlist = new ArrayList<>();

    //#if MC > 12004
    public static class OpenPackage implements CustomPayload{
       public static final Id<OpenPackage> OPEN_INVENTORY_ID = new Id<>(OPEN_INVENTORY);
       public static final PacketCodec<RegistryByteBuf,OpenPackage> CODEC = new PacketCodec<>() {

           @Override
           public void encode(RegistryByteBuf buf, OpenPackage value) {
               buf.writeRegistryKey(value.world);
               buf.writeBlockPos(value.pos);
           }
           @Override
           public OpenPackage decode(RegistryByteBuf buf) {
               OpenPackage openPackage = new OpenPackage();
               openPackage.world = buf.readRegistryKey(RegistryKeys.WORLD);
               openPackage.pos = buf.readBlockPos();
               return openPackage;
           }
       };
       RegistryKey<World> world = null;
       BlockPos pos = null;
       public OpenPackage() {
       }
        @Override
        public Id<? extends CustomPayload> getId() {
            return OPEN_INVENTORY_ID;
        }
    }

    public static class HelloPackage implements CustomPayload{
        public static final Id<HelloPackage> HELLO_REMOTE_INTERACTIONS_ID = new Id<>(HELLO_REMOTE_INTERACTIONS);
        public static final PacketCodec<RegistryByteBuf,HelloPackage> CODEC = new PacketCodec<>() {
            @Override
            public void encode(RegistryByteBuf buf, HelloPackage value) {
            }
            @Override
            public HelloPackage decode(RegistryByteBuf buf) {
                return new HelloPackage();
            }
        };
        @Override
        public Id<? extends CustomPayload> getId() {
            return HELLO_REMOTE_INTERACTIONS_ID;
        }
    }
    public static class ReturnPackage implements CustomPayload{
        BlockState state = null;
        boolean isOpen = false;
        public static final Id<ReturnPackage> OPEN_RETURN_ID = new Id<>(OPEN_RETURN);
        public static final PacketCodec<RegistryByteBuf,ReturnPackage> CODEC = new PacketCodec<>() {
            @Override
            public void encode(RegistryByteBuf buf, ReturnPackage value) {
                buf.writeInt(Block.getRawIdFromState(value.state));
                buf.writeBoolean(value.isOpen);
            }
            @Override
            public ReturnPackage decode(RegistryByteBuf buf) {
                ReturnPackage returnPackage = new ReturnPackage();
                returnPackage.state = Block.getStateFromRawId(buf.readInt());
                returnPackage.isOpen = buf.readBoolean();
                return returnPackage;
            }
        };

        @Override
        public Id<? extends CustomPayload> getId() {
            return OPEN_RETURN_ID;
        }
    }
    //#endif
    public static void registerClientReceivePacket() {
        //#if MC > 12004
        ClientPlayNetworking.registerGlobalReceiver(OPEN_RETURN_ID, (payload, context) -> {
            try {
                if (payload instanceof ReturnPackage returnPackage) {
                    boolean isOpen = returnPackage.isOpen;
                    BlockState state = returnPackage.state;
                    client.execute(() -> openReturn(isOpen,state));
                }
            } catch (Exception ignored) {
                ZxyUtils.actionBar("服务端回复异常，箱子追踪库存无法更新");
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(HELLO_REMOTE_INTERACTIONS_ID,(openInventoryPacket,context) -> {
            isRemote = true;
            client.execute(() -> {
                if (LitematicaMixinMod.AUTO_INVENTORY.getBooleanValue()) {
                    ZxyUtils.actionBar("已自动启用远程交互容器!!!");
                    LitematicaMixinMod.CLOUD_INVENTORY.setBooleanValue(true);
                }
            });
        });
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(OPEN_RETURN, (client, playNetworkHandler, packetByteBuf, packetSender) -> {
        //$$     try {
        //$$         MyPacket packet = MyPacket.decode(packetByteBuf);
        //$$         client.execute(() -> {
        //$$             client.execute(() -> openReturn(packet.isOpen(), packet.blockState()));
        //$$         });
        //$$     } catch (Exception ignored) {
        //$$         ZxyUtils.actionBar("服务端回复异常，箱子追踪库存无法更新");
        //$$     }
        //$$ });
        //$$ ClientPlayNetworking.registerGlobalReceiver(HELLO_REMOTE_INTERACTIONS, (client, playNetworkHandler, packetByteBuf, packetSender) -> {
        //$$     isRemote = true;
        //$$     client.execute(() -> {
        //$$         if (LitematicaMixinMod.AUTO_INVENTORY.getBooleanValue()) {
        //$$             ZxyUtils.actionBar("已自动启用远程交互容器!!!");
        //$$             LitematicaMixinMod.CLOUD_INVENTORY.setBooleanValue(true);
        //$$         }
        //$$     });
        //$$ });
        //#endif
    }
    public static void init() {
        //#if MC > 12004
        PayloadTypeRegistry.playC2S().register(OPEN_INVENTORY_ID, OpenPackage.CODEC);
        PayloadTypeRegistry.playC2S().register(OPEN_RETURN_ID, ReturnPackage.CODEC);
        PayloadTypeRegistry.playC2S().register(HELLO_REMOTE_INTERACTIONS_ID, HelloPackage.CODEC);
        PayloadTypeRegistry.playS2C().register(OPEN_INVENTORY_ID, OpenPackage.CODEC);
        PayloadTypeRegistry.playS2C().register(OPEN_RETURN_ID, ReturnPackage.CODEC);
        PayloadTypeRegistry.playS2C().register(HELLO_REMOTE_INTERACTIONS_ID, HelloPackage.CODEC);
        //#endif
    }

    public static void registerReceivePacket() {
        //#if MC > 12004
        ServerPlayNetworking.registerGlobalReceiver(OPEN_INVENTORY_ID, (payload,context) -> {
            if (payload instanceof OpenPackage packetByteBuf) {
                context.player().getWorld().getServer().execute(() -> openInv(context.player().getWorld().getServer(), context.player(), packetByteBuf.pos, packetByteBuf.world));
            }
        });
        //#else
        //$$ ServerPlayNetworking.registerGlobalReceiver(OPEN_INVENTORY, (server, player, serverPlayNetworkHandler, packetByteBuf, packetSender) -> {
        //$$     BlockPos pos = packetByteBuf.readBlockPos();
        //#if MC < 11904
        //$$ RegistryKey<World> key = RegistryKey.of(Registry.WORLD_KEY, packetByteBuf.readIdentifier());
        //#else
        //$$ RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, packetByteBuf.readIdentifier());
        //#endif
        //$$     server.execute(() -> openInv(server, player, pos, key));
        //$$ });
        //#endif
    }

    public static void helloRemote(ServerPlayerEntity player) {
        //#if MC > 12004
        ServerPlayNetworking.send(player,new HelloPackage());
        //#else
        //$$ ServerPlayNetworking.send(player, HELLO_REMOTE_INTERACTIONS, new PacketByteBuf(Unpooled.buffer()));
        //#endif
    }

    public static void openInv(MinecraftServer server, ServerPlayerEntity player, BlockPos pos, RegistryKey<World> key) {
        ServerWorld world = server.getWorld(key);
        if (world == null) return;
        BlockState blockState = world.getBlockState(pos);
        if (blockState == null) {
            //#if MC > 12104
            //$$ world.getChunkManager().addTicket(OPEN_TICKET, new ChunkPos(pos), 2);
            //#else
            world.getChunkManager().addTicket(OPEN_TICKET, new ChunkPos(pos), 2, new ChunkPos(pos));
            //#endif
        }
        playerlist.add(player);
        if (blockState == null) return;
        tickMap.put(player, new TickList(blockState.getBlock(), world, pos, blockState));
        BlockEntity blockEntity = world.getBlockEntity(pos);
        boolean isInv = isContainer(blockEntity);

        if (!isInv || blockState.isAir() || (blockEntity instanceof ShulkerBoxBlockEntity entity &&
                //#if MC > 12103
                !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(1.0F, blockState.get(FACING), 0.0F, 0.5F, pos.toBottomCenterPos()).offset(pos).contract(1.0E-6)) &&
                //#elseif MC <= 12103 && MC > 12004
                //!client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(1.0F, blockState.get(FACING), 0.0F, 0.5F).offset(pos).contract(1.0E-6)) &&
                //#elseif MC <= 12004
                //$$ !client.world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(blockState.get(FACING), 0.0f, 0.5f).offset(pos).contract(1.0E-6)) &&
                //#endif
                entity.getAnimationStage() == ShulkerBoxBlockEntity.AnimationStage.CLOSED)) {
            System.out.println("openFail" + pos);
            openReturn(player, blockState, false);
            return;
        }
//        NamedScreenHandlerFactory handler = null;
//        try {
//            //#if MC < 12005
//            handler = ((BlockWithEntity) blockState.getBlock()).createScreenHandlerFactory(blockState, world, pos);
//            //#else
//            //$$ handler = ((me.aleksilassila.litematica.printer.mixin.openinv.BlockWithEntityMixin) blockState.getBlock()).createScreenHandlerFactory(blockState, world, pos);
//            //#endif
//        } catch (Exception ignored) {
//            openReturn(player, blockState, false);
//            return;
//        }

        //#if MC > 12004
        ActionResult r = blockState.onUse(world, player, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
        //#else
        //$$ ActionResult r = blockState.onUse(world, player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
        //#endif

        if ((r != null && (!r.equals(ActionResult.CONSUME)
                //#if MC > 12101
                 && !r.equals(ActionResult.SUCCESS)
                //#endif
        ))) {
            System.out.println("openFail" + pos);
            openReturn(player, blockState, false);
            return;
        }
        openReturn(player, blockState, true);
//        System.out.println("player " + player.getName());
    }

    public static void sendOpenInventory(BlockPos pos, RegistryKey<World> key) {
        //先置空，避免箱子追踪库存在奇妙的状态保存
        OpenInventoryPacket.pos = null;
        OpenInventoryPacket.key = null;
        //避免箱子追踪重复保存，
        //#if MC >= 12001
        //避免箱子追踪胡乱记录，若不清空，则会吧打开容器前右键的方块视为目标容器
        InteractionTracker.INSTANCE.clear();
        //#endif
        if (client.player != null && !client.player.currentScreenHandler.equals(client.player.playerScreenHandler))
            client.player.closeHandledScreen();
        openIng = true;
        OpenInventoryPacket.pos = pos;
        OpenInventoryPacket.key = key;
//        System.out.println(pos+"   key: "+key);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeIdentifier(key.getValue());
        //#if MC > 12004
        OpenPackage openPackage = new OpenPackage();
        openPackage.world = key;
        openPackage.pos = pos;
        ClientPlayNetworking.send(openPackage);
        //#else
        //$$ ClientPlayNetworking.send(OPEN_INVENTORY, new PacketByteBuf(buf));
        //#endif

    }

    public static void openReturn(boolean open, BlockState state) {
        if(clientTry){
            ZxyUtils.actionBar("已自动启用远程交互容器!!!");
            LitematicaMixinMod.CLOUD_INVENTORY.setBooleanValue(true);
            key = null;
            pos = null;
            remoteTime = 0;
            openIng = false;
            clientTry = false;
            return;
        }
        if (open) {
            //#if MC >= 12001
            MemoryUtils.blockState = state;
            //#endif
//            client.player.sendMessage(Text.of("return "+state.toString()));
        } else {
            if (key != null) {
                //#if MC < 11904
                //$$ String translationKey = key.getValue().toString();
                //$$ String translate = StringUtils.translate(translationKey);
                //$$ if (client.player != null) client.player.sendMessage(Text.of("打开容器失败 \n位于"+ translate+"  "+pos.toString()),false);
                //#else
                String translationKey = key.getValue().toTranslationKey();
                String translate = StringUtils.translate(translationKey);
                    //#if MC > 12101
                    if (client.player != null) client.player.sendMessage(Text.of("打开容器失败 \n位于"+ translate+"  "+pos.toCenterPos().toString()),false);
                    //#else
                    //$$ if (client.player != null) client.player.sendMessage(Text.of("打开容器失败 \n位于"+ translate+"  "+pos.toCenterPos().toString()));
                    //#endif
                //#endif

                //#if MC >= 12001
                MemoryUtils.PRINTER_MEMORY.removeMemory(key.getValue(), pos);
                //#else
                //$$ red.jackf.chesttracker.memory.MemoryDatabase.getCurrent().removePos(key.getValue() , pos);
                //$$ me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase.getCurrent().removePos(key.getValue() , pos);
                //#endif
            }
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.closeHandledScreen();
            }
            Statistics.closeScreen--;
            openIng = false;
            isOpenHandler = false;
            printerMemorySync = false;
            key = null;
            pos = null;
        }
    }

    public static void openReturn(ServerPlayerEntity player, BlockState state, boolean open) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        MyPacket.encode(new MyPacket(state, open), buf);
        //#if MC > 12004
        ReturnPackage returnPackage = new ReturnPackage();
        returnPackage.state = state;
        returnPackage.isOpen = open;
        ServerPlayNetworking.send(player,returnPackage);
        //#else
        //$$ ServerPlayNetworking.send(player, OPEN_RETURN, buf);
        //#endif
    }

    public static void reSet() {
        key = null;
        pos = null;
        openIng = false;
    }

    public static void tick(){
        if (!LitematicaMixinMod.AUTO_INVENTORY.getBooleanValue()) return;
        if(remoteTime != 0 && !isRemote && remoteTime + 3000L < System.currentTimeMillis()){
            if(!clientTry) {
                clientTryTime = System.currentTimeMillis();
                sendOpenInventory(new BlockPos(0,-999,0), client.world.getRegistryKey());
            }
            clientTry = true;
            if(clientTryTime + 3000L < System.currentTimeMillis() && clientTry){
                ZxyUtils.actionBar("已自动关闭远程交互容器");
                LitematicaMixinMod.CLOUD_INVENTORY.setBooleanValue(false);
                remoteTime = 0;
                clientTry = false;
            }
        }
    }

//    //#if MC > 12004
//    @Override
//    public Id<? extends CustomPayload> getId() {
//        return null;
//    }
//    //#else
//    //$$
//    //#endif
    public static boolean isContainer(BlockEntity blockEntity){
        return blockEntity instanceof Inventory;
    }
}
