package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import io.netty.buffer.Unpooled;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.bedrockUtils.Messager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;

import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

//#if MC > 12001
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

import static me.aleksilassila.litematica.printer.printer.Printer.isOpenHandler;
import static me.aleksilassila.litematica.printer.printer.Printer.printerMemorySync;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.client;
import static net.minecraft.block.ShulkerBoxBlock.FACING;
//#if MC > 12004
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
public class OpenInventoryPacket implements CustomPayload {
//#else
//$$ public class OpenInventoryPacket {
//#endif

    private static final ChunkTicketType<ChunkPos> OPEN_TICKET =
            ChunkTicketType.create("ender_pearl", Comparator.comparingLong(ChunkPos::toLong), 2);
    public static HashMap<ServerPlayerEntity, TickList> tickMap = new HashMap<>();
    public static boolean openIng = false;
    public static RegistryKey<World> key = null;
    public static BlockPos pos = null;
    public static boolean isRemote = false;
    public static long remoteTime = 0;
    private static final Identifier OPEN_INVENTORY = new Identifier("remoteinventory", "open_inventory");
    private static final Identifier OPEN_RETURN = new Identifier("openreturn", "open_return");
    private static final Identifier HELLO_REMOTE_INTERACTIONS = new Identifier("hello", "hello_remote_interactions");
    public static ArrayList<ServerPlayerEntity> playerlist = new ArrayList<>();

