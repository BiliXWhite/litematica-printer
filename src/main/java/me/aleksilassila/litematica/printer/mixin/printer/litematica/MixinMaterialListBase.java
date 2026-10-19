package me.aleksilassila.litematica.printer.mixin.printer.litematica;

//#if MC >= 12111
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 「只显示缺料」过滤器（{@code MaterialListBase#recreateFilteredList}）的比较基准修正。
 *
 * <h2>原版在比什么</h2>
 * <p>判据是 {@code entry.getCountAvailable() < countMissing}，含义是
 * 「持有 &lt; 需求 ⟹ 这个物品还缺着，留在列表里」。它依赖一条不变式：
 * {@code missing} 是<b>需求</b>、{@code available} 是<b>持有</b>。</p>
 *
 * <h2>为什么改了「缺失」口径之后它会歪</h2>
 * <p>{@code MixinMaterialListEntry} 把 {@code getCountMissing()} 换成了<b>净缺口</b>
 * （需求 − 已有）。于是这里实际在比 {@code 已有 < 需求 − 已有}，等价于
 * {@code 已有 < 需求的一半} —— 只要仓库里备了一半以上，这一项就会被判成「不缺」，
 * 从「只显示缺料」的列表里<b>消失</b>。一个明明还差一半材料的物品被藏起来，
 * 比不做这个功能更糟。</p>
 *
 * <h2>修法：把「持有」那一侧当成 0</h2>
 * <p>判据就退回它真正想表达的那句话：「净缺口大于 0 吗」。
 * 这与原版语义<b>等价</b>：原式 {@code 持有 < 需求} ⟺ {@code max(0, 需求 − 持有) > 0}。
 * 所以功能没生效时返回真实持有量、生效时返回 0，两条路得出同一个「留还是不留」。</p>
 * <p>同理下面那个 {@code else if (this.hideAvailable)} 分支（把已经备齐的条目
 * 从预过滤列表里永久移除）要的是「持有 ≥ 需求」，等价于「净缺口 == 0」，也一致。</p>
 *
 * <p>⚠️ 唯一对不齐的是 {@code multiplier > 1} 的情形：那时原版拿
 * {@code multiplier * countTotal}（而不是 {@code getCountMissing()}）当需求，
 * 这里把持有侧置 0 会让判据恒真。实际影响是「把倍率调大之后，那些已经备齐的条目
 * 不会再被自动移出列表」—— 只影响这个冷门开关下的列表清理，不影响显示的数字。</p>
 */
@Mixin(value = MaterialListBase.class, remap = false)
public abstract class MixinMaterialListBase {

    @Redirect(
            method = "recreateFilteredList",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/materials/MaterialListEntry;getCountAvailable()I"
            ),
            remap = false
    )
    private int litematicaPrinter$compareAgainstNetGap(MaterialListEntry entry) {
        return VoidWarehouseMaterialCache.get().isReady() ? 0 : entry.getCountAvailable();
    }
}
//#endif
