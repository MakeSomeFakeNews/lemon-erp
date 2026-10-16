package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.entity.QmsInspectionPlan;
import com.lemon.erp.modules.mes.model.SpcChart;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 质量管理的查询结果
 */
public final class QmsViews {

    private QmsViews() {
    }

    /**
     * 新建检验单时匹配的检验方案与抽样结果
     *
     * @param sampleQty  抽样数量（全检为送检数量）
     * @param sampleCode AQL 抽样的样本量字码
     * @param acceptQty  允收不良数 Ac（全检、固定数量没有设置时为 0）
     */
    public record PlanMatch(QmsInspectionPlan plan, BigDecimal sampleQty, String sampleCode, Integer acceptQty, Integer rejectQty) {
    }

    /** 月度检验合格率（按检验类型） */
    public record TrendRow(String month, String type, long total, long pass, long fail) {
    }

    /** 不良类型柏拉图 */
    public record ParetoRow(String defectType, BigDecimal qty, long count) {
    }

    /**
     * 控制图的一个子组（一张检验单的实测值）
     *
     * @param spread 离散程度：极差 R、移动极差 MR（第一个子组为空）或标准差 s，与控制图类型对应
     */
    public record SpcPoint(Long inspectionId, String inspNo, LocalDate date, int n, BigDecimal mean, BigDecimal spread) {
    }

    /**
     * SPC 结果：均值图（单值图）与离散图（R / MR / s）的中心线、控制限，过程能力指数
     *
     * @param n       子组大小（取各子组样本数的众数，其他大小的子组不参与计算）
     * @param chart   控制图类型，按子组大小选择
     * @param sigma   估计的过程标准差（R̄/d₂、MR̄/d₂ 或 s̄/c₄）
     * @param cp      过程能力指数（需要上下限）
     * @param cpk     考虑偏移的过程能力指数（有一侧规格限即可）
     * @param signals 超出控制限的子组、数据不足等提示
     */
    public record Spc(String itemName, String unit, BigDecimal lowerLimit, BigDecimal upperLimit, int n, SpcChart chart,
                      List<SpcPoint> points, BigDecimal meanCenter, BigDecimal meanUcl, BigDecimal meanLcl,
                      BigDecimal spreadCenter, BigDecimal spreadUcl, BigDecimal spreadLcl,
                      BigDecimal sigma, BigDecimal cp, BigDecimal cpk, List<String> signals) {
    }

    /** 可做 SPC 的定量检验项目 */
    public record SpcItem(Long planItemId, String planName, String itemName, String unit, String materialName, long subgroups) {
    }
}
