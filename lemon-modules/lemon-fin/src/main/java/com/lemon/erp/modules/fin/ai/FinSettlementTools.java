package com.lemon.erp.modules.fin.ai;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.fin.dto.PartnerAmountRow;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.modules.fin.service.FinSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 回款（收款）/ 付款汇总：日期范围内已生效的收付款按往来单位汇总，权限与收款单 / 付款单页面一致
 */
final class FinSettlementTools {

    private FinSettlementTools() {
    }

    /** 往来单位的收付款笔数与金额（元）；第一行为合计 */
    record PartnerAmount(String partnerCode, String partnerName, long count, BigDecimal amount) {
    }

    @RequiredArgsConstructor
    abstract static class Base implements AiTool {

        private final FinSettlementService settlementService;
        private final SettleKind kind;

        @Override
        public Map<String, Object> parameters() {
            AiSchema s = FinAiSupport.dateRange(AiSchema.object());
            FinAiSupport.keyword(s, "fin.ai.param.partnerKeyword");
            return FinAiSupport.top(s).build();
        }

        @Override
        public Object execute(AiToolArgs args) {
            LocalDate begin = FinAiSupport.start(args);
            LocalDate end = FinAiSupport.end(args);
            List<PartnerAmountRow> list = settlementService.partnerSummary(kind, begin, end, args.getString(FinAiSupport.KEYWORD));
            long count = 0;
            for (PartnerAmountRow r : list) {
                count += r.getSettleCount() == null ? 0 : r.getSettleCount();
            }
            List<PartnerAmount> rows = new ArrayList<>();
            rows.add(new PartnerAmount(null, FinAiSupport.totalLabel(), count, Decimals.sum(list, PartnerAmountRow::getAmount)));
            list.stream().limit(FinAiSupport.top(args)).forEach(r -> rows.add(new PartnerAmount(r.getPartnerCode(), r.getPartnerName(),
                    r.getSettleCount() == null ? 0 : r.getSettleCount(), Decimals.nz(r.getAmount()))));
            return AiToolResult.rows(rows, (long) list.size() + 1);
        }
    }

    /** 回款汇总（按客户） */
    @Component
    static class Receipt extends Base {

        Receipt(FinSettlementService settlementService) {
            super(settlementService, SettleKind.RECEIPT);
        }

        @Override
        public String name() {
            return "fin_receipt_summary";
        }

        @Override
        public String labelKey() {
            return "fin.ai.tool.receiptSummary";
        }

        @Override
        public String descriptionKey() {
            return "fin.ai.tool.receiptSummary.desc";
        }

        @Override
        public String permission() {
            return "fin:receipt:list";
        }
    }

    /** 付款汇总（按供应商） */
    @Component
    static class Payment extends Base {

        Payment(FinSettlementService settlementService) {
            super(settlementService, SettleKind.PAYMENT);
        }

        @Override
        public String name() {
            return "fin_payment_summary";
        }

        @Override
        public String labelKey() {
            return "fin.ai.tool.paymentSummary";
        }

        @Override
        public String descriptionKey() {
            return "fin.ai.tool.paymentSummary.desc";
        }

        @Override
        public String permission() {
            return "fin:payment:list";
        }
    }
}
