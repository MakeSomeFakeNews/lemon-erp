package com.lemon.erp.modules.crm.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.crm.dto.CrmAnalysis;
import com.lemon.erp.modules.crm.service.CrmAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 客户与商机漏斗：经营概览（客户、线索、商机、赢单、工单）、各阶段商机数与金额及转化率、销售人员的客户 / 商机 / 赢单 / 跟进，
 * 复用客户分析 {@link CrmAnalysisService}（与「客户分析」页面相同的全公司口径与权限）
 */
@Component
@RequiredArgsConstructor
public class CrmSalesFunnelTool implements AiTool {

    /** 销售人员最多返回的人数 */
    private static final int OWNER_LIMIT = 20;

    /**
     * 经营概览，金额单位元
     *
     * @param newCustomersThisMonth 本月新增客户
     * @param pipeline              进行中商机金额
     * @param weightedPipeline      按赢率加权的商机金额
     * @param winRatePercent        历史赢率（%）
     */
    record Overview(Long newCustomersThisMonth, Long ownedCustomers, Long poolCustomers, Long newLeadsThisMonth, Long convertedLeadsThisMonth,
                    Long openOpportunities, BigDecimal pipeline, BigDecimal weightedPipeline, Long wonCountThisMonth, BigDecimal wonAmountThisMonth,
                    BigDecimal winRatePercent, Long openTickets, Long overdueTickets) {
    }

    /**
     * 漏斗的一个阶段
     *
     * @param count             当前处于该阶段的商机数
     * @param reached           曾到达该阶段的商机数
     * @param conversionPercent 到达下一阶段的转化率（%）
     */
    record Stage(String stage, long count, BigDecimal amount, Long reached, BigDecimal conversionPercent) {
    }

    /**
     * 销售人员
     *
     * @param wonCountThisYear      本年赢单数
     * @param followupsThisMonth    本月跟进次数
     */
    record Owner(String name, Long customers, Long openOpportunities, BigDecimal pipeline, Long wonCountThisYear, BigDecimal wonAmountThisYear,
                 Long followupsThisMonth) {
    }

    record Funnel(Overview overview, List<Stage> stages, List<Owner> owners) {
    }

    private final CrmAnalysisService analysisService;

    @Override
    public String name() {
        return "crm_sales_funnel";
    }

    @Override
    public String labelKey() {
        return "crm.ai.tool.salesFunnel";
    }

    @Override
    public String descriptionKey() {
        return "crm.ai.tool.salesFunnel.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object().build();
    }

    @Override
    public String permission() {
        return "crm:analysis:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        CrmAnalysis.Overview o = analysisService.overview();
        Overview overview = new Overview(o.getNewCustomers(), o.getOwnedCustomers(), o.getPoolCustomers(), o.getNewLeads(), o.getConvertedLeads(),
                o.getOpenOpps(), o.getPipeline(), o.getWeightedPipeline(), o.getWonCount(), o.getWonAmount(), o.getWinRate(), o.getOpenTickets(),
                o.getOverdueTickets());
        List<Stage> stages = analysisService.funnel().stream()
                .map(f -> new Stage(f.stage(), f.count(), f.amount(), f.reached(), f.conversion())).toList();
        List<Owner> owners = analysisService.owners().stream().limit(OWNER_LIMIT)
                .map(r -> new Owner(r.getName(), r.getCustomers(), r.getOpenOpps(), r.getPipeline(), r.getWonCount(), r.getWonAmount(),
                        r.getFollowups())).toList();
        return AiToolResult.of(new Funnel(overview, stages, owners));
    }
}
