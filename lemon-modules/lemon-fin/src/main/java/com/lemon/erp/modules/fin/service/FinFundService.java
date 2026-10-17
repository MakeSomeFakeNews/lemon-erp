package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.FundPlan;
import com.lemon.erp.modules.fin.dto.FundPlanEntry;
import com.lemon.erp.modules.fin.dto.FundTransferRequest;
import com.lemon.erp.modules.fin.entity.FinFundPlanItem;
import com.lemon.erp.modules.fin.entity.FinFundTransfer;
import com.lemon.erp.modules.fin.mapper.FinFundPlanMapper;
import com.lemon.erp.modules.fin.mapper.FinFundTransferMapper;
import com.lemon.erp.modules.fin.model.FundDirection;
import com.lemon.erp.modules.fin.model.FundSource;
import com.lemon.erp.modules.fin.model.PlanUnit;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 资金管理：账户调拨、资金计划（含手工计划项）
 */
@Service
@RequiredArgsConstructor
public class FinFundService {

    /** 账户调拨单号的编码规则标识 */
    public static final String TRANSFER_NO = "HB";
    private static final int MAX_PERIODS = 26;

    private final FinFundTransferMapper transferMapper;
    private final FinFundPlanMapper planMapper;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinVoucherService voucherService;
    private final FinBizPeriodGuard periodGuard;
    private final DocNoService docNoService;

    // ---------------------------------------------------------------- 账户调拨

    public PageResult<FinFundTransfer> pageTransfers(long current, long size, String keyword, LocalDate begin, LocalDate end) {
        Page<FinFundTransfer> page = Page.of(current, size);
        transferMapper.selectPageList(page, keyword, begin, end);
        return PageResult.of(page);
    }

    /** 调拨：借 转入账户（金额）、财务费用（手续费），贷 转出账户（金额 + 手续费） */
    @Transactional
    public Long createTransfer(FundTransferRequest req) {
        if (req.fromAccountId().equals(req.toAccountId())) {
            throw new BizException("转出、转入账户不能相同");
        }
        accountService.requireCashAccount(req.fromAccountId());
        accountService.requireCashAccount(req.toAccountId());
        periodGuard.checkOpen(req.transferDate(), "账户调拨");
        BigDecimal fee = req.fee() == null ? BigDecimal.ZERO : req.fee();
        FinFundTransfer t = new FinFundTransfer();
        t.setTransferNo(docNoService.next(TRANSFER_NO));
        t.setTransferDate(req.transferDate());
        t.setFromAccountId(req.fromAccountId());
        t.setToAccountId(req.toAccountId());
        t.setAmount(req.amount());
        t.setFee(fee);
        t.setRemark(req.remark());
        t.setCreateBy(SecurityUtils.getUserId());
        String summary = "账户调拨" + (req.remark() == null || req.remark().isBlank() ? "" : " " + req.remark());
        List<VoucherLine> lines = new ArrayList<>();
        lines.add(VoucherLine.debit(summary, req.toAccountId(), null, req.amount()));
        if (fee.signum() > 0) {
            lines.add(VoucherLine.debit(summary + "-手续费", accountRules.accountId(AccountRole.FINANCE_EXPENSE), null, fee));
        }
        lines.add(VoucherLine.credit(summary, req.fromAccountId(), null, req.amount().add(fee)));
        t.setVoucherId(voucherService.createGenerated(req.transferDate(), VoucherSource.TRANSFER, t.getTransferNo(), lines));
        transferMapper.insert(t);
        return t.getId();
    }

    /** 删除调拨，同时删除其凭证（须未审核） */
    @Transactional
    public void deleteTransfers(List<Long> ids) {
        for (FinFundTransfer t : transferMapper.selectByIds(ids)) {
            periodGuard.checkOpen(t.getTransferDate(), "账户调拨");
            if (t.getVoucherId() != null) {
                voucherService.deleteOwned(t.getVoucherId());
            }
            transferMapper.deleteById(t.getId());
        }
    }

    // ---------------------------------------------------------------- 资金计划

