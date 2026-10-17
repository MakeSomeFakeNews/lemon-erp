package com.lemon.erp.modules.hr.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.hr.entity.HrPayroll;
import com.lemon.erp.modules.hr.mapper.HrPayrollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 工资表确认后锁定当月考勤：该月的补卡、删除考勤、请假加班的申请、审批、撤销都不能再做，否则与已确认的工资不一致。
 * 需要调整时先在工资表中取消确认（草稿状态可重新计算）
 */
@Component
@RequiredArgsConstructor
public class HrPayrollLock {

    private final HrPayrollMapper payrollMapper;

    /** @param what 被禁止的操作（消息键） */
    public void checkOpen(LocalDate date, String what) {
        if (date == null) {
            return;
        }
        int period = date.getYear() * 100 + date.getMonthValue();
        if (payrollMapper.exists(Wrappers.<HrPayroll>lambdaQuery().eq(HrPayroll::getPeriod, period).ne(HrPayroll::getStatus, "DRAFT"))) {
            throw new BizException("hr.error.payrollHasConfirmedCannot", date.getYear(), date.getMonthValue(), Messages.get(what));
        }
    }
}
