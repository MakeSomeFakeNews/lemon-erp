package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.dto.FinAnalysis;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.mapper.FinAnalysisMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherEntryMapper;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.MarginGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.lemon.erp.modules.fin.model.FinConstants.previous;
import static com.lemon.erp.modules.fin.model.FinConstants.toYearMonth;

/**
 * 经营分析：销售毛利、存货对账（总账与成本核算、数量与库存流水）、现金流量表（直接法）
 */
@Service
@RequiredArgsConstructor
public class FinAnalysisService {


    private static final int MARGIN_DETAIL_LIMIT = 5000;

    private final FinAnalysisMapper analysisMapper;
    private final FinVoucherEntryMapper entryMapper;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;

    /** 销售毛利：按物料、客户或月份汇总，返回 {rows, total} */
    public FinAnalysis.GrossMargin grossMargin(LocalDate begin, LocalDate end, String groupBy, Long customerId) {
        if (end.isBefore(begin)) {
            throw new BizException("fin.error.endDateCannotEarlierStartDate");
        }
        String g = MarginGroup.of(groupBy).name();
        List<FinAnalysis.MarginRow> rows = analysisMapper.selectGrossMargin(begin, end, g, customerId);
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (FinAnalysis.MarginRow r : rows) {
            BigDecimal rev = Decimals.money(dec(r.getRevenue()));
            BigDecimal c = Decimals.money(dec(r.getCost()));
            r.setRevenue(rev);
            r.setCost(c);
            r.setProfit(rev.subtract(c));
            r.setMargin(margin(rev, c));
            revenue = revenue.add(rev);
            cost = cost.add(c);
        }
        return new FinAnalysis.GrossMargin(rows, new FinAnalysis.MarginTotal(revenue, cost, revenue.subtract(cost), margin(revenue, cost)));
    }

    /** 销售毛利明细：按销售出库 / 退货单据明细逐行计算，最多 5000 行 */
    public FinAnalysis.MarginDetail grossMarginDetail(LocalDate begin, LocalDate end, Long customerId, String keyword) {
        if (end.isBefore(begin)) {
            throw new BizException("fin.error.endDateCannotEarlierStartDate");
        }
        List<FinAnalysis.MarginDetailRow> list = analysisMapper.selectMarginDetail(begin, end, customerId, keyword, MARGIN_DETAIL_LIMIT + 1);
        List<FinAnalysis.MarginDetailRow> rows = new ArrayList<>(list.subList(0, Math.min(list.size(), MARGIN_DETAIL_LIMIT)));
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (FinAnalysis.MarginDetailRow r : rows) {
            BigDecimal rev = Decimals.money(dec(r.getRevenue()));
            BigDecimal c = Decimals.money(dec(r.getCost()));
            r.setRevenue(rev);
            r.setCost(c);
            r.setProfit(rev.subtract(c));
            r.setMargin(margin(rev, c));
            revenue = revenue.add(rev);
            cost = cost.add(c);
        }
        return new FinAnalysis.MarginDetail(rows, new FinAnalysis.MarginTotal(revenue, cost, revenue.subtract(cost), margin(revenue, cost)),
                list.size() > MARGIN_DETAIL_LIMIT);
    }

