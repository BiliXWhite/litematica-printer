package me.aleksilassila.litematica.printer.mixin.printer.litematica;

//#if MC >= 12111
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 信息 HUD 那一行数字（{@code MaterialListHudRenderer#render}）的修正。
 *
 * <h2>原版本来就在做减法</h2>
 * <p>原版这里是 {@code count = entry.getCountMissing() - entry.getCountAvailable()}，
 * 也就是「需求 − 持有」= <b>还差多少</b> —— 与材料列表那个「缺失」列想表达的其实是同一件事，
 * 只是 GUI 那一列显示的是没做减法的原值。</p>
 *
 * <h2>为什么这里必须改</h2>
 * <p>{@code MixinMaterialListEntry} 已经把 {@code getCountMissing()} 换成了净缺口，
 * 而 HUD 又自己减了一次持有量 ⇒ 变成 {@code 净缺口 − 已有}。仓库备得多时这个数会走到
 * 很大的负值（界面上就是一行红色的负数）。所以这里把参与减法的「持有」侧置 0：
 * {@code 净缺口 − 0} 正好就是 HUD 想显示的那个数。</p>
 * <p>顺带修掉一个原版的小毛病：原式没有夹紧，{@code 持有 > 需求} 时会显示负数，
 * 而净值本身最小就是 0。</p>
 *
 * <h2>为什么这一个注入点要覆盖两处</h2>
 * <p>{@code render} 里有两段结构完全相同的表达式（一个是量列宽、一个是真正画出来），
 * 都写成 {@code getCountMissing() - getCountAvailable()}。Mixin 的 {@code @Redirect}
 * 本来就会注入到<b>所有</b>匹配到的调用点上，所以一个处理者足够，
 * 也刻意<b>不写 ordinal</b> —— 靠序号定位等于把「上游在这段代码里恰好有几处这样的调用」
 * 变成一个隐含依赖，将来上游多画一行就会指错地方。</p>
 */
@Mixin(value = MaterialListHudRenderer.class, remap = false)
public abstract class MixinMaterialListHudRenderer {

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/materials/MaterialListEntry;getCountAvailable()I"
            ),
            remap = false
    )
    private int litematicaPrinter$displayNetGap(MaterialListEntry entry) {
        return VoidWarehouseMaterialCache.get().isReady() ? 0 : entry.getCountAvailable();
    }
}
//#endif
