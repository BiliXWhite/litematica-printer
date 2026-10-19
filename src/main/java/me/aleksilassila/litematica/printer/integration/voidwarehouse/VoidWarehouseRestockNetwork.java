package me.aleksilassila.litematica.printer.integration.voidwarehouse;

//#if MC >= 12111
import neko.voidwarehouse.restock.PrinterTakePayload;
import neko.voidwarehouse.restock.PrinterTakeResultPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import java.util.List;

/**
 * 客户端一侧的收发管家：把取货包发出去，并把服务端回来的回执记住。
 *
 * <p>之所以做成一个静态的收发层而不是直接写在 adapter 里：
 * 「<b>服务端认不认这个通道</b>」这件事必须尽可能早地定下来 —— 注册要在
 * {@code onInitializeClient} 里完成，晚于建连就再也收不到包了；
 * 而 {@code PrinterRuntime} 里的 provider 链是<b>用到才懒加载</b>的，
 * 两者的时机对不上。所以把收发单独放一层，adapter 只负责查询。</p>
 *
 * <h2>payload 类为什么直接引用 {@code neko.*}（服务端的包）</h2>
 * <p>见 {@link PrinterTakePayload} 的类注释：单人 / 局域网里两端同进程、
 * 共用 {@code PayloadTypeRegistryImpl} 的静态表，而 {@code writeCap} 是按
 * <b>通道名</b>查表拿 codec 的。只要两端的 payload <b>类名相同</b>，
 * classpath 加载出的就是同一个 {@code Class}，服务端注册的 codec 编客户端对象
 * 天然合法。<br>
 * 反过来说，历史上客户端用自己的 {@code VoidWarehouseTakePayload}、
 * 服务端用自己的 {@code PrinterTakePayload}，编码时就炸
 * {@code ClassCastException} —— 那条路已经彻底废弃。</p>
 *
 * <h2>为什么要先问 {@code canSend}</h2>
 * <p>本地注册 codec 只负责编码；{@code canSend} 检查的是远端是否接受这个通道。
 * 两者缺一不可，不能依赖单人环境中服务端模组顺带注册的 codec。</p>
 *
 * <h2>回执怎么存</h2>
 * <p>只保留<b>最新的一对</b>（nonce + 状态）。打印机同时只会有一个未完成的材料请求，
 * 再复杂的结构都是自找麻烦。回执从网络线程写进来、渲染线程读出去，
 * 所以用 {@code volatile} 保证可见性。</p>
 */
public final class VoidWarehouseRestockNetwork {

    /**
     * 已经注册过就不用再注册。
     *
     * <p>用 {@code volatile}：理论上只有主线程会碰它，这里防的是「万一被调第二次」——
     * 重复注册会让 {@code registerGlobalReceiver} 返回 false，多一道门闩最省心。</p>
     */
    private static volatile boolean initialized;

    /** 下一次请求要用的流水号；正数递增，绕回时跳过 0（0 不是合法 nonce）。 */
    private static int nextNonce = 1;

    private static volatile int latestNonce = 0;
    private static volatile int latestStatus = PrinterTakeResultPayload.NONE;

    private VoidWarehouseRestockNetwork() {
    }

    /** 在模组客户端初始化时调用一次。 */
    public static void init() {
        if (initialized) {
            return;
        }
        registerTakePayload();
        registerResultReceiver();
        initialized = true;
    }

    private static void registerTakePayload() {
        try {
            //#if MC >= 260100
            PayloadTypeRegistry.serverboundPlay().register(PrinterTakePayload.TYPE, PrinterTakePayload.CODEC);
            //#else
            //$$ PayloadTypeRegistry.playC2S().register(PrinterTakePayload.TYPE, PrinterTakePayload.CODEC);
            //#endif
        } catch (IllegalArgumentException alreadyRegisteredByServerMod) {
            // Integrated servers share this registry and the identical payload class.
        }
    }

