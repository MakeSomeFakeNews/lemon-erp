package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.modules.fin.dto.AccountSum;
import com.lemon.erp.modules.fin.dto.FinancialRowAccount;
import com.lemon.erp.modules.fin.dto.FinancialRow;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.model.AccountCategory;
import com.lemon.erp.modules.fin.model.AccountDirection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 报表模板：按行定义取数规则，计算各列金额。
 * <ul>
 *   <li>科目行：编码以所列前缀开头的末级科目，按行的方向取余额（借方行 = 借 - 贷，贷方行 = 贷 - 借）；
 *       每个科目只计入第一个匹配的科目行，所以备抵科目（累计折旧等）写在主科目之后也不会重复</li>
 *   <li>兜底：科目行可以收纳某些类别中没有被任何行匹配的科目（如其他流动资产），保证报表平衡</li>
 *   <li>其中项：只展示，不占用科目</li>
 *   <li>合计行：其他行相加减</li>
 * </ul>
 */
final class FinancialTemplate {

    enum Kind { TITLE, ACCOUNT, SUB, TOTAL }

    static final class Row {
        final Kind kind;
        final String key;
        final String name;
        final int level;
        final boolean bold;
        final boolean credit;
        final List<String> prefixes;
        final List<String> sumKeys;
        final Set<AccountCategory> catchCategories = new HashSet<>();
        AccountDirection catchDirection;
        String note;

        Row(Kind kind, String key, String name, int level, boolean bold, boolean credit, List<String> prefixes, List<String> sumKeys) {
            this.kind = kind;
            this.key = key;
            this.name = name;
            this.level = level;
            this.bold = bold;
            this.credit = credit;
            this.prefixes = prefixes;
            this.sumKeys = sumKeys;
        }
    }

    /** 计算结果：报表行与按行标识取值（用于指标） */
    record Result(List<FinancialRow> rows, Map<String, BigDecimal[]> values, int nextLineNo) {

