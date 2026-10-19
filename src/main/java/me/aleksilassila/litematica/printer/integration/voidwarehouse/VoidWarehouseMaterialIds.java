package me.aleksilassila.litematica.printer.integration.voidwarehouse;

//#if MC >= 12111
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 物品 → 虚空仓库用的那个键。
 *
 * <h2>为什么要单独一个类</h2>
 * <p>这个转换只有一行，但它是<b>跨进程契约</b>里最容易静默失效的一环：
 * 客户端拼清单时写成什么，服务端就用什么去查仓库，两边只要差一个字符
 * （命名空间写法、{@code toString} 与 {@code getPath} 混用、大小写），
 * 表现就是「材料列表上仓库库存永远是 0」—— 不报错、不崩服，只是没效果。
 * 抽出来之后，它能被执行 <b>真实服务端同一套规则</b>的单元测试直接咬住
 * （见 {@code VoidWarehouseMaterialIdsTest}）。</p>
 *
 * <h2>为什么是 {@code toString()} 而不是 {@code getPath()}</h2>
 * <p>服务端 {@code PrinterQueryService} 的处理链是：</p>
 * <pre>
 *   Identifier.tryParse(raw) → BuiltInRegistries.ITEM.getOptional(id)
 *                            → BuiltInRegistries.ITEM.getKey(item).toString()
 * </pre>
 * <p>即「再解析一次、再序列化一次」。{@code Identifier.toString()} 给的是
 * {@code namespace:path}（{@code getPath()} 会把 {@code minecraft:} 丢掉），
 * 这正是服务端会吐回来的那种形式，所以两边相等。</p>
 *
 * <h2>为什么整类都在 {@code MC >= 12111} 里</h2>
 * <p>1.21.11 之前这个类叫 {@code ResourceLocation}，方法签名一样但类型不同；
 * 而虚空仓库这条能力本来就只在 1.21.11+ 存在（服务端 carpet-Neko-Addition 也只支持
 * 26.x 与 1.21.11）。与其为一个不存在的功能维护两个分支，不如整类只在支持的版本上编译。</p>
 */
public final class VoidWarehouseMaterialIds {

    private VoidWarehouseMaterialIds() {
    }

    /**
     * @param stack 要转换的物品堆
     * @return 注册表 id（如 {@code minecraft:stone}）；空气 / {@code null} 返回 {@code null}
     */
    public static String idOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return idOfItem(stack.getItem());
    }

    /**
     * 真正干活的那个重载。
     *
     * <p>之所以把「物品」这一层单独暴露出来：{@code new ItemStack(...)} 需要数据组件注册表
     * 已经绑定，而这个绑定在单元测试的引导流程里是<b>没有</b>的
     * （{@code Bootstrap.bootStrap()} 之后构造 {@code ItemStack} 会抛
     * {@code NullPointerException: Components not bound yet}）。
     * 只认 {@code ItemStack} 的话，这条跨端契约就只能靠「跑一次真实客户端看看」来验证。</p>
     *
     * <p>名字不叫 {@code idOf} 重载是为了避开 {@code idOf(null)} 的歧义 —— 两个重载同名的
     * 话，任何一处传 {@code null} 字面量的调用点都会变成编译错误。</p>
     *
     * @param item 物品；{@code null} 或空气返回 {@code null}
     */
    public static String idOfItem(Item item) {
        if (item == null || item == Items.AIR) {
            return null;
        }
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        return key == null ? null : key.toString();
    }
}
//#endif
