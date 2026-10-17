package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.PeriodStatus;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.AccountSum;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinPeriod;
import com.lemon.erp.modules.fin.entity.FinVoucher;
import com.lemon.erp.modules.fin.entity.FinVoucherEntry;
import com.lemon.erp.modules.fin.mapper.FinPeriodMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherEntryMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherMapper;
import com.lemon.erp.modules.fin.model.AccountCategory;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.fin.model.VoucherStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.fin.model.FinConstants.lastDay;

/**
 * 期末结账：本期凭证必须全部记账，之前的期间必须已结账；结账时把损益类科目的本期余额结转到本年利润（自动生成并记账），
 * 结账后本期不能再录入、修改凭证。反结账只能从最后一个已结账期间开始，同时删除结转损益凭证
 */
@Service
@RequiredArgsConstructor
public class FinCloseService {

    private final FinPeriodMapper periodMapper;
    private final FinPeriodService periodService;
    private final FinVoucherMapper voucherMapper;
    private final FinVoucherEntryMapper entryMapper;
    private final FinVoucherService voucherService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;

    @Transactional
    public void close(int period) {
        FinPeriod p = periodService.ensure(period);
        if (p.getStatus() == PeriodStatus.CLOSED) {
            throw new BizException("fin.error.periodHasClosed", period);
        }
        FinPeriod earlierOpen = Queries.first(periodMapper, Wrappers.<FinPeriod>lambdaQuery()
                .lt(FinPeriod::getPeriod, period).eq(FinPeriod::getStatus, PeriodStatus.OPEN)
                .orderByAsc(FinPeriod::getPeriod));
        if (earlierOpen != null) {
            throw new BizException("fin.error.closePeriodFirst", earlierOpen.getPeriod());
        }
        long unposted = voucherMapper.selectCount(Wrappers.<FinVoucher>lambdaQuery()
                .eq(FinVoucher::getPeriod, period).ne(FinVoucher::getStatus, VoucherStatus.POSTED));
        if (unposted > 0) {
            throw new BizException("fin.error.periodStillHasUnpostedVoucherS", period, unposted);
        }
        carryForwardProfit(period);
        if (period % 100 == 12) {
            carryForwardYearProfit(period);
        }
        StatusTransitions.change(periodMapper, FinPeriod::getPeriod, period, FinPeriod::getStatus, PeriodStatus.OPEN, PeriodStatus.CLOSED,
                w -> w.set(FinPeriod::getCloseBy, SecurityUtils.getUserId()).set(FinPeriod::getCloseTime, LocalDateTime.now()),
                "fin.error.periodStatusHasChangedRefresh");
    }

    @Transactional
    public void reopen(int period) {
        FinPeriod p = periodService.get(period);
        if (p == null || p.getStatus() != PeriodStatus.CLOSED) {
            throw new BizException("fin.error.periodHasNotClosed", period);
        }
        if (periodMapper.exists(Wrappers.<FinPeriod>lambdaQuery()
                .gt(FinPeriod::getPeriod, period).eq(FinPeriod::getStatus, PeriodStatus.CLOSED))) {
            throw new BizException("fin.error.reopenLaterPeriodsFirst");
        }
        periodMapper.update(Wrappers.<FinPeriod>lambdaUpdate()
                .set(FinPeriod::getStatus, PeriodStatus.OPEN)
                .set(FinPeriod::getCloseBy, null)
                .set(FinPeriod::getCloseTime, null)
                .eq(FinPeriod::getPeriod, period));
        List<Long> closeVouchers = voucherMapper.selectList(Wrappers.<FinVoucher>lambdaQuery()
                        .select(FinVoucher::getId)
                        .eq(FinVoucher::getPeriod, period).eq(FinVoucher::getSourceType, VoucherSource.CLOSE))
                .stream().map(FinVoucher::getId).toList();
        if (!closeVouchers.isEmpty()) {
            entryMapper.delete(Wrappers.<FinVoucherEntry>lambdaQuery().in(FinVoucherEntry::getVoucherId, closeVouchers));
            voucherMapper.deleteByIds(closeVouchers);
        }
    }

    /** 损益类末级科目的本期发生额结转到本年利润：收入借记、费用贷记，差额记本年利润 */
    private void carryForwardProfit(int period) {
        Map<Long, FinAccount> profitAccounts = accountService.listAll().stream()
                .filter(a -> a.getCategory() == AccountCategory.PROFIT)
                .collect(Collectors.toMap(FinAccount::getId, Function.identity()));
        String summary = "结转 " + period + " 期间损益"; // i18n-ignore
        List<VoucherLine> lines = new ArrayList<>();
        BigDecimal profit = BigDecimal.ZERO;
        List<AccountSum> sums = entryMapper.sumByAccount(period, period, false).stream()
                .filter(s -> profitAccounts.containsKey(s.getAccountId()))
                .sorted(Comparator.comparing(s -> profitAccounts.get(s.getAccountId()).getCode()))
                .toList();
        for (AccountSum s : sums) {
            BigDecimal net = s.getDebit().subtract(s.getCredit());
            if (net.signum() == 0) {
                continue;
            }
            // 借方余额（费用）贷记冲平，贷方余额（收入）借记冲平
            lines.add(net.signum() > 0 ? VoucherLine.credit(summary, s.getAccountId(), null, net)
                    : VoucherLine.debit(summary, s.getAccountId(), null, net.negate()));
            profit = profit.add(net.negate());
        }
        if (lines.isEmpty()) {
            return;
        }
        Long thisYear = accountRules.accountId(AccountRole.PROFIT_THIS_YEAR);
        lines.add(profit.signum() >= 0 ? VoucherLine.credit(summary, thisYear, null, profit)
                : VoucherLine.debit(summary, thisYear, null, profit.negate()));
        voucherService.createPosted(lastDay(period), VoucherSource.CLOSE, String.valueOf(period), lines);
    }

    /** 年末（12 月结账）：本年利润的累计余额转入利润分配-未分配利润 */
    private void carryForwardYearProfit(int period) {
        Long thisYear = accountRules.accountId(AccountRole.PROFIT_THIS_YEAR);
        AccountSum sum = entryMapper.sumByAccount(null, period, false).stream()
                .filter(s -> s.getAccountId().equals(thisYear)).findFirst().orElse(null);
        BigDecimal credit = sum == null ? BigDecimal.ZERO : sum.getCredit().subtract(sum.getDebit());
        if (credit.signum() == 0) {
            return;
        }
        String summary = "结转 " + period / 100 + " 年度利润"; // i18n-ignore
        Long distribution = accountRules.accountId(AccountRole.PROFIT_DISTRIBUTION);
        // 盈利：借 本年利润 / 贷 利润分配；亏损相反
        voucherService.createPosted(lastDay(period), VoucherSource.CLOSE, String.valueOf(period), List.of(
                VoucherLine.debit(summary, thisYear, null, credit),
                VoucherLine.credit(summary, distribution, null, credit)));
    }
}
