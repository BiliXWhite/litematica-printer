package me.aleksilassila.litematica.printer.integration.voidwarehouse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 「已有数量」那个加法（{@code merge}）与「缺失数量」那个减法（{@code netGap}）的边界。
 *
 * <p>这里刻意不碰任何 Minecraft 类型：{@link VoidWarehouseMaterialCounts} 是纯函数，
 * 而它处理的边界（仓库数超大、背包数被写成负数、需求与已有的大小关系、极端值环绕）
 * 全是「不测就一定会有人踩」的地方。</p>
 */
class VoidWarehouseMaterialCountsTest {

    @ParameterizedTest
    @CsvSource({
            "1,9223372036854775807,2147483647",
            "64,9223372036854775807,2147483647",
            "2147483647,9223372036854775807,2147483647",
            "64,2147483582,2147483646",
            "64,2147483583,2147483647",
            "64,2147483584,2147483647"
    })
    void positiveInventorySaturatesBeforeAddingWarehouseStock(int inventory, long warehouse, int expected) {
        assertEquals(expected, VoidWarehouseMaterialCounts.merge(inventory, warehouse));
    }

    @Test
    @DisplayName("仓库没有这种材料时，原样返回背包数量（功能等价于没装）")
    void warehouseAbsentLeavesInventoryCountUntouched() {
        assertEquals(64, VoidWarehouseMaterialCounts.merge(64, 0L));
        assertEquals(0, VoidWarehouseMaterialCounts.merge(0, 0L));
    }

    @Test
    @DisplayName("仓库有货时相加")
    void warehouseStockIsAddedOnTopOfInventory() {
        assertEquals(64 + 1728, VoidWarehouseMaterialCounts.merge(64, 1728L));
        // 背包一个没有、仓库有货：必须能显示出仓库那部分，否则材料会被误判成"缺"
        assertEquals(4096, VoidWarehouseMaterialCounts.merge(0, 4096L));
    }

    @Test
    @DisplayName("负数不被放大：背包数取 0 下限")
    void negativeInventoryCountIsClampedToZero() {
        // 原版自己不会给负数，但仓库那边是 long、还可能来自网络。
        // 若不做下限，(-64) + 100 会算出 36，凭空吃掉 64 个库存。
        assertEquals(100, VoidWarehouseMaterialCounts.merge(-64, 100L));
        assertEquals(0, VoidWarehouseMaterialCounts.merge(-1, 0L));
    }

    @Test
    @DisplayName("仓库数超过 int 上限时饱和，不环绕成负数")
    void overflowSaturatesInsteadOfWrapping() {
        // 仓库的账是 BigInteger，可以到天文数字。直接 (int) 强转会环绕，
        // 界面上就成了「已有 -2147483648 个」——比数字大一点糟糕得多。
        assertEquals(Integer.MAX_VALUE, VoidWarehouseMaterialCounts.merge(0, Long.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, VoidWarehouseMaterialCounts.merge(Integer.MAX_VALUE, 1L));
        assertEquals(Integer.MAX_VALUE, VoidWarehouseMaterialCounts.merge(100, Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("刚好装得下时不饱和（饱和判据不能把边界也算进去）")
    void exactBoundaryIsNotSaturated() {
        assertEquals(Integer.MAX_VALUE, VoidWarehouseMaterialCounts.merge(0, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE - 1, VoidWarehouseMaterialCounts.merge(0, Integer.MAX_VALUE - 1L));
    }

    @Test
    @DisplayName("负的仓库数按 0 处理，不参与相减")
    void negativeWarehouseAmountIsIgnored() {
        // 服务端只会回正数，但快照来自网络：一个被改过的服务端回负值时，
        // 不能让它把背包里真有的材料减掉。
        assertEquals(64, VoidWarehouseMaterialCounts.merge(64, -1L));
        assertEquals(64, VoidWarehouseMaterialCounts.merge(64, Long.MIN_VALUE));
    }

    @Test
    @DisplayName("没有需求时缺失是 0")
    void noDemandMeansNothingMissing() {
        assertEquals(0, VoidWarehouseMaterialCounts.netGap(0, 0));
        assertEquals(0, VoidWarehouseMaterialCounts.netGap(0, 100));
    }

    @Test
    @DisplayName("已备齐时缺失是 0，而不是负数（反证：朴素相减会给出负数）")
    void coveredDemandShowsZeroInsteadOfNegative() {
        // 反证组：同一组输入下「需求 − 已有」是负数。这几条断言的作用是钉住
        // 「夹到 0」这一步 —— 去掉夹紧的话界面立刻会出现「-8999」这种数。
        assertEquals(-8999, 1000 - 9999);
        assertEquals(0, VoidWarehouseMaterialCounts.netGap(1000, 9999));
        assertEquals(0, VoidWarehouseMaterialCounts.netGap(1000, 1000));
    }

    @Test
    @DisplayName("只备了一部分时，缺失正好是还差的那部分")
    void partialStockShowsTheRemainingGap() {
        // 需求 5000、已有 2000 ⟹ 还差 3000。这正是玩家要看的数：
        // 「还需要去收集多少」，而不是「这个投影一共要多少」。
        assertEquals(3000, VoidWarehouseMaterialCounts.netGap(5000, 2000));
        // 差一个也要显示 1 —— 不能被任何取整/阈值吃掉
        assertEquals(1, VoidWarehouseMaterialCounts.netGap(5000, 4999));
    }

    @Test
    @DisplayName("仓库里一个都没有时，缺失就是全部需求")
    void emptyStockShowsTheFullDemand() {
        // 与「没有快照」的情形取值相同（都是仓库贡献 0），
        // 区别只在调用方 —— 前者照常改口径，后者整体保持原版行为。
        assertEquals(5000, VoidWarehouseMaterialCounts.netGap(5000, 0));
    }

    @Test
    @DisplayName("负的已有量按 0 处理：不能反过来把需求放大")
    void negativeAvailableDoesNotInflateDemand() {
        assertEquals(5000, VoidWarehouseMaterialCounts.netGap(5000, -64));
        assertEquals(5000, VoidWarehouseMaterialCounts.netGap(5000, Integer.MIN_VALUE));
    }

    @Test
    @DisplayName("极端值不环绕：需求写成负数也不能冒出正缺口")
    void extremesDoNotWrapAround() {
        assertEquals(Integer.MAX_VALUE, VoidWarehouseMaterialCounts.netGap(Integer.MAX_VALUE, 0));
        assertEquals(0, VoidWarehouseMaterialCounts.netGap(Integer.MAX_VALUE, Integer.MAX_VALUE));
        // 这条是「用 long 相减」的理由：int 相减时 MIN − MAX 会环绕成 +1，
        // 于是一个根本不存在的「还差 1 个」会出现在界面上。
        assertEquals(0, VoidWarehouseMaterialCounts.netGap(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }
}