        BigDecimal get(String key, int column) {
            BigDecimal[] v = values.get(key);
            return v == null ? BigDecimal.ZERO : v[column];
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private boolean credit;

    /** 之后添加的科目行的方向 */
    FinancialTemplate side(boolean credit) {
        this.credit = credit;
        return this;
    }

    FinancialTemplate title(String name) {
        rows.add(new Row(Kind.TITLE, null, name, 0, true, credit, List.of(), List.of()));
        return this;
    }

    FinancialTemplate acc(String key, String name, String... prefixes) {
        return acc(key, name, 1, false, credit, prefixes);
    }

    FinancialTemplate acc(String key, String name, int level, boolean bold, boolean credit, String... prefixes) {
        rows.add(new Row(Kind.ACCOUNT, key, name, level, bold, credit, List.of(prefixes), List.of()));
        return this;
    }

    FinancialTemplate sub(String key, String name, String... prefixes) {
        rows.add(new Row(Kind.SUB, key, name, 2, false, credit, List.of(prefixes), List.of()));
        return this;
    }

    /** 合计行：keys 中以 - 开头的表示减去 */
    FinancialTemplate total(String key, String name, int level, boolean bold, String... keys) {
        rows.add(new Row(Kind.TOTAL, key, name, level, bold, credit, List.of(), List.of(keys)));
        return this;
    }

    /** 上一行收纳这些类别中未被匹配的科目 */
    FinancialTemplate catchAll(AccountCategory... categories) {
        last().catchCategories.addAll(Arrays.asList(categories));
        return this;
    }

    /** 上一行只收纳该余额方向的未匹配科目 */
    FinancialTemplate catchDirection(AccountDirection direction) {
        last().catchDirection = direction;
        return this;
    }

    /** 上一行的取数说明补充 */
    FinancialTemplate note(String note) {
        last().note = note;
        return this;
    }

    private Row last() {
        return rows.get(rows.size() - 1);
    }

    /**
     * @param leaves      末级科目
     * @param columns     各列的科目发生额（已按期间汇总）
     * @param firstLineNo 第一行的行次
     */
    Result evaluate(List<FinAccount> leaves, List<Map<Long, AccountSum>> columns, int firstLineNo) {
        int n = columns.size();
        Set<Long> used = new HashSet<>();
        Map<Row, List<FinAccount>> matched = new HashMap<>();
        // 第一遍：按前缀匹配，先到先得
        for (Row row : rows) {
            if (row.kind == Kind.ACCOUNT) {
                List<FinAccount> list = new ArrayList<>();
                for (FinAccount a : leaves) {
                    if (!used.contains(a.getId()) && row.prefixes.stream().anyMatch(p -> a.getCode().startsWith(p))) {
                        used.add(a.getId());
                        list.add(a);
                    }
                }
                matched.put(row, list);
            }
        }
        // 第二遍：兜底收纳未匹配的科目
        for (Row row : rows) {
            if (row.kind == Kind.ACCOUNT && !row.catchCategories.isEmpty()) {
                for (FinAccount a : leaves) {
                    if (!used.contains(a.getId()) && row.catchCategories.stream().anyMatch(c -> c.name().equals(a.getCategory()))
                            && (row.catchDirection == null || row.catchDirection.name().equals(a.getDirection()))) {
                        used.add(a.getId());
                        matched.get(row).add(a);
                    }
                }
            }
        }
        Map<String, BigDecimal[]> values = new LinkedHashMap<>();
        Map<String, Integer> lineNos = new HashMap<>();
        List<FinancialRow> result = new ArrayList<>();
        int lineNo = firstLineNo;
        // 行名、取数说明返回给前端显示，按请求语言翻译（计算只用 key，不依赖名称）
        for (Row row : rows) {
            if (row.kind == Kind.TITLE) {
                result.add(new FinancialRow(null, I18n.t(row.name), row.level, true, true, null, null, null));
                continue;
            }
            BigDecimal[] v = zeros(n);
            List<FinancialRowAccount> accounts = null;
            String formula;
            if (row.kind == Kind.TOTAL) {
                List<String> parts = new ArrayList<>();
                for (String k : row.sumKeys) {
                    boolean minus = k.startsWith("-");
                    String key = minus ? k.substring(1) : k;
                    BigDecimal[] x = values.get(key);
                    for (int i = 0; i < n; i++) {
                        v[i] = minus ? v[i].subtract(x[i]) : v[i].add(x[i]);
                    }
                    parts.add((parts.isEmpty() ? (minus ? "-" : "") : (minus ? " - " : " + ")) + lineNos.get(key));
                }
                formula = I18n.t("行 {0}", String.join("", parts));
            } else {
                List<FinAccount> list = row.kind == Kind.SUB
                        ? leaves.stream().filter(a -> row.prefixes.stream().anyMatch(p -> a.getCode().startsWith(p))).toList()
                        : matched.get(row);
                accounts = new ArrayList<>();
                for (FinAccount a : list.stream().sorted(Comparator.comparing(FinAccount::getCode)).toList()) {
                    List<BigDecimal> av = new ArrayList<>(n);
                    boolean nonZero = false;
                    for (int i = 0; i < n; i++) {
                        BigDecimal x = net(columns.get(i).get(a.getId()), row.credit);
                        av.add(x);
                        v[i] = v[i].add(x);
                        nonZero |= x.signum() != 0;
                    }
                    if (nonZero) {
                        accounts.add(new FinancialRowAccount(a.getId(), a.getCode(), a.getName(), av));
                    }
                }
                formula = formula(row);
            }
            values.put(row.key, v);
            lineNos.put(row.key, lineNo);
            result.add(new FinancialRow(lineNo++, I18n.t(row.name), row.level, row.bold, false, formula, List.of(v), accounts));
        }
        return new Result(result, values, lineNo);
    }

    private static String formula(Row row) {
        boolean zh = I18n.isDefault();
        String listSep = zh ? "、" : ", ";
        StringBuilder sb = new StringBuilder();
        if (!row.prefixes.isEmpty()) {
            String codes = String.join(listSep, row.prefixes);
            sb.append(row.credit ? I18n.t("科目 {0} 的贷方余额", codes) : I18n.t("科目 {0} 的借方余额", codes));
        }
        if (!row.catchCategories.isEmpty()) {
            String categories = row.catchCategories.stream().sorted().map(c -> I18n.t(categoryLabel(c))).collect(Collectors.joining(listSep));
            sb.append(sb.isEmpty() ? "" : zh ? "，" : ", ");
            if (row.catchDirection == null) {
                sb.append(I18n.t("以及未列示的其他{0}科目", categories));
            } else if (row.catchDirection == AccountDirection.CREDIT) {
                sb.append(I18n.t("以及未列示的其他{0}科目（贷方科目）", categories));
            } else {
                sb.append(I18n.t("以及未列示的其他{0}科目（借方科目）", categories));
            }
        }
        if (row.note != null) {
            sb.append(sb.isEmpty() ? "" : zh ? "；" : "; ").append(I18n.t(row.note));
        }
        return sb.toString();
    }

    private static String categoryLabel(AccountCategory c) {
        return switch (c) {
            case ASSET -> "资产类";
            case LIABILITY -> "负债类";
            case EQUITY -> "所有者权益类";
            case COST -> "成本类";
            case PROFIT -> "损益类";
        };
    }

    /** 借方行 = 借 - 贷，贷方行 = 贷 - 借 */
    private static BigDecimal net(AccountSum s, boolean credit) {
        if (s == null) {
            return BigDecimal.ZERO;
        }
        return credit ? s.getCredit().subtract(s.getDebit()) : s.getDebit().subtract(s.getCredit());
    }

    private static BigDecimal[] zeros(int n) {
        BigDecimal[] a = new BigDecimal[n];
        Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }
}
