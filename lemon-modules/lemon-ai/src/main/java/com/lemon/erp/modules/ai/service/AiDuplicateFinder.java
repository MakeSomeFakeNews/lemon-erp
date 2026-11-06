package com.lemon.erp.modules.ai.service;

import com.lemon.erp.framework.ai.AiCleanseRecord;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据整理的疑似重复候选分组（纯计算，在 Java 中完成，不把全部数据发给模型）：
 * <ol>
 *   <li>规范化：全角转半角（NFKC）、忽略大小写、去掉空格和括号等符号、乘号写法（× * ✕）统一为 x、常见单位中英文统一（毫米 → mm）、
 *       去掉公司名称后缀（有限公司、Co., Ltd.）</li>
 *   <li>名称和规格拆成词（连续的汉字、连续的字母数字）后排序拼接，名称和规格的拆分方式、词序不同也视为相同</li>
 *   <li>规范化后完全相同的归为一组；不同的要求其中的数字完全相同（M6x20 与 M6x25 不算），去掉数字后按字符二元组的 Dice 相似度 ≥ {@link #SIMILARITY} 归为一组；
 *       税号（extra.taxNo）相同的归为一组</li>
 * </ol>
 * 只是候选，是否真的重复由模型逐组判断。
 */
public final class AiDuplicateFinder {

    /** 相似度阈值 */
    public static final double SIMILARITY = 0.8;
    /** 一组最多的记录数（再多的多半是通用名称，只取前面的） */
    public static final int MAX_GROUP_SIZE = 20;
    /** 出现在太多记录中的二元组不用于找相似候选（避免两两比较） */
    private static final int COMMON_GRAM_LIMIT = 300;

    private static final Pattern MULTIPLY = Pattern.compile("[×✕✖*]");
    private static final Pattern DOT_NOT_DECIMAL = Pattern.compile("(?<!\\d)\\.|\\.(?!\\d)");
    private static final Pattern NOT_WORD = Pattern.compile("[^\\p{L}\\p{N}.]+");
    private static final Pattern TOKEN = Pattern.compile("\\p{IsHan}+|[\\p{L}\\p{N}.&&[^\\p{IsHan}]]+");
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final Pattern COMPANY_SUFFIX = Pattern.compile("(有限责任公司|股份有限公司|有限公司|公司|coltd|ltd)$"); // i18n-ignore 匹配中文公司名称后缀
    /** 单位的中文写法 → 英文（长的在前） */
    private static final String[][] UNITS = {{"毫米", "mm"}, {"厘米", "cm"}, {"公斤", "kg"}, {"千克", "kg"}, {"毫升", "ml"}}; // i18n-ignore 匹配中文单位

    private AiDuplicateFinder() {
    }

    /** 一组疑似重复的记录（按 ID 排序） */
    public record Group(List<AiCleanseRecord> records) {
    }

    /**
     * 规范化一段文字（用于比较）：NFKC、小写、单位与乘号统一、去掉空格和符号、去掉公司名称后缀
     */
    public static String normalize(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String t = Normalizer.normalize(s, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        for (String[] u : UNITS) {
            t = t.replace(u[0], u[1]);
        }
        t = MULTIPLY.matcher(t).replaceAll("x");
        t = NOT_WORD.matcher(t).replaceAll("");
        t = DOT_NOT_DECIMAL.matcher(t).replaceAll("");
        String stripped = COMPANY_SUFFIX.matcher(t).replaceFirst("");
        return stripped.isEmpty() ? t : stripped;
    }

    /** 比较用的键：名称、规格规范化后拆成词，排序后以空格拼接 */
    public static String canonical(AiCleanseRecord r) {
        List<String> tokens = new ArrayList<>();
        addTokens(tokens, normalize(r.name()));
        addTokens(tokens, normalize(r.spec()));
        tokens.sort(Comparator.naturalOrder());
        return String.join(" ", tokens);
    }

    private static void addTokens(List<String> out, String s) {
        Matcher m = TOKEN.matcher(s);
        while (m.find()) {
            out.add(m.group());
        }
    }

    /** 文字中的数字（排序后），相似候选要求数字完全相同 */
    static List<String> numbers(String canonical) {
        List<String> list = new ArrayList<>();
        Matcher m = NUMBER.matcher(canonical);
        while (m.find()) {
            list.add(m.group());
        }
        list.sort(Comparator.naturalOrder());
        return list;
    }

    /** 字符二元组（只有一个字时为该字） */
    static Set<String> grams(String s) {
        Set<String> set = new HashSet<>();
        if (s.length() == 1) {
            set.add(s);
        }
        for (int i = 0; i + 1 < s.length(); i++) {
            set.add(s.substring(i, i + 2));
        }
        return set;
    }

    /** Dice 相似度 */
    static double dice(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int common = 0;
        for (String g : a) {
            if (b.contains(g)) {
                common++;
            }
        }
        return 2.0 * common / (a.size() + b.size());
    }

    /**
     * 找出疑似重复的候选组
     *
     * @param records 候选记录
     * @return 至少 2 条记录的组（每组最多 {@link #MAX_GROUP_SIZE} 条），按组内最小 ID 排序
     */
    public static List<Group> find(List<AiCleanseRecord> records) {
        int n = records.size();
        String[] keys = new String[n];
        List<Set<String>> grams = new ArrayList<>(n);
        List<List<String>> nums = new ArrayList<>(n);
        UnionFind uf = new UnionFind(n);
        Map<String, Integer> exact = new HashMap<>();
        Map<String, Integer> taxNos = new HashMap<>();
        for (int i = 0; i < n; i++) {
            AiCleanseRecord r = records.get(i);
            keys[i] = canonical(r);
            grams.add(grams(NUMBER.matcher(keys[i]).replaceAll("").strip()));
            nums.add(numbers(keys[i]));
            if (!keys[i].isEmpty()) {
                Integer first = exact.putIfAbsent(keys[i], i);
                if (first != null) {
                    uf.union(first, i);
                }
            }
            Object tax = r.extra().get(AiCleanseRecord.TAX_NO);
            String taxNo = tax == null ? "" : normalize(tax.toString());
            if (!taxNo.isEmpty()) {
                Integer first = taxNos.putIfAbsent(taxNo, i);
                if (first != null) {
                    uf.union(first, i);
                }
            }
        }
        // 相似：按二元组建倒排索引，只比较有共同（不太常见的）二元组的记录
        Map<String, List<Integer>> index = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (exact.get(keys[i]) == null || exact.get(keys[i]) != i) {
                continue; // 完全相同的只取第一条参与比较
            }
            for (String g : grams.get(i)) {
                index.computeIfAbsent(g, k -> new ArrayList<>()).add(i);
            }
        }
        for (int i = 0; i < n; i++) {
            if (keys[i].isEmpty() || exact.get(keys[i]) != i) {
                continue;
            }
            Set<Integer> seen = new HashSet<>();
            for (String g : grams.get(i)) {
                List<Integer> postings = index.get(g);
                if (postings == null || postings.size() > COMMON_GRAM_LIMIT) {
                    continue;
                }
                for (int j : postings) {
                    if (j <= i || !seen.add(j) || uf.find(i) == uf.find(j)) {
                        continue;
                    }
                    if (nums.get(i).equals(nums.get(j)) && dice(grams.get(i), grams.get(j)) >= SIMILARITY) {
                        uf.union(i, j);
                    }
                }
            }
        }
        Map<Integer, List<AiCleanseRecord>> groups = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            groups.computeIfAbsent(uf.find(i), k -> new ArrayList<>()).add(records.get(i));
        }
        return groups.values().stream()
                .filter(g -> g.size() > 1)
                .map(g -> g.stream().sorted(Comparator.comparing(AiCleanseRecord::id)).limit(MAX_GROUP_SIZE).toList())
                .sorted(Comparator.comparing(g -> g.get(0).id()))
                .map(Group::new)
                .toList();
    }

    private static final class UnionFind {
        private final int[] parent;

        UnionFind(int n) {
            parent = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }

        int find(int x) {
            while (parent[x] != x) {
                parent[x] = parent[parent[x]];
                x = parent[x];
            }
            return x;
        }

        void union(int a, int b) {
            int ra = find(a);
            int rb = find(b);
            if (ra != rb) {
                parent[Math.max(ra, rb)] = Math.min(ra, rb);
            }
        }
    }
}
