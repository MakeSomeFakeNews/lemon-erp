package com.lemon.erp.modules.fin.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.fin.dto.FinancialMetric;
import com.lemon.erp.modules.fin.dto.FinancialRow;
import com.lemon.erp.modules.fin.dto.FinancialStatement;
import com.lemon.erp.modules.fin.service.FinFinancialStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 利润表 / 资产负债表的项目金额与关键指标（毛利率、净利率、资产负债率、流动比率等），复用财务报表 {@link FinFinancialStatementService}。
 * 每行为「项目」+ 报表各列（利润表：本月、本年累计、上年同期累计；资产负债表：期末余额、年初余额），指标行在最后
 */
@Component
@RequiredArgsConstructor
public class FinStatementTool implements AiTool {

    /** 报表：利润表 / 资产负债表 */
    enum Report {
        INCOME, BALANCE
    }

    static final String ITEM = "item";

    private final FinFinancialStatementService statementService;

    @Override
    public String name() {
        return "fin_financial_statement";
    }

    @Override
    public String labelKey() {
        return "fin.ai.tool.financialStatement";
    }

    @Override
    public String descriptionKey() {
        return "fin.ai.tool.financialStatement.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        AiSchema s = AiSchema.object().prop("report", AiSchema.enumOf("fin.ai.param.report", Report.INCOME.name(), Report.BALANCE.name()), true);
        return FinAiSupport.period(s).build();
    }

    @Override
    public String permission() {
        return "fin:report:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        int period = FinAiSupport.period(args);
        Report report = Report.valueOf(args.getString("report"));
        FinancialStatement st = report == Report.INCOME ? statementService.incomeStatement(period) : statementService.balanceSheet(period);
        List<Map<String, Object>> rows = new ArrayList<>();
        addRows(rows, st.columns(), st.rows());
        if (st.right() != null) {
            addRows(rows, st.columns(), st.right());
        }
        for (FinancialMetric m : st.metrics()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(ITEM, m.name() + " (" + m.unit() + ")");
            // 资产负债表：期末、年初；利润表：本月、本年累计
            row.put(st.columns().get(0), m.value());
            row.put(st.columns().get(1), m.value2());
            rows.add(row);
        }
        return AiToolResult.rows(rows);
    }

    /** 报表行只保留项目名称和各列金额（不含分组标题、取数公式、构成科目） */
    private static void addRows(List<Map<String, Object>> out, List<String> columns, List<FinancialRow> rows) {
        for (FinancialRow r : rows) {
            if (r.title()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(ITEM, r.name());
            for (int i = 0; i < columns.size() && i < r.values().size(); i++) {
                row.put(columns.get(i), r.values().get(i));
            }
            out.add(row);
        }
    }
}
