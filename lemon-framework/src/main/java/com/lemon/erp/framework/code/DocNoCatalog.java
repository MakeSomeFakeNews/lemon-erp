package com.lemon.erp.framework.code;

import java.util.List;

/**
 * 模块的单据编号类型。各模块以 Bean 的形式声明（如 ScmDocNos），「编码规则」页面据此列出可配置的单据，
 * 取号时 {@code DocNoService.next(key)} 按租户配置的规则生成编号，未配置时使用这里的默认规则。
 * Bean 上用 @Order 指定页面中的模块顺序
 *
 * @param module 模块名称，页面分组用
 */
public record DocNoCatalog(String module, List<Type> types) {

    public static DocNoCatalog of(String module, Type... types) {
        return new DocNoCatalog(module, List.of(types));
    }

    /**
     * @param key         规则标识，也是默认前缀（如 CG）；调用 {@code DocNoService.next(key)} 时使用
     * @param name        单据名称
     * @param dateFormat  默认日期格式
     * @param seqLength   默认流水位数
     * @param resetCycle  默认流水重置周期
     */
    public record Type(String key, String name, DateFormat dateFormat, int seqLength, ResetCycle resetCycle) {

        /** 单据：前缀 + yyyyMMdd + 4 位当天流水 */
        public static Type doc(String key, String name) {
            return new Type(key, name, DateFormat.YYYYMMDD, 4, ResetCycle.DAY);
        }

        /** 主数据（物料、供应商、客户）：前缀 + 6 位流水，不重置；编码留空时自动生成 */
        public static Type master(String key, String name) {
            return new Type(key, name, DateFormat.NONE, 6, ResetCycle.NEVER);
        }
    }

    /** 编号中的日期部分 */
    public enum DateFormat {
        YYYYMMDD("yyyyMMdd", ResetCycle.DAY),
        YYMMDD("yyMMdd", ResetCycle.DAY),
        YYYYMM("yyyyMM", ResetCycle.MONTH),
        YYMM("yyMM", ResetCycle.MONTH),
        YYYY("yyyy", ResetCycle.YEAR),
        NONE("", ResetCycle.NEVER);

        private final String pattern;
        /** 日期部分精确到的周期：流水按更粗的周期重置时编号仍不重复 */
        private final ResetCycle precision;

        DateFormat(String pattern, ResetCycle precision) {
            this.pattern = pattern;
            this.precision = precision;
        }

        public String pattern() {
            return pattern;
        }

        /** 流水按 cycle 重置时，日期部分能否保证编号不重复（日期至少精确到重置周期） */
        public boolean supports(ResetCycle cycle) {
            return precision.ordinal() <= cycle.ordinal();
        }
    }

    /** 流水重置周期，按从细到粗排列 */
    public enum ResetCycle {
        DAY, MONTH, YEAR, NEVER
    }
}
