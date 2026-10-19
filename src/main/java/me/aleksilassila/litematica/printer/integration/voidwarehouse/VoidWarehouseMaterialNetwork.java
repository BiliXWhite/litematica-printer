package me.aleksilassila.litematica.printer.integration.voidwarehouse;

//#if MC >= 12111
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.mixin.printer.malilib.GuiListBaseAccessor;
import neko.voidwarehouse.restock.PrinterQueryPayload;
import neko.voidwarehouse.restock.PrinterQueryResultPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 客户端一侧的收发 + 节流管家：定期问服务端「材料列表里这些物品，仓库各有多少」。
 *
 * <h2>为什么要自己轮询，而不是等别人来问</h2>
 * <p>材料列表的「已有数量」是被<b>渲染路径</b>读的：GUI 每帧画一次、HUD 每 2 秒刷一次。
 * 渲染路径里发网络包是禁忌（帧率会被网络抖动绑架，而且同一次渲染里可能被调用几百次）。
 * 所以拆成两半：这里按固定节奏轮询、把结果塞进
 * {@link VoidWarehouseMaterialCache}；渲染那一侧只读内存缓存（一次哈希查找）。</p>
 *
 * <h2>为什么要判断「玩家真的在看」</h2>
 * <p>问一次要来回一趟包，而材料列表在玩家不建造的时候是完全没人看的。
 * 所以只有<b>材料列表界面开着、或者它的信息 HUD 开着</b>的时候才发请求。
 * 这也顺带让「打开界面的那一刻」必然触发一次刷新 —— 看到的永远是刚刚问来的数。</p>
 *
 * <h2>节流的两道闸</h2>
 * <ol>
 *   <li><b>刷新间隔</b> {@value #REFRESH_INTERVAL_TICKS} tick（5 秒）：正常节奏；</li>
 *   <li><b>拒绝退避</b> {@value #REJECT_BACKOFF_TICKS} tick（60 秒）：服务端明确回
 *       {@code RULE_OFF}（服主没开规则）时不值得每 5 秒问一次，退到一分钟一次，
 *       这样服主中途把规则打开的话，最多一分钟就会自动生效。</li>
 * </ol>
 * <p>另有一次<b>超时</b>：请求发出去 {@value #RESPONSE_TIMEOUT_TICKS} tick 没等到回执就放弃，
 * 免得一次丢包把刷新永久卡死（{@code pending} 再也出不来）。</p>
 *
 * <h2>C2S 为什么要自己注册</h2>
 * <p>查询与取货通道都必须在客户端注册编码器，专用服务器的注册表不会传到本地。
 * 这里两个方向都注册，并且都写成幂等形式（单人环境下服务端已经注册过，
 * {@code register} 撞名会抛 {@code IllegalArgumentException}，吞掉即可）。</p>
 */
public final class VoidWarehouseMaterialNetwork {

    /** 正常的刷新间隔：100 tick = 5 秒。 */
    public static final int REFRESH_INTERVAL_TICKS = 100;

    /** 服务端回 {@code RULE_OFF} 之后的退避：1200 tick = 60 秒。 */
    public static final int REJECT_BACKOFF_TICKS = 1200;

    /** 一次请求最多等多久回执：40 tick = 2 秒。 */
    public static final int RESPONSE_TIMEOUT_TICKS = 40;

    private static volatile boolean initialized;

    private static int nextNonce = 1;
    private static volatile int latestNonce = 0;
    private static volatile boolean pending;
    private static volatile long sentAtTick;

    private static long lastRequestTick;
    private static long rejectedUntilTick;

    private VoidWarehouseMaterialNetwork() {
    }

    /** 在模组客户端初始化时调用一次。 */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        registerC2S();
        registerResultReceiver();
    }

    /**
     * 注册「客户端 → 服务端」的查询通道。
     *
     * <p>单人 / 局域网里服务端模组（{@code environment="*"}）已经在
     * {@code AutoMendingMod#onInitialize} 里注册过同一个 key 了 —— 在同一进程里
     * {@code PayloadTypeRegistry} 是全局静态表，{@code register} 的实现是
     * {@code containsKey → throw}（<b>严格去重，不是 putIfAbsent</b>），
     * 所以这里必须吞掉撞名异常。</p>
     */
    private static void registerC2S() {
        try {
            //#if MC >= 260100
            PayloadTypeRegistry.serverboundPlay().register(
                    PrinterQueryPayload.TYPE, PrinterQueryPayload.CODEC);
            //#else
            //$$ PayloadTypeRegistry.playC2S().register(
            //$$         PrinterQueryPayload.TYPE, PrinterQueryPayload.CODEC);
            //#endif
        } catch (IllegalArgumentException alreadyRegisteredByServerMod) {
            // 类型名与 codec 两端完全相同，谁注册的没有任何区别，直接用即可。
        }
    }

    /**
     * 注册「服务端 → 客户端」的回执通道。
     *
     * <p>客户端是<b>解包方</b>，不在自己的表里登记就解不出来。单人环境下服务端模组
     * 也可能已经登记过 → 同样的幂等处理。{@code registerGlobalReceiver} 按通道名挂
     * handler，与「谁注册了类型」是两件事，所以它无论如何都要自己挂。</p>
     */
    private static void registerResultReceiver() {
        try {
            //#if MC >= 260100
            PayloadTypeRegistry.clientboundPlay().register(
                    PrinterQueryResultPayload.TYPE, PrinterQueryResultPayload.CODEC);
            //#else
            //$$ PayloadTypeRegistry.playS2C().register(
            //$$         PrinterQueryResultPayload.TYPE, PrinterQueryResultPayload.CODEC);
            //#endif
        } catch (IllegalArgumentException alreadyRegisteredByServerMod) {
            // 同上
        }
        ClientPlayNetworking.registerGlobalReceiver(
                PrinterQueryResultPayload.TYPE, VoidWarehouseMaterialNetwork::receive);
    }

    /**
     * 服务端认不认这条查询通道。
     *
     * @return {@code true} = 对面装了支持该特性的服务端模组
     */
    public static boolean isSupported() {
        if (!initialized) {
            return false;
        }
        try {
            return ClientPlayNetworking.canSend(PrinterQueryPayload.TYPE);
        } catch (RuntimeException ignored) {
            // canSend 在没有可用连接时会抛；那种情况下就是「不支持」的一种。
            // 不 catch 的话，一次世界切换的时间窗足以把整个材料列表刷新的节奏打断。
            return false;
        }
    }

    /**
     * 每个客户端 tick 调一次。
     *
     * <p>顺序刻意如此：先排除「根本不该工作」的情形（没进世界 / 功能关 / 对面不支持），
     * 再排除「还在等回执 / 还没到刷新时间 / 没人看」，最后才去拼清单发包 ——
     * 拼清单要遍历整个材料列表，是这条路径上唯一有真实成本的一步。</p>
     */
    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            reset();
            return;
        }
        if (!Configs.Special.VOID_WAREHOUSE_MATERIAL_LIST.getBooleanValue()) {
            reset();
            return;
        }
        if (!isSupported()) {
            reset();
            return;
        }

        long now = client.level.getGameTime();

        if (pending) {
            if (now - sentAtTick <= RESPONSE_TIMEOUT_TICKS) {
                return;
            }
            // 超时：放行下一轮，否则一次丢包会把刷新永久卡死。
            pending = false;
        }
        if (now < rejectedUntilTick) {
            return;
        }
        if (now - lastRequestTick < REFRESH_INTERVAL_TICKS) {
            return;
        }
        if (!isBeingViewed(client)) {
            return;
        }

        List<String> ids = collectItemIds();
        if (ids.isEmpty()) {
            // 材料列表是空的（还没算完 / 换了空气投影）：清掉旧快照，
            // 否则上一个投影的库存会继续挂在界面上。
            clearMaterialCache();
            return;
        }

        lastRequestTick = now;
        int nonce = send(ids);
        if (nonce == 0) {
            pending = false;
            return;
        }
        pending = true;
        sentAtTick = now;
    }

    /** 玩家真的在看材料列表吗（界面开着，或者它的信息 HUD 开着）。 */
    private static boolean isBeingViewed(Minecraft client) {
        MaterialListBase list = DataManager.getMaterialList();
        if (list == null) {
            return false;
        }
        if (isMaterialListScreenOpen(client)) {
            return true;
        }
        MaterialListHudRenderer hud = list.getHudRenderer();
        return hud != null && hud.getShouldRenderCustom();
    }

    /**
     * 材料列表界面是不是正开着。
     *
     * <p>26.2 起 {@code Minecraft} 上的 {@code screen} 字段被搬进了 {@code Gui}
     * （变成一个 {@code screen()} 取值方法），26.1 与 1.21.11 还是原来的公开字段。
     * 这个分叉在本仓库里已经有过先例（见 {@code MixinContainerScreenGuard}），
     * 判据同样是 {@code MC > 260100}。</p>
     */
    private static boolean isMaterialListScreenOpen(Minecraft client) {
        //#if MC > 260100
        //$$ return client.gui.screen() instanceof GuiMaterialList;
        //#else
        return client.screen instanceof GuiMaterialList;
        //#endif
    }

    /**
     * 把当前材料列表里出现的物品去重成注册表 id。
     *
     * <p>用 {@link LinkedHashSet}：顺序稳定（便于日志和调试），
     * 而且天然去重 —— 同一个物品在列表里只出现一次，白问一遍没有意义。</p>
     */
    private static List<String> collectItemIds() {
        MaterialListBase list = DataManager.getMaterialList();
        if (list == null) {
            return List.of();
        }
        ImmutableList<MaterialListEntry> materials = list.getMaterialsAll();
        if (materials.isEmpty()) {
            return List.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        for (MaterialListEntry entry : materials) {
            if (ids.size() >= PrinterQueryPayload.MAX_IDS) {
                // 超过单次上限就只问前 MAX_IDS 种。宁可少显示一部分，
                // 也不发一个超规格的包让服务端解码失败。
                break;
            }
            if (entry == null) {
                continue;
            }
            // 与 Mixin 侧共用同一个转换 —— 「物品 → 仓库的键」只允许有一份实现，
            // 两边各写一份迟早会分叉（一边改了命名空间写法，另一边静默查不到）。
            String id = VoidWarehouseMaterialIds.idOf(entry.getStack());
            if (id != null) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    /**
     * 发出一次查询。
     *
     * @return 本次请求的流水号；没发出去返回 0
     */
    private static int send(List<String> ids) {
        int nonce = allocateNonce();
        latestNonce = nonce;
        try {
            ClientPlayNetworking.send(new PrinterQueryPayload(nonce, ids));
        } catch (RuntimeException ignored) {
            latestNonce = 0;
            return 0;
        }
        return nonce;
    }

    /**
     * 收到回执。
     *
     * <p>先比流水号：只认<b>最新一次</b>请求的答案。投影换得快的时候会有旧回执晚到，
     * 拿它去写缓存就是把上一份清单的数量盖到新清单上。</p>
     */
    private static void receive(PrinterQueryResultPayload payload, ClientPlayNetworking.Context context) {
        if (payload.nonce() != latestNonce) {
            return;
        }
        pending = false;
        if (payload.status() == PrinterQueryResultPayload.RULE_OFF) {
            // 服主没开规则：退避到一分钟一次，中途开了也能自动恢复。
            rejectedUntilTick = currentTick() + REJECT_BACKOFF_TICKS;
            clearMaterialCache();
            return;
        }
        if (payload.status() != PrinterQueryResultPayload.OK) {
            clearMaterialCache();
            return;
        }
        VoidWarehouseMaterialCache.get().apply(payload.itemIds(), payload.amounts());
        refreshMaterialLists();
    }

    /** 回到「什么都没问过」的状态，并抹掉快照。 */
    private static void reset() {
        pending = false;
        latestNonce = 0;
        lastRequestTick = 0L;
        rejectedUntilTick = 0L;
        clearMaterialCache();
    }

    private static void clearMaterialCache() {
        boolean hadSnapshot = VoidWarehouseMaterialCache.get().isReady();
        VoidWarehouseMaterialCache.get().clear();
        if (hadSnapshot) refreshMaterialLists();
    }

    /** Both the Fabric receiver and tick callback run on the client thread. */
    private static void refreshMaterialLists() {
        MaterialListBase list = DataManager.getMaterialList();
        // hideAvailable removes entries from this candidate list; restore them when stock falls.
        // refreshPreFilteredList preserves the user's explicitly ignored entries.
        if (list != null) list.refreshPreFilteredList();
        Minecraft client = Minecraft.getInstance();
        //#if MC > 260100
        //$$ var screen = client.gui.screen();
        //#else
        var screen = client.screen;
        //#endif
        if (screen instanceof GuiMaterialList gui) {
            if (gui.getMaterialList() != list) gui.getMaterialList().refreshPreFilteredList();
            var browser = ((GuiListBaseAccessor) gui).litematicaPrinter$getListWidget();
            if (browser != null) browser.refreshEntries();
        }
    }

    private static long currentTick() {
        Minecraft client = Minecraft.getInstance();
        return client.level == null ? 0L : client.level.getGameTime();
    }

    private static synchronized int allocateNonce() {
        int nonce = nextNonce;
        nextNonce = nonce >= Integer.MAX_VALUE ? 1 : nonce + 1;
        return nonce;
    }
}
//#endif
