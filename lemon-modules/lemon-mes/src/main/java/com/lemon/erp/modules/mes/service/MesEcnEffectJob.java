package com.lemon.erp.modules.mes.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 工程变更生效：已批准且到了生效日期的变更切换 BOM 版本
 */
@Component
@RequiredArgsConstructor
public class MesEcnEffectJob implements JobHandler {

    private final MesEcnService ecnService;

    @Override
    public String key() {
        return "mes.ecnEffect";
    }

    @Override
    public String name() {
        return "mes.label.ecnTakesEffect";
    }

    @Override
    public String defaultCron() {
        return "0 5 0 * * *";
    }

    @Override
    public String run(JobParams params) {
        int n = ecnService.effectDue(LocalDate.now());
        return n == 0 ? "没有到期生效的工程变更" : "生效 " + n + " 项工程变更"; // i18n-ignore
    }
}
