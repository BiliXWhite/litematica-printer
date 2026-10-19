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
 * 打印机 → 服务端：「这些物品在虚空仓库里各有多少？」。
 *
 * <p><b>这是 {@code carpet-Neko-Addition} 服务端模组里的同名类在客户端的镜像。</b>
 * 包名、类名、字段名、{@code CODEC} 的结构、限长常量必须与服务端那份
 * <b>逐字节等价</b>（javadoc 无所谓，不进 class 文件）。改这里务必同步改那边，
 * 反之亦然 —— 为什么必须共用一个类，见 {@link PrinterTakePayload} 的类注释。</p>
 *
 * <h2>它和取货通道（{@link PrinterTakePayload}）的分工</h2>
 * <p>取货会真的改仓库和背包；这条查询<b>纯只读</b>，只为了在材料列表上把库存画出来。
 * 分成两条通道是因为两者的节奏差了一个数量级：查询 5 秒一次、取货只在缺料时发一次，
 * 而服务端对取货有 50ms 的强限流。</p>
 *
 * <h2>为什么由客户端列出物品，而不是让服务端把整个仓库发过来</h2>
 * <p>仓库可能有上千种物品，而投影的材料清单通常只有几十到几百种。只问自己要显示的那些，
 * 包小了、服务端也省了。投影一换就重问一次 —— 这本来就是必须的。</p>
 */
public record PrinterQueryPayload(int nonce, List<String> itemIds) implements CustomPacketPayload {

    /** 唯一的查询通道：{@code carpet_neko_addition:printer_query}（C2S）。 */
    public static final CustomPacketPayload.Type<PrinterQueryPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("carpet_neko_addition", "printer_query"));

    /** 一次最多问多少种物品。与服务端必须一致（解码时按这个值限长）。 */
    public static final int MAX_IDS = 1024;

    private static final StreamCodec<ByteBuf, List<String>> IDS_CODEC =
            ByteBufCodecs.<ByteBuf, String>list(MAX_IDS).apply(ByteBufCodecs.STRING_UTF8);

    public static final StreamCodec<FriendlyByteBuf, PrinterQueryPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PrinterQueryPayload::nonce,
            IDS_CODEC, PrinterQueryPayload::itemIds,
            PrinterQueryPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//#endif
