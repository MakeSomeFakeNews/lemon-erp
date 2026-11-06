package com.lemon.erp.modules.fin.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.fin.dto.CashierViews;
import com.lemon.erp.modules.fin.service.FinCashierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 资金余额：各现金、银行账户（科目）在日期范围内的期初、收入、支出、期末余额，复用资金日报 {@link FinCashierService#daily}
 */
@Component
@RequiredArgsConstructor
public class FinFundBalanceTool implements AiTool {

    /** 账户的资金变动，金额单位元；第一行为合计 */
    record FundItem(String accountCode, String accountName, BigDecimal opening, BigDecimal income, BigDecimal expense, BigDecimal closing,
                    long inCount, long outCount) {
    }

    private final FinCashierService cashierService;

    @Override
    public String name() {
        return "fin_fund_balance";
    }

    @Override
    public String labelKey() {
        return "fin.ai.tool.fundBalance";
    }

    @Override
    public String descriptionKey() {
        return "fin.ai.tool.fundBalance.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return FinAiSupport.dateRange(AiSchema.object()).build();
    }

    @Override
    public String permission() {
        return "fin:cashier:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        CashierViews.Daily daily = cashierService.daily(FinAiSupport.start(args), FinAiSupport.end(args), false);
        CashierViews.DailyTotal t = daily.total();
        List<FundItem> rows = new ArrayList<>();
        rows.add(new FundItem(null, FinAiSupport.totalLabel(), t.open(), t.debit(), t.credit(), t.close(), t.inCount(), t.outCount()));
        for (CashierViews.DailyRow r : daily.rows()) {
            rows.add(new FundItem(r.code(), r.name(), r.open(), r.debit(), r.credit(), r.close(), r.inCount(), r.outCount()));
        }
        return AiToolResult.rows(rows);
    }
}
