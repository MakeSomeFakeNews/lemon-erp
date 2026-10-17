package com.lemon.erp.modules.crm.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.crm.mapper.CrmJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 公海回收：负责人超过 days 天（默认 30）没有跟进的未成交客户退回公海，并通知原负责人
 */
@Component
@RequiredArgsConstructor
public class CrmPoolReclaimJob implements JobHandler {

    private final CrmJobMapper jobMapper;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "crm.poolReclaim";
    }

    @Override
    public String name() {
        return "crm.label.customerPublicPoolReclaim";
    }

    @Override
    public String defaultCron() {
        return "0 30 2 * * *";
    }

    @Override
    public String defaultParams() {
        return "days=30";
    }

    @Override
    public String run(JobParams params) {
        int days = params.getInt("days", 30);
        List<Map<String, Object>> rows = jobMapper.selectIdle(LocalDateTime.now().minusDays(days));
        if (rows.isEmpty()) {
            return "没有需要回收的客户"; // i18n-ignore 任务日志存库
        }
        jobMapper.releaseToPool(rows.stream().map(r -> ((Number) r.get("id")).longValue()).toList());
        for (Map<String, Object> r : rows) {
            messageSender.send(List.of(((Number) r.get("ownerId")).longValue()), Message.of(MessageCategory.REMIND,
                    "客户「" + r.get("name") + "」已退回公海", "超过 " + days + " 天没有跟进，已自动退回公海", "/crm/customer")); // i18n-ignore 站内消息存库
        }
        return "回收 " + rows.size() + " 个客户"; // i18n-ignore 任务日志存库
    }
}
