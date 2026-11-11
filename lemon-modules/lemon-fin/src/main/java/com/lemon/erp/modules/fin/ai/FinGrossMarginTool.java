package com.lemon.erp.modules.fin.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.fin.dto.FinAnalysis;
import com.lemon.erp.modules.fin.model.MarginGroup;
import com.lemon.erp.modules.fin.service.FinAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 销售毛利分析：按物料、客户或月份汇总销售收入、销售金额、成本、毛利、毛利率，复用经营分析 {@link FinAnalysisService#grossMargin}
 * （销售金额 = 销售收入 − 本公司运费 − 收款折让 − 坏账核销，毛利 = 销售金额 − 成本）
 */
@Component
@RequiredArgsConstructor
public class FinGrossMarginTool implements AiTool {

    /**
     * 一组的毛利，金额单位元；第一行为合计
     *
     * @param revenue       销售收入（扣整单优惠后，不含税）
     * @param salesAmount   销售金额（再扣本公司运费、收款折让、坏账核销）
     * @param marginPercent 毛利率（%）= 毛利 ÷ 销售金额
     * @param missingCost   没有成本的明细行数（毛利偏高）
     */
    record MarginItem(String name, BigDecimal qty, BigDecimal revenue, BigDecimal salesAmount, BigDecimal cost, BigDecimal profit, BigDecimal marginPercent,
                      Long missingCost) {
    }

    /** 排序指标 */
    enum SortBy {
        REVENUE, PROFIT, MARGIN
    }

    private final FinAnalysisService analysisService;

    @Override
    public String name() {
        return "fin_gross_margin";
    }

    @Override
    public String labelKey() {
        return "fin.ai.tool.grossMargin";
    }

    @Override
    public String descriptionKey() {
        return "fin.ai.tool.grossMargin.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        AiSchema s = FinAiSupport.dateRange(AiSchema.object())
                .prop("groupBy", AiSchema.enumOf("fin.ai.param.marginGroupBy", MarginGroup.MATERIAL.name(), MarginGroup.CUSTOMER.name(),
                        MarginGroup.MONTH.name()), false)
                .prop("sortBy", AiSchema.enumOf("fin.ai.param.marginSortBy", SortBy.REVENUE.name(), SortBy.PROFIT.name(), SortBy.MARGIN.name()), false);
        return FinAiSupport.top(s).build();
    }

    @Override
    public String permission() {
        return "fin:analysis:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        MarginGroup group = MarginGroup.of(args.getString("groupBy"));
        FinAnalysis.GrossMargin gm = analysisService.grossMargin(FinAiSupport.start(args), FinAiSupport.end(args), group.name(), null);
        List<FinAnalysis.MarginRow> list = new ArrayList<>(gm.rows());
        Comparator<FinAnalysis.MarginRow> order = switch (args.has("sortBy") ? SortBy.valueOf(args.getString("sortBy")) : SortBy.REVENUE) {
            case PROFIT -> Comparator.comparing(FinAnalysis.MarginRow::getProfit, Comparator.nullsFirst(Comparator.naturalOrder()));
            case MARGIN -> Comparator.comparing(FinAnalysis.MarginRow::getMargin, Comparator.nullsFirst(Comparator.naturalOrder()));
            case REVENUE -> Comparator.comparing(FinAnalysis.MarginRow::getRevenue, Comparator.nullsFirst(Comparator.naturalOrder()));
        };
        // 月份按时间顺序，其他按指标降序
        list.sort(group == MarginGroup.MONTH ? Comparator.comparing(FinAnalysis.MarginRow::getGroupKey) : order.reversed());
        FinAnalysis.MarginTotal t = gm.total();
        List<MarginItem> rows = new ArrayList<>();
        rows.add(new MarginItem(FinAiSupport.totalLabel(), null, t.revenue(), t.salesAmount(), t.cost(), t.profit(), t.margin(), null));
        list.stream().limit(FinAiSupport.top(args)).forEach(r -> rows.add(new MarginItem(r.getGroupName(), r.getQty(), r.getRevenue(),
                r.getSalesAmount(), r.getCost(), r.getProfit(), r.getMargin(), r.getMissingCost() == null || r.getMissingCost() == 0 ? null : r.getMissingCost())));
        return AiToolResult.rows(rows, (long) list.size() + 1);
    }
}
