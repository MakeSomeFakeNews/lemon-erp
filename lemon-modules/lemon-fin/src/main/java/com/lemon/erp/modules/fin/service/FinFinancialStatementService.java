package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.i18n.I18n;
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

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static final FinancialTemplate ASSETS = new FinancialTemplate().side(false)
            .title("流动资产：")
            .acc("cash", "货币资金", "1001", "1002", "1012")
            .acc("shortInvest", "短期投资", "1101")
            .acc("noteReceivable", "应收票据", "1121")
            .acc("receivable", "应收账款", "1122")
            .acc("prepaid", "预付账款", "1123")
            .acc("dividendReceivable", "应收股利", "1131")
            .acc("interestReceivable", "应收利息", "1132")
            .acc("otherReceivable", "其他应收款", "1221")
            .acc("inventory", "存货", "140", "141", "142", "5", "4301").catchAll(AccountCategory.COST)
            .note("含原材料、库存商品、周转材料、委托加工物资，以及生产成本、制造费用等成本类科目的余额")
            .sub("raw", "其中：原材料", "1403")
            .sub("wip", "在产品", "5001")
            .sub("goods", "库存商品", "1405")
            .sub("turnover", "周转材料", "1411")
            .acc("otherCurrentAsset", "其他流动资产").catchAll(AccountCategory.ASSET)
            .total("currentAsset", "流动资产合计", 0, true, "cash", "shortInvest", "noteReceivable", "receivable", "prepaid",
                    "dividendReceivable", "interestReceivable", "otherReceivable", "inventory", "otherCurrentAsset")
            .title("非流动资产：")
            .acc("bondInvest", "长期债券投资", "1501")
            .acc("equityInvest", "长期股权投资", "1511")
            .acc("fixedAssetCost", "固定资产原价", "1601")
            .acc("depreciation", "减：累计折旧", 1, false, true, "1602")
            .total("fixedAsset", "固定资产账面价值", 1, false, "fixedAssetCost", "-depreciation")
            .acc("construction", "在建工程", "1604")
            .acc("constructionMaterial", "工程物资", "1605")
            .acc("fixedAssetClear", "固定资产清理", "1606")
            .acc("bioAsset", "生产性生物资产", "1621", "1622")
            .acc("intangible", "无形资产", "1701", "1702")
            .acc("longPrepaid", "长期待摊费用", "1801")
            .acc("otherNonCurrentAsset", "其他非流动资产")
            .total("nonCurrentAsset", "非流动资产合计", 0, true, "bondInvest", "equityInvest", "fixedAsset", "construction",
                    "constructionMaterial", "fixedAssetClear", "bioAsset", "intangible", "longPrepaid", "otherNonCurrentAsset")
            .total("assetTotal", "资产总计", 0, true, "currentAsset", "nonCurrentAsset");

    private static final FinancialTemplate EQUITIES = new FinancialTemplate().side(true)
            .title("流动负债：")
            .acc("shortLoan", "短期借款", "2001")
            .acc("notePayable", "应付票据", "2201")
            .acc("payable", "应付账款", "2202")
            .acc("advance", "预收账款", "2203", "2205")
            .acc("salary", "应付职工薪酬", "2211")
            .acc("tax", "应交税费", "2221")
            .acc("interestPayable", "应付利息", "2231")
            .acc("profitPayable", "应付利润", "2232")
            .acc("otherPayable", "其他应付款", "2241")
            .acc("otherCurrentLiability", "其他流动负债").catchAll(AccountCategory.LIABILITY)
            .total("currentLiability", "流动负债合计", 0, true, "shortLoan", "notePayable", "payable", "advance", "salary", "tax",
                    "interestPayable", "profitPayable", "otherPayable", "otherCurrentLiability")
            .title("非流动负债：")
            .acc("longLoan", "长期借款", "2501")
            .acc("longPayable", "长期应付款", "2701")
            .acc("deferredIncome", "递延收益", "2401")
            .acc("otherNonCurrentLiability", "其他非流动负债")
            .total("nonCurrentLiability", "非流动负债合计", 0, true, "longLoan", "longPayable", "deferredIncome", "otherNonCurrentLiability")
            .total("liabilityTotal", "负债合计", 0, true, "currentLiability", "nonCurrentLiability")
            .title("所有者权益（或股东权益）：")
            .acc("capital", "实收资本（或股本）", "4001")
            .acc("capitalReserve", "资本公积", "4002")
            .acc("surplusReserve", "盈余公积", "4101")
            .acc("retained", "未分配利润", "4103", "4104", "6").catchAll(AccountCategory.EQUITY, AccountCategory.PROFIT)
            .note("含尚未结转到本年利润的损益")
            .total("equityTotal", "所有者权益合计", 0, true, "capital", "capitalReserve", "surplusReserve", "retained")
            .total("total", "负债和所有者权益总计", 0, true, "liabilityTotal", "equityTotal");

    private static final FinancialTemplate INCOME = new FinancialTemplate()
            .acc("revenue", "一、营业收入", 0, true, true, "6001", "6051")
            .side(false)
            .acc("cost", "减：营业成本", "6401", "6402")
            .acc("tax", "税金及附加", "6403")
            .acc("selling", "销售费用", "6601")
            .acc("admin", "管理费用", "6602")
            .acc("finance", "财务费用", "6603")
            .acc("invest", "加：投资收益", 1, false, true, "6111")
            .total("operating", "二、营业利润", 0, true, "revenue", "-cost", "-tax", "-selling", "-admin", "-finance", "invest")
            .acc("nonOpIncome", "加：营业外收入", 1, false, true, "6301")
            .catchAll(AccountCategory.PROFIT).catchDirection(AccountDirection.CREDIT)
            .acc("nonOpExpense", "减：营业外支出", "6711")
            .catchAll(AccountCategory.PROFIT).catchDirection(AccountDirection.DEBIT)
            .total("profitTotal", "三、利润总额", 0, true, "operating", "nonOpIncome", "-nonOpExpense")
            .acc("incomeTax", "减：所得税费用", "6801")
            .total("net", "四、净利润", 0, true, "profitTotal", "-incomeTax");

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
                metric("资产负债率", right, "liabilityTotal", left, "assetTotal", true, "负债合计 ÷ 资产总计"),
                ratio("流动比率", left, right, false, "流动资产合计 ÷ 流动负债合计"),
                ratio("速动比率", left, right, true, "（流动资产合计 - 存货）÷ 流动负债合计"));
        return new FinancialStatement(I18n.t("资产负债表"), I18n.t("会小企01表"), List.of(I18n.t("期末余额"), I18n.t("年初余额")), left.rows(), right.rows(),
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
                incomeMetric("毛利率", r, "（营业收入 - 营业成本）÷ 营业收入", "revenue", "-cost"),
                incomeMetric("营业利润率", r, "营业利润 ÷ 营业收入", "operating"),
                incomeMetric("净利率", r, "净利润 ÷ 营业收入", "net"),
                incomeMetric("期间费用率", r, "（销售费用 + 管理费用 + 财务费用）÷ 营业收入", "selling", "admin", "finance"));
        return new FinancialStatement(I18n.t("利润表"), I18n.t("会小企02表"), List.of(I18n.t("本月金额"), I18n.t("本年累计金额"), I18n.t("上年同期累计")),
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
        return new FinancialMetric(I18n.t(name), divide(a.get(aKey, 0), b.get(bKey, 0), percent), divide(a.get(aKey, 1), b.get(bKey, 1), percent),
                percent ? "%" : I18n.t("倍"), I18n.t(hint));
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
        return new FinancialMetric(I18n.t(name), v[0], v[1], I18n.t("倍"), I18n.t(hint));
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
        return new FinancialMetric(I18n.t(name), v[0], v[1], "%", I18n.t(hint));
    }

    /** 分母为 0 时为空 */
    private static BigDecimal divide(BigDecimal a, BigDecimal b, boolean percent) {
        if (b.signum() == 0) {
            return null;
        }
        return (percent ? a.multiply(HUNDRED) : a).divide(b, 2, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------- 工具

    private List<FinAccount> leaves() {
        return accountService.listAll().stream().filter(FinAccount::getLeaf).toList();
    }

    private List<FinAccount> profitLeaves() {
        return accountService.listAll().stream()
                .filter(a -> a.getLeaf() && AccountCategory.PROFIT.name().equals(a.getCategory())).toList();
    }

    private static Map<Long, AccountSum> toMap(List<AccountSum> list) {
        return list.stream().collect(Collectors.toMap(AccountSum::getAccountId, s -> s, (a, b) -> a, HashMap::new));
    }
}
