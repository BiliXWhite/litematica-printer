package me.aleksilassila.litematica.printer.integration.voidwarehouse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 快照缓存：整体替换、查不到即 0、清空即忘。
 *
 * <p>单例是共享的，所以每个用例先 {@code clear()} —— 否则前一个用例留下的键
 * 会让"查不到即 0"这类断言失去意义。</p>
 */
class VoidWarehouseMaterialCacheTest {

    private static final String STONE = "minecraft:stone";
    private static final String GLASS = "minecraft:glass";

    private VoidWarehouseMaterialCache cache;

    @BeforeEach
    void freshCache() {
        this.cache = VoidWarehouseMaterialCache.get();
        this.cache.clear();
    }

    @Test
    @DisplayName("没收到过快照时查任何键都是 0，且 isReady 为 false")
    void emptyCacheReportsZeroAndNotReady() {
        assertFalse(this.cache.isReady());
        assertEquals(0L, this.cache.amountOf(STONE));
        assertEquals(0, this.cache.size());
    }

    @Test
    @DisplayName("apply 之后 isReady 为真，能查到值")
    void applyPublishesSnapshot() {
        this.cache.apply(List.of(STONE, GLASS), List.of(1728L, 64L));

        assertTrue(this.cache.isReady());
        assertEquals(1728L, this.cache.amountOf(STONE));
        assertEquals(64L, this.cache.amountOf(GLASS));
        assertEquals(2, this.cache.size());
    }

    @Test
    @DisplayName("新快照是整体替换：上一份里的键必须消失（不能让旧投影的库存挂在界面上）")
    void applyReplacesInsteadOfMerging() {
        this.cache.apply(List.of(STONE), List.of(1728L));
        this.cache.apply(List.of(GLASS), List.of(64L));

        assertEquals(0L, this.cache.amountOf(STONE));
        assertEquals(64L, this.cache.amountOf(GLASS));
        assertEquals(1, this.cache.size());
    }

    @Test
    @DisplayName("空清单也是合法快照：清空内容但保持 isReady（表示“问过了，仓库里就是没有”）")
    void emptySnapshotStillCountsAsReady() {
        this.cache.apply(List.of(STONE), List.of(1728L));
        this.cache.apply(List.of(), List.of());

        assertTrue(this.cache.isReady());
        assertEquals(0L, this.cache.amountOf(STONE));
    }

    @Test
    @DisplayName("clear 连 isReady 一起清掉（否则会显示上一台服务器的库存）")
    void clearForgetsEverything() {
        this.cache.apply(List.of(STONE), List.of(1728L));
        this.cache.clear();

        assertFalse(this.cache.isReady());
        assertEquals(0L, this.cache.amountOf(STONE));
        assertEquals(0, this.cache.size());
    }

    @Test
    @DisplayName("非正数、空 id、null 一律不进快照")
    void badEntriesAreSkipped() {
        this.cache.apply(
                List.of(STONE, GLASS, "", "minecraft:dirt", "minecraft:sand"),
                List.of(0L, -5L, 100L, 100L, 100L));

        // STONE 的数量传的是 0、GLASS 传的是负数、空 id 无效 —— 这三条都该被丢掉；
        // dirt 与 sand 是正常的 100。
        assertEquals(2, this.cache.size());
        assertEquals(0L, this.cache.amountOf(STONE));
        assertEquals(0L, this.cache.amountOf(GLASS));
        assertEquals(0L, this.cache.amountOf(""));
        assertEquals(100L, this.cache.amountOf("minecraft:dirt"));
        assertEquals(100L, this.cache.amountOf("minecraft:sand"));
    }

    @Test
    @DisplayName("两个数组长度不一致时按短的截断，不越界")
    void mismatchedLengthsAreTruncated() {
        this.cache.apply(List.of(STONE, GLASS), List.of(10L));
        assertEquals(10L, this.cache.amountOf(STONE));
        assertEquals(0L, this.cache.amountOf(GLASS));

        this.cache.apply(List.of(STONE), List.of(10L, 20L));
        assertEquals(10L, this.cache.amountOf(STONE));
        assertEquals(1, this.cache.size());
    }

    @Test
    @DisplayName("同一个 id 出现两次时累加（服务端已去重，这里只是不让两条数组对不上账）")
    void duplicateIdsAccumulate() {
        this.cache.apply(List.of(STONE, STONE), List.of(10L, 32L));
        assertEquals(42L, this.cache.amountOf(STONE));
        assertEquals(1, this.cache.size());
    }

    @Test
    @DisplayName("null 入参不崩，并得到一个空快照")
    void nullListsAreTolerated() {
        this.cache.apply(List.of(STONE), List.of(1L));
        this.cache.apply(null, null);

        assertTrue(this.cache.isReady());
        assertEquals(0L, this.cache.amountOf(STONE));
    }

    @Test
    @DisplayName("超过上限的条目被截断（防一个被改过的服务端撑爆内存）")
    void oversizedSnapshotIsCapped() {
        int overflow = 4096 + 64;
        List<String> ids = new ArrayList<>(overflow);
        List<Long> amounts = new ArrayList<>(overflow);
        for (int i = 0; i < overflow; i++) {
            ids.add("minecraft:test_" + i);
            amounts.add(1L);
        }

        this.cache.apply(ids, amounts);

        assertEquals(4096, this.cache.size());
        assertEquals(1L, this.cache.amountOf("minecraft:test_4095"));
        assertEquals(0L, this.cache.amountOf("minecraft:test_4096"));
    }
}