    /**
     * 存货对账：原材料、库存商品的总账余额与成本核算期末金额；成本核算期末数量与截至期末的库存流水不一致的物料
     */
    public FinAnalysis.InventoryReconciliation inventoryReconciliation(int period) {
        toYearMonth(period);
        Map<String, BigDecimal> costByAccount = new HashMap<>();
        for (FinAnalysis.CategoryAmount r : analysisMapper.selectCostCloseByCategory(period)) {
            costByAccount.merge(accountRules.code(AccountRole.INVENTORY, r.getCategory()), dec(r.getCloseAmount()), BigDecimal::add);
        }
        String outsource = accountRules.code(AccountRole.OUTSOURCE_MATERIAL, null);
        Set<String> codes = new TreeSet<>(costByAccount.keySet());
        codes.add(accountRules.code(AccountRole.INVENTORY, FinConstants.CATEGORY_RAW));
        codes.add(accountRules.code(AccountRole.INVENTORY, FinConstants.CATEGORY_FINISHED));
        codes.add(outsource);
        List<FinAccount> all = accountService.listAll();
        List<FinAnalysis.InventoryAccount> accounts = new ArrayList<>();
        for (String code : codes) {
            FinAccount parent = all.stream().filter(a -> a.getCode().equals(code)).findFirst().orElse(null);
            if (parent == null) {
                continue;
            }
            BigDecimal gl = BigDecimal.ZERO;
            for (FinAccount a : all) {
                if (Boolean.TRUE.equals(a.getLeaf()) && a.getCode().startsWith(code)) {
                    gl = gl.add(entryMapper.sumBalanceUpTo(a.getId(), period));
                }
            }
            BigDecimal costAmount = outsource.equals(code) ? null : costByAccount.getOrDefault(code, BigDecimal.ZERO);
            accounts.add(new FinAnalysis.InventoryAccount(code, parent.getName(), gl, costAmount,
                    costAmount == null ? null : gl.subtract(costAmount)));
        }
        List<FinAnalysis.QtyDiff> qty = analysisMapper.selectQtyDiff(period, toYearMonth(period).atEndOfMonth());
        return new FinAnalysis.InventoryReconciliation(period, accounts, qty);
    }

    /**
     * 现金流量表（直接法）：已记账凭证中库存现金、银行存款的发生额，按对方科目分类；一张凭证有多个对方科目时按金额比例分摊，
     * 只有现金科目之间的划转不计入
     */
    public FinAnalysis.CashFlow cashFlow(int begin, int end) {
        toYearMonth(begin);
        toYearMonth(end);
        if (end < begin) {
            throw new BizException("fin.error.endPeriodCannotEarlierStartPeriod");
        }
        // 货币资金、进项 / 销项税额按「凭证科目设置」取科目编码，其下级科目同样归类
        List<String> cash = accountService.cashCodes();
        String inputVat = accountService.roleCode(AccountRole.INPUT_VAT);
        String outputVat = accountService.roleCode(AccountRole.OUTPUT_VAT);
        Map<Long, List<FinAnalysis.CashEntry>> byVoucher = new LinkedHashMap<>();
        for (FinAnalysis.CashEntry e : analysisMapper.selectCashVoucherEntries(begin, end, cash)) {
            byVoucher.computeIfAbsent(e.getVoucherId(), k -> new ArrayList<>()).add(e);
        }
        Map<CashItem, BigDecimal> items = new EnumMap<>(CashItem.class);
        for (CashItem item : CashItem.values()) {
            items.put(item, BigDecimal.ZERO);
        }
        for (List<FinAnalysis.CashEntry> entries : byVoucher.values()) {
            BigDecimal cashNet = BigDecimal.ZERO;
            List<FinAnalysis.CashEntry> others = new ArrayList<>();
            for (FinAnalysis.CashEntry e : entries) {
                String code = e.getAccountCode();
                BigDecimal net = dec(e.getDebit()).subtract(dec(e.getCredit()));
                if (FinAccountService.inScope(code, cash)) {
                    cashNet = cashNet.add(net);
                } else {
                    others.add(e);
                }
            }
            if (cashNet.signum() == 0 || others.isEmpty()) {
                continue;
            }
            // 对方科目与现金反方向的金额作为分摊基数（现金流入时取对方贷方，流出时取对方借方）
            boolean inflow = cashNet.signum() > 0;
            BigDecimal base = BigDecimal.ZERO;
            for (FinAnalysis.CashEntry e : others) {
                base = base.add(side(e, inflow));
            }
            if (base.signum() == 0) {
                continue;
            }
            BigDecimal allocated = BigDecimal.ZERO;
            for (int i = 0; i < others.size(); i++) {
                FinAnalysis.CashEntry e = others.get(i);
                BigDecimal share = side(e, inflow);
                if (share.signum() == 0) {
                    continue;
                }
                BigDecimal amount = i == others.size() - 1 ? cashNet.abs().subtract(allocated)
                        : cashNet.abs().multiply(share).divide(base, Decimals.MONEY_SCALE, Decimals.ROUNDING);
                allocated = allocated.add(amount);
                CashItem item = classify(e.getAccountCode(), inflow, inputVat, outputVat);
                items.merge(item, amount, BigDecimal::add);
            }
        }
        List<FinAnalysis.CashFlowLine> lines = new ArrayList<>();
        BigDecimal netTotal = BigDecimal.ZERO;
        for (CashSection section : CashSection.values()) {
            BigDecimal in = BigDecimal.ZERO;
            BigDecimal out = BigDecimal.ZERO;
            lines.add(line(section.title, null, 0, true));
            for (CashItem item : CashItem.values()) {
                if (item.section != section) {
                    continue;
                }
                BigDecimal v = items.get(item);
                lines.add(line(item.label, v, 1, false));
                if (item.inflow) {
                    in = in.add(v);
                } else {
                    out = out.add(v);
                }
            }
            lines.add(line(section.inTotal, in, 1, true));
            lines.add(line(section.outTotal, out, 1, true));
            lines.add(line(section.net, in.subtract(out), 1, true));
            netTotal = netTotal.add(in.subtract(out));
        }
        BigDecimal opening = cashBalance(previous(begin), cash);
        BigDecimal closing = cashBalance(end, cash);
        lines.add(line("fin.label.ivNetIncreaseCashCashEquivalents", netTotal, 0, true));
        lines.add(line("fin.label.addCashCashEquivalentsBeginningPeriod", opening, 1, false));
        lines.add(line("fin.label.vCashCashEquivalentsEndPeriod", opening.add(netTotal), 0, true));
        return new FinAnalysis.CashFlow(lines, closing, opening.add(netTotal).compareTo(closing) == 0);
    }

