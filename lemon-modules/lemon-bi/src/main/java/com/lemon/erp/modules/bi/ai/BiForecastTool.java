package com.lemon.erp.modules.bi.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.bi.dto.BiInsight;
import com.lemon.erp.modules.bi.service.BiDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 销售预测：近 12 个月销售额的线性趋势（满两年时乘季节指数）预测未来几个月，复用 {@link BiDecisionService#forecast}（需要全部数据权限）
 */
@Component
@RequiredArgsConstructor
public class BiForecastTool implements AiTool {

    /** 历史只返回最近的月数 */
    private static final int HISTORY_MONTHS = 12;

    /**
     * @param slope    每月趋势增量（元）
     * @param history  最近 12 个月的实际销售额（本月未结束）
     * @param forecast 从本月开始的预测销售额
     */
    record Forecast(String method, boolean seasonal, BigDecimal slope, List<BiInsight.PeriodAmount> history, List<BiInsight.PeriodAmount> forecast) {
    }

    private final BiDecisionService decisionService;

    @Override
    public String name() {
        return "bi_sales_forecast";
    }

    @Override
    public String labelKey() {
        return "bi.ai.tool.forecast";
    }

    @Override
    public String descriptionKey() {
        return "bi.ai.tool.forecast.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object()
                .prop("months", AiSchema.integer("bi.ai.param.forecastMonths").min(1).max(12), false)
                .prop("category", AiSchema.string("bi.ai.param.category").maxLength(32), false)
                .build();
    }

    @Override
    public String permission() {
        return "bi:decision:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        BiAiSupport.requireAllData();
        BiInsight.Forecast f = decisionService.forecast(args.getString("category"), args.getInt("months", 3));
        List<BiInsight.PeriodAmount> history = f.history().subList(Math.max(0, f.history().size() - HISTORY_MONTHS), f.history().size());
        return AiToolResult.of(new Forecast(f.method(), f.seasonal(), f.slope(), history, f.forecast()));
    }
}
