package me.aleksilassila.litematica.printer.mixin.printer.litematica;

//#if MC >= 12111
import fi.dy.masa.litematica.materials.MaterialListEntry;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialCache;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialCounts;
import me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialIds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 材料列表的两个数字都改由虚空仓库参与：
 * <ul>
 *   <li><b>已有数量</b>（{@code getCountAvailable}）= 玩家背包 + 虚空仓库；</li>
 *   <li><b>缺失数量</b>（{@code getCountMissing}）= 需求 − 已有，也就是
 *       <b>还需要去收集多少</b>（夹到 0，不会出现负数）。</li>
 * </ul>
 *
 * <h2>为什么要动「缺失数量」</h2>
 * <p>只补「已有」是不够的。原版那两个数是各算各的：{@code countAvailable} 每次刷新
 * 被 {@code MaterialListUtils#updateAvailableCounts} 覆盖重算，而 {@code countMissing}
 * 是<b>创建列表时定死的</b>（开「原理图」列表时直接拿 {@code countsTotal.clone()}，
 * 即恒等于总数）—— 它是个 {@code final} 字段，连 setter 都没有。于是只补「已有」
 * 会让界面出现「已有 500、还缺 300」这种自相矛盾的行。</p>
 *
 * <h2>为什么挂在两个 getter 的返回值上</h2>
 * <p>刷新路径有两条（GUI 构造时一次、信息 HUD 每 2 秒一次），而读它的地方有四个。
 * 挂在 getter 上<b>幂等</b>：纯读、不改字段，同一个 entry 被读几百次也只算同样多的数。
 * 写在 setter 上则要求「每次覆盖之后恰好补一次」—— 一旦哪条刷新路径漏调或重调，
 * 账面就飘了。另外 {@code getCountMissing()} 的签名在四个 Litematica 版本里逐字节一致。</p>
 *
 * <h2>为什么这里不判断配置开关</h2>
 * <p>开关、通道可用性、服务端规则状态，全部由
 * {@link me.aleksilassila.litematica.printer.integration.voidwarehouse.VoidWarehouseMaterialNetwork}
 * 在<b>发包那一侧</b>统一裁决：只要功能不可用它就
 * {@code VoidWarehouseMaterialCache#clear()}。所以这里只认一个信号 ——
 * {@link VoidWarehouseMaterialCache#isReady()}「有没有拿到过快照」。
 * 没拿到 = 本模组当不存在（「已有」不加、「缺失」不动），与没装这个功能完全一致。</p>
 *
 * <h2>⚠️ 改了「缺失」的口径，就必须同步修三处消费者</h2>
 * <p>原版所有判断都建立在一个不变式上：{@code missing} 是<b>需求</b>、
 * {@code available} 是<b>持有</b>，于是 {@code available >= missing} 就是「材料够不够」。
 * 本注入把 {@code missing} 换成了<b>净缺口</b>，这个不变式当场失效 ——
 * 凡是拿这两个数做比较的地方，语义都会歪掉。逐一看四个消费点：</p>
 * <table border="1">
 *   <caption>消费点与处理</caption>
 *   <tr><th>消费点</th><th>原式</th><th>是否需要额外处理</th></tr>
 *   <tr><td>GUI 列表的「缺失」列 / 列宽 / 悬浮提示</td>
 *       <td>直接显示 {@code getCountMissing()}</td>
 *       <td>不需要 —— 显示净值正是本功能要的</td></tr>
 *   <tr><td>顶部进度行（{@code MaterialListBase#updateCounts} 求和）</td>
 *       <td>Σ {@code getCountMissing()}</td>
 *       <td>不需要 —— 求和自然变成「还没有收集齐的部分」</td></tr>
 *   <tr><td>排序（{@code COUNT_MISSING}）/ CSV·JSON 导出</td>
 *       <td>直接读 {@code getCountMissing()}</td>
 *       <td>不需要 —— 同样按净值排、按净值导出</td></tr>
 *   <tr><td>「只显示缺料」过滤器（{@code MaterialListBase#recreateFilteredList}）</td>
 *       <td>{@code available < missing}</td>
 *       <td><b>要</b>，见 {@code MixinMaterialListBase}</td></tr>
 *   <tr><td>信息 HUD（{@code MaterialListHudRenderer#render}）</td>
 *       <td>{@code missing - available}</td>
 *       <td><b>要</b>，见 {@code MixinMaterialListHudRenderer}</td></tr>
 *   <tr><td>行的绿/金/红配色（{@code WidgetMaterialListEntry#render}）</td>
 *       <td>{@code countAvailable >= countMissing}</td>
 *       <td><b>要</b>，见 {@code MixinWidgetMaterialListEntry}</td></tr>
 * </table>
 * <p>三处的修法都是同一个动作：<b>把参与比较的「持有」侧当成 0</b>，
 * 使判据退回它真正想表达的那句话 —— 「净缺口是不是 0」。
 * 三处的详细理由写在各自的类里。</p>
 */
@Mixin(value = MaterialListEntry.class, remap = false)
public abstract class MixinMaterialListEntry {

    @Inject(method = "getCountAvailable", at = @At("RETURN"), cancellable = true, remap = false)
    private void litematicaPrinter$addVoidWarehouseStock(CallbackInfoReturnable<Integer> cir) {
        MaterialListEntry self = (MaterialListEntry) (Object) this;
        String id = VoidWarehouseMaterialIds.idOf(self.getStack());
        if (id == null) {
            return;
        }
        long warehouseAmount = VoidWarehouseMaterialCache.get().amountOf(id);
        if (warehouseAmount <= 0L) {
            // 绝大多数条目走这条分支（仓库里没有这种材料，或者功能压根没生效）。
            // 提前返回，省掉一次 merge 调用 —— 这个方法每帧会被调几百次。
            return;
        }
        cir.setReturnValue(VoidWarehouseMaterialCounts.merge(cir.getReturnValueI(), warehouseAmount));
    }

    /**
     * 把「缺失数量」改成净值：需求 − 已有（已有 = 背包 + 仓库）。
     *
     * <p>注意这里刻意<b>复用 {@code getCountAvailable()}</b> 而不是自己去查一遍仓库：
     * 那个数已经由上面那个注入补成了「背包 + 仓库」，两处各查一次是让同一件事有两个实现，
     * 迟早会分叉（比如一边忘了算末影箱）。代价只是一次多余的 id 转换。</p>
     */
    @Inject(method = "getCountMissing", at = @At("RETURN"), cancellable = true, remap = false)
    private void litematicaPrinter$subtractAvailableStock(CallbackInfoReturnable<Integer> cir) {
        if (VoidWarehouseMaterialCache.get().isReady() == false) {
            // 没拿到过快照：功能没在生效（没进世界 / 配置关了 / 对面没装服务端模组 /
            // 服主没开规则）。此时保持原版的值，等于本模组不存在。
            // 判断放在最前面，因为它是三个条件里最便宜的一个。
            return;
        }
        MaterialListEntry self = (MaterialListEntry) (Object) this;
        cir.setReturnValue(VoidWarehouseMaterialCounts.netGap(cir.getReturnValueI(), self.getCountAvailable()));
    }
}
//#endif
