package com.lemon.erp.modules.crm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户分析的返回结构。数据库查询结果用带 setter 的类（MyBatis 按列名映射，H2 / MySQL 均适用），
 * 为空的列不输出（与原先返回 Map 时一致）；在 Java 中组装的结构用 record
 */
public final class CrmAnalysis {

    private CrmAnalysis() {
    }

    /** 经营概览：关键指标 + 赢单率 + 客户阶段分布 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Overview {
        private Long newCustomers;
        private Long ownedCustomers;
        private Long poolCustomers;
        private Long newLeads;
        private Long convertedLeads;
        private Long openOpps;
        private BigDecimal pipeline;
        private BigDecimal weightedPipeline;
        private Long wonCount;
        private BigDecimal wonAmount;
        private Long wonTotal;
        private Long lostTotal;
        private Long openTickets;
        private Long overdueTickets;
        private BigDecimal satisfaction;
        /** 赢单率（%），没有赢单和输单时为空 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal winRate;
        private List<StageCount> customerStages;
    }

    /** 按阶段计数（客户阶段、商机阶段、到达过的阶段） */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StageCount {
        private String stage;
        private Long count;
        private BigDecimal amount;
    }

    /**
     * 销售漏斗的一行
     *
     * @param reached    到达过本阶段的商机数（输单行为空）
     * @param conversion 到达下一阶段的比例（%），最后一个阶段、输单行和没有到达数时为空
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FunnelRow(String stage, long count, BigDecimal amount, Long reached, BigDecimal conversion) {
    }

    /** RFM 分析的数据源：客户近一年的最近出库日期、出库次数、净销售额 */
    @Data
    public static class RfmSource {
        private Long customerId;
        private String code;
        private String name;
        private String ownerName;
        private String lastDate;
        private Long frequency;
        private BigDecimal monetary;
    }

    /** RFM 分层汇总 */
    public record RfmSegment(String segment, long count, BigDecimal amount) {
    }

    /** RFM 分析结果；没有销售数据时只有 rows、segments */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Rfm(List<RfmRow> rows, List<RfmSegment> segments, BigDecimal avgRecency, BigDecimal avgFrequency,
                      BigDecimal avgMonetary) {
    }

    /** 按月汇总：销售额与成交客户数、赢单数与金额 */
    @Data
    public static class MonthlyRow {
        private Integer period;
        private Long count;
        private Long customers;
        private BigDecimal amount;
    }

    /** 近 12 个月趋势 */
    public record TrendRow(int period, BigDecimal sales, long customers, long wonCount, BigDecimal wonAmount) {
    }

    /** 来源分析 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SourceStat {
        private String source;
        private Long leads;
        private Long converted;
        private Long opps;
        private BigDecimal wonAmount;
    }

    /** 销售人员分析 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OwnerStat {
        private Long userId;
        private String name;
        private Long customers;
        private Long openOpps;
        private BigDecimal pipeline;
        private Long wonCount;
        private BigDecimal wonAmount;
        private Long followups;
    }

    /** 行业分布 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IndustryStat {
        private String industry;
        private Long customers;
        private BigDecimal sales;
    }

    /** 工单统计（按类型） */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TicketStat {
        private String type;
        private Long count;
        private Long open;
        /** 平均解决时长（分钟） */
        private BigDecimal avgMinutes;
        private Long onTime;
        private Long resolved;
        private BigDecimal satisfaction;
    }

    /** 客户详情的经营汇总 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomerSummary {
        private BigDecimal salesAmount;
        private Long orderCount;
        private String lastOrderDate;
        private BigDecimal arBalance;
        private Long wonCount;
        private BigDecimal wonAmount;
        private Long openTickets;
    }
}
