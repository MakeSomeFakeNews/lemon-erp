package com.lemon.erp.modules.hr.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.hr.mapper.HrJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 转正提醒：15 天内（参数 days）试用期到期的员工，提醒人事（hr:employee:list）和所在部门负责人（每人一次）
 */
@Component
@RequiredArgsConstructor
public class HrProbationRemindJob implements JobHandler {

    private final HrJobMapper jobMapper;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "hr.probationRemind";
    }

    @Override
    public String name() {
        return "hr.label.probationExpiryReminder";
    }

    @Override
    public String defaultCron() {
        return "0 10 8 * * *";
    }

    @Override
    public String defaultParams() {
        return "days=15";
    }

    @Override
    public String run(JobParams params) {
        List<Map<String, Object>> rows = jobMapper.selectProbationDue(LocalDate.now().plusDays(params.getInt("days", 15)));
        for (Map<String, Object> r : rows) {
            Message m = Message.of("REMIND", "员工「" + r.get("name") + "」试用期即将到期",  // i18n-ignore 站内消息存库
                    "工号 " + r.get("empNo") + "，预计转正日期 " + r.get("regularDate") + "，请办理转正或延长试用", "/hr/employee")  // i18n-ignore 站内消息存库
                    .dedup("probation:" + r.get("id") + ":" + r.get("regularDate"));
            messageSender.sendToPermission("hr:employee:change", m);
            if (r.get("leaderId") != null) {
                messageSender.send(List.of(((Number) r.get("leaderId")).longValue()), m);
            }
        }
        return "提醒 " + rows.size() + " 名员工";  // i18n-ignore 任务日志存库
    }
}
