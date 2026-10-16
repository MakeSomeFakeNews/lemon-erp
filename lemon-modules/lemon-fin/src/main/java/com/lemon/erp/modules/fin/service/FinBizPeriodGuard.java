package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
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

    @Override
    public void checkOpen(LocalDate bizDate, String docName) {
        if (bizDate == null) {
            return;
        }
        int period = periodOf(bizDate);
        if (periodService.isClosed(period)) {
            throw new BizException(format(period) + " 已结账，不能再处理业务日期在该期间的" + docName);
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
            throw new BizException(format(period) + " 已核算成本，不能再处理业务日期在该期间的" + docName + "，请先在「成本核算」中撤销");
        }
    }

    private static String format(int period) {
        return period / 100 + " 年 " + period % 100 + " 月";
    }
}
