package me.aleksilassila.litematica.printer.printer.zxy.inventory;

import fi.dy.masa.malilib.util.StringUtils;
import io.netty.buffer.Unpooled;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import me.aleksilassila.litematica.printer.utils.ResourceLocationUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

//#if MC > 12004
import net.minecraft.world.level.block.Block;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#else
//$$ import net.minecraft.world.InteractionHand;
//#endif

//#if MC >= 12001 && MC <= 12104
//$$ import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//$$ import red.jackf.chesttracker.api.providers.InteractionTracker;
//#endif
import java.util.ArrayList;
import java.util.HashMap;

//#if MC <= 12105
//$$ import java.util.Comparator;
//#endif

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils.isOpenHandler;

//#if MC > 12004
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.HelloPackage.HELLO_REMOTE_INTERACTIONS_ID;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.OpenPackage.OPEN_INVENTORY_ID;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.ReturnPackage.OPEN_RETURN_ID;
//#endif

//#if MC >= 11904
import net.minecraft.core.registries.Registries;
//#else
//$$ import net.minecraft.core.Registry;
//#endif

public class OpenInventoryPacket {

    private static final @NotNull Minecraft client = Minecraft.getInstance();
    private static final ResourceLocation OPEN_INVENTORY = ResourceLocationUtils.of("remoteinventory", "open_inventory");
    private static final ResourceLocation OPEN_RETURN = ResourceLocationUtils.of("openreturn", "open_return");
    private static final ResourceLocation HELLO_REMOTE_INTERACTIONS = ResourceLocationUtils.of("hello", "hello_remote_interactions");


    //#if MC > 12104
    private static final TicketType OPEN_TICKET = TicketType.UNKNOWN;
    //#else
    //$$ private static final TicketType<ChunkPos> OPEN_TICKET = TicketType.create("openInv", Comparator.comparingLong(ChunkPos::toLong), 2);
    //#endif

    public static HashMap<ServerPlayer, TickList> tickMap = new HashMap<>();
    public static boolean openIng = false;
    public static ResourceKey<Level> key = null;
    public static BlockPos pos = null;
    public static boolean isRemote = false;
    public static boolean clientTry = false;
    public static long clientTryTime = 0;
    public static long remoteTime = 0;

    public static ArrayList<ServerPlayer> playerlist = new ArrayList<>();

