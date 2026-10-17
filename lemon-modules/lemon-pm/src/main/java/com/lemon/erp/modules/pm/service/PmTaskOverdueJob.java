package com.lemon.erp.modules.pm.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.pm.mapper.PmJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 任务逾期提醒：进行中项目里已过计划完成日期的任务，提醒负责人（每个任务每天一次）
 */
@Component
@RequiredArgsConstructor
public class PmTaskOverdueJob implements JobHandler {

    private final PmJobMapper jobMapper;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "pm.taskOverdue";
    }

    @Override
    public String name() {
        return "pm.label.projectTaskOverdueReminder";
    }

    @Override
    public String defaultCron() {
        return "0 50 8 * * *";
    }

    @Override
    public String run(JobParams params) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = jobMapper.selectOverdueTasks(today);
        for (Map<String, Object> r : rows) {
            messageSender.send(List.of(((Number) r.get("assigneeId")).longValue()), Message.of("ALERT",
                    "任务「" + r.get("name") + "」已逾期", "项目「" + r.get("projectName") + "」，计划完成 " + r.get("planEnd"), "/pm/plan")  // i18n-ignore 站内消息存库
                    .dedup("task-overdue:" + r.get("id") + ":" + today));
        }
        return "提醒 " + rows.size() + " 个任务"; // i18n-ignore 任务日志存库
    }
}
