package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class Filters {
    /**
     * 根据给定的参数数组，判断两个字符串是否满足特定条件。
     * 如果参数数组中包含 "c"，则检查第一个字符串是否包含第二个字符串。
     * 同时也会检查两个字符串是否完全相等。
     *
     * @param str1 第一个待比较的字符串
     * @param str2 第二个待比较的字符串
     * @param argument 用于控制比较逻辑的参数数组
     * @return 如果str1包含str2（当argument含有"c"时）或两者相等，则返回true；否则返回false。
     */
    public static boolean filters(String str1,String str2,String[] argument) {
        ArrayList<String> strArr = new ArrayList<>(Arrays.asList(argument));
        AtomicBoolean b = new AtomicBoolean(false);
        strArr.forEach(str -> {
            switch (str){
                case "c" -> b.set(str1.contains(str2));
            }
        });
        return b.get() || str1.equals(str2);
    }
    public static boolean equalsBlockName(String blockName, BlockState blockState){
        return equalsName(blockName,blockState);
    }
    public static boolean equalsItemName(String itemName, ItemStack itemStack){
        return equalsName(itemName,itemStack);
    }

    /**
     * 比较给定的名字（blockName）与对象o的名称是否相等。此方法支持比较方块状态（BlockState）和物品堆（ItemStack）。
     * 比较逻辑包括直接字符串比较、标签匹配以及中文和拼音的模糊匹配。
     *
     * @param blockName 待比较的名称，可以是逗号分隔的字符串，其中第一个元素是主要的比较依据，其余作为附加条件。
     * @param o 要与blockName进行比较的对象，必须是BlockState或ItemStack类型之一。
     * @return 如果根据定义的规则blockName与o的名称匹配，则返回true；否则返回false。
     */
    public static boolean equalsName(String blockName, Object o) {
        BlockState blockState = null;
        ItemStack itemStack = null;

        if(o instanceof BlockState ob){
            blockState = ob;
        }else if(o instanceof ItemStack oi){
            itemStack = oi;
        }else return false;

        String string = blockState != null ?
                Registries.BLOCK.getId(blockState.getBlock()).toString() : Registries.ITEM.getId(itemStack.getItem()).toString();
        String[] strs = blockName.split(",");
        String args = strs[0];
        if(strs.length > 1){
            strs = Arrays.copyOfRange(strs,1,strs.length);
        }else strs = new String[]{};

        try {
           return blockState !=null ? getTag(blockState.streamTags(),blockName,strs) : getTag(itemStack.streamTags(),blockName,strs);
        }catch (Exception ignored){}

        //中文 、 拼音
        String name = blockState != null ?  blockState.getBlock().getName().getString() : itemStack.getName().getString();
        ArrayList<String> pinYin = PinYinSearch.getPinYin(name);
        String[] finalStrs1 = strs;
        boolean py = pinYin.stream().anyMatch(p -> Filters.filters(p,args, finalStrs1));
        return Filters.filters(name,args,strs) || py || Filters.filters(string,args,strs);
    }

    private static<T> boolean getTag(Stream<TagKey<T>> t, String name, String[] tags) throws NoTag {
        //标签
        if (name.length() > 1 && name.charAt(0) == '#') {
            AtomicBoolean theLabelIsTheSame = new AtomicBoolean(false);
            String fix1 = name.split("#")[1];
            t.forEach(tag -> {
                String tagName = tag.id().toString();
                if (Filters.filters(tagName,fix1, tags)) {
                    theLabelIsTheSame.set(true);
                }
            });
            return theLabelIsTheSame.get();
        }else {
            throw new NoTag();
        }
    }
}
class NoTag extends Exception {

}