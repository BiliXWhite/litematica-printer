package me.aleksilassila.litematica.printer.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼音搜索工具类：将中文字符串转换为全拼/简拼组合，支持拼音包含性判断
 * 无状态设计，线程安全
 */
public class PinYinSearchUtils {
    // 复用拼音格式配置（常量），避免重复创建
    private static final HanyuPinyinOutputFormat PINYIN_FORMAT;

    static {
        // 静态初始化：配置拼音输出格式（小写、无声调、v代替ü）
        PINYIN_FORMAT = new HanyuPinyinOutputFormat();
        PINYIN_FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        PINYIN_FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        PINYIN_FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    /**
     * 将字符串转换为所有可能的拼音组合（全拼 + 简拼）
     * @param str 输入字符串（支持中文、英文、数字等混合）
     * @return 拼音组合列表（非null）
     */
    public static synchronized ArrayList<String> getPinYin(@Nullable String str) {
        // 空值处理：输入null/空字符串返回空列表
        if (str == null || str.isEmpty()) {
            return new ArrayList<>();
        }

        char[] chars = str.toCharArray();
        // 改用方法内局部变量存储每个字符的拼音数组，移除静态变量
        List<String[]> charPinyinList = new ArrayList<>();

        try {
            for (char c : chars) {
                if (c < 128) {
                    // 非中文字符：直接保留原字符
                    charPinyinList.add(new String[]{String.valueOf(c)});
                } else {
                    // 中文字符：获取所有读音，处理null情况
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, PINYIN_FORMAT);
                    if (pinyinArray == null || pinyinArray.length == 0) {
                        // 生僻字/无法识别的汉字：保留原字符
                        charPinyinList.add(new String[]{String.valueOf(c)});
                    } else {
                        charPinyinList.add(pinyinArray);
                    }
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            // 友好的异常提示，携带上下文信息
            throw new RuntimeException("拼音格式配置错误，无法转换字符串：" + str, e);
        }

        // 生成全拼+简拼组合
        return generatePinyinCombinations(charPinyinList);
    }

    /**
     * 判断中文字符串是否包含指定拼音片段
     * @param zh 中文字符串
     * @param py 拼音片段（大小写不敏感）
     * @return true=包含，false=不包含
     */
    public static boolean hasPinYin(@Nullable String zh, @Nullable String py) {
        // 空值快速返回
        if (zh == null || zh.isEmpty() || py == null || py.isEmpty()) {
            return false;
        }
        String lowerPy = py.toLowerCase();
        return getPinYin(zh).stream().anyMatch(s -> s.contains(lowerPy));
    }

    /**
     * 生成拼音组合（全拼 + 简拼）
     * @param charPinyinList 每个字符的拼音数组列表
     * @return 全拼+简拼组合列表
     */
    @NotNull
    private static ArrayList<String> generatePinyinCombinations(List<String[]> charPinyinList) {
        ArrayList<String> fullPinyinList = new ArrayList<>();   // 全拼组合列表
        ArrayList<String> shortPinyinList = new ArrayList<>();  // 简拼组合列表
        // 遍历每个字符的拼音数组，生成组合
        for (int i = 0; i < charPinyinList.size(); i++) {
            String[] currentPinyinArray = charPinyinList.get(i);
            // 临时存储本轮生成的全拼/简拼
            ArrayList<String> tempFullList = new ArrayList<>();
            ArrayList<String> tempShortList = new ArrayList<>();
            for (String pinyin : currentPinyinArray) {
                if (i == 0) {
                    // 第一个字符：直接添加
                    tempFullList.add(pinyin);
                    tempShortList.add(String.valueOf(pinyin.charAt(0)));
                } else {
                    // 非第一个字符：和已有的组合拼接
                    for (String existingFull : fullPinyinList) {
                        tempFullList.add(existingFull + pinyin);
                    }
                    for (String existingShort : shortPinyinList) {
                        tempShortList.add(existingShort + pinyin.charAt(0));
                    }
                }
            }
            // 更新全拼/简拼列表
            if (i == 0) {
                fullPinyinList = tempFullList;
                shortPinyinList = tempShortList;
            } else {
                fullPinyinList = tempFullList;
                shortPinyinList = tempShortList;
            }
        }
        // 合并全拼和简拼
        fullPinyinList.addAll(shortPinyinList);
        return fullPinyinList;
    }
}