    /**
     * 注册「服务端 → 客户端」的回执通道。
     *
     * <p>C2S 与 S2C 都在客户端注册；这里登记回执的解码器和接收器。</p>
     *
     * <h2>为什么要先查表再注册</h2>
     * <p>服务端模组（{@code environment="*"}）在单人环境下也会初始化，
     * 它的 {@code PrinterRestockNetwork.init()} 已经往 {@code CLIENTBOUND_PLAY}
     * 塞过 {@code carpet_neko_addition:printer_take_result} 了。
     * Fabric 的 {@code register} 是 {@code containsKey → throw IllegalArgumentException}
     * （<b>严格去重，不是 putIfAbsent</b>），再注册一次就是启动崩溃。</p>
     *
     * <p>Fabric 没有提供「只查不注册」的公开 API，所以这里用
     * {@code register} 的副作用来达成幂等：直接注册，撞车就吞掉异常 ——
     * 语义上「已经就位」与「刚注册好」对后续毫无区别。</p>
     *
     * <h2>为什么 receiver 无论如何都要挂</h2>
     * <p>{@code registerGlobalReceiver} 是<b>按通道名</b>挂 handler 的，
     * 与「谁注册了类型」是两件事：服务端模组注册类型时并不会替客户端挂 receiver
     * （它挂的是服务端侧的）。客户端要收包，必须自己挂。
     * {@code registerGlobalReceiver} 自身对同名重复挂是幂等的（内部 {@code putIfAbsent}），
     * 所以这里不用额外防护。</p>
     */
    private static void registerResultReceiver() {
        try {
            //#if MC >= 260100
            PayloadTypeRegistry.clientboundPlay().register(
                    PrinterTakeResultPayload.TYPE, PrinterTakeResultPayload.CODEC);
            //#else
            //$$ PayloadTypeRegistry.playS2C().register(
            //$$         PrinterTakeResultPayload.TYPE, PrinterTakeResultPayload.CODEC);
            //#endif
        } catch (IllegalArgumentException alreadyRegisteredByServerMod) {
            // 单人环境下服务端模组先注册过了。类型名与 codec 两端完全相同，
            // 谁注册的没有任何区别，直接用即可。
        }
        ClientPlayNetworking.registerGlobalReceiver(
                PrinterTakeResultPayload.TYPE, VoidWarehouseRestockNetwork::receive);
    }

    /**
     * 服务端认不认这条取货通道。
     *
     * @return {@code true} = 对面装了支持该特性的服务端模组
     */
    public static boolean isSupported() {
        if (!initialized) {
            return false;
        }
        try {
            return ClientPlayNetworking.canSend(PrinterTakePayload.TYPE);
        } catch (RuntimeException ignored) {
            // canSend 在没有可用连接时会抛；那种情况下就是「不支持」的一种，按 false 处理。
            // 不 catch 的话，一次世界切换的时间窗足以把整个打印机的 tick 打断。
            return false;
        }
    }

    /**
     * 发出一次取货请求。
     *
     * @param candidates 候选物品的注册表 id，按顺序尝试
     * @return 本次请求的流水号；取不到流水号返回 0（表示没发出去）
     */
    public static int requestTake(List<String> candidates) {
        if (!initialized || candidates == null || candidates.isEmpty()) {
            return 0;
        }
        int nonce = allocateNonce();
        // 先清空再发：否则一个旧的成功回执会被当成这次的结果，造成「还没发货就以为到手了」。
        latestNonce = nonce;
        latestStatus = PrinterTakeResultPayload.NONE;
        try {
            ClientPlayNetworking.send(new PrinterTakePayload(nonce, candidates));
        } catch (RuntimeException ignored) {
            latestNonce = 0;
            return 0;
        }
        return nonce;
    }

    /**
     * 查指定流水号的回执。
     *
     * @return 状态码；还没收到回执时返回 {@link PrinterTakeResultPayload#NONE}
     */
    public static int statusOf(int nonce) {
        if (nonce <= 0 || latestNonce != nonce) {
            return PrinterTakeResultPayload.NONE;
        }
        return latestStatus;
    }

    /** 丢弃当前回执，用于请求作废或超时收尾。 */
    public static void forget() {
        latestNonce = 0;
        latestStatus = PrinterTakeResultPayload.NONE;
    }

    private static void receive(PrinterTakeResultPayload payload, ClientPlayNetworking.Context context) {
        latestNonce = payload.nonce();
        latestStatus = payload.status();
    }

    private static synchronized int allocateNonce() {
        int nonce = nextNonce;
        nextNonce = nonce >= Integer.MAX_VALUE ? 1 : nonce + 1;
        return nonce;
    }
}
//#endif
