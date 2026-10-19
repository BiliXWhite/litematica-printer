package me.aleksilassila.litematica.printer.integration.voidwarehouse;

/**
 * 材料列表那两个数字的纯计算：{@link #merge} 算出「已有数量」，
 * {@link #netGap} 算出「缺失数量」。
 *
 * <h2>为什么单独一个类</h2>
 * <p>已有数量由 {@code MaterialListEntry#getCountAvailable} 注入，收集缺口只在 GUI/HUD 计算。
 * 普通单元测试没有 Fabric 的加载期织入。
 * 把判断逻辑挪到这里，注入点就退化成「拿返回值、算一下、塞回去」三行胶水，
 * 而所有真正会出错的边界（溢出、负数、仓库没数据）都能被普通 JUnit 直接咬住。</p>
 *
 * <h2>口径：仓库 + 背包</h2>
 * <p>这是与玩家确认过的口径。理由：这台服务器上仓库才是主要库存，
 * 而玩家手上永远会有一些「刚从仓库取出来、正拿在手里准备放」的材料。
 * 只看仓库会让这类材料凭空显示成 0，只看背包则完全反映不出仓库有多少 ——
 * 两个都要算，才回答得了「我还差多少」。</p>
 */
public final class VoidWarehouseMaterialCounts {

    private VoidWarehouseMaterialCounts() {
    }

    /** Multiplier 1 uses unfinished world blocks; larger multipliers request complete copies. */
    public static int demand(int total, int missing, int multiplier) {
        return multiplier == 1 ? missing
                : (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) total * multiplier));
    }

    /**
     * 合成。
     *
     * @param inventoryCount  原版算出来的「已有数量」（玩家背包 + 末影箱）
     * @param warehouseAmount 虚空仓库里的数量；没有这条数据时传 0
     * @return 相加后的数量，<b>饱和</b>在 {@link Integer#MAX_VALUE}
     */
    public static int merge(int inventoryCount, long warehouseAmount) {
        // 界面上这个数只会被当「够不够」用，负数没有任何意义。原版自己不会给负数，
        // 但仓库那边是 long、还可能来自网络，多一道夹紧不亏。
        long base = Math.max(0, inventoryCount);
        if (warehouseAmount <= 0) {
            return (int) Math.min(base, Integer.MAX_VALUE);
        }
        // 仓库可能已经是 Long.MAX_VALUE，必须先判断饱和，避免加上背包数量后 long 也溢出。
        if (warehouseAmount >= Integer.MAX_VALUE - base) {
            return Integer.MAX_VALUE;
        }
        return (int) (base + warehouseAmount);
    }

    /**
     * 算出材料列表要显示的「缺失数量」= 还需要去收集多少个。
     *
     * <h2>口径：需求 − 已有，夹到 0</h2>
     * <p>「已有」的口径与上面一致（背包 + 虚空仓库）。所以仓库里已经备齐的材料会把缺失压到
     * 0；只备了一部分的，缺失就正好是还差的那部分。这正是玩家要看的数：
     * <b>还需要去收集多少</b>，而不是「这个投影一共要多少」。</p>
     *
     * <h2>为什么这里要夹到 0</h2>
     * <p>已经备齐甚至备多了的时候，代数结果是负数。界面上一个「-300」既没有含义、
     * 又会让原版的颜色判据（够不够）彻底失去依据，所以负数一律按 0。
     * 顺带说一句：这也正是原版信息 HUD 那一行想要的数
     * （它自己写的是 {@code missing - available}，只是没有夹紧，会显示成负数）。</p>
     *
     * @param demand    该物品的需求量（原版算出来的「缺失/还需放置」方块数）
     * @param available 已有的数量（{@link #merge} 的结果：背包 + 虚空仓库）
     * @return 还需要收集的数量，最小 0
     */
    public static int netGap(int demand, int available) {
        // 用 long 相减：int 相减在极端值上会环绕，而环绕出来的正数会被当成一个真实的缺口
        // （「已经备齐」反而显示成还差一大截）。
        long gap = (long) demand - Math.max(0, available);
        if (gap <= 0L) {
            return 0;
        }
        // 走到这里 gap ≤ demand ≤ Integer.MAX_VALUE，强转不丢数。
        return (int) gap;
    }
}
