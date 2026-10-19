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
 * 打印机 → 服务端：「我这里缺料了，从虚空仓库给我一组」。
 *
 * <p><b>这是 {@code carpet-Neko-Addition} 服务端模组里的同名类在客户端的镜像。</b>
 * 包名、类名、字段名、{@code CODEC} 的结构必须与服务端那份
 * （{@code carpet-Neko-Addition/modern/26.3/src/main/java/neko/voidwarehouse/restock/PrinterTakePayload.java}）
 * <b>逐字节等价</b>。改这里的时候务必同步改那边，反之亦然。</p>
 *
 * <h2>为什么客户端要「抄」一份服务端的类，而不是用自己的类</h2>
 * <p>单人存档 / 局域网主机里，客户端与服务端跑在<b>同一个 JVM</b> 里，共用
 * {@code PayloadTypeRegistryImpl} 那几个<b>全局静态 map</b>。客户端把包发出去时，
 * 底层 {@code CustomPacketPayload.writeCap} 的流程是：</p>
 * <pre>{@code
 * buf.writeIdentifier(type.id());                     // 先写通道名
 * StreamCodec c = findCodec(type.id());               // 按「通道名」查注册表拿 codec
 * c.encode(buf, payload);                             // 用查到的 codec 编我的 payload
 * }</pre>
 * <p>注意第二步：它查的是<b>通道名</b>，不关心 payload 是什么类；而第三步拿到的 codec
 * 是<b>服务端注册时塞进去的那个</b>。如果两端各有各的类，服务端的
 * {@code CODEC}（{@code StreamCodec.composite} 生成）内部会把入参 {@code checkcast}
 * 成服务端的类型 → 客户端对象当场
 * {@code ClassCastException: VoidWarehouseTakePayload cannot be cast to
 * neko.voidwarehouse.restock.PrinterTakePayload}，连接断开。</p>
 *
 * <p>而 {@code KnotClassLoader} 对同一个类名<b>只会加载一次</b>（按 classpath 的 jar
 * 顺序取第一个命中的），所以只要两边<b>类名相同</b>，运行时解析到的就是<b>同一个
 * {@code Class} 对象</b>，cast 永远成立 —— 谁先谁后都不影响。这就是「共用同一个类」
 * 的全部意义。</p>
 *
 * <h2>纯客户端环境（多人服务器）怎么办</h2>
 * <p>客户端必须自行注册 C2S codec，远端服务端的注册表不会传到本地。
 * 发送前还要通过 {@code ClientPlayNetworking.canSend} 检查对端的通道支持；
 * 本地能够编码并不表示远端能够接收。</p>
 *
 * <h2>关于 {@code MAX_CANDIDATES}</h2>
 * <p>{@code ByteBufCodecs.list(n)} 在解码时会按 {@code n} 限长，这个值两端必须一致，
 * 否则读出来的条数会错位。服务端同为 8。</p>
 */
public record PrinterTakePayload(int nonce, List<String> candidates) implements CustomPacketPayload {

    /**
     * 唯一的取货通道：{@code carpet_neko_addition:printer_take}（C2S）。
     *
     * <p>语义上这是<b>服务端的能力</b>，所以通道归服务端命名空间；
     * 客户端与服务端 {@code Identifier} 相等 → 查表能互相命中。</p>
     */
    public static final CustomPacketPayload.Type<PrinterTakePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("carpet_neko_addition", "printer_take"));

    /** 与服务端的 MAX_CANDIDATES 必须一致（解码时长校验） */
    public static final int MAX_CANDIDATES = 8;

    /**
     * 候选物品清单的编解码器。
     *
     * <p>显式写出 {@code <ByteBuf, String>}：{@code ByteBufCodecs.list} 是 CodecOperation，
     * 三个类型参数全靠推断，不写会退化成 {@code Object}。<br>
     * 结果保留为 {@code StreamCodec<ByteBuf, ...>}：{@code composite} 的形参是
     * {@code StreamCodec<? super B, T>}，更宽的 {@code ByteBuf} 两边都能接纳。</p>
     */
    private static final StreamCodec<ByteBuf, List<String>> CANDIDATES_CODEC =
            ByteBufCodecs.<ByteBuf, String>list(MAX_CANDIDATES).apply(ByteBufCodecs.STRING_UTF8);

    public static final StreamCodec<FriendlyByteBuf, PrinterTakePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PrinterTakePayload::nonce,
            CANDIDATES_CODEC, PrinterTakePayload::candidates,
            PrinterTakePayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//#endif
