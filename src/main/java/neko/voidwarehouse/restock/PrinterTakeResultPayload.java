package neko.voidwarehouse.restock;

//#if MC >= 12111
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 打印机：「刚才那次要货的结果」。
 *
 * <p><b>这也是 {@code carpet-Neko-Addition} 里的同名类在客户端的镜像</b>，
 * 与 {@link PrinterTakePayload} 同理：包名、类名、状态码常量、{@code CODEC} 结构
 * 必须与服务端那份逐字节等价。</p>
 *
 * <h2>S2C 与 C2S 的一条重要差别：这条要客户端自己注册</h2>
 * <p>C2S 那边客户端可以不注册 —— 编码时 {@code writeCap} 查的
 * {@code SERVERBOUND_PLAY} 表，同进程时正好是服务端填好的那张。但 S2C 反过来：
 * <b>解包方是客户端</b>，它得在自己的 {@code CLIENTBOUND_PLAY} 表里找到这个类型
 * 才知道怎么解。所以客户端 {@code onInitializeClient} 里必须注册这条；
 * 而服务端 {@code send} 时要用 {type, codec} 构造包，它也会注册一份。</p>
 *
 * <p>两者同进程时会撞同一个 key。Fabric 的 {@code register} 是
 * {@code containsKey → throw}（<b>不是 putIfAbsent</b>），所以客户端这份
 * 必须先查表、表里有了就跳过 —— 见
 * {@code VoidWarehouseRestockNetwork#registerResultReceiver}。</p>
 */
public record PrinterTakeResultPayload(int nonce, int status) implements CustomPacketPayload {

    /**
     * 唯一的回执通道：{@code carpet_neko_addition:printer_take_result}（S2C）。
     *
     * <p>回执沿「请求走哪条通道、回执就走哪条命名空间」的对应关系，
     * 两端共用一个名字才不会出现「客户端注册的类型没人给它发」。</p>
     */
    public static final CustomPacketPayload.Type<PrinterTakeResultPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("carpet_neko_addition", "printer_take_result"));

    /** 成功：服务端已经把东西放进玩家背包 */
    public static final int OK = 0;
    /** 拒绝：Carpet 规则 {@code voidWarehouseRestock} 没开 */
    public static final int RULE_OFF = 1;
    /** 仓库里一件都没有（候选全军覆没） */
    public static final int EMPTY = 2;
    /** 背包塞不下，一件都没给（仓库也没扣，不会产生幽灵消耗） */
    public static final int INVENTORY_FULL = 3;
    /** 撞上限流：客户端发太快 */
    public static final int THROTTLED = 4;
    /** 包本身不合法（候选为空 / 物品 id 解析不出来） */
    public static final int BAD_REQUEST = 5;

    /** 本地状态：还没收到任何回执（与服务端的状态码不冲突，服务端从 0 开始） */
    public static final int NONE = -1;

    public static final StreamCodec<FriendlyByteBuf, PrinterTakeResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PrinterTakeResultPayload::nonce,
            ByteBufCodecs.VAR_INT, PrinterTakeResultPayload::status,
            PrinterTakeResultPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//#endif
