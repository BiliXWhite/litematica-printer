//package me.aleksilassila.litematica.printer.printer.zxy.Utils;
//
//import fi.dy.masa.malilib.util.InventoryUtils;
//import net.fabricmc.loader.api.FabricLoader;
//import net.kyrptonaught.quickshulker.client.ClientUtil;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.network.ClientPlayerEntity;
//import net.minecraft.entity.player.PlayerInventory;
//import net.minecraft.item.ItemStack;
//import net.minecraft.registry.Registries;
//import net.minecraft.screen.PlayerScreenHandler;
//import net.minecraft.screen.ScreenHandler;
//import net.minecraft.screen.slot.Slot;
//import net.minecraft.text.Text;
//import net.minecraft.util.collection.DefaultedList;
////import net.minecraft.util.registry.Registry;
//import org.jetbrains.annotations.NotNull;
//
//public class QuickShulkerUtils {
//    @NotNull
//    public static MinecraftClient client = MinecraftClient.getInstance();
//    public static boolean loadQuickShulker = FabricLoader.getInstance().isModLoaded("quickshulker");
//    public static int hideGui = 0;
//    public static int targetSlot = 0;
//    static int slot = -1;
//    public static ItemStack waitForTheItemToBeSwitched;
//
//    //打开盒子
//    public static void openShulker(ItemStack itemStack, int slot) {
//        if (!loadQuickShulker) {
//            client.inGameHud.setOverlayMessage(Text.of("没有安装快捷盒子啊！！！ 八嘎呀路"), false);
//            return;
//        }
//        //itemStack：要打开的盒子 //slot 盒子所在槽
//        ClientUtil.CheckAndSend(itemStack, slot);
//    }
//
//    //根据传入的物品寻找对应的盒子并打开
//    public static void searchItem(ItemStack itemStack, boolean ignoreNbt) {
//        ClientPlayerEntity player = client.player;
//        if (player == null || !player.currentScreenHandler.equals(player.playerScreenHandler)) return;
//        DefaultedList<Slot> sc = player.playerScreenHandler.slots;
//        for (int i = 9; i < sc.size(); i++) {
//            ItemStack stack = sc.get(i).getStack();
//            if (!Registries.ITEM.getId(stack.getItem()).toString().contains("shulker_box")) continue;
//            DefaultedList<ItemStack> storedItems = InventoryUtils.getStoredItems(stack,-1);
//            for (int i1 = 0; i1 < storedItems.size(); i1++) {
//                ItemStack itemStack1 = storedItems.get(i1);
////                Item item = itemStack.getItem();
////                Item item1 = itemStack1.getItem();
//                if (itemStack.getItem().equals(itemStack1.getItem()) &&
//                        (ignoreNbt || itemStack.getNbt() == null || itemStack.getNbt().equals(itemStack1.getNbt()))) {
//                    SwitchItem.newItem(itemStack, null, null, i1, i);
//                    openShulker(stack, i);
//                    waitForTheItemToBeSwitched = itemStack;
//                    slot = i1;
//                    hideGui++;
//                    return;
//                }
//            }
//        }
//    }
//
//    //切换物品 需要填入替换到哪个槽
//    public static void switchItem(int slot) {
//        ClientPlayerEntity player = client.player;
//        if (player != null && slot != -1 && !player.currentScreenHandler.equals(player.playerScreenHandler)) {
//            InventoryUtils.swapSlots(player.currentScreenHandler, QuickShulkerUtils.slot, slot);
//            waitForTheItemToBeSwitched = null;
//            player.closeHandledScreen();
//        }
//    }
//
//    //将槽与背包中的空位互换
//    public static void switchPlayerInvToHotbarAir(int slot) {
//        if (client.player == null) return;
//        ClientPlayerEntity player = client.player;
//        ScreenHandler sc = player.currentScreenHandler;
//        DefaultedList<Slot> slots = sc.slots;
//        int i = sc.equals(player.playerScreenHandler) ? 9 : 0;
//        for (; i < slots.size(); i++) {
//            if (slots.get(i).getStack().isEmpty() && slots.get(i).inventory instanceof PlayerInventory) {
//                InventoryUtils.swapSlots(sc, i, slot);
//                return;
//            }
//        }
//    }
//
//    public static void test() {
//        ClientPlayerEntity player = client.player;
//        if (player == null) return;
//        //要将物品替换到哪个槽 0~8
//        targetSlot = 0;
//        //需要的物品
//        ItemStack mainHandStack = player.getMainHandStack();
//        //检查背包是否有空位 没有则将尝试把之前的物品放回原位
//        PlayerScreenHandler sc = player.playerScreenHandler;
//        if (sc.slots.stream().skip(9).limit(sc.slots.size() - 10).noneMatch(slot -> slot.getStack().isEmpty())) {
//            SwitchItem.checkItems();
//            return;
//        }
//        //切换到空手 避免将奇怪的物品放入盒子
//        switchPlayerInvToHotbarAir(targetSlot);
//        //搜索背包中的盒子
//        QuickShulkerUtils.searchItem(mainHandStack, true);
//
//        //需要补充的内容
//        //使用某个物品的时候可以刷新缓存中的使用时间
//        //SwitchItem.syncUseTime(mainHandStack);
//
//        //当物品没有时清除缓存
//        //SwitchItem.removeItem(mainHandStack);
//
//        //退出时清空缓存
//        //SwitchItem.reSet();
//    }
//}
