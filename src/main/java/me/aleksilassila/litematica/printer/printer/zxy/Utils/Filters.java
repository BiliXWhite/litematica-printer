package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class Filters {
    //包含/比较
    public static boolean filters(String str1,String str2,String[] argument){
        ArrayList<String> strArr = new ArrayList<>(Arrays.asList(argument));
        AtomicBoolean b = new AtomicBoolean(false);
        strArr.forEach(str -> {
            switch (str){
                case "c" -> {
                    b.set(str1.contains(str2));
                }
            }
        });
        return b.get() || str1.equals(str2);
    }
    public static boolean equalsBlockName(String blockName, BlockState blockState, BlockPos pos){
        return equalsName(blockName,blockState);
    }
    public static boolean equalsItemName(String itemName, ItemStack itemStack){
        return equalsName(itemName,itemStack);
    }

    public static boolean equalsName(String blockName, Object o){
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