    /** 现金流量项目 */
    private enum CashItem {
        SALES_IN(CashSection.OPERATING, true, "fin.label.cashReceivedSalesGoodsRenderingServices"),
        OTHER_OPERATING_IN(CashSection.OPERATING, true, "fin.label.otherCashReceivedRelatingOperatingActivities"),
        PURCHASE_OUT(CashSection.OPERATING, false, "fin.label.cashPaidGoodsServices"),
        SALARY_OUT(CashSection.OPERATING, false, "fin.label.cashPaidBehalfEmployees"),
        TAX_OUT(CashSection.OPERATING, false, "fin.label.taxesPaid"),
        OTHER_OPERATING_OUT(CashSection.OPERATING, false, "fin.label.otherCashPaidRelatingOperatingActivities"),
        ASSET_DISPOSAL_IN(CashSection.INVESTING, true, "fin.label.netCashReceivedDisposalFixedAssets"),
        ASSET_OUT(CashSection.INVESTING, false, "fin.label.cashPaidAcquireFixedAssetsIntangible"),
        CAPITAL_IN(CashSection.FINANCING, true, "fin.label.cashReceivedCapitalContributions"),
        BORROW_IN(CashSection.FINANCING, true, "fin.label.cashReceivedBorrowings"),
        REPAY_OUT(CashSection.FINANCING, false, "fin.label.cashRepaymentsDebts"),
        DIVIDEND_OUT(CashSection.FINANCING, false, "fin.label.cashPaidDividendsProfitDistributionInterest");

        private final CashSection section;
        private final boolean inflow;
        private final String label;

        CashItem(CashSection section, boolean inflow, String label) {
            this.section = section;
            this.inflow = inflow;
            this.label = label;
        }
    }

