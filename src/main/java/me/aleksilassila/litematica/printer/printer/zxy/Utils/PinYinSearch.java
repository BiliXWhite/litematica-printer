package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public class PinYinSearch {
    public static void main(String[] args){
        System.out.println((char) 122);
        char c = 123;
//        System.out.println(getPinYin("曾0长0"));
        System.out.println(getPinYin(c+"曾"));
        try {
            System.out.println(Arrays.toString(PinyinHelper.toHanyuPinyinStringArray((char) 123)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    static ArrayList<String[]> pinyin = new ArrayList<>();
    public static ArrayList<String> getPinYin(String str){

        char[] ch = str.toCharArray();
        HanyuPinyinOutputFormat gs = new HanyuPinyinOutputFormat();
        gs.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        gs.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        gs.setVCharType(HanyuPinyinVCharType.WITH_V);

        try {
            pinyin = new ArrayList<>();
            for (char c : ch) {
                if(c<=123)pinyin.add(new String[]{""+c});
                else pinyin.add(PinyinHelper.toHanyuPinyinStringArray(c, gs));
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            throw new RuntimeException(e);
        }
        return getStrings();
    }
    public static boolean hasPinYin(String zh,String py){
        return getPinYin(zh).stream().anyMatch(s->s.contains(py));
    }

    @NotNull
    private static ArrayList<String> getStrings() {
        ArrayList<String> pys1 = new ArrayList<>();
        ArrayList<String> pys2;

        ArrayList<String> pys3 = new ArrayList<>();
        ArrayList<String> pys4;

        for (int i = 0; i < pinyin.size(); i++) {
            pys2 = new ArrayList<>();
            pys4 = new ArrayList<>();
            for (int i1 = 0; i1 < pinyin.get(i).length && pinyin.get(i).length > 0; i1++) {
                if(i==0) {
                    pys1.add(pinyin.get(i)[i1]);
                    pys3.add(""+ pinyin.get(i)[i1].charAt(0));
                }else {
                    for (int i2 = 0; i2 < pys1.size(); i2++) {
                        pys2.add(pys1.get(i2) + pinyin.get(i)[i1]);
                        pys4.add(pys3.get(i2) + pinyin.get(i)[i1].charAt(0));
                    }
                }
            }
            if(i!=0) {
                pys1 = pys2;
                pys3 = pys4;
            }
        }
        pys1.addAll(pys3);
//        System.out.println(pys1);
        return pys1;
    }
}
