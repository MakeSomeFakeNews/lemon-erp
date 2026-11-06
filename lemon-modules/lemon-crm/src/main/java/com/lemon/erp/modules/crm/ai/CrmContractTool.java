package com.lemon.erp.modules.crm.ai;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.crm.entity.CrmCollectionPlan;
import com.lemon.erp.modules.crm.entity.CrmContract;
import com.lemon.erp.modules.crm.model.SalesContractStatus;
import com.lemon.erp.modules.crm.service.CrmContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 销售合同与回款计划：列出合同的金额、已回款、未回款，或列出执行中合同的逾期回款计划；
 * 复用 {@link CrmContractService#page} / {@link CrmContractService#visibleOverduePlans}（按数据权限：部门、负责人）
 */
@Component
@RequiredArgsConstructor
public class CrmContractTool implements AiTool {

    /** 查询方式：合同列表 / 逾期回款计划 */
    enum View {
        CONTRACTS, OVERDUE_PLANS
    }

    /** 合同，金额单位元 */
    record Contract(String contractNo, String name, String customerName, String ownerName, SalesContractStatus status, LocalDate signDate,
                    LocalDate endDate, BigDecimal amount, BigDecimal receivedAmount, BigDecimal unreceivedAmount) {
    }

    /** 逾期的回款计划，金额单位元；overdueDays 为逾期天数 */
    record OverduePlan(String contractNo, String customerName, int seq, LocalDate planDate, long overdueDays, BigDecimal planAmount,
                       BigDecimal receivedAmount, BigDecimal unreceivedAmount) {
    }

    private final CrmContractService contractService;

    @Override
    public String name() {
        return "crm_contract_collection";
    }

    @Override
    public String labelKey() {
        return "crm.ai.tool.contracts";
    }

    @Override
    public String descriptionKey() {
        return "crm.ai.tool.contracts.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object()
                .prop("view", AiSchema.enumOf("crm.ai.param.contractView", View.CONTRACTS.name(), View.OVERDUE_PLANS.name()), false)
                .prop("status", AiSchema.enumOf("crm.ai.param.contractStatus", SalesContractStatus.DRAFT.name(), SalesContractStatus.APPROVING.name(),
                        SalesContractStatus.ACTIVE.name(), SalesContractStatus.COMPLETED.name(), SalesContractStatus.TERMINATED.name()), false)
                .prop("keyword", AiSchema.string("crm.ai.param.contractKeyword").maxLength(64), false)
                .prop("top", AiSchema.integer("crm.ai.param.top").min(1).max(AiTool.MAX_ROWS), false)
                .build();
    }

    @Override
    public String permission() {
        return "crm:contract:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        int top = args.getInt("top", 50);
        String keyword = args.getString("keyword");
        View view = args.has("view") ? View.valueOf(args.getString("view")) : View.CONTRACTS;
        if (view == View.OVERDUE_PLANS) {
            LocalDate today = LocalDate.now();
            List<OverduePlan> plans = contractService.visibleOverduePlans().stream()
                    .filter(p -> keyword == null || contains(p.getContractNo(), keyword) || contains(p.getCustomerName(), keyword))
                    .map(p -> new OverduePlan(p.getContractNo(), p.getCustomerName(), p.getSeq() == null ? 0 : p.getSeq(), p.getPlanDate(),
                            ChronoUnit.DAYS.between(p.getPlanDate(), today),
                            p.getAmount(), Decimals.nz(p.getReceivedAmount()), p.getAmount().subtract(Decimals.nz(p.getReceivedAmount()))))
                    .sorted(Comparator.comparing(OverduePlan::planDate))
                    .toList();
            return AiToolResult.rows(plans.subList(0, Math.min(plans.size(), top)), (long) plans.size());
        }
        String status = args.has("status") ? args.getString("status") : SalesContractStatus.ACTIVE.name();
        PageResult<CrmContract> page = contractService.page(1, top, keyword, status, null, null);
        return AiToolResult.rows(page.records().stream().map(c -> {
            BigDecimal received = Decimals.nz(c.getReceivedAmount());
            return new Contract(c.getContractNo(), c.getName(), c.getCustomerName(), c.getOwnerName(), c.getStatus(), c.getSignDate(),
                    c.getEndDate(), c.getAmount(), received, Decimals.nz(c.getAmount()).subtract(received).max(BigDecimal.ZERO));
        }).toList(), page.total());
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }
}
