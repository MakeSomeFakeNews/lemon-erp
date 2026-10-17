package com.lemon.erp.modules.crm.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.crm.mapper.CrmJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 跟进提醒：下次跟进时间在今天及之前的客户，提醒负责人（每个客户每天一次）
 */
@Component
@RequiredArgsConstructor
public class CrmFollowRemindJob implements JobHandler {

    private final CrmJobMapper jobMapper;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "crm.followRemind";
    }

    @Override
    public String name() {
        return "crm.label.customerFollowUpReminder";
    }

    @Override
    public String defaultCron() {
        return "0 0 8 * * *";
    }

    @Override
    public String run(JobParams params) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = jobMapper.selectFollowDue(today.plusDays(1).atStartOfDay());
        for (Map<String, Object> r : rows) {
            Long owner = ((Number) r.get("ownerId")).longValue();
            messageSender.send(List.of(owner), Message.of("REMIND", "今天需要跟进客户「" + r.get("name") + "」", // i18n-ignore 站内消息存库
                    "计划跟进时间 " + r.get("nextFollowTime"), "/crm/customer").dedup("crm-follow:" + r.get("customerId") + ":" + today)); // i18n-ignore 站内消息存库
        }
        return "提醒 " + rows.size() + " 个客户"; // i18n-ignore 任务日志存库
    }
}
