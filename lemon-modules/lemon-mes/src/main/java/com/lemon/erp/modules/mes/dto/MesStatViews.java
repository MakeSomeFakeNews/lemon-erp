package com.lemon.erp.modules.mes.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.mes.model.WoDueState;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.mes.model.WorkOrderType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产统计报表的返回结构。比率为百分数（保留 1 位），分母为 0 时为空
 */
public final class MesStatViews {

    private MesStatViews() {
    }

    /**
     * 汇总结果
     *
     * @param rows      汇总行（超过上限时只返回前面的行）
     * @param total     合计（全部行）
     * @param truncated 行数超过上限
     */
    public record Summary<T>(List<T> rows, T total, boolean truncated) {
    }

    /** 报工汇总的公共维度和指标：人员计件统计、工序 / 工作中心产量统计 */
    @Data
    public static class ReportRow {
        /** 汇总行的键（前端行 key、穿透） */
        private String groupKey;
        private Long operatorId;
        private String operatorName;
        private String deptName;
        private Long workCenterId;
        private String workCenterName;
        private String opName;
        private Long productId;
        private String productCode;
        private String productName;
        private String unit;
        private LocalDate day;
        /** 月份 yyyyMM */
        private Integer period;

        private Long reportCount;
        private Long woCount;
        private Long operatorCount;
        private BigDecimal goodQty;
        private BigDecimal scrapQty;
        /** 良品率 = 合格 / (合格 + 报废) */
        private BigDecimal yieldRate;
        /** 实际工时 */
        private BigDecimal hours;
        /** 标准工时 = (合格 + 报废) × 工序标准工时 */
        private BigDecimal stdHours;
        /** 效率 = 标准工时 / 实际工时 */
        private BigDecimal efficiency;
        /** 计件工资 = Σ 报工的计件工资（合格数 × 工序计件单价，与工资表的计件工资同一口径） */
        private BigDecimal pieceWage;
    }

    /** 报工明细 */
    @Data
    public static class ReportDetail {
        private Long id;
        private LocalDate reportDate;
        private Long woId;
        private String woNo;
        private String productCode;
        private String productName;
        private String unit;
        private Integer opSeq;
        private String opName;
        private String workCenterName;
        private String operatorName;
        private BigDecimal goodQty;
        private BigDecimal scrapQty;
        private BigDecimal hours;
        private BigDecimal pieceRate;
        private BigDecimal pieceWage;
        private String equipmentName;
        private String remark;
    }

    /** 工单完成情况：按工单列出，或按产品 / 车间 / 月份汇总 */
    @Data
    public static class WoRow {
        private String groupKey;
        private Long woId;
        private String woNo;
        private WorkOrderType woType;
        private WorkOrderStatus status;
        private LocalDate planStart;
        private LocalDate planEnd;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime finishTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime closeTime;
        /** 交期状态（按工单列出时） */
        private WoDueState dueState;
        /** 延期天数：延期完工为完工晚于计划的天数，逾期未完工为今天晚于计划的天数 */
        private Integer delayDays;

        private Long productId;
        private String productCode;
        private String productName;
        private String unit;
        private Long deptId;
        private String deptName;
        private Integer period;

        private Long woCount;
        private BigDecimal qty;
        private BigDecimal finishedQty;
        private BigDecimal scrapQty;
        /** 完成率 = 完工 / 计划 */
        private BigDecimal completionRate;
        /** 良品率 = 完工 / (完工 + 报废) */
        private BigDecimal yieldRate;
        private Long completedCount;
        private Long onTimeCount;
        private Long lateCount;
        private Long overdueCount;
        private Long closedCount;
        /** 按期完工率 = 按期完工 / 已完工 */
        private BigDecimal onTimeRate;
    }

    /** 产品产量汇总：完工入库、报废入库 */
    @Data
    public static class OutputRow {
        private String groupKey;
        private Long productId;
        private String productCode;
        private String productName;
        private String spec;
        private String unit;
        private String category;
        private Long deptId;
        private String deptName;
        private LocalDate day;
        private Integer period;

        private BigDecimal finishQty;
        private BigDecimal scrapInQty;
        private Long finishCount;
        private Long woCount;
        /** 完工入库成本（料工费合计），需要「查看成本金额」权限 */
        private BigDecimal finishCost;
        /** 单位成本 = 完工入库成本 / 完工数量 */
        private BigDecimal unitCost;
    }

    /** 用料差异：结束（完工 / 关闭）的工单，标准用量（完工数量 × 单位用量）与实际净领料的对比 */
    @Data
    public static class UsageRow {
        private String groupKey;
        private Long woId;
        private String woNo;
        private WorkOrderStatus status;
        private String productCode;
        private String productName;
        private BigDecimal woQty;
        private BigDecimal finishedQty;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String spec;
        private String unit;

        private Long woCount;
        private BigDecimal stdQty;
        private BigDecimal actualQty;
        /** 差异 = 实际 − 标准，正数为超耗 */
        private BigDecimal varianceQty;
        /** 差异率 = 差异 / 标准 */
        private BigDecimal varianceRate;
    }
}
