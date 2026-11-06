package com.lemon.erp.modules.fin.ai;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.fin.dto.AgingRow;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.service.FinBillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 应收 / 应付账龄（按往来单位）：复用账龄分析 {@link FinBillService#aging}，权限与应收单 / 应付单页面一致
 */
final class FinAgingTools {

    /** 账龄分析单页条数（分页上限），最多取 PAGE_LIMIT 页 */
    private static final int PAGE_SIZE = 500;
    private static final int PAGE_LIMIT = 20;

    private FinAgingTools() {
    }

    /**
     * 往来单位的账龄，金额单位元；第一行为合计
     *
     * @param overdue 已逾期合计
     */
    record AgingItem(String partnerCode, String partnerName, BigDecimal balance, BigDecimal notDue, BigDecimal overdue,
                     BigDecimal overdue1To30, BigDecimal overdue31To60, BigDecimal overdue61To90, BigDecimal overdueOver90) {

        static AgingItem of(AgingRow r) {
            BigDecimal d30 = Decimals.nz(r.getD30());
            BigDecimal d60 = Decimals.nz(r.getD60());
            BigDecimal d90 = Decimals.nz(r.getD90());
            BigDecimal over90 = Decimals.nz(r.getOver90());
            return new AgingItem(r.getPartnerCode(), r.getPartnerName(), Decimals.nz(r.getBalance()), Decimals.nz(r.getNotDue()),
                    d30.add(d60).add(d90).add(over90), d30, d60, d90, over90);
        }

        AgingItem plus(AgingItem o) {
            return new AgingItem(partnerCode, partnerName, balance.add(o.balance), notDue.add(o.notDue), overdue.add(o.overdue),
                    overdue1To30.add(o.overdue1To30), overdue31To60.add(o.overdue31To60), overdue61To90.add(o.overdue61To90),
                    overdueOver90.add(o.overdueOver90));
        }
    }

    @RequiredArgsConstructor
    abstract static class Base implements AiTool {

        private final FinBillService billService;
        private final BillKind kind;

        @Override
        public Map<String, Object> parameters() {
            AiSchema s = AiSchema.object().prop("asOf", AiSchema.date("fin.ai.param.asOf"), false);
            FinAiSupport.keyword(s, "fin.ai.param.partnerKeyword");
            s.prop("overdueOnly", AiSchema.bool("fin.ai.param.overdueOnly"), false);
            return FinAiSupport.top(s).build();
        }

        @Override
        public Object execute(AiToolArgs args) {
            LocalDate asOf = args.has("asOf") ? args.getDate("asOf") : LocalDate.now();
            String keyword = args.getString(FinAiSupport.KEYWORD);
            boolean overdueOnly = Boolean.TRUE.equals(args.getBoolean("overdueOnly"));
            List<AgingItem> items = new ArrayList<>();
            for (int page = 1; page <= PAGE_LIMIT; page++) {
                PageResult<AgingRow> result = billService.aging(page, PAGE_SIZE, kind, asOf);
                for (AgingRow r : result.records()) {
                    AgingItem item = AgingItem.of(r);
                    if (FinAiSupport.matches(keyword, r.getPartnerCode(), r.getPartnerName()) && (!overdueOnly || item.overdue().signum() != 0)) {
                        items.add(item);
                    }
                }
                if ((long) page * PAGE_SIZE >= result.total()) {
                    break;
                }
            }
            if (overdueOnly) {
                items.sort(Comparator.comparing(AgingItem::overdue).reversed());
            }
            BigDecimal zero = BigDecimal.ZERO;
            AgingItem total = new AgingItem(null, FinAiSupport.totalLabel(), zero, zero, zero, zero, zero, zero, zero);
            for (AgingItem i : items) {
                total = total.plus(i);
            }
            List<AgingItem> rows = new ArrayList<>();
            rows.add(total);
            rows.addAll(items.subList(0, Math.min(items.size(), FinAiSupport.top(args))));
            // 总数为往来单位数 + 合计行
            return AiToolResult.rows(rows, (long) items.size() + 1);
        }
    }

    /** 应收账龄（按客户） */
    @Component
    static class Receivable extends Base {

        Receivable(FinBillService billService) {
            super(billService, BillKind.AR);
        }

        @Override
        public String name() {
            return "fin_receivable_aging";
        }

        @Override
        public String labelKey() {
            return "fin.ai.tool.receivableAging";
        }

        @Override
        public String descriptionKey() {
            return "fin.ai.tool.receivableAging.desc";
        }

        @Override
        public String permission() {
            return "fin:ar:list";
        }
    }

    /** 应付账龄（按供应商） */
    @Component
    static class Payable extends Base {

        Payable(FinBillService billService) {
            super(billService, BillKind.AP);
        }

        @Override
        public String name() {
            return "fin_payable_aging";
        }

        @Override
        public String labelKey() {
            return "fin.ai.tool.payableAging";
        }

        @Override
        public String descriptionKey() {
            return "fin.ai.tool.payableAging.desc";
        }

        @Override
        public String permission() {
            return "fin:ap:list";
        }
    }
}