    //#if MC > 12004
    public static class OpenPackage implements CustomPacketPayload{
        public static final Type<OpenPackage> OPEN_INVENTORY_ID = new Type<>(OPEN_INVENTORY);
        public static final StreamCodec<RegistryFriendlyByteBuf,OpenPackage> CODEC = new StreamCodec<>() {

            @Override
            public void encode(RegistryFriendlyByteBuf buf, OpenPackage value) {
                buf.writeResourceKey(value.world);
                buf.writeBlockPos(value.pos);
            }
            @Override
            public OpenPackage decode(RegistryFriendlyByteBuf buf) {
                OpenPackage openPackage = new OpenPackage();
                openPackage.world = buf.readResourceKey(Registries.DIMENSION);
                openPackage.pos = buf.readBlockPos();
                return openPackage;
            }
        };
        ResourceKey<Level> world = null;
        BlockPos pos = null;
        public OpenPackage() {
        }
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return OPEN_INVENTORY_ID;
        }
    }

    public static class HelloPackage implements CustomPacketPayload{
        public static final Type<HelloPackage> HELLO_REMOTE_INTERACTIONS_ID = new Type<>(HELLO_REMOTE_INTERACTIONS);
        public static final StreamCodec<RegistryFriendlyByteBuf,HelloPackage> CODEC = new StreamCodec<>() {
            @Override
            public void encode(RegistryFriendlyByteBuf buf, HelloPackage value) {
            }
            @Override
            public HelloPackage decode(RegistryFriendlyByteBuf buf) {
                return new HelloPackage();
            }
        };
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return HELLO_REMOTE_INTERACTIONS_ID;
        }
    }
    public static class ReturnPackage implements CustomPacketPayload{
        BlockState state = null;
        boolean isOpen = false;
        public static final Type<ReturnPackage> OPEN_RETURN_ID = new Type<>(OPEN_RETURN);
        public static final StreamCodec<RegistryFriendlyByteBuf,ReturnPackage> CODEC = new StreamCodec<>() {
            @Override
            public void encode(RegistryFriendlyByteBuf buf, ReturnPackage value) {
                buf.writeInt(Block.getId(value.state));
                buf.writeBoolean(value.isOpen);
            }
            @Override
            public ReturnPackage decode(RegistryFriendlyByteBuf buf) {
                ReturnPackage returnPackage = new ReturnPackage();
                returnPackage.state = Block.stateById(buf.readInt());
                returnPackage.isOpen = buf.readBoolean();
                return returnPackage;
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
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
                if (Configs.General.AUTO_INVENTORY.getBooleanValue()) {
                    ZxyUtils.actionBar("已自动启用远程交互容器!!!");
                    Configs.General.CLOUD_INVENTORY.setBooleanValue(true);
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
        //$$         if (Configs.General.AUTO_INVENTORY.getBooleanValue()) {
        //$$             ZxyUtils.actionBar("已自动启用远程交互容器!!!");
        //$$             Configs.General.CLOUD_INVENTORY.setBooleanValue(true);
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
                context.player().level().getServer().execute(() -> openInv(context.player().level().getServer(), context.player(), packetByteBuf.pos, packetByteBuf.world));
            }
        });
        //#else
        //$$ ServerPlayNetworking.registerGlobalReceiver(OPEN_INVENTORY, (server, player, serverPlayNetworkHandler, packetByteBuf, packetSender) -> {
        //$$     BlockPos pos = packetByteBuf.readBlockPos();
            //#if MC < 11904
            //$$ ResourceKey<Level> key = ResourceKey.create(Registry.DIMENSION_REGISTRY, packetByteBuf.readResourceLocation());
            //#else
            //$$ ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, packetByteBuf.readResourceLocation());
            //#endif
        //$$     server.execute(() -> openInv(server, player, pos, key));
        //$$ });
        //#endif
    }

    public static void helloRemote(ServerPlayer player) {
        //#if MC > 12004
        ServerPlayNetworking.send(player,new HelloPackage());
        //#else
        //$$ ServerPlayNetworking.send(player, HELLO_REMOTE_INTERACTIONS, new FriendlyByteBuf(Unpooled.buffer()));
        //#endif
    }

    public static void openInv(MinecraftServer server, ServerPlayer player, BlockPos pos, ResourceKey<Level> key) {
        ServerLevel world = server.getLevel(key);
        if (world == null) return;
        BlockState blockState = world.getBlockState(pos);
        if (blockState == null) {
            //#if MC > 12104
            world.getChunkSource().addTicketWithRadius(OPEN_TICKET, new ChunkPos(pos), 2);
            //#else
            //$$ world.getChunkSource().addRegionTicket(OPEN_TICKET, new ChunkPos(pos), 2, new ChunkPos(pos));
            //#endif
        }
        playerlist.add(player);
        if (blockState == null) return;
        tickMap.put(player, new TickList(blockState.getBlock(), world, pos, blockState));
        BlockEntity blockEntity = world.getBlockEntity(pos);
        boolean isInv = isContainer(blockEntity);

        if (!isInv || blockState.isAir() || (blockEntity instanceof ShulkerBoxBlockEntity entity &&
                //#if MC > 12103
                !client.level.noCollision(Shulker.getProgressDeltaAabb(1.0F, blockState.getValue(BlockStateProperties.FACING), 0.0F, 0.5F, pos.getBottomCenter()).move(pos).deflate(1.0E-6)) &&
                //#elseif MC <= 12103 && MC > 12004
                //$$ !client.level.noCollision(Shulker.getProgressDeltaAabb(1.0F, blockState.getValue(BlockStateProperties.FACING), 0.0F, 0.5F).move(pos).deflate(1.0E-6)) &&
                //#elseif MC <= 12004
                //$$ !client.level.noCollision(Shulker.getProgressDeltaAabb(blockState.getValue(BlockStateProperties.FACING), 0.0f, 0.5f).move(pos).deflate(1.0E-6)) &&
                //#endif
                entity.getAnimationStatus() == ShulkerBoxBlockEntity.AnimationStatus.CLOSED)) {
            System.out.println("openFail" + pos);
            openReturn(player, blockState, false);
            return;
        }
//        NamedScreenHandlerFactory handler = null;
//        try {
//            //#if MC < 12005
//            handler = ((BlockWithEntity) blockState.getBlock()).createScreenHandlerFactory(blockState, level, pos);
//            //#else
//            //$$ handler = ((me.aleksilassila.litematica.printer.mixin.openinv.BlockWithEntityMixin) blockState.getBlock()).createScreenHandlerFactory(blockState, level, pos);
//            //#endif
//        } catch (Exception ignored) {
//            openReturn(player, blockState, false);
//            return;
//        }

        //#if MC > 12004
        InteractionResult r = blockState.useWithoutItem(world, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        //#else
        //$$ InteractionResult r = blockState.use(world, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        //#endif

        if ((r != null && (!r.equals(InteractionResult.CONSUME)
                //#if MC > 12101
                && !r.equals(InteractionResult.SUCCESS)
                //#endif
        ))) {
            System.out.println("openFail" + pos);
            openReturn(player, blockState, false);
            return;
        }
        openReturn(player, blockState, true);
//        System.out.println("player " + player.getName());
    }

    public static void sendOpenInventory(BlockPos pos, ResourceKey<Level> key) {
        //先置空，避免箱子追踪库存在奇妙的状态保存
        OpenInventoryPacket.pos = null;
        OpenInventoryPacket.key = null;
        //避免箱子追踪重复保存，
        //#if MC >= 12001 && MC <= 12104
        //避免箱子追踪胡乱记录，若不清空，则会吧打开容器前右键的方块视为目标容器
        //$$ InteractionTracker.INSTANCE.clear();
        //#endif
        if (client.player != null && !client.player.containerMenu.equals(client.player.inventoryMenu))
            client.player.closeContainer();
        openIng = true;
        OpenInventoryPacket.pos = pos;
        OpenInventoryPacket.key = key;
//        System.out.println(pos+"   key: "+key);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeResourceLocation(key.location());
        //#if MC > 12004
        OpenPackage openPackage = new OpenPackage();
        openPackage.world = key;
        openPackage.pos = pos;
        ClientPlayNetworking.send(openPackage);
        //#else
        //$$ ClientPlayNetworking.send(OPEN_INVENTORY, new FriendlyByteBuf(buf));
        //#endif

    }

    public static void openReturn(boolean open, BlockState state) {
        if(clientTry){
            ZxyUtils.actionBar("已自动启用远程交互容器!!!");
            Configs.General.CLOUD_INVENTORY.setBooleanValue(true);
            key = null;
            pos = null;
            remoteTime = 0;
            openIng = false;
            clientTry = false;
            return;
        }
        if (open) {
            //#if MC >= 12001 && MC <= 12104
            //$$ MemoryUtils.blockState = state;
            //#endif
//            client.player.sendMessage(Text.of("return "+state.toString()));
        } else {
            if (key != null) {
                //#if MC < 11904
                //$$ String translationKey = key.location().toString();
                //$$ String translate = StringUtils.translate(translationKey);
                //$$ if (client.player != null) me.aleksilassila.litematica.printer.utils.MessageUtils.addMessage("打开容器失败 \n位于"+ translate+"  "+pos.toString());
                //#else
                String translationKey = key.location().toLanguageKey();
                String translate = StringUtils.translate(translationKey);
                if (client.player != null) client.player.displayClientMessage(Component.nullToEmpty("打开容器失败 \n位于"+ translate+"  "+pos.getCenter().toString()),false);
                //#endif

                //#if MC >= 12001 && MC <= 12104
                //$$ MemoryUtils.PRINTER_MEMORY.removeMemory(key.location(), pos);
                //#elseif MC < 12001
                //$$ red.jackf.chesttracker.memory.MemoryDatabase.getCurrent().removePos(key.location() , pos);
                //$$ me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase.getCurrent().removePos(key.location() , pos);
                //#endif
            }
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.closeContainer();
            }
            ModLoadStatus.closeScreen--;
            openIng = false;
            isOpenHandler = false;
            Printer.getInstance().printerMemorySync = false;
            key = null;
            pos = null;
        }
    }

    public static void openReturn(ServerPlayer player, BlockState state, boolean open) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
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
        if (!Configs.General.AUTO_INVENTORY.getBooleanValue()) return;
        if(remoteTime != 0 && !isRemote && remoteTime + 3000L < System.currentTimeMillis()){
            if(!clientTry) {
                clientTryTime = System.currentTimeMillis();
                sendOpenInventory(new BlockPos(0,-999,0), client.level.dimension());
            }
            clientTry = true;
            if(clientTryTime + 3000L < System.currentTimeMillis() && clientTry){
                ZxyUtils.actionBar("已自动关闭远程交互容器");
                Configs.General.CLOUD_INVENTORY.setBooleanValue(false);
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
        return blockEntity instanceof Container;
    }
}