    /**
     * 按对方科目归类现金流量项目。除进项、销项税额（取凭证科目设置）外，按企业会计准则一级科目编码前缀归类，
     * 与资产负债表、利润表的报表模板（FinFinancialStatementService）一样属于报表格式，不随凭证科目设置变化
     */
    private static CashItem classify(String code, boolean inflow, String inputVat, String outputVat) {
        if (code.startsWith("2001")) {
            return inflow ? CashItem.BORROW_IN : CashItem.REPAY_OUT;
        }
        if (code.startsWith("4001")) {
            return inflow ? CashItem.CAPITAL_IN : CashItem.DIVIDEND_OUT;
        }
        if (code.startsWith("6603") || code.startsWith("4104")) {
            return inflow ? CashItem.OTHER_OPERATING_IN : CashItem.DIVIDEND_OUT;
        }
        if (code.startsWith("1601") || code.startsWith("1606") || code.startsWith("1604")) {
            return inflow ? CashItem.ASSET_DISPOSAL_IN : CashItem.ASSET_OUT;
        }
        if (inflow) {
            return code.startsWith("1122") || code.startsWith("2203") || code.startsWith("6001") || code.startsWith("6051")
                    || code.startsWith(outputVat) ? CashItem.SALES_IN : CashItem.OTHER_OPERATING_IN;
        }
        if (code.startsWith("2202") || code.startsWith("1123") || code.startsWith("14") || code.startsWith("5001")
                || code.startsWith("5101") || code.startsWith(inputVat)) {
            return CashItem.PURCHASE_OUT;
        }
        if (code.startsWith("2211")) {
            return CashItem.SALARY_OUT;
        }
        if (code.startsWith("2221") || code.startsWith("6403") || code.startsWith("6801")) {
            return CashItem.TAX_OUT;
        }
        return CashItem.OTHER_OPERATING_OUT;
    }

    /** 现金流量表的三类活动：标题、流入小计、流出小计、净额的行名 */
    private enum CashSection {
        OPERATING("fin.label.iCashFlowsOperatingActivities", "fin.label.subtotalCashInflowsOperatingActivities", "fin.label.subtotalCashOutflowsOperatingActivities", "fin.label.netCashFlowsOperatingActivities"),
        INVESTING("fin.label.iiCashFlowsInvestingActivities", "fin.label.subtotalCashInflowsInvestingActivities", "fin.label.subtotalCashOutflowsInvestingActivities", "fin.label.netCashFlowsInvestingActivities"),
        FINANCING("fin.label.iiiCashFlowsFinancingActivities", "fin.label.subtotalCashInflowsFinancingActivities", "fin.label.subtotalCashOutflowsFinancingActivities", "fin.label.netCashFlowsFinancingActivities");

        private final String title;
        private final String inTotal;
        private final String outTotal;
        private final String net;

        CashSection(String title, String inTotal, String outTotal, String net) {
            this.title = title;
            this.inTotal = inTotal;
            this.outTotal = outTotal;
            this.net = net;
        }
    }

    private BigDecimal cashBalance(int period, List<String> cash) {
        BigDecimal sum = BigDecimal.ZERO;
        for (FinAccount a : accountService.listAll()) {
            if (Boolean.TRUE.equals(a.getLeaf()) && FinAccountService.inScope(a.getCode(), cash)) {
                sum = sum.add(entryMapper.sumBalanceUpTo(a.getId(), period));
            }
        }
        return sum;
    }

    private static BigDecimal side(FinAnalysis.CashEntry e, boolean inflow) {
        return inflow ? dec(e.getCredit()) : dec(e.getDebit());
    }

    /** 报表行：行名返回给前端显示，按请求语言翻译 */
    private static FinAnalysis.CashFlowLine line(String name, BigDecimal amount, int level, boolean bold) {
        return new FinAnalysis.CashFlowLine(Messages.get(name), amount, level, bold);
    }

    private static BigDecimal margin(BigDecimal revenue, BigDecimal cost) {
        return Decimals.percent(revenue.subtract(cost), revenue, 2);
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
