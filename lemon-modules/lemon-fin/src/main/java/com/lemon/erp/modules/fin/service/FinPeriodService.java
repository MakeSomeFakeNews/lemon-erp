package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.PeriodStatus;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.fin.entity.FinPeriod;
import com.lemon.erp.modules.fin.mapper.FinPeriodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.lemon.erp.modules.fin.model.FinConstants.toYearMonth;

/**
 * 会计期间：凭证日期所在月份，首次录入凭证时自动创建（未结账）。结账、反结账见 {@link FinCloseService}
 */
@Service
@RequiredArgsConstructor
public class FinPeriodService {

    private final FinPeriodMapper periodMapper;

    public PageResult<FinPeriod> page(long current, long size) {
        Page<FinPeriod> page = Page.of(current, size);
        periodMapper.selectPageList(page);
        return PageResult.of(page);
    }

    /** 分配期间的下一个凭证号：期间不存在时创建；已结账报错。在新增凭证的事务中执行，流水行锁到事务结束 */
    @Transactional(propagation = Propagation.MANDATORY)
    public int nextVoucherSeq(int period) {
        ensure(period);
        if (periodMapper.nextSeq(period) == 0) {
            throw new BizException("fin.error.periodClosedVouchersNoLongerEntered", period);
        }
        return get(period).getVoucherSeq();
    }

    /** 期间未结账（不存在视为未结账） */
    public void checkOpen(int period) {
        FinPeriod p = get(period);
        if (p != null && p.getStatus() == PeriodStatus.CLOSED) {
            throw new BizException("fin.error.periodClosedVouchersNoLongerEntered", period);
        }
    }

    public boolean isClosed(int period) {
        FinPeriod p = get(period);
        return p != null && p.getStatus() == PeriodStatus.CLOSED;
    }

    FinPeriod get(int period) {
        return periodMapper.selectOne(Wrappers.<FinPeriod>lambdaQuery().eq(FinPeriod::getPeriod, period));
    }

    /** 期间不存在时创建（并发时另一个事务先创建也没关系） */
    FinPeriod ensure(int period) {
        toYearMonth(period);
        FinPeriod p = get(period);
        if (p != null) {
            return p;
        }
        p = new FinPeriod();
        p.setPeriod(period);
        p.setStatus(PeriodStatus.OPEN);
        p.setVoucherSeq(0);
        try {
            periodMapper.insert(p);
        } catch (DuplicateKeyException e) {
            return get(period);
        }
        return p;
    }
}
