package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PinYinSearch {
    public static void main(String[] args){
        System.out.println(getPinYin("曾长"));
    }

    //[cengzhang, zengzhang, cengchang, zengchang, cz, zz, cc, zc]
    static ArrayList<String[]> pinyin = new ArrayList<>();//存储拼音，String[]数组的方式存储包含了各个读音 {String[zeng,ceng],String[chang,zhang]}
    public static synchronized ArrayList<String> getPinYin(String str){

        char[] ch = str.toCharArray();
        HanyuPinyinOutputFormat gs = new HanyuPinyinOutputFormat();
        gs.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        gs.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        gs.setVCharType(HanyuPinyinVCharType.WITH_V);

        try {
            pinyin = new ArrayList<>();
            for (char c : ch) {
                
                if(c<128)pinyin.add(new String[]{""+c});
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
        //存储全拼
        ArrayList<String> pys1 = new ArrayList<>();
        ArrayList<String> pys2;
        //简拼
        ArrayList<String> pys3 = new ArrayList<>();
        ArrayList<String> pys4;

        for (int pyss = 0; pyss < pinyin.size(); pyss++) {
            //pyss.get(0) == String[ceng,zeng]
            //pyss.get(1) == String[zhang,chang]
            pys2 = new ArrayList<>();
            pys4 = new ArrayList<>();
            //逻辑类似于小学题目中的握手
            //循环String[]中的各读音
            /*
            * list          list
            * ceng     ->   cengzhang
            * zeng          zengzhang
            *
            *               cengchang
            *               cengchang
            * */
            for (int stringS = 0; stringS < pinyin.get(pyss).length && pinyin.get(pyss).length > 0; stringS++) {
                //记录第一个字的各种读音
                //pys1 = ceng -> ceng+zhang
                if(pyss==0) {
                    //ceng / zeng
                    pys1.add(pinyin.get(pyss)[stringS]);
                    pys3.add(""+ pinyin.get(pyss)[stringS].charAt(0));
                }else {
                    for (int currentString = 0; currentString < pys1.size(); currentString++) {
                        pys2.add(pys1.get(currentString) + pinyin.get(pyss)[stringS]);
                        pys4.add(pys3.get(currentString) + pinyin.get(pyss)[stringS].charAt(0));
                    }
                }
            }
            if(pyss!=0) {
                pys1 = pys2;
                pys3 = pys4;
            }
        }
        pys1.addAll(pys3);
//        System.out.println(pys1);
        return pys1;
    }
}
