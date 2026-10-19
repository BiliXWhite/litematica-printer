package me.aleksilassila.litematica.printer.integration.voidwarehouse;

//#if MC >= 12111
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.integration.inventory.InventoryProvider;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialRequest;
import me.aleksilassila.litematica.printer.integration.inventory.MaterialReservation;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import neko.voidwarehouse.restock.PrinterTakePayload;
import neko.voidwarehouse.restock.PrinterTakeResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 把服务端 carpet-Neko-Addition 的虚空仓库接进打印机的补货责任链。
 *
 * <h2>它取代的是什么</h2>
 * <p>原版打印机缺料时会按顺序拜托 Take It Out（世界容器）、QuickShulker（背包潜影盒）、
 * Chest Tracker（记忆里的箱子）去要货。这些来源的共同点是：<b>发的都是玩家自己攒下的物资</b>。
 * 虚空仓库则是服务端的一笔独立库存。把它挂在 Take It Out 之后、快捷潜影盒之前，
 * 语义就是「确实有地方拿就先拿真的，都拿不到才动用仓库」。</p>
 *
 * <h2>一次请求的时间线</h2>
 * <ol>
 *   <li>{@code request}：把候选物品的注册表 id 打包发过去，标记这套 token 在等回执；</li>
 *   <li>{@code status}：每 tick 轮询 —— 背包里真的出现了就算成功，
 *      收到否定回执就立刻判定缺料，不用干等到超时；</li>
 *   <li>真超时由 {@code MaterialRequestCoordinator} 兜底，它会调用 {@code reset}。</li>
 * </ol>
 *
 * <h2>为什么成功与否以「背包里有没有」为准</h2>
 * <p>服务端回 {@code OK} 只说明它发货了，还要经过一次背包同步才会到客户端手上。
 * 以回执为准会出现「判定成功但手上还是空的」，下一步放置必然失败。</p>
 *
 * <h2>为什么不抢租约</h2>
 * <p>这条路径全程不开容器、不点屏幕，东西由服务端直接塞进背包，
 * 客户端这一侧没有任何需要占用主手或背包的操作，因此不申请 {@code ResourceLease}，
 * 同时 {@code blocksPrinterWhilePending} 返回 false，等货期间打印继续。</p>
 */
public final class VoidWarehouseRestockAdapter implements InventoryProvider {

    /**
     * 同一次缺料失败后，隔多久才肯再问一次服务端。
     *
     * <p>coordinator 判定缺料后就把 request 清掉了，而打印机每 tick 都会为同一个缺料重发请求。
     * 没有这个冷却的话，缺一种方块就会每秒二十次地问服务端要同一种东西 ——
     * 服务端虽然有 50 毫秒限流拦着，但白白的坐标系写盘开销一点也没省下来。
     * 5 秒足够让「真的去补货」这件事完成。</p>
     */
    private static final long FAILED_COOLDOWN_TICKS = 100L;

    private final Minecraft client = Minecraft.getInstance();

    private MaterialRequest activeRequest;
    private int activeNonce;

    private Item lastFailedItem;
    private long lastFailedTick = Long.MIN_VALUE;

    @Override
    public String id() {
        return "void_warehouse";
    }

    @Override
    public MaterialReservation request(MaterialRequest request) {
        if (!enabled(request)) {
            return unavailable(request);
        }
        Item availableItem = findAvailableItem(request);
        if (availableItem != null) {
            return MaterialReservation.available(request, availableItem);
        }
        List<String> candidates = candidatesOf(request);
        if (candidates.isEmpty()) {
            return unavailable(request);
        }
        int nonce = VoidWarehouseRestockNetwork.requestTake(candidates);
        if (nonce == 0) {
            return unavailable(request);
        }
        this.activeRequest = request;
        this.activeNonce = nonce;
        return pending(request);
    }

