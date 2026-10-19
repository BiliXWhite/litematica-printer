package neko.voidwarehouse.restock;

//#if MC >= 12111
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 服务端 → 打印机：「你要的那批物品，仓库里各有多少」。
 *
 * <p><b>同样是 {@code carpet-Neko-Addition} 里那份的服务端原件在客户端的镜像</b>，
 * 与 {@link PrinterQueryPayload} 同理：包名、类名、状态码常量、{@code CODEC} 结构
 * 必须与服务端逐字节等价。</p>
 *
 * <h2>只回有货的物品</h2>
 * <p>仓库里没有的不占位置。「查不到 = 0」与「回一条 0」对使用方完全等价，
 * 而投影要的、仓库里没有的那部分往往占大多数，省下来的都是净赚。</p>
 *
 * <h2>数量是 {@code long}</h2>
 * <p>仓库的账是任意精度，传输时饱和到 {@code long}。客户端拿到之后再饱和到
 * {@code int} 交给界面 —— 两层饱和各自独立，都是为了「宁可显示成大数，也绝不显示负数」。</p>
 */
public record PrinterQueryResultPayload(int nonce, int status, List<String> itemIds, List<Long> amounts)
        implements CustomPacketPayload {

    /** 查询回执通道：{@code carpet_neko_addition:printer_query_result}（S2C）。 */
    public static final CustomPacketPayload.Type<PrinterQueryResultPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("carpet_neko_addition", "printer_query_result"));

    /** 成功：{@code itemIds} / {@code amounts} 有效（可能为空 = 这些物品仓库里一个都没有） */
    public static final int OK = 0;
    /** 拒绝：Carpet 规则没开（{@code voidWarehouse} 或 {@code voidWarehouseRestock}） */
    public static final int RULE_OFF = 1;
    /** 包本身不合法（清单为空 / 全是解析不出来的 id） */
    public static final int BAD_REQUEST = 2;
    /** 撞上限流：客户端问得太快 */
    public static final int THROTTLED = 3;

    /** 回执里最多多少条。必须 ≥ 请求侧的 {@link PrinterQueryPayload#MAX_IDS}。 */
    public static final int MAX_ENTRIES = PrinterQueryPayload.MAX_IDS;

    private static final StreamCodec<ByteBuf, List<String>> IDS_CODEC =
            ByteBufCodecs.<ByteBuf, String>list(MAX_ENTRIES).apply(ByteBufCodecs.STRING_UTF8);

    private static final StreamCodec<ByteBuf, List<Long>> AMOUNTS_CODEC =
            ByteBufCodecs.<ByteBuf, Long>list(MAX_ENTRIES).apply(ByteBufCodecs.VAR_LONG);

    public static final StreamCodec<FriendlyByteBuf, PrinterQueryResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PrinterQueryResultPayload::nonce,
            ByteBufCodecs.VAR_INT, PrinterQueryResultPayload::status,
            IDS_CODEC, PrinterQueryResultPayload::itemIds,
            AMOUNTS_CODEC, PrinterQueryResultPayload::amounts,
            PrinterQueryResultPayload::new
    );

    /** 失败路径的简写：只有状态码，两个清单都是空的。 */
    public static PrinterQueryResultPayload of(int nonce, int status) {
        return new PrinterQueryResultPayload(nonce, status, List.of(), List.of());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//#endif
