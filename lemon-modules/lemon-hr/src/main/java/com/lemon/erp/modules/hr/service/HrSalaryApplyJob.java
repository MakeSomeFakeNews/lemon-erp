package com.lemon.erp.modules.hr.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.modules.hr.mapper.HrJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 薪资生效：今天生效的调薪同步到薪资档案（工资计算按生效日期取薪资历史，档案用于显示和项目小时成本）
 */
@Component
@RequiredArgsConstructor
public class HrSalaryApplyJob implements JobHandler {

    private final HrJobMapper jobMapper;

    @Override
    public String key() {
        return "hr.salaryApply";
    }

    @Override
    public String name() {
        return "调薪生效";  // i18n-ignore 定时任务名称，任务管理处翻译
    }

    @Override
    public String defaultCron() {
        return "0 5 0 * * *";
    }

    @Override
    public String run(JobParams params) {
        return "更新 " + jobMapper.applyEffectiveSalary(LocalDate.now()) + " 份薪资档案";  // i18n-ignore 任务日志存库
    }
}