    /**
     * @param unit    按周 / 按月
     * @param periods 期数（不含「已逾期」），最多 26
     */
    public FundPlan plan(PlanUnit unit, int periods) {
        boolean week = unit != PlanUnit.MONTH;
        int n = Math.max(1, Math.min(periods, MAX_PERIODS));
        LocalDate today = LocalDate.now();
        List<LocalDate[]> ranges = new ArrayList<>();
        LocalDate start = week ? today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : today.withDayOfMonth(1);
        for (int i = 0; i < n; i++) {
            LocalDate end = week ? start.plusDays(6) : start.with(TemporalAdjusters.lastDayOfMonth());
            ranges.add(new LocalDate[]{start, end});
            start = end.plusDays(1);
        }
        LocalDate horizon = ranges.get(n - 1)[1];
        List<FundPlanEntry> entries = planMapper.selectEntries(horizon);
        BigDecimal balance = planMapper.selectCashBalance();
        FundPlan result = new FundPlan(balance, new ArrayList<>());
        result.periods().add(period("已逾期", null, today.minusDays(1), entries, balance));
        balance = result.periods().get(0).closing();
        DateTimeFormatter md = DateTimeFormatter.ofPattern("M/d");
        for (LocalDate[] r : ranges) {
            // 本期中今天之前的部分已计入「已逾期」
            LocalDate from = r[0].isBefore(today) ? today : r[0];
            String label = week ? r[0].format(md) + " - " + r[1].format(md) : r[0].format(DateTimeFormatter.ofPattern("yyyy-MM"));
            FundPlan.Period p = period(label, from, r[1], entries, balance);
            result.periods().add(p);
            balance = p.closing();
        }
        return result;
    }

    /** 一段时间内的预计收支明细（from 为空表示已逾期） */
    public List<FundPlanEntry> planEntries(LocalDate from, LocalDate to) {
        return planMapper.selectEntries(to).stream()
                .filter(e -> from == null ? e.dueDate().isBefore(LocalDate.now()) : !e.dueDate().isBefore(from))
                .sorted(Comparator.comparing(FundPlanEntry::dueDate))
                .toList();
    }

    private static FundPlan.Period period(String label, LocalDate from, LocalDate to, List<FundPlanEntry> entries,
                                          BigDecimal opening) {
        Map<FundSource, BigDecimal> in = new EnumMap<>(FundSource.class);
        Map<FundSource, BigDecimal> out = new EnumMap<>(FundSource.class);
        for (FundPlanEntry e : entries) {
            if ((from != null && e.dueDate().isBefore(from)) || e.dueDate().isAfter(to)) {
                continue;
            }
            (e.direction() == FundDirection.IN ? in : out).merge(e.source(), e.amount(), BigDecimal::add);
        }
        BigDecimal totalIn = in.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOut = out.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal closing = opening.add(totalIn).subtract(totalOut);
        return new FundPlan.Period(label, from, to, in, out, totalIn, totalOut, closing, closing.signum() < 0);
    }

    // ---------------------------------------------------------------- 计划项

    public PageResult<FinFundPlanItem> pageItems(long current, long size, LocalDate begin, LocalDate end) {
        return PageResult.of(planMapper.selectPage(Page.of(current, size), Wrappers.<FinFundPlanItem>lambdaQuery()
                .ge(begin != null, FinFundPlanItem::getPlanDate, begin)
                .le(end != null, FinFundPlanItem::getPlanDate, end)
                .orderByAsc(FinFundPlanItem::getPlanDate)));
    }

    public void saveItem(FinFundPlanItem item) {
        if (item.getId() == null) {
            item.setCreateBy(SecurityUtils.getUserId());
            planMapper.insert(item);
        } else {
            if (planMapper.selectById(item.getId()) == null) {
                throw new BizException("计划项不存在");
            }
            item.setCreateBy(null);
            planMapper.updateById(item);
        }
    }

    public void deleteItems(List<Long> ids) {
        planMapper.deleteByIds(ids);
    }
}
