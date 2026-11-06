package com.lemon.erp.framework.ai;

import com.lemon.erp.common.i18n.Messages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能录入 {@link AiExtractTarget#prefill} 的辅助类：读取模型输出、按名称匹配主数据、数值和日期的合理性检查，
 * 收集表单数据和 {@link AiPrefill.Warning}（提示文字为 framework 资源包中的 {@code framework.ai.*} 消息，按当前语言生成）。
 * <p>
 * 只做转换和查询，不写库。名称匹配的顺序：编码 / 名称精确匹配 → 名称模糊搜索 → 去掉「有限公司」等后缀后再模糊搜索；
 * 模糊搜索只有一个结果时采用并提示确认，有多个或没有时不填 ID、保留原文并提示。
 *
 * <pre>{@code
 * AiPrefillBuilder b = new AiPrefillBuilder();
 * ScmSupplier supplier = b.match("partnerId", "partnerName", AiPrefillBuilder.str(extracted, "supplierName"),
 *         Messages.get("scm.label.supplier"), exact, search, s -> s.getCode() + " " + s.getName());
 * b.put("partnerId", supplier == null ? null : supplier.getId());
 * b.put("orderDate", b.date("orderDate", AiPrefillBuilder.str(extracted, "orderDate")));
 * return b.build();
 * }</pre>
 */
public final class AiPrefillBuilder {

    /** 数量上限：超过时提示核对（多半是把单号、电话识别成了数量） */
    private static final BigDecimal QTY_LIMIT = new BigDecimal("100000000");
    /** 金额与数量 × 单价的允许误差：1% 或 0.05 元中较大的 */
    private static final BigDecimal AMOUNT_TOLERANCE_RATE = new BigDecimal("0.01");
    private static final BigDecimal AMOUNT_TOLERANCE_MIN = new BigDecimal("0.05");
    private static final Pattern CN_DATE = Pattern.compile("^(\\d{4})\\s*[年/.\\-]\\s*(\\d{1,2})\\s*[月/.\\-]\\s*(\\d{1,2})\\s*日?$"); // i18n-ignore 识别中文日期
    /** 公司名称中可以忽略的部分：括号内的地区、常见的组织形式后缀 */
    private static final Pattern COMPANY_NOISE = Pattern.compile("[（(][^）)]*[）)]|股份有限公司$|有限责任公司$|有限公司$|分公司$|公司$|厂$" // i18n-ignore 匹配中文公司名称
            + "|(?i)\\s*(co\\.?,?\\s*ltd\\.?|ltd\\.?|inc\\.?|corp\\.?|llc)$");

    private final Map<String, Object> data = new LinkedHashMap<>();
    private final List<AiPrefill.Warning> warnings = new ArrayList<>();

    // ---------------------------------------------------------------- 结果

    /** 设置表单字段，值为 null 时不设置 */
    public AiPrefillBuilder put(String field, Object value) {
        if (value != null) {
            data.put(field, value);
        }
        return this;
    }

    /** 加一条提示，文字为消息键按当前语言生成 */
    public void warn(String field, Object value, String messageKey, Object... args) {
        warnings.add(new AiPrefill.Warning(field, value == null ? null : String.valueOf(value), Messages.get(messageKey, args)));
    }

    public List<AiPrefill.Warning> warnings() {
        return warnings;
    }

    public AiPrefill build() {
        return AiPrefill.of(data, warnings);
    }

    // ---------------------------------------------------------------- 读取模型输出

    /** 字符串：去掉首尾空白，空串和字面量 "null" 视为 null */
    public static String str(Map<String, ?> m, String key) {
        Object v = m == null ? null : m.get(key);
        if (v == null || v instanceof Map || v instanceof List) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? null : s;
    }

    /** 数字：也接受「1,200.50」「¥12」「12 元」这样的字符串，无法识别时为 null */
    public static BigDecimal dec(Map<String, ?> m, String key) {
        Object v = m == null ? null : m.get(key);
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        String s = str(m, key);
        if (s == null) {
            return null;
        }
        String cleaned = s.replaceAll("[,，\\s¥￥$€元%]", ""); // i18n-ignore 去掉金额中的货币符号和单位
        try {
            return cleaned.isEmpty() ? null : new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 对象数组（明细行），不是数组时为空列表；最多取 max 行，超出时提示 */
    public List<Map<String, Object>> list(Map<String, ?> m, String key, String field, int max) {
        Object v = m == null ? null : m.get(key);
        if (!(v instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> map) {
                if (out.size() >= max) {
                    warn(field, null, "framework.ai.warn.truncated", max);
                    break;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                map.forEach((k, val) -> row.put(String.valueOf(k), val));
                out.add(row);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 检查

    /**
     * 日期：yyyy-MM-dd，也接受 2026/1/5、2026年1月5日；无法识别时提示并返回 null，距今超过一年时提示核对
     *
     * @return yyyy-MM-dd
     */
    public String date(String field, String raw) {
        if (raw == null) {
            return null;
        }
        LocalDate d = parseDate(raw);
        if (d == null) {
            warn(field, raw, "framework.ai.warn.invalidDate", raw);
            return null;
        }
        LocalDate today = LocalDate.now();
        if (d.isBefore(today.minusYears(1)) || d.isAfter(today.plusYears(1))) {
            warn(field, raw, "framework.ai.warn.dateFar", d);
        }
        return d.toString();
    }

    static LocalDate parseDate(String raw) {
        String s = raw.trim();
        try {
            return LocalDate.parse(s.length() > 10 && s.charAt(10) == 'T' ? s.substring(0, 10) : s);
        } catch (DateTimeParseException e) {
            Matcher m = CN_DATE.matcher(s);
            if (m.matches()) {
                try {
                    return LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
                } catch (RuntimeException ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    /** 数量：必须大于 0，过大时提示；不合理时返回 null（由用户填写） */
    public BigDecimal qty(String field, BigDecimal qty) {
        if (qty == null) {
            return null;
        }
        if (qty.signum() <= 0) {
            warn(field, qty.toPlainString(), "framework.ai.warn.qtyInvalid", qty.toPlainString());
            return null;
        }
        if (qty.compareTo(QTY_LIMIT) >= 0) {
            warn(field, qty.toPlainString(), "framework.ai.warn.qtyLarge", qty.toPlainString());
        }
        return plain(qty.setScale(4, RoundingMode.HALF_UP));
    }

    /** 单价 / 金额：不能为负数，负数时提示并返回 null */
    public BigDecimal nonNegative(String field, BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.signum() < 0) {
            warn(field, value.toPlainString(), "framework.ai.warn.negative", value.toPlainString());
            return null;
        }
        return value;
    }

    /**
     * 单价：有单价时检查「金额 ≈ 数量 × 单价」，不一致时提示；没有单价但有金额和数量时按金额 ÷ 数量计算
     *
     * @return 单价（4 位小数），无法确定时为 null
     */
    public BigDecimal price(String field, BigDecimal qty, BigDecimal price, BigDecimal amount) {
        price = nonNegative(field, price);
        amount = nonNegative(field, amount);
        if (price == null) {
            if (amount != null && qty != null && qty.signum() > 0) {
                return plain(amount.divide(qty, 4, RoundingMode.HALF_UP));
            }
            return null;
        }
        if (amount != null && qty != null && qty.signum() > 0) {
            BigDecimal expected = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tolerance = expected.abs().multiply(AMOUNT_TOLERANCE_RATE).max(AMOUNT_TOLERANCE_MIN);
            if (expected.subtract(amount).abs().compareTo(tolerance) > 0) {
                warn(field, amount.toPlainString(), "framework.ai.warn.amountMismatch", amount.toPlainString(), expected.toPlainString());
            }
        }
        return plain(price.setScale(4, RoundingMode.HALF_UP));
    }

    /** 单据合计与明细金额之和不一致（超过 1% 或 0.05）时提示 */
    public void total(String field, BigDecimal documentTotal, BigDecimal linesTotal) {
        if (documentTotal == null || linesTotal == null || linesTotal.signum() == 0) {
            return;
        }
        BigDecimal tolerance = linesTotal.abs().multiply(AMOUNT_TOLERANCE_RATE).max(AMOUNT_TOLERANCE_MIN);
        if (documentTotal.subtract(linesTotal).abs().compareTo(tolerance) > 0) {
            warn(field, documentTotal.toPlainString(), "framework.ai.warn.totalMismatch", documentTotal.toPlainString(),
                    linesTotal.setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
    }

    /** 税率（%）：0.13 这样的小数换算为 13；不在 0 ～ 100 之间时提示并返回 null */
    public BigDecimal taxRate(String field, BigDecimal rate) {
        if (rate == null) {
            return null;
        }
        BigDecimal r = rate.signum() > 0 && rate.compareTo(BigDecimal.ONE) < 0 ? rate.multiply(BigDecimal.valueOf(100)) : rate;
        if (r.signum() < 0 || r.compareTo(BigDecimal.valueOf(100)) > 0) {
            warn(field, rate.toPlainString(), "framework.ai.warn.taxRateInvalid", rate.toPlainString());
            return null;
        }
        return plain(r.setScale(2, RoundingMode.HALF_UP));
    }

    /** 去掉末尾的 0，不用科学计数法（100 不会变成 1E+2） */
    public static BigDecimal plain(BigDecimal v) {
        if (v == null) {
            return null;
        }
        BigDecimal s = v.stripTrailingZeros();
        return s.scale() < 0 ? s.setScale(0) : s;
    }

    /** 必填但没有识别到时提示 */
    public void required(String field, Object value, String nameLabel) {
        if (value == null) {
            warn(field, null, "framework.ai.warn.missing", nameLabel);
        }
    }

    // ---------------------------------------------------------------- 名称匹配

    /**
     * 按文字匹配主数据：先 exact（编码、名称、税号等精确匹配，返回 null 表示没有），再用 search 模糊搜索原文，
     * 没有结果时去掉公司后缀等再搜索一次。只有一个结果时采用（模糊匹配提示确认），多个或没有时提示并保留原文。
     *
     * @param idField   表单中 ID 字段的路径（如 {@code partnerId}、{@code items[0].materialId}），提示据此定位
     * @param textField 匹配不上时保存原文的字段（相对 data 根，如 {@code partnerName}），为 null 时不保存
     * @param text      识别出的文字，为 null 时直接返回 null
     * @param kindLabel 主数据的名称（如「供应商」，已按当前语言取好）
     * @param exact     精确匹配
     * @param search    模糊搜索（建议限制条数）
     * @param label     结果的显示名称（如「S001 柠檬供应商」）
     * @return 匹配到的记录，没有时为 null
     */
    public <T> T match(String idField, String textField, String text, String kindLabel,
                       Function<String, T> exact, Function<String, List<T>> search, Function<T, String> label) {
        if (text == null) {
            return null;
        }
        T hit = exact.apply(text);
        if (hit != null) {
            return hit;
        }
        List<T> found = search.apply(text);
        String core = coreName(text);
        if (found.isEmpty() && !core.isEmpty() && !core.equals(text)) {
            found = search.apply(core);
        }
        if (found.size() == 1) {
            T only = found.get(0);
            warn(idField, text, "framework.ai.warn.fuzzy", kindLabel, text, label.apply(only));
            return only;
        }
        if (textField != null) {
            data.put(textField, text);
        }
        if (found.isEmpty()) {
            warn(idField, text, "framework.ai.warn.notFound", kindLabel, text);
        } else {
            String names = String.join(Messages.get("framework.ai.listSep"), found.stream().limit(3).map(label).toList());
            warn(idField, text, "framework.ai.warn.ambiguous", kindLabel, text, found.size() > 3 ? names + "…" : names);
        }
        return null;
    }

    /** 去掉括号中的地区、「有限公司」等后缀和空白，用于模糊搜索公司名称 */
    public static String coreName(String name) {
        String s = name == null ? "" : name.trim();
        String prev;
        do {
            prev = s;
            s = COMPANY_NOISE.matcher(s).replaceAll("").trim();
        } while (!s.equals(prev) && !s.isEmpty());
        return s.isEmpty() ? name.trim() : s;
    }
}
