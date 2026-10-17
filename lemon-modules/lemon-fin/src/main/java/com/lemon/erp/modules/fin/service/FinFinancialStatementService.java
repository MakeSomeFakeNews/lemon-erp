package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.fin.dto.AccountSum;
import com.lemon.erp.modules.fin.dto.FinancialStatement;
import com.lemon.erp.modules.fin.dto.FinancialMetric;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.mapper.FinVoucherEntryMapper;
import com.lemon.erp.modules.fin.model.AccountCategory;
import com.lemon.erp.modules.fin.model.AccountDirection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.fin.model.FinConstants.toYearMonth;

/**
 * 财务报表：格式参照《小企业会计准则》（会小企01表、会小企02表），只统计已记账凭证。
 * <ul>
 *   <li>资产负债表：期末余额、年初余额，左右两栏；未分配利润含尚未结转的损益，成本类科目余额计入存货</li>
 *   <li>利润表：本月金额、本年累计、上年同期累计，不含结转损益凭证；未列示的损益科目按方向计入营业外收入 / 支出</li>
 *   <li>每行附构成科目，可从报表数字追溯到科目、明细账；附常用财务指标</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FinFinancialStatementService {


    private static final FinancialTemplate ASSETS = new FinancialTemplate().side(false)
            .title("fin.label.currentAssets")
            .acc("cash", "fin.label.cashCashEquivalents", "1001", "1002", "1012")
            .acc("shortInvest", "fin.label.shortTermInvestments", "1101")
            .acc("noteReceivable", "fin.label.notesReceivable", "1121")
            .acc("receivable", "fin.label.accountsReceivable", "1122")
            .acc("prepaid", "fin.label.prepayments", "1123")
            .acc("dividendReceivable", "fin.label.dividendsReceivable", "1131")
            .acc("interestReceivable", "fin.label.interestReceivable", "1132")
            .acc("otherReceivable", "fin.label.otherReceivables", "1221")
            .acc("inventory", "fin.label.inventories", "140", "141", "142", "5", "4301").catchAll(AccountCategory.COST)
            .note("fin.label.includesRawMaterialsFinishedGoodsTurnover")
            .sub("raw", "fin.label.includingRawMaterials", "1403")
            .sub("wip", "fin.label.workProcess", "5001")
            .sub("goods", "fin.label.finishedGoods", "1405")
            .sub("turnover", "fin.label.turnoverMaterials", "1411")
            .acc("otherCurrentAsset", "fin.label.otherCurrentAssets").catchAll(AccountCategory.ASSET)
            .total("currentAsset", "fin.label.totalCurrentAssets", 0, true, "cash", "shortInvest", "noteReceivable", "receivable", "prepaid",
                    "dividendReceivable", "interestReceivable", "otherReceivable", "inventory", "otherCurrentAsset")
            .title("fin.label.nonCurrentAssets")
            .acc("bondInvest", "fin.label.longTermBondInvestments", "1501")
            .acc("equityInvest", "fin.label.longTermEquityInvestments", "1511")
            .acc("fixedAssetCost", "fin.label.fixedAssetsCost", "1601")
            .acc("depreciation", "fin.label.lessAccumulatedDepreciation", 1, false, true, "1602")
            .total("fixedAsset", "fin.label.fixedAssetsCarryingAmount", 1, false, "fixedAssetCost", "-depreciation")
            .acc("construction", "fin.label.constructionProgress", "1604")
            .acc("constructionMaterial", "fin.label.constructionMaterials", "1605")
            .acc("fixedAssetClear", "fin.label.fixedAssetsPendingDisposal", "1606")
            .acc("bioAsset", "fin.label.productiveBiologicalAssets", "1621", "1622")
            .acc("intangible", "fin.label.intangibleAssets", "1701", "1702")
            .acc("longPrepaid", "fin.label.longTermPrepaidExpenses", "1801")
            .acc("otherNonCurrentAsset", "fin.label.otherNonCurrentAssets")
            .total("nonCurrentAsset", "fin.label.totalNonCurrentAssets", 0, true, "bondInvest", "equityInvest", "fixedAsset", "construction",
                    "constructionMaterial", "fixedAssetClear", "bioAsset", "intangible", "longPrepaid", "otherNonCurrentAsset")
            .total("assetTotal", "fin.label.totalAssets", 0, true, "currentAsset", "nonCurrentAsset");

    private static final FinancialTemplate EQUITIES = new FinancialTemplate().side(true)
            .title("fin.label.currentLiabilities")
            .acc("shortLoan", "fin.label.shortTermBorrowings", "2001")
            .acc("notePayable", "fin.label.notesPayable", "2201")
            .acc("payable", "fin.label.accountsPayable", "2202")
            .acc("advance", "fin.label.advancesCustomers", "2203", "2205")
            .acc("salary", "fin.label.employeeBenefitsPayable", "2211")
            .acc("tax", "fin.label.taxesPayable", "2221")
            .acc("interestPayable", "fin.label.interestPayable", "2231")
            .acc("profitPayable", "fin.label.profitsPayable", "2232")
            .acc("otherPayable", "fin.label.otherPayables", "2241")
            .acc("otherCurrentLiability", "fin.label.otherCurrentLiabilities").catchAll(AccountCategory.LIABILITY)
            .total("currentLiability", "fin.label.totalCurrentLiabilities", 0, true, "shortLoan", "notePayable", "payable", "advance", "salary", "tax",
                    "interestPayable", "profitPayable", "otherPayable", "otherCurrentLiability")
            .title("fin.label.nonCurrentLiabilities")
            .acc("longLoan", "fin.label.longTermBorrowings", "2501")
            .acc("longPayable", "fin.label.longTermPayables", "2701")
            .acc("deferredIncome", "fin.label.deferredIncome", "2401")
            .acc("otherNonCurrentLiability", "fin.label.otherNonCurrentLiabilities")
            .total("nonCurrentLiability", "fin.label.totalNonCurrentLiabilities", 0, true, "longLoan", "longPayable", "deferredIncome", "otherNonCurrentLiability")
            .total("liabilityTotal", "fin.label.totalLiabilities", 0, true, "currentLiability", "nonCurrentLiability")
            .title("fin.label.ownersEquityShareholdersEquity")
            .acc("capital", "fin.label.paidCapitalShareCapital", "4001")
            .acc("capitalReserve", "fin.label.capitalReserve", "4002")
            .acc("surplusReserve", "fin.label.surplusReserve", "4101")
            .acc("retained", "fin.label.retainedEarnings", "4103", "4104", "6").catchAll(AccountCategory.EQUITY, AccountCategory.PROFIT)
            .note("fin.label.includesProfitLossNotYetTransferred")
            .total("equityTotal", "fin.label.totalOwnersEquity", 0, true, "capital", "capitalReserve", "surplusReserve", "retained")
            .total("total", "fin.label.totalLiabilitiesOwnersEquity", 0, true, "liabilityTotal", "equityTotal");

    private static final FinancialTemplate INCOME = new FinancialTemplate()
            .acc("revenue", "fin.label.iOperatingRevenue", 0, true, true, "6001", "6051")
            .side(false)
            .acc("cost", "fin.label.lessOperatingCosts", "6401", "6402")
            .acc("tax", "fin.label.taxesSurcharges", "6403")
            .acc("selling", "fin.label.sellingExpenses", "6601")
            .acc("admin", "fin.label.administrativeExpenses", "6602")
            .acc("finance", "fin.label.financialExpenses", "6603")
            .acc("invest", "fin.label.addInvestmentIncome", 1, false, true, "6111")
            .total("operating", "fin.label.iiOperatingProfit", 0, true, "revenue", "-cost", "-tax", "-selling", "-admin", "-finance", "invest")
            .acc("nonOpIncome", "fin.label.addNonOperatingIncome", 1, false, true, "6301")
            .catchAll(AccountCategory.PROFIT).catchDirection(AccountDirection.CREDIT)
            .acc("nonOpExpense", "fin.label.lessNonOperatingExpenses", "6711")
            .catchAll(AccountCategory.PROFIT).catchDirection(AccountDirection.DEBIT)
            .total("profitTotal", "fin.label.iiiTotalProfit", 0, true, "operating", "nonOpIncome", "-nonOpExpense")
            .acc("incomeTax", "fin.label.lessIncomeTaxExpense", "6801")
            .total("net", "fin.label.ivNetProfit", 0, true, "profitTotal", "-incomeTax");

    private final FinVoucherEntryMapper entryMapper;
    private final FinAccountService accountService;

    /** 资产负债表：period 期末、年初余额 */
    public FinancialStatement balanceSheet(int period) {
        toYearMonth(period);
        List<FinAccount> leaves = leaves();
        List<Map<Long, AccountSum>> columns = List.of(
                toMap(entryMapper.sumByAccount(null, period, false)),
                toMap(entryMapper.sumByAccount(null, (period / 100 - 1) * 100 + 12, false)));
        FinancialTemplate.Result left = ASSETS.evaluate(leaves, columns, 1);
        FinancialTemplate.Result right = EQUITIES.evaluate(leaves, columns, left.nextLineNo());
        boolean balanced = true;
        for (int i = 0; i < columns.size(); i++) {
            balanced &= left.get("assetTotal", i).compareTo(right.get("total", i)) == 0;
        }
        List<FinancialMetric> metrics = List.of(
                metric("fin.label.debtAssetRatio", right, "liabilityTotal", left, "assetTotal", true, "fin.label.totalLiabilitiesTotalAssets"),
                ratio("fin.label.currentRatio", left, right, false, "fin.label.totalCurrentAssetsTotalCurrentLiabilities"),
                ratio("fin.label.quickRatio", left, right, true, "fin.label.totalCurrentAssetsInventoriesTotalCurrent"));
        return new FinancialStatement(Messages.get("fin.text.balanceSheet"), Messages.get("fin.text.formSe"), List.of(Messages.get("fin.text.closingBalance"), Messages.get("fin.text.beginningYearBalance")), left.rows(), right.rows(),
                metrics, balanced);
    }

    /** 利润表：本月金额、本年累计、上年同期累计 */
    public FinancialStatement incomeStatement(int period) {
        toYearMonth(period);
        int year = period / 100;
        int month = period % 100;
        List<Map<Long, AccountSum>> columns = List.of(
                toMap(entryMapper.sumByAccount(period, period, true)),
                toMap(entryMapper.sumByAccount(year * 100 + 1, period, true)),
                toMap(entryMapper.sumByAccount((year - 1) * 100 + 1, (year - 1) * 100 + month, true)));
        FinancialTemplate.Result r = INCOME.evaluate(profitLeaves(), columns, 1);
        List<FinancialMetric> metrics = List.of(
                incomeMetric("fin.label.grossMargin", r, "fin.label.operatingRevenueOperatingCostsOperatingRevenue", "revenue", "-cost"),
                incomeMetric("fin.label.operatingMargin", r, "fin.label.operatingProfitOperatingRevenue", "operating"),
                incomeMetric("fin.label.netMargin", r, "fin.label.netProfitOperatingRevenue", "net"),
                incomeMetric("fin.label.periodExpenseRatio", r, "fin.label.sellingAdministrativeFinancialExpensesOperatingRevenue", "selling", "admin", "finance"));
        return new FinancialStatement(Messages.get("fin.text.incomeStatement"), Messages.get("fin.text.formSe2"), List.of(Messages.get("fin.text.currentMonth"), Messages.get("fin.text.yearDate"), Messages.get("fin.text.priorYearDate")),
                r.rows(), null, metrics, null);
    }

    /** 本年各月（截至 period）的营业收入、营业成本、营业利润、净利润 */
    public List<Map<String, Object>> incomeTrend(int period) {
        toYearMonth(period);
        int first = period / 100 * 100 + 1;
        Map<Integer, Map<Long, AccountSum>> byPeriod = new HashMap<>();
        for (AccountSum s : entryMapper.sumByAccountPeriod(first, period)) {
            byPeriod.computeIfAbsent(s.getPeriod(), k -> new HashMap<>()).put(s.getAccountId(), s);
        }
        List<FinAccount> leaves = profitLeaves();
        List<Map<String, Object>> list = new ArrayList<>();
        for (int p = first; p <= period; p++) {
            FinancialTemplate.Result r = INCOME.evaluate(leaves, List.of(byPeriod.getOrDefault(p, Map.of())), 1);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("period", p);
            m.put("revenue", r.get("revenue", 0));
            m.put("cost", r.get("cost", 0));
            m.put("operating", r.get("operating", 0));
            m.put("net", r.get("net", 0));
            list.add(m);
        }
        return list;
    }

    // ---------------------------------------------------------------- 指标（名称、单位、说明返回给前端显示，按请求语言翻译）

    private static FinancialMetric metric(String name, FinancialTemplate.Result a, String aKey, FinancialTemplate.Result b, String bKey,
                                          boolean percent, String hint) {
        return new FinancialMetric(Messages.get(name), divide(a.get(aKey, 0), b.get(bKey, 0), percent), divide(a.get(aKey, 1), b.get(bKey, 1), percent),
                percent ? "%" : Messages.get("fin.text.x"), Messages.get(hint));
    }

    private static FinancialMetric ratio(String name, FinancialTemplate.Result left, FinancialTemplate.Result right, boolean quick, String hint) {
        BigDecimal[] v = new BigDecimal[2];
        for (int i = 0; i < 2; i++) {
            BigDecimal current = left.get("currentAsset", i);
            if (quick) {
                current = current.subtract(left.get("inventory", i));
            }
            v[i] = divide(current, right.get("currentLiability", i), false);
        }
        return new FinancialMetric(Messages.get(name), v[0], v[1], Messages.get("fin.text.x"), Messages.get(hint));
    }

    private static FinancialMetric incomeMetric(String name, FinancialTemplate.Result r, String hint, String... keys) {
        BigDecimal[] v = new BigDecimal[2];
        for (int i = 0; i < 2; i++) {
            BigDecimal x = BigDecimal.ZERO;
            for (String k : keys) {
                x = k.startsWith("-") ? x.subtract(r.get(k.substring(1), i)) : x.add(r.get(k, i));
            }
            v[i] = divide(x, r.get("revenue", i), true);
        }
        return new FinancialMetric(Messages.get(name), v[0], v[1], "%", Messages.get(hint));
    }

    /** 分母为 0 时为空 */
    private static BigDecimal divide(BigDecimal a, BigDecimal b, boolean percent) {
        if (b.signum() == 0) {
            return null;
        }
        return (percent ? a.multiply(Decimals.HUNDRED) : a).divide(b, 2, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------- 工具

    private List<FinAccount> leaves() {
        return accountService.listAll().stream().filter(FinAccount::getLeaf).toList();
    }

    private List<FinAccount> profitLeaves() {
        return accountService.listAll().stream()
                .filter(a -> a.getLeaf() && a.getCategory() == AccountCategory.PROFIT).toList();
    }

    private static Map<Long, AccountSum> toMap(List<AccountSum> list) {
        return list.stream().collect(Collectors.toMap(AccountSum::getAccountId, s -> s, (a, b) -> a, HashMap::new));
    }
}
