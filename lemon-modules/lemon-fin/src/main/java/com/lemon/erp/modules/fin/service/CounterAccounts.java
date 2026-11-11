package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.dto.CashierViews;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 日记账的对方科目：按分录配对，而不是取整张凭证的其他科目。规则：
 * <ol>
 *     <li>分组：凭证分录按行号顺序累计「借 - 贷」，每回到 0 就结束一组。一张凭证中依次录入的多笔业务各自成组
 *     （如账户调拨的本金 借 转入账户 / 贷 转出账户，和手续费 借 财务费用 / 贷 支付账户）；普通的一借多贷、多借一贷只有一组；</li>
 *     <li>对方科目是同组中与本行方向相反的分录科目（一借多贷时借方行显示全部贷方科目，每个贷方行显示借方科目）；</li>
 *     <li>同组两边都有多行（多借多贷）时，本行金额与对方恰好一行相等、且本方没有其他同金额的行，则一一对应只显示这一行；</li>
 *     <li>组内没有反方向分录（如同方向红蓝冲销）时退回整张凭证中反方向的分录，再没有则为凭证中的其他分录。</li>
 * </ol>
 * 方向按借方金额是否为 0 区分（红字负数仍按所在方向），科目名称去重后用顿号连接
 */
final class CounterAccounts {

    private CounterAccounts() {
    }

    /**
     * @param entries 凭证分录，同一凭证按行号排列
     * @return 分录 ID → 对方科目
     */
    static Map<Long, String> of(List<CashierViews.VoucherLine> entries) {
        Map<Long, List<CashierViews.VoucherLine>> byVoucher = new HashMap<>();
        for (CashierViews.VoucherLine e : entries) {
            byVoucher.computeIfAbsent(e.getVoucherId(), k -> new ArrayList<>()).add(e);
        }
        Map<Long, String> result = new HashMap<>();
        byVoucher.values().forEach(lines -> voucher(lines, result));
        return result;
    }

    private static void voucher(List<CashierViews.VoucherLine> lines, Map<Long, String> result) {
        for (List<CashierViews.VoucherLine> group : groups(lines)) {
            for (CashierViews.VoucherLine e : group) {
                List<CashierViews.VoucherLine> counters = counters(e, group);
                if (counters.isEmpty()) {
                    counters = opposite(e, lines);
                }
                if (counters.isEmpty()) {
                    counters = lines.stream().filter(o -> o != e).toList();
                }
                Set<String> names = new LinkedHashSet<>();
                counters.forEach(o -> names.add(o.getAccountName()));
                if (!names.isEmpty()) {
                    result.put(e.getEntryId(), String.join("、", names));
                }
            }
        }
    }

    /** 按行号累计借贷，回到平衡时结束一组；末尾不平衡的剩余分录为最后一组 */
    private static List<List<CashierViews.VoucherLine>> groups(List<CashierViews.VoucherLine> lines) {
        List<List<CashierViews.VoucherLine>> groups = new ArrayList<>();
        List<CashierViews.VoucherLine> current = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (CashierViews.VoucherLine e : lines) {
            current.add(e);
            running = running.add(nz(e.getDebit())).subtract(nz(e.getCredit()));
            if (running.signum() == 0) {
                groups.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }

    /** 组内的对方分录：反方向的分录；多借多贷时金额唯一相等的一一对应 */
    private static List<CashierViews.VoucherLine> counters(CashierViews.VoucherLine e, List<CashierViews.VoucherLine> group) {
        List<CashierViews.VoucherLine> opposite = opposite(e, group);
        List<CashierViews.VoucherLine> same = group.stream().filter(o -> debit(o) == debit(e)).toList();
        if (opposite.size() > 1 && same.size() > 1) {
            BigDecimal amount = amount(e);
            List<CashierViews.VoucherLine> equal = opposite.stream().filter(o -> amount(o).compareTo(amount) == 0).toList();
            long sameAmount = same.stream().filter(o -> amount(o).compareTo(amount) == 0).count();
            if (equal.size() == 1 && sameAmount == 1) {
                return equal;
            }
        }
        return opposite;
    }

    private static List<CashierViews.VoucherLine> opposite(CashierViews.VoucherLine e, List<CashierViews.VoucherLine> lines) {
        return lines.stream().filter(o -> debit(o) != debit(e)).toList();
    }

    private static boolean debit(CashierViews.VoucherLine e) {
        return nz(e.getDebit()).signum() != 0;
    }

    private static BigDecimal amount(CashierViews.VoucherLine e) {
        return debit(e) ? nz(e.getDebit()) : nz(e.getCredit());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
