package com.lemon.erp.modules.bi.service;

import com.lemon.erp.modules.bi.model.EtlTrigger;
import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.modules.bi.entity.BiEtlLog;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 数据仓库同步：默认每天凌晨 1:30（lemon.bi.etl-cron，设为 - 时任务默认停用）
 */
@Component
@RequiredArgsConstructor
public class BiEtlJob implements JobHandler {

    private static final String FALLBACK_CRON = "0 30 1 * * *";

    private final BiEtlService etlService;

    @Value("${lemon.bi.etl-cron:0 30 1 * * *}")
    private String cron;

    @Override
    public String key() {
        return "bi.etl";
    }

    @Override
    public String name() {
        return "bi.label.dataWarehouseSync";
    }

    @Override
    public String defaultCron() {
        return "-".equals(cron) ? FALLBACK_CRON : cron;
    }

    @Override
    public boolean defaultEnabled() {
        return !"-".equals(cron);
    }

    @Override
    public String run(JobParams params) {
        BiEtlLog log = etlService.run(EtlTrigger.SCHEDULE);
        return log.getStatus() + (log.getRowCounts() == null ? "" : "：" + log.getRowCounts());
    }
}
