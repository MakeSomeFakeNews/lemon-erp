package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.framework.period.BizPeriodGuard;
import com.lemon.erp.modules.fin.entity.FinCostRun;
import com.lemon.erp.modules.fin.mapper.FinCostRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static com.lemon.erp.modules.fin.model.FinConstants.periodOf;

/**
 * 业务单据的期间锁：已结账的期间不能再录入业务；已核算成本的期间不能再发生库存变动（否则成本与库存流水不一致），
 * 需要先撤销该期间的成本核算
 */
@Component
@RequiredArgsConstructor
public class FinBizPeriodGuard implements BizPeriodGuard {

    private final FinPeriodService periodService;
    private final FinCostRunMapper costRunMapper;

    /** docName 为调用方传入的中文单据名称，在此按请求语言翻译（各模块的单据名称译文已登记） */
    @Override
    public void checkOpen(LocalDate bizDate, String docName) {
        if (bizDate == null) {
            return;
        }
        int period = periodOf(bizDate);
        if (periodService.isClosed(period)) {
            throw new BizException("{0} 已结账，不能再处理业务日期在该期间的{1}", format(period), I18n.t(docName));
        }
    }

    @Override
    public void checkStockOpen(LocalDate bizDate, String docName) {
        checkOpen(bizDate, docName);
        if (bizDate == null) {
            return;
        }
        int period = periodOf(bizDate);
        if (costRunMapper.exists(Wrappers.<FinCostRun>lambdaQuery().ge(FinCostRun::getPeriod, period))) {
            throw new BizException("{0} 已核算成本，不能再处理业务日期在该期间的{1}，请先在「成本核算」中撤销", format(period), I18n.t(docName));
        }
    }

    private static String format(int period) {
        return I18n.t("{0} 年 {1} 月", period / 100, period % 100);
    }
}
