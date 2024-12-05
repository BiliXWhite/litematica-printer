package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class TextFilters {
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
}
