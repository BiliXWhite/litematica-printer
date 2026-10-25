package me.aleksilassila.litematica.printer.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class MatchUtils {
    private static final char TAG_PREFIX = '#';
    private static final String ENTRY_SEPARATOR = ",";
    private static final String CONTAINS_FLAG = "c";
    private static final char STATE_START = '[';
    private static final char STATE_END = ']';
    private static final char STATE_ASSIGN = '=';

    private static final Map<String, Entry> ENTRY_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> PINYIN_CACHE = new ConcurrentHashMap<>();
    private static final HanyuPinyinOutputFormat PINYIN_FORMAT = new HanyuPinyinOutputFormat();

    static {
        PINYIN_FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        PINYIN_FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        PINYIN_FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    private MatchUtils() {
    }

    public static boolean matches(String rule, Object target) {
        if (rule == null || target == null) {
            return false;
        }

        Entry entry = ENTRY_CACHE.computeIfAbsent(rule, MatchUtils::parseEntry);

        String registryName = getRegistryName(target);
        if (registryName == null) {
            return false;
        }

        if (entry.tag()) {
            if (target instanceof BlockState blockState) {
                //#if MC >= 260100
                //$$ return matchesTag(blockState.tags(), entry) && entry.stateMatches(target);
                //#else
                return matchesTag(blockState.getTags(), entry) && entry.stateMatches(target);
                //#endif
            }
            return target instanceof ItemStack itemStack
                    && matchesTag(itemStack.getTags(), entry)
                    && entry.stateMatches(target);
        }

        if (!entry.stateMatches(target)) {
            return false;
        }
        if (matchesRegistry(entry, registryName)) {
            return true;
        }

        String displayName = getDisplayName(target);
        if (displayName == null) {
            return false;
        }
        if (entry.matches(displayName)) {
            return true;
        }

        // 纯英文名称的拼音结果就是原名，上面已经比较过了
        return entry.isAscii()
                && !isAscii(displayName)
                && getPinyin(displayName).stream().anyMatch(entry::matches);
    }

    public static List<String> getPinyin(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        return PINYIN_CACHE.computeIfAbsent(text, MatchUtils::createPinyin);
    }

    private static List<String> createPinyin(String text) {
        List<String[]> charPinyinList = new ArrayList<>();
        try {
            for (char c : text.toCharArray()) {
                String[] readings = c < 128 ? null : PinyinHelper.toHanyuPinyinStringArray(c, PINYIN_FORMAT);
                charPinyinList.add(readings == null || readings.length == 0
                        ? new String[]{String.valueOf(c)}
                        : readings);
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            return List.of(text);
        }
        return List.copyOf(generatePinyinCombinations(charPinyinList));
    }

    private static List<String> generatePinyinCombinations(List<String[]> charPinyinList) {
        List<String> fullPinyin = new ArrayList<>();
        List<String> shortPinyin = new ArrayList<>();

        for (int i = 0; i < charPinyinList.size(); i++) {
            List<String> nextFull = new ArrayList<>();
            List<String> nextShort = new ArrayList<>();
            for (String reading : charPinyinList.get(i)) {
                if (i == 0) {
                    nextFull.add(reading);
                    nextShort.add(String.valueOf(reading.charAt(0)));
                    continue;
                }
                for (String existing : fullPinyin) {
                    nextFull.add(existing + reading);
                }
                for (String existing : shortPinyin) {
                    nextShort.add(existing + reading.charAt(0));
                }
            }
            fullPinyin = nextFull;
            shortPinyin = nextShort;
        }

        // 同时生成全拼（xiaomai）和简拼（xm）
        fullPinyin.addAll(shortPinyin);
        return fullPinyin;
    }

    private static Entry parseEntry(String rule) {
        String[] parts = splitEntry(rule);
        String head = parts[0].trim();

        boolean contains = false;
        for (int i = 1; i < parts.length; i++) {
            if (CONTAINS_FLAG.equals(parts[i].trim())) {
                contains = true;
                break;
            }
        }

        String name = head;
        Map<String, String> properties = Map.of();
        int stateStart = head.indexOf(STATE_START);
        if (stateStart > 0 && head.charAt(head.length() - 1) == STATE_END) {
            Map<String, String> parsed = parseStateProperties(head.substring(stateStart + 1, head.length() - 1));
            if (parsed != null) {
                name = head.substring(0, stateStart).trim();
                properties = parsed;
            }
        }

        boolean tag = name.startsWith(String.valueOf(TAG_PREFIX));
        if (tag) {
            name = name.substring(1);
        }
        return new Entry(name, name.toLowerCase(Locale.ROOT), contains, properties, tag);
    }

    private static String[] splitEntry(String rule) {
        List<String> parts = new ArrayList<>();
        int bracketDepth = 0;
        int start = 0;

        for (int i = 0; i < rule.length(); i++) {
            char c = rule.charAt(i);
            if (c == STATE_START) {
                bracketDepth++;
            } else if (c == STATE_END && bracketDepth > 0) {
                bracketDepth--;
            } else if (c == ENTRY_SEPARATOR.charAt(0) && bracketDepth == 0) {
                // 方括号里的逗号属于方块状态，不能当成分隔符
                parts.add(rule.substring(start, i));
                start = i + 1;
            }
        }

        parts.add(rule.substring(start));
        return parts.toArray(new String[0]);
    }

    private static @Nullable Map<String, String> parseStateProperties(String text) {
        if (text.isBlank()) {
            return null;
        }

        Map<String, String> properties = new LinkedHashMap<>();
        for (String pair : text.split(ENTRY_SEPARATOR, -1)) {
            int assignment = pair.indexOf(STATE_ASSIGN);
            if (assignment <= 0) {
                return null;
            }
            String key = pair.substring(0, assignment).trim();
            String value = pair.substring(assignment + 1).trim();
            if (key.isEmpty() || value.isEmpty()) {
                return null;
            }
            properties.put(key, value);
        }
        return Map.copyOf(properties);
    }

    private static boolean matchesRegistry(Entry entry, String registryName) {
        if (entry.matchesLower(registryName)) {
            return true;
        }
        int namespaceEnd = registryName.indexOf(':');
        // 允许省略命名空间，obsidian 也能匹配 minecraft:obsidian
        return namespaceEnd >= 0 && entry.matchesLower(registryName.substring(namespaceEnd + 1));
    }

    private static <T> boolean matchesTag(Stream<TagKey<T>> tags, Entry entry) {
        return tags.map(tag -> tag.location().toString()).anyMatch(tag -> matchesRegistry(entry, tag));
    }

    private static boolean matchesProperty(BlockState blockState, String propertyName, String expectedValue) {
        for (Property<?> property : blockState.getProperties()) {
            if (property.getName().equalsIgnoreCase(propertyName)) {
                return matchesPropertyValue(blockState, property, expectedValue);
            }
        }
        return false;
    }

    private static <T extends Comparable<T>> boolean matchesPropertyValue(
            BlockState blockState, Property<T> property, String expectedValue) {
        // getName 返回 north、top 这类序列化名，而不是枚举常量名
        return property.getName(blockState.getValue(property)).equalsIgnoreCase(expectedValue);
    }

    private static String getRegistryName(Object target) {
        if (target instanceof BlockState blockState) {
            return BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
        }
        if (target instanceof ItemStack itemStack) {
            return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        }
        return null;
    }

    private static String getDisplayName(Object target) {
        if (target instanceof BlockState blockState) {
            return blockState.getBlock().getName().getString();
        }
        if (target instanceof ItemStack itemStack) {
            return itemStack.getHoverName().getString();
        }
        return null;
    }

    private static boolean isAscii(String text) {
        return text.chars().allMatch(c -> c < 128);
    }

    private record Entry(String name, String lowerName, boolean contains, Map<String, String> stateProperties, boolean tag) {
        private boolean matches(String candidate) {
            return !name.isEmpty() && (candidate.equals(name) || (contains && candidate.contains(name)));
        }

        private boolean matchesLower(String candidate) {
            return !lowerName.isEmpty() && (candidate.equals(lowerName) || (contains && candidate.contains(lowerName)));
        }

        private boolean stateMatches(Object target) {
            if (stateProperties.isEmpty()) {
                return true;
            }
            if (!(target instanceof BlockState blockState)) {
                return false;
            }
            for (Map.Entry<String, String> property : stateProperties.entrySet()) {
                if (!matchesProperty(blockState, property.getKey(), property.getValue())) {
                    return false;
                }
            }
            return true;
        }

        private boolean isAscii() {
            return MatchUtils.isAscii(name);
        }
    }
}
