package com.lemon.erp.framework.data;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import net.sourceforge.pinyin4j.PinyinHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 主数据的关键字检索（下拉搜索、列表关键字）：
 * <ul>
 *     <li>编码、名称、规格等原字段包含关键字（原样 LIKE）；</li>
 *     <li>检索列 {@code search_key} 包含规范化后的关键字。检索列由各字段计算，空格分隔多段：
 *     去掉分隔符的小写串（「12/R60 34TH」→「12r6034th」）、只保留数字的串（→「126034」）、
 *     拼音首字母（「深圳智控」→「szzk」，字母数字原样保留）。关键字同样去掉分隔符、转小写后匹配，
 *     因此「126034」「12r60」「szzk」「深圳智控」都能找到对应记录。</li>
 * </ul>
 * 多音字：首字母取 pinyin4j 的第一个读音（少数常用字按 {@link #PREFERRED} 修正），
 * 含多音字时再生成一份把多音字换成第二个读音首字母的串（如「重庆」同时有 zq、cq），
 * 同一词中两个以上多音字需要不同读音组合的（如「重庆长城」读作 cqcc）匹配不到，可用汉字或编码检索。
 * <p>
 * 计算规则改变时，把相应表的 {@code search_key} 置为 NULL（升级脚本），启动时会按新规则回填。
 */
public final class SearchKeys {

    /** 检索列长度（VARCHAR(1000)），超出时截掉末尾的段 */
    public static final int MAX_LENGTH = 1000;

    /** pinyin4j 第一个读音不是最常用读音的字：字 → 常用读音首字母 */
    private static final Map<Character, Character> PREFERRED = Map.of(
            '厦', 'x', '长', 'c', '都', 'd', '曾', 'z', '朴', 'p', '藏', 'c', '会', 'h');

    private SearchKeys() {
    }

    /** 规范化：转小写，只保留字母、数字和汉字（去掉空格、斜杠、横线、括号等分隔符） */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        text.codePoints().filter(Character::isLetterOrDigit)
                .forEach(cp -> sb.appendCodePoint(Character.toLowerCase(cp)));
        return sb.toString();
    }

    /** 根据各字段计算检索列，没有内容时为空串（区别于未计算的 NULL） */
    public static String build(String... texts) {
        Set<String> parts = new LinkedHashSet<>();
        for (String text : texts) {
            String norm = normalize(text);
            if (norm.isEmpty()) {
                continue;
            }
            parts.add(norm);
            String digits = digits(norm);
            if (digits.length() >= 2) {
                parts.add(digits);
            }
            parts.addAll(initials(norm));
        }
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            int len = sb.isEmpty() ? part.length() : sb.length() + 1 + part.length();
            if (len > MAX_LENGTH) {
                break;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(part);
        }
        return sb.toString();
    }

    /** 实体的检索列 */
    public static String of(Searchable entity) {
        return build(entity.searchTexts());
    }

    /**
     * 关键字匹配条件，用在 {@code wrapper.and(StringUtils.hasText(keyword), SearchKeys.matches(...))} 中：
     * 任一原字段包含关键字，或检索列包含规范化后的关键字
     *
     * @param keyword   关键字（调用方判断非空白；条件不成立时不会执行返回的 Consumer）
     * @param searchKey 检索列
     * @param columns   原样匹配的字段（编码、名称、规格等）
     */
    @SafeVarargs
    public static <T> Consumer<LambdaQueryWrapper<T>> matches(String keyword, SFunction<T, ?> searchKey, SFunction<T, ?>... columns) {
        return w -> {
            String kw = keyword.trim();
            String norm = normalize(kw);
            boolean first = true;
            for (SFunction<T, ?> column : columns) {
                if (!first) {
                    w.or();
                }
                w.like(column, kw);
                first = false;
            }
            if (!norm.isEmpty()) {
                if (!first) {
                    w.or();
                }
                w.like(searchKey, norm);
            }
        };
    }

    /**
     * 把编码（或条码）与关键字完全一致的记录放在第一页的最前面：关键字为纯数字等时匹配的记录可能很多，
     * 按编码查找（导入、扫码、按编码带出）不能因为排在后面而找不到。不在本页时追加到最前（本页多一条，不挤掉原有的记录）
     *
     * @param exact 完全一致的记录，没有时为 null
     */
    public static <T> List<T> exactFirst(List<T> rows, T exact, Function<T, ?> id) {
        if (exact == null) {
            return rows;
        }
        Object key = id.apply(exact);
        List<T> result = new ArrayList<>(rows.size() + 1);
        result.add(exact);
        rows.stream().filter(r -> !Objects.equals(id.apply(r), key)).forEach(result::add);
        return result;
    }

    private static String digits(String norm) {
        StringBuilder sb = new StringBuilder();
        norm.chars().filter(c -> c >= '0' && c <= '9').forEach(c -> sb.append((char) c));
        return sb.length() == norm.length() ? "" : sb.toString();
    }

    /**
     * 拼音首字母：汉字取首字母，其他字符（已规范化为字母、数字）原样保留。没有汉字时返回空列表；
     * 含多音字时多返回一份取第二个读音首字母的串
     */
    static List<String> initials(String norm) {
        StringBuilder main = new StringBuilder();
        StringBuilder alt = new StringBuilder();
        boolean han = false;
        boolean poly = false;
        for (int i = 0; i < norm.length(); i++) {
            char c = norm.charAt(i);
            List<Character> letters = letters(c);
            if (letters.isEmpty()) {
                // 不是汉字（或字库中没有读音的生僻字）：字母数字原样保留，生僻字跳过
                if (c < 128) {
                    main.append(c);
                    alt.append(c);
                }
                continue;
            }
            han = true;
            main.append(letters.get(0));
            if (letters.size() > 1) {
                poly = true;
                alt.append(letters.get(1));
            } else {
                alt.append(letters.get(0));
            }
        }
        if (!han) {
            return List.of();
        }
        return poly ? List.of(main.toString(), alt.toString()) : List.of(main.toString());
    }

    /** 汉字各读音的首字母（去重，常用读音在前），不是汉字时为空 */
    private static List<Character> letters(char c) {
        if (c < 0x3400) {
            return List.of();
        }
        String[] readings = PinyinHelper.toHanyuPinyinStringArray(c);
        if (readings == null) {
            return List.of();
        }
        List<Character> letters = new ArrayList<>(2);
        Character preferred = PREFERRED.get(c);
        if (preferred != null) {
            letters.add(preferred);
        }
        for (String reading : readings) {
            if (reading == null || reading.isEmpty() || reading.startsWith("none")) {
                continue;
            }
            char first = reading.charAt(0);
            if (first >= 'a' && first <= 'z' && !letters.contains(first)) {
                letters.add(first);
            }
        }
        return letters;
    }
}
