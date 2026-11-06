package com.lemon.erp.modules.wms.ai;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 问数工具（{@link AiTool}）的公共部分：通用参数（日期范围、行数上限）的 Schema 与取值、按名称匹配基础资料、
 * 精简数值。供应链、生产模块依赖仓库模块，也使用这里的方法；通用参数说明的消息键在 wms 资源包（{@code wms.ai.param.*}）
 */
public final class AiQuerySupport {

    /** 默认返回的行数 */
    public static final int DEFAULT_LIMIT = 50;
    /** 日期范围的最大跨度（天） */
    public static final int MAX_RANGE_DAYS = 366;
    /** 分页查询的单页条数（分页插件上限 500） */
    private static final int PAGE_SIZE = 500;
    /** 匹配到多个候选时最多列出的个数 */
    private static final int CANDIDATES_SHOWN = 8;

    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    public static final String LIMIT = "limit";

    private AiQuerySupport() {
    }

    // ---------------------------------------------------------------- 参数

    /** 开始日期 yyyy-MM-dd */
    public static AiSchema startDate() {
        return AiSchema.date("wms.ai.param.startDate");
    }

    /** 结束日期 yyyy-MM-dd */
    public static AiSchema endDate() {
        return AiSchema.date("wms.ai.param.endDate");
    }

    /** 返回的最多行数：默认 {@link #DEFAULT_LIMIT}，最大 {@link AiTool#MAX_ROWS} */
    public static AiSchema limit() {
        return AiSchema.integer("wms.ai.param.limit").min(1).max(AiTool.MAX_ROWS);
    }

    public static int limit(AiToolArgs args) {
        return Math.max(1, Math.min(AiTool.MAX_ROWS, args.getInt(LIMIT, DEFAULT_LIMIT)));
    }

    /** 日期范围（含首尾） */
    public record Range(LocalDate begin, LocalDate end) {

        /** 结束日期次日零点，用于时间字段的半开区间 */
        public LocalDateTime endExclusive() {
            return end.plusDays(1).atStartOfDay();
        }
    }

    /**
     * 读取 startDate / endDate：结束日期默认今天，开始日期默认 defaultBegin(结束日期)；结束不能早于开始，跨度不超过一年
     */
    public static Range range(AiToolArgs args, Function<LocalDate, LocalDate> defaultBegin) {
        LocalDate end = args.getDate(END_DATE);
        LocalDate begin = args.getDate(START_DATE);
        if (end == null) {
            end = begin != null && begin.isAfter(LocalDate.now()) ? begin : LocalDate.now();
        }
        if (begin == null) {
            begin = defaultBegin.apply(end);
        }
        if (end.isBefore(begin)) {
            throw new BizException("wms.error.endDateCannotEarlierStartDate");
        }
        if (begin.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw new BizException("wms.error.dateRangeCannotExceedOneYear");
        }
        return new Range(begin, end);
    }

    /** 默认本月：结束日期所在月的 1 日 */
    public static Range monthToDate(AiToolArgs args) {
        return range(args, end -> end.withDayOfMonth(1));
    }

    // ---------------------------------------------------------------- 匹配基础资料

    /**
     * 按编码或名称从候选中选出一个：编码或名称完全一致的优先；否则编码或名称包含关键字的只有一个时选它；
     * 没有匹配时抛出 notFoundKey(关键字)，匹配到多个时抛出 ambiguousKey(关键字, 候选名称)，模型据此请用户确认
     */
    public static <T> T pick(List<T> candidates, String keyword, Function<T, String> code, Function<T, String> name,
                             String notFoundKey, String ambiguousKey) {
        String kw = keyword.trim();
        String lower = kw.toLowerCase(Locale.ROOT);
        List<T> exact = candidates.stream()
                .filter(c -> kw.equalsIgnoreCase(nv(code.apply(c))) || kw.equalsIgnoreCase(nv(name.apply(c)))).toList();
        if (exact.size() == 1) {
            return exact.get(0);
        }
        List<T> matched = exact.isEmpty() ? candidates.stream()
                .filter(c -> nv(code.apply(c)).toLowerCase(Locale.ROOT).contains(lower)
                        || nv(name.apply(c)).toLowerCase(Locale.ROOT).contains(lower)).toList() : exact;
        if (matched.isEmpty()) {
            throw new BizException(notFoundKey, kw);
        }
        if (matched.size() > 1) {
            String names = matched.stream().limit(CANDIDATES_SHOWN).map(c -> nv(code.apply(c)) + " " + nv(name.apply(c)))
                    .collect(Collectors.joining(", "));
            throw new BizException(ambiguousKey, kw, names);
        }
        return matched.get(0);
    }

    private static String nv(String s) {
        return s == null ? "" : s;
    }

    // ---------------------------------------------------------------- 查询与数值

    /**
     * 分页读取，直到读完或达到 max 条（用于需要在 Java 中排序 / 汇总的结果）
     *
     * @param pager (页码, 条数) -> 分页结果
     */
    public static <T> List<T> fetch(BiFunction<Long, Long, PageResult<T>> pager, int max) {
        List<T> all = new ArrayList<>();
        for (long current = 1; all.size() < max; current++) {
            PageResult<T> page = pager.apply(current, (long) PAGE_SIZE);
            all.addAll(page.records());
            if (page.records().size() < PAGE_SIZE || all.size() >= page.total()) {
                break;
            }
        }
        return all.size() > max ? new ArrayList<>(all.subList(0, max)) : all;
    }

    /** 去掉末尾的 0（如数量 12.5000 → 12.5），不用科学计数法，减少发给模型的字符 */
    public static BigDecimal plain(BigDecimal v) {
        if (v == null) {
            return null;
        }
        BigDecimal s = v.stripTrailingZeros();
        return s.scale() < 0 ? s.setScale(0) : s;
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
