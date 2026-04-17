package me.aleksilassila.litematica.printer.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class PinYinSearchUtils {
    private static final HanyuPinyinOutputFormat PINYIN_FORMAT;
    private static final char TAG_PREFIX = '#';
    private static final String SPLIT_SEPARATOR = ",";
    private static final String CONTAINS_FLAG = "c";

    static {
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
            fullPinyinList = tempFullList;
            shortPinyinList = tempShortList;
        }
        // 合并全拼和简拼
        fullPinyinList.addAll(shortPinyinList);
        return fullPinyinList;
    }

    /**
     * 字符串匹配规则：支持"包含"（c参数）或"精确相等"
     * @param targetStr 目标字符串（待匹配的字符串）
     * @param matchStr  匹配字符串（要检查的内容）
     * @param matchRules 匹配规则参数（包含"c"则启用包含匹配）
     * @return true=匹配成功，false=匹配失败
     */
    public static boolean matchString(String targetStr, String matchStr, String[] matchRules) {
        // 空值防护：任意字符串为null则直接不匹配
        if (targetStr == null || matchStr == null) {
            return false;
        }
        // 规则1：是否启用"包含"匹配（只要有一个"c"参数就启用）
        boolean enableContainsMatch = Arrays.asList(matchRules).contains(CONTAINS_FLAG);
        boolean containsMatchResult = enableContainsMatch && targetStr.contains(matchStr);
        // 规则2：精确相等匹配
        boolean exactMatchResult = targetStr.equals(matchStr);
        // 满足任一规则即匹配成功
        return containsMatchResult || exactMatchResult;
    }

    /**
     * 匹配方块名称（封装matchName，提升语义）
     */
    public static boolean matchBlockName(String expectedName, BlockState blockState) {
        return matchName(expectedName, blockState);
    }

    /**
     * 匹配物品名称（封装matchName，提升语义）
     */
    public static boolean matchItemName(String expectedName, ItemStack itemStack) {
        return matchName(expectedName, itemStack);
    }

    /**
     * 核心匹配逻辑：支持方块/物品的名称、标签、中文/拼音模糊匹配
     * @param expectedName 期望的名称（支持#开头的标签，逗号分隔规则参数）
     * @param targetObj    目标对象（仅支持BlockState/ItemStack）
     * @return true=匹配成功，false=匹配失败
     */
    public static boolean matchName(String expectedName, Object targetObj) {
        // 空值防护
        if (expectedName == null || targetObj == null) {
            return false;
        }

        // 解析期望名称和匹配规则（逗号分隔，第一个是名称，后续是规则）
        String[] nameAndRules = expectedName.split(SPLIT_SEPARATOR, -1); // -1保留空字符串，避免拆分异常
        String coreName = nameAndRules[0];
        String[] matchRules = nameAndRules.length > 1
                ? Arrays.copyOfRange(nameAndRules, 1, nameAndRules.length)
                : new String[0];

        // 获取目标对象的核心标识（注册表名称）
        String targetRegistryName = getTargetRegistryName(targetObj);
        if (targetRegistryName == null) {
            return false;
        }

        // 优先匹配标签（如果期望名称以#开头）
        if (coreName.startsWith(String.valueOf(TAG_PREFIX))) {
            String tagName = coreName.substring(1);
            // 拆分处理Block和Item的标签，避免泛型转换错误
            if (targetObj instanceof BlockState blockState) {
                return matchBlockTag(blockState, tagName, matchRules);
            } else if (targetObj instanceof ItemStack itemStack) {
                return matchItemTag(itemStack, tagName, matchRules);
            }
            return false;
        }

        // 匹配中文名称、拼音、注册表名称
        String targetDisplayName = getTargetDisplayName(targetObj);
        if (targetDisplayName == null) {
            return false;
        }

        // 中文名称匹配
        boolean displayNameMatch = matchString(targetDisplayName, coreName, matchRules);
        // 拼音匹配
        boolean pinyinMatch = getPinYin(targetDisplayName)
                .stream()
                .anyMatch(pinyin -> matchString(pinyin, coreName, matchRules));
        // 注册表名称匹配
        boolean registryNameMatch = matchString(targetRegistryName, coreName, matchRules);

        // 任一匹配成功即返回 true
        return displayNameMatch || pinyinMatch || registryNameMatch;
    }

    /**
     * 获取目标对象（BlockState/ItemStack）的注册表名称
     */
    private static String getTargetRegistryName(Object targetObj) {
        if (targetObj instanceof BlockState blockState) {
            return BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
        } else if (targetObj instanceof ItemStack itemStack) {
            return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        }
        return null;
    }

    /**
     * 获取目标对象（BlockState/ItemStack）的显示名称（中文名称）
     */
    private static String getTargetDisplayName(Object targetObj) {
        if (targetObj instanceof BlockState blockState) {
            return blockState.getBlock().getName().getString();
        } else if (targetObj instanceof ItemStack itemStack) {
            return itemStack.getHoverName().getString();
        }
        return null;
    }

    /**
     * 匹配方块的标签（类型安全，无泛型转换）
     */
    private static boolean matchBlockTag(BlockState blockState, String tagName, String[] matchRules) {
        if (tagName.isEmpty()) {
            return false;
        }
        // 直接处理Block类型的TagKey流，无类型转换
        //#if MC >= 260100
        //$$ Stream<TagKey<Block>> blockTagStream = blockState.tags();
        //#else
        Stream<TagKey<Block>> blockTagStream = blockState.getTags();
        //#endif
        return blockTagStream
                .map(tag -> tag.location().toString())
                .anyMatch(tagFullName -> matchString(tagFullName, tagName, matchRules));
    }

    /**
     * 匹配物品的标签（类型安全，无泛型转换）
     */
    private static boolean matchItemTag(ItemStack itemStack, String tagName, String[] matchRules) {
        if (tagName.isEmpty()) {
            return false;
        }
        // 直接处理Item类型的TagKey流，无类型转换
        Stream<TagKey<Item>> itemTagStream = itemStack.getTags();
        return itemTagStream
                .map(tag -> tag.location().toString())
                .anyMatch(tagFullName -> matchString(tagFullName, tagName, matchRules));
    }
}