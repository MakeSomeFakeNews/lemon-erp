package com.lemon.erp.framework.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * 经营预警的「AI 分析」数据来源（商务智能模块实现）：给出一条预警规则的当前值、阈值、近期趋势和相关的报告来源，
 * AI 模块再取当前用户有权限的相关来源快照，一起交给模型分析可能的原因和建议。只查询，不修改数据
 */
public interface AiAlertSource {

    /** 查看预警所需的权限（{@code PermissionService#hasAnyPerm} 语义，满足其一即可） */
    String[] permissions();

    /**
     * 预警规则的当前情况
     *
     * @return 规则不存在或已停用时返回 null
     */
    AiAlert alert(Long ruleId);

    /**
     * 一条预警的分析材料（发送给模型，字段名有含义，金额单位为元）
     *
     * @param ruleId         规则
     * @param name           规则名称
     * @param metric         指标编码
     * @param metricLabel    指标名称（当前语言）
     * @param unit           单位（当前语言）
     * @param operator       比较方式（GT / GE / LT / LE）
     * @param threshold      阈值
     * @param level          级别（INFO / WARN / DANGER）
     * @param value          当前值
     * @param triggered      是否触发
     * @param remark         规则中填写的处理建议
     * @param trend          近期趋势（如近 6 个月的毛利率），没有时为空列表
     * @param details        与指标直接相关的少量明细（如呆滞金额最大的物料、超支的预算科目），没有时为空列表
     * @param reportSources  相关的智能报告来源标识（{@link AiReportSource#key()}），AI 模块按当前用户权限取近 30 天快照
     */
    record AiAlert(Long ruleId, String name, String metric, String metricLabel, String unit, String operator, BigDecimal threshold,
                   String level, BigDecimal value, boolean triggered, String remark, List<?> trend, List<?> details,
                   List<String> reportSources) {
    }
}