    @Override
    public MaterialReservation status(MaterialRequest request) {
        if (this.activeRequest == null || this.activeRequest.token() != request.token()) {
            return unavailable(request);
        }
        Item availableItem = findAvailableItem(request);
        if (availableItem != null) {
            selectCompletedPickBlockItem(request, availableItem);
            finish();
            return MaterialReservation.available(request, availableItem);
        }
        int status = VoidWarehouseRestockNetwork.statusOf(this.activeNonce);
        if (status == PrinterTakeResultPayload.NONE) {
            // 还没等到回执，继续等；真超时由 coordinator 负责
            return pending(request);
        }
        if (status == PrinterTakeResultPayload.OK) {
            // 服务端说给了，但背包同步还没到。继续轮询背包，直到超时为止。
            return pending(request);
        }
        rememberFailure(request);
        finish();
        return unavailable(request);
    }

    @Override
    public long pendingTimeoutTicks() {
        return 100L;
    }

    @Override
    public boolean blocksPrinterWhilePending() {
        return false;
    }

    @Override
    public void reset() {
        this.activeRequest = null;
        this.activeNonce = 0;
        VoidWarehouseRestockNetwork.forget();
    }

    /**
     * 双开关：服务端那边要开 Carpet 规则（不知道开没开也无所谓，服务端会回 RULE_OFF），
     * 本地这边要有配置项。
     */
    private boolean enabled(MaterialRequest request) {
        if (!Configs.Special.VOID_WAREHOUSE_RESTOCK.getBooleanValue()) {
            return false;
        }
        if (this.client.player == null || this.client.level == null) {
            return false;
        }
        if (failedRecently(request)) {
            return false;
        }
        return VoidWarehouseRestockNetwork.isSupported();
    }

    private boolean failedRecently(MaterialRequest request) {
        Item item = request.preferredItem();
        if (item == null || item != this.lastFailedItem) {
            return false;
        }
        return gameTick() - this.lastFailedTick < FAILED_COOLDOWN_TICKS;
    }

    private void rememberFailure(MaterialRequest request) {
        this.lastFailedItem = request.preferredItem();
        this.lastFailedTick = gameTick();
    }

    private void finish() {
        this.activeRequest = null;
        this.activeNonce = 0;
        VoidWarehouseRestockNetwork.forget();
    }

    private Item findAvailableItem(MaterialRequest request) {
        for (Item item : request.acceptedItems()) {
            if (InventoryUtils.playerHasItemInInventory(this.client.player, item)) {
                return item;
            }
        }
        return null;
    }

    private static void selectCompletedPickBlockItem(MaterialRequest request, Item item) {
        Minecraft client = Minecraft.getInstance();
        if (request.source() == MaterialRequest.Source.PICK_BLOCK
                && client.player != null
                && client.player.containerMenu == client.player.inventoryMenu) {
            InventoryUtils.setPickedItemToHand(new ItemStack(item), client);
        }
    }

    /**
     * 把请求里的候选物品翻译成服务端能解析的注册表 id，首选物品排在最前。
     *
     * <p>为什么不是只发首选：打印机常常接受一组「可以互相替代」的方块
     * （比如同一个投影位允许若干种台阶）。让服务端按优先级依次挑一个真有货的，
     * 一次往返就能解决问题。</p>
     */
    private static List<String> candidatesOf(MaterialRequest request) {
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, request.preferredItem());
        for (Item item : request.acceptedItems()) {
            addCandidate(candidates, item);
        }
        return List.copyOf(candidates);
    }

    private static void addCandidate(List<String> candidates, Item item) {
        if (item == null || candidates.size() >= PrinterTakePayload.MAX_CANDIDATES) {
            return;
        }
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) {
            return;
        }
        String id = key.toString();
        if (id.isEmpty() || id.equals("minecraft:air") || candidates.contains(id)) {
            return;
        }
        candidates.add(id);
    }

    private long gameTick() {
        return this.client.level == null ? 0L : this.client.level.getGameTime();
    }

    private static MaterialReservation pending(MaterialRequest request) {
        return new MaterialReservation(request.token(), MaterialReservation.State.PENDING);
    }

    private static MaterialReservation unavailable(MaterialRequest request) {
        return new MaterialReservation(request.token(), MaterialReservation.State.UNAVAILABLE);
    }
}
//#endif