    //#if MC > 12004
    public PackageData packageData = new PackageData();
    public static final Id<OpenInventoryPacket> OPEN_INVENTORY_ID = new Id<>(OPEN_INVENTORY);
    public static final Id<OpenInventoryPacket> OPEN_RETURN_ID = new Id<>(OPEN_RETURN);
    public static final Id<OpenInventoryPacket> HELLO_REMOTE_INTERACTIONS_ID = new Id<>(HELLO_REMOTE_INTERACTIONS);
    public Id<OpenInventoryPacket> id = null;
    public static final PacketCodec<RegistryByteBuf,OpenInventoryPacket> CODEC = new PacketCodec<>() {
       @Override
       public OpenInventoryPacket decode(RegistryByteBuf buf) {
           OpenInventoryPacket openInventoryPacket = new OpenInventoryPacket();
           openInventoryPacket.packageData = new PackageData(Block.getStateFromRawId(buf.readVarInt()), buf.readBoolean(), buf.readRegistryKey(RegistryKeys.WORLD), buf.readBlockPos());
           return openInventoryPacket;
       }
       @Override
       public void encode(RegistryByteBuf buf, OpenInventoryPacket value) {
           PackageData packageData1 = value.packageData;
           if (packageData1.state != null) buf.writeVarInt(Block.getRawIdFromState(packageData1.state));
           buf.writeBoolean(packageData1.isOpen);
           if (packageData1.world != null) buf.writeRegistryKey(packageData1.world);
           if (packageData1.pos != null) buf.writeBlockPos(packageData1.pos);
       }
    };
    public static class PackageData{
       BlockState state = null;
       boolean isOpen = false;
       RegistryKey<World> world = null;
       BlockPos pos = null;
       public PackageData() {
       }
       public PackageData(BlockState state, boolean isOpen, RegistryKey<World> world, BlockPos pos) {
           this.state = state;
           this.isOpen = isOpen;
           this.world = world;
           this.pos = pos;
       }
    }
    //#endif
    public static void registerClientReceivePacket() {
        //#if MC > 12004
        ClientPlayNetworking.registerGlobalReceiver(new Id<>(OPEN_INVENTORY), (payload,context) -> {
            try {
                if (payload instanceof OpenInventoryPacket packetByteBuf) {
                    boolean isOpen = packetByteBuf.packageData.isOpen;
                    BlockState state = packetByteBuf.packageData.state;
                    client.execute(() -> {
                        client.execute(() -> openReturn(isOpen,state));
                    });
                }
            } catch (Exception ignored) {
                Messager.actionBar("服务端回复异常，箱子追踪库存无法更新");
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(HELLO_REMOTE_INTERACTIONS_ID,(openInventoryPacket,context) -> {
            isRemote = true;
            client.execute(() -> {
                if (LitematicaMixinMod.AUTO_INVENTORY.getBooleanValue()) {
                    Messager.actionBar("已自动启用远程交互容器!!!");
                    LitematicaMixinMod.INVENTORY.setBooleanValue(true);
                }
            });
        });
        //#else
        //$$ ClientPlayNetworking.registerGlobalReceiver(OPEN_RETURN, (client, playNetworkHandler, packetByteBuf, packetSender) -> {
        //$$     try {
        //$$         MyPacket packet = MyPacket.decode(packetByteBuf);
        //$$         client.execute(() -> {
        //$$             client.execute(() -> openReturn(packet.getIsOpen(), packet.getBlockState()));
        //$$         });
        //$$     } catch (Exception ignored) {
        //$$         Messager.actionBar("服务端回复异常，箱子追踪库存无法更新");
        //$$     }
        //$$ });
        //$$ ClientPlayNetworking.registerGlobalReceiver(HELLO_REMOTE_INTERACTIONS, (client, playNetworkHandler, packetByteBuf, packetSender) -> {
        //$$     isRemote = true;
        //$$     client.execute(() -> {
        //$$         if (LitematicaMixinMod.AUTO_INVENTORY.getBooleanValue()) {
        //$$             Messager.actionBar("已自动启用远程交互容器!!!");
        //$$             LitematicaMixinMod.INVENTORY.setBooleanValue(true);
        //$$         }
        //$$     });
        //$$ });
        //#endif
    }
    public static void init(){
        //#if MC > 12004
        PayloadTypeRegistry.playC2S().register(OPEN_INVENTORY_ID, CODEC);
        PayloadTypeRegistry.playC2S().register(OPEN_RETURN_ID, CODEC);
        PayloadTypeRegistry.playC2S().register(HELLO_REMOTE_INTERACTIONS_ID, CODEC);
        PayloadTypeRegistry.playS2C().register(OPEN_INVENTORY_ID, CODEC);
        PayloadTypeRegistry.playS2C().register(OPEN_RETURN_ID, CODEC);
        PayloadTypeRegistry.playS2C().register(HELLO_REMOTE_INTERACTIONS_ID, CODEC);
        //#endif
    }

    public static void registerReceivePacket() {
        //#if MC > 12004
        ServerPlayNetworking.registerGlobalReceiver(OPEN_INVENTORY_ID, (payload,context) -> {
            if (payload instanceof OpenInventoryPacket packetByteBuf) {
                PackageData packageData1 = packetByteBuf.packageData;
                context.player().server.execute(() -> {
                    openInv(context.player().server, context.player(),packageData1.pos,packageData1.world);
                });
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
        OpenInventoryPacket openInventoryPacket = new OpenInventoryPacket();
        openInventoryPacket.id = HELLO_REMOTE_INTERACTIONS_ID;
        ServerPlayNetworking.send(player,openInventoryPacket);
        //#else
        //$$ ServerPlayNetworking.send(player, HELLO_REMOTE_INTERACTIONS, new PacketByteBuf(Unpooled.buffer()));
        //#endif
    }

    public static void openInv(MinecraftServer server, ServerPlayerEntity player, BlockPos pos, RegistryKey<World> key) {
        ServerWorld world = server.getWorld(key);
        if (world == null) return;
        BlockState blockState = world.getBlockState(pos);
        if (blockState == null) {
            world.getChunkManager().addTicket(OPEN_TICKET, new ChunkPos(pos), 2, new ChunkPos(pos));
        }
        playerlist.add(player);
        if (blockState == null) return;
        tickMap.put(player, new TickList(blockState.getBlock(), world, pos, blockState));
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof ShulkerBoxBlockEntity entity &&
                //#if MC > 12004
                !world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(0.0f,blockState.get(FACING),  0.5f).offset(pos).contract(1.0E-6)) &&
                //#else
                //$$ !world.isSpaceEmpty(ShulkerEntity.calculateBoundingBox(blockState.get(FACING), 0.0f, 0.5f).offset(pos).contract(1.0E-6)) &&
                //#endif

                entity.getAnimationStage() == ShulkerBoxBlockEntity.AnimationStage.CLOSED) {
            System.out.println("openFail" + pos);
            openReturn(player, blockState, false);
            return;
        }
        NamedScreenHandlerFactory handler = null;
        try {
            handler = ((me.aleksilassila.litematica.printer.mixin.openinv.BlockWithEntityMixin) blockState.getBlock()).createScreenHandlerFactory(blockState, world, pos);
        } catch (Exception ignored) {
        }
        //#if MC > 12004
        ActionResult r = blockState.onUse(world, player, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
        //#else
        //$$ ActionResult r = blockState.onUse(world, player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
        //#endif

        if ((r != null && !r.equals(ActionResult.CONSUME)) || handler == null) {
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
        //#if MC > 12001
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
        OpenInventoryPacket openInventoryPacket = new OpenInventoryPacket();
        openInventoryPacket.id = OPEN_INVENTORY_ID;
        PackageData packageData1 = openInventoryPacket.packageData;
        packageData1.world = key;
        packageData1.pos = pos;
        ClientPlayNetworking.send(openInventoryPacket);
        //#else
        //$$ ClientPlayNetworking.send(OPEN_INVENTORY, new PacketByteBuf(buf));
        //#endif

    }

    public static void openReturn(boolean open, BlockState state) {
        if (open) {
            //#if MC > 12001
            MemoryUtils.blockState = state;
            //#endif

//            client.player.sendMessage(Text.of("return "+state.toString()));
        } else {
//            System.out.println("fail");
//        MemoryDatabase.getCurrent().removePos(key.getValue() , pos);
//        me.aleksilassila.litematica.printer.printer.memory.MemoryDatabase.getCurrent().removePos(key.getValue() , pos);
//            client.inGameHud.setOverlayMessage(Text.of("打开容器失败1"),false);
            //#if MC < 11904
            //$$ if (client.player != null) client.player.sendMessage(Text.of("打开容器失败."),false);
            //#else
            if (client.player != null) client.player.sendMessage(Text.of("打开容器失败."));
            //#endif

            if (key != null) {
                //#if MC > 12001
                MemoryUtils.PRINTER_MEMORY.removeMemory(key.getValue(), pos);
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
        OpenInventoryPacket openInventoryPacket = new OpenInventoryPacket();
        openInventoryPacket.id = OPEN_RETURN_ID;
        PackageData packageData1 = openInventoryPacket.packageData;
        packageData1.state = state;
        packageData1.isOpen = open;
        ServerPlayNetworking.send(player, openInventoryPacket);
        //#else
        //$$ ServerPlayNetworking.send(player, OPEN_RETURN, buf);
        //#endif
    }

    public static void reSet() {
        key = null;
        pos = null;
        isRemote = false;
        remoteTime = 0;
        openIng = false;
    }

    public static void tick(){
        boolean booleanValue = LitematicaMixinMod.AUTO_INVENTORY.getBooleanValue();
        if(booleanValue && remoteTime != 0 && !isRemote && remoteTime + 3000L < System.currentTimeMillis()){
            Messager.actionBar("已自动关闭远程交互容器");
            LitematicaMixinMod.INVENTORY.setBooleanValue(false);
            remoteTime = 0;
        }
    }

    //#if MC > 12004
    @Override
    public Id<? extends CustomPayload> getId() {
        return id;
    }
    //#else
    //$$
    //#endif

}
