package me.aleksilassila.litematica.printer.mixin.printer.litematica;

//#if MC >= 12111
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialCache;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 材料列表每一行的配色修正（{@code WidgetMaterialListEntry#render}）。
 *
 * <h2>原版的三级配色在比什么</h2>
 * <p>一行里画两个数字：<b>缺失</b>与<b>已有</b>。两个数字各自的颜色都出自同一个判据
 * {@code countAvailable >= countMissing}：</p>
 * <pre>
 *   缺失列：countMissing == 0 ? 绿 : (countAvailable >= countMissing ? 金 : 红)
 *   已有列：countAvailable >= countMissing ? 绿 : 红
 * </pre>
 * <p>三个颜色的含义是「世界不缺了（绿）／世界还缺但我材料够（金）／材料不够（红）」，
 * 全都建立在「{@code missing} = 需求、{@code available} = 持有」这条不变式上。</p>
 *
 * <h2>为什么改了「缺失」口径之后它会撒谎</h2>
 * <p>{@code MixinMaterialListEntry} 把 {@code getCountMissing()} 换成了净缺口之后，
 * {@code countAvailable >= countMissing} 变成在问「已有 ≥ 净缺口」，也就是
 * 「已有是否达到需求的一半」。于是一个只收集了六成材料的物品会被判成<b>金色</b>——
 * 看起来像「材料够了」，实际还差四成。这一列的颜色是玩家扫一眼挑材料用的，
 * 一个会撒谎的绿色/金色比没有颜色更糟。</p>
 *
 * <h2>修法：改由「净缺口是否为 0」决定颜色</h2>
 * <p>两个颜色常量都是通过 {@code GuiBase.TXT_GREEN / TXT_GOLD} 的
 * <b>静态字段读取</b>取出来的（不是编译期内联的常量 —— 这一点在字节码里确认过：
 * {@code render} 里就一条 {@code getstatic GuiBase.TXT_GREEN:Ljava/lang/String;}），
 * 所以在渲染方法内重定向这两次读取即可，不需要去动分支结构：</p>
 * <ul>
 *   <li>绿：功能生效时改成「净缺口为 0 才是绿，否则红」；</li>
 *   <li>金：功能生效时一律换成红 —— 它原本的意思是「世界还缺但材料够」，
 *       而新口径下这一情形本身就表现为「缺失 = 0」，早已被上一分支判成绿色，
 *       金色这一档自然消失。</li>
 * </ul>
 * <p>净效果：<b>绿 = 这个物品已经备齐（缺失显示 0），红 = 还差一些</b>。
 * 功能没生效时两个处理者都返回原版常量，配色与没装这个功能完全一致。</p>
 * <p>⚠️ 代价是原版「绿/金/红」三级配色在本功能生效时塌成两级。这是口径变更的必然结果：
 * 「世界不缺了」与「世界还缺但材料齐了」在新口径下都等价于「缺失 = 0」，
 * 而对外可见的数字只有这一个，没有依据再把它们分开染色。</p>
 *
 * <h2>⚠️ 字段访问的说明符是 {@code FIELD}，不是 {@code GETSTATIC}</h2>
 * <p>这是个能把整个 GUI 点崩的坑，特意写在这里：Mixin 的 {@code @At} 里
 * {@code GETSTATIC / GETFIELD / PUTSTATIC / PUTFIELD} <b>都不是合法取值</b>。
 * 说明符的合法集合就是 {@code InjectionPoint} 各内置实现上的 {@code @AtCode} 注解值，
 * 其中字段访问只有一个 {@code FIELD}（{@code BeforeFieldAccess} 上的
 * {@code @AtCode("FIELD")}，用 {@code javap -v} 读出来），具体读还是写由
 * {@code opcode} 参数区分。写成 {@code value = "GETSTATIC"} 时 Mixin 认不出这个值，
 * 会退化成「把它当成 InjectionPoint 子类的全限定名去加载」，
 * 抛 {@code InvalidInjectionException: GETSTATIC is not a valid injection point specifier}。</p>
 * <p>⚠️ 这个错是<b>类加载期</b>抛的：目标类是懒加载的 GUI 类，只有玩家真的点开材料列表
 * 那一刻才被织入。所以它骗过了编译、骗过了 {@code build}，直到玩家点按钮时
 * {@code GuiMaterialList.<init>} 崩在加载 {@code WidgetMaterialListEntry} 上 ——
 * 表现就是「点材料列表毫无反应」。{@code build} 成功从来不等于 Mixin 挂上了。</p>
 */
@Mixin(value = WidgetMaterialListEntry.class, remap = false)
public abstract class MixinWidgetMaterialListEntry {

    @Shadow
    @Final
    private MaterialListEntry entry;

    @Redirect(
            method = "render",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Lfi/dy/masa/malilib/gui/GuiBase;TXT_GREEN:Ljava/lang/String;"
            ),
            remap = false
    )
    private String litematicaPrinter$greenByNetGap() {
        if (VoidWarehouseMaterialCache.get().isReady() == false) {
            return GuiBase.TXT_GREEN;
        }
        return this.litematicaPrinter$isCovered() ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Lfi/dy/masa/malilib/gui/GuiBase;TXT_GOLD:Ljava/lang/String;"
            ),
            remap = false
    )
    private String litematicaPrinter$goldByNetGap() {
        if (VoidWarehouseMaterialCache.get().isReady() == false) {
            return GuiBase.TXT_GOLD;
        }
        // 金色那一档在新口径下不存在（见类注释）：走到这里就一定是「还差一些」。
        return GuiBase.TXT_RED;
    }

    /** 净缺口是否为 0，也就是这个物品是不是已经备齐了。 */
    private boolean litematicaPrinter$isCovered() {
        return this.entry != null && this.entry.getCountMissing() <= 0;
    }
}
//#endif
