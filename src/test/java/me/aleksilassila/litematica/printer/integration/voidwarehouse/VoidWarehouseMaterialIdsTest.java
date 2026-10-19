package me.aleksilassila.litematica.printer.integration.voidwarehouse;

//#if MC >= 12111
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 客户端写出去的键，能不能被服务端原封不动地认出来。
 *
 * <p>这是整个功能里唯一一条<b>跨进程</b>的约定，也是最容易静默失效的一环：
 * 写错了不会报错、不会崩服，表现只是「材料列表上仓库库存永远是 0」。
 * 所以这里的断言刻意<b>照抄服务端的处理链</b>（见 {@link VoidWarehouseMaterialIds} 的类注释），
 * 而不是只断言一个硬编码字符串 —— 后者只能证明今天是对的，证明不了两边同构。</p>
 *
 * <p>注意：这里一律走 {@code idOfItem} 而不是 {@code new ItemStack(item)}。
 * {@code Bootstrap.bootStrap()} 并<b>不</b>绑定数据组件注册表，
 * 单元测试里构造 {@code ItemStack} 会抛 {@code NullPointerException: Components not bound yet}。</p>
 */
class VoidWarehouseMaterialIdsTest {

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("注册名是 namespace:path 形式")
    void idIsNamespaceAndPath() {
        assertEquals("minecraft:stone", VoidWarehouseMaterialIds.idOfItem(Items.STONE));
    }

    @Test
    @DisplayName("空气与 null 没有键（不能把 minecraft:air 发出去白问一场）")
    void airHasNoId() {
        assertNull(VoidWarehouseMaterialIds.idOfItem(Items.AIR));
        assertNull(VoidWarehouseMaterialIds.idOfItem(null));
    }

    @Test
    @DisplayName("物品堆这一层：空堆没有键")
    void emptyStackHasNoId() {
        assertNull(VoidWarehouseMaterialIds.idOf(ItemStack.EMPTY));
    }

    @Test
    @DisplayName("服务端往返：tryParse → getOptional → getKey().toString() 必须原样回到同一个键")
    void idSurvivesTheServerSideRoundTrip() {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                // 空气没有键，前面单独测过。服务端的 canonicalId 也会把它判掉。
                continue;
            }
            String id = VoidWarehouseMaterialIds.idOfItem(item);
            assertNotNull(id, "注册表里的物品都应当有键");

            // 第一步：服务端 canonicalId() 的 Identifier.tryParse(raw)
            Identifier parsed = Identifier.tryParse(id);
            assertNotNull(parsed, "服务端解析失败就永远不会命中：" + id);

            // 第二步：服务端的 BuiltInRegistries.ITEM.getOptional(id) —— 解析不出来的物品会被丢弃
            assertFalse(BuiltInRegistries.ITEM.getOptional(parsed).isEmpty(),
                    "服务端会认为这不是有效物品：" + id);

            // 第三步：服务端回执里带的是 idOf(item)，也就是再序列化一次
            assertEquals(id, parsed.toString(), "命名空间写法与服务端不一致：" + id);
        }
    }

    @Test
    @DisplayName("反证：往返断言咬的是命名空间，不是「随便什么字符串都能过」")
    void roundTripAssertionHasTeeth() {
        // ① 这套往返确实会拒绝非法写法 —— tryParse 不是「来者不拒」
        assertNull(Identifier.tryParse("Minecraft:Stone"), "大写命名空间本该非法");

        // ② 关键反证：原版物品全在 minecraft 命名空间下，光看它们区分不出
        //    toString() 与 getPath()（后者会被 tryParse 补回 minecraft:）。
        //    换一个命名空间，两种写法立刻分道扬镳 —— 这正是上面那条往返断言真正在检查的东西。
        Identifier foreign = Identifier.fromNamespaceAndPath("carpet_neko_addition", "stone");
        String canonical = foreign.toString();
        assertEquals(canonical, Identifier.tryParse(canonical).toString(), "规范写法应当原样往返");
        assertNotEquals(canonical, Identifier.tryParse(foreign.getPath()).toString(),
                "丢掉命名空间的写法会走样，说明这条断言是会报警的");
    }
}
//#endif
