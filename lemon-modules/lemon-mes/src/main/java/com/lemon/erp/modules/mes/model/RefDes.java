package com.lemon.erp.modules.mes.model;

import com.lemon.erp.common.exception.BizException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BOM 子件位号（元器件在电路板上的标识，如 R1、C12、U3）。
 * 多个位号用逗号、空格、分号分隔，连续的可写成区间 C1-C8（前缀相同、编号递增）；保存时统一为大写、逗号分隔，区间保持原样
 */
public final class RefDes {

    /** 单个位号或区间：字母前缀 + 数字，区间的结束可以省略前缀（C1-8） */
    private static final Pattern TOKEN = Pattern.compile("^([A-Z]+)(\\d+)(?:-([A-Z]*)(\\d+))?$");
    private static final String SEPARATORS = "[,，;；、\\s]+";
    /** 单个区间最多展开的位号数 */
    private static final int MAX_RANGE = 5000;

    private RefDes() {
    }

    /** 规范化：去空白、转大写、逗号分隔；为空返回 null */
    public static String normalize(String text) {
        List<String> tokens = tokens(text);
        tokens.forEach(RefDes::expand);
        return tokens.isEmpty() ? null : String.join(",", tokens);
    }

    /** 展开为单个位号（区间逐个列出） */
    public static List<String> expandAll(String text) {
        List<String> all = new ArrayList<>();
        tokens(text).forEach(t -> all.addAll(expand(t)));
        return all;
    }

    /**
     * 校验一个 BOM 中所有子件的位号不重复
     *
     * @param refDesList 各子件的位号文本
     */
    public static void checkUnique(List<String> refDesList) {
        Set<String> seen = new LinkedHashSet<>();
        for (String text : refDesList) {
            for (String ref : expandAll(text)) {
                if (!seen.add(ref)) {
                    throw new BizException("位号 {0} 重复", ref);
                }
            }
        }
    }

    private static List<String> tokens(String text) {
        List<String> list = new ArrayList<>();
        if (text == null) {
            return list;
        }
        for (String t : text.trim().toUpperCase(Locale.ROOT).split(SEPARATORS)) {
            if (!t.isEmpty()) {
                list.add(t.replace('－', '-').replace('~', '-').replace('～', '-'));
            }
        }
        return list;
    }

    private static List<String> expand(String token) {
        Matcher m = TOKEN.matcher(token);
        if (!m.matches()) {
            throw new BizException("位号「{0}」格式不正确，应为字母加数字，如 R1，连续的可写成 R1-R8", token);
        }
        if (m.group(4) == null) {
            return List.of(token);
        }
        String prefix = m.group(1);
        if (!m.group(3).isEmpty() && !m.group(3).equals(prefix)) {
            throw new BizException("位号区间「{0}」的前缀不一致", token);
        }
        long start = Long.parseLong(m.group(2));
        long end = Long.parseLong(m.group(4));
        if (end < start) {
            throw new BizException("位号区间「{0}」的结束编号小于起始编号", token);
        }
        if (end - start + 1 > MAX_RANGE) {
            throw new BizException("位号区间「{0}」超过 {1} 个", token, MAX_RANGE);
        }
        List<String> list = new ArrayList<>();
        for (long i = start; i <= end; i++) {
            list.add(prefix + i);
        }
        return list;
    }
}
