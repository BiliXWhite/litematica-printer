package me.aleksilassila.litematica.printer.integration.voidwarehouse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端这一侧「虚空仓库现在有什么、各多少」的快照缓存。
 *
 * <h2>为什么是「一份快照」而不是「逐条累加」</h2>
 * <p>每次服务端回执里带的都是<b>当前那次询问的完整答案</b>（只含仓库里真有货的物品）。
 * 所以这里整体<b>替换</b>而不是合并：换了投影、换了配方之后，上一份快照里的键就不该再留着，
 * 否则「上一个投影要的方块」会继续在界面上显示一个过时的数量。
 * 整体替换还顺带解决了「怎么知道一个物品是 0」——不在表里就是 0。</p>
 *
 * <h2>为什么所有方法都 synchronized</h2>
 * <p>回执是在客户端网络线程写进来的（Fabric 的 {@code registerGlobalReceiver} 回调），
 * 而读它的是渲染线程 —— 材料列表每帧都会问一遍。写是一次性的、读是每帧几百次，
 * 所以用最朴素的一把锁：写的时候没人读得到半张表，读的时候也绝不会看到
 * 一个「size 已经变了、桶数组还没换」的中间态（那正是 {@code HashMap} 并发读最典型的翻车方式）。</p>
 *
 * <h2>为什么键是 {@code String}</h2>
 * <p>网络上传的就是注册表 id 字符串。直接拿它当键，收包时不用做任何转换；
 * 而查询方（{@link #amountOf(String)}）拿到的也是同一个 id —— 由调用方负责
 * 「物品 → id」那一次转换（只有一处，在 Mixin 里）。</p>
 */
public final class VoidWarehouseMaterialCache {

    /** 快照里最多存多少条；与服务端回执的上限一致，防一个被改过的服务端撑爆内存。 */
    private static final int MAX_ENTRIES = 4096;

    private static final VoidWarehouseMaterialCache INSTANCE = new VoidWarehouseMaterialCache();

    private final Map<String, Long> amounts = new HashMap<>();
    private boolean ready;

    private VoidWarehouseMaterialCache() {
    }

    public static VoidWarehouseMaterialCache get() {
        return INSTANCE;
    }

    /**
     * 用一份新快照整体替换旧内容。
     *
     * @param ids     物品注册表 id
     * @param values  与 {@code ids} 下标一一对应的数量
     */
    public synchronized void apply(List<String> ids, List<Long> values) {
        this.amounts.clear();
        if (ids != null && values != null) {
            int size = Math.min(ids.size(), values.size());
            if (size > MAX_ENTRIES) {
                size = MAX_ENTRIES;
            }
            for (int i = 0; i < size; i++) {
                String id = ids.get(i);
                Long value = values.get(i);
                if (id == null || id.isEmpty() || value == null || value <= 0L) {
                    continue;
                }
                // 同一个 id 出现两次时累加 —— 服务端已经去过重，这里只是不让它对不上账。
                this.amounts.merge(id, value, Long::sum);
            }
        }
        this.ready = true;
    }

    /**
     * 忘掉当前快照。
     *
     * <p>断开连接、关掉功能、服务端说规则没开时都调它。留着旧数据比没有数据更糟：
     * 换到另一台没装服务端模组的服务器之后，材料列表会继续显示上一台服务器的库存。</p>
     */
    public synchronized void clear() {
        this.amounts.clear();
        this.ready = false;
    }

    /**
     * @return 该 id 在仓库里的数量；没有快照、或者仓库里没有它，都是 0
     */
    public synchronized long amountOf(String id) {
        if (id == null) {
            return 0L;
        }
        Long value = this.amounts.get(id);
        return value == null ? 0L : value;
    }

    /** 是否已经拿到过至少一份快照（用于区分「仓库里没有」与「还没问到」）。 */
    public synchronized boolean isReady() {
        return this.ready;
    }

    /** 当前快照的条目数，仅供诊断/测试。 */
    public synchronized int size() {
        return this.amounts.size();
    }
}
