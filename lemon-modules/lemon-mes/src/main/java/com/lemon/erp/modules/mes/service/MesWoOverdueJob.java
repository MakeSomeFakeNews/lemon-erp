package com.lemon.erp.modules.mes.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.mes.mapper.MesJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 工单逾期提醒：已过计划完工日期仍未完工的工单，提醒制单人（每张工单每天一次）
 */
@Component
@RequiredArgsConstructor
public class MesWoOverdueJob implements JobHandler {

    private final MesJobMapper jobMapper;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "mes.woOverdue";
    }

    @Override
    public String name() {
        return "工单逾期提醒";
    }

    @Override
    public String defaultCron() {
        return "0 40 8 * * *";
    }

    @Override
    public String run(JobParams params) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = jobMapper.selectOverdueWorkOrders(today);
        for (Map<String, Object> r : rows) {
            if (r.get("createBy") == null) {
                continue;
            }
            messageSender.send(List.of(((Number) r.get("createBy")).longValue()), Message.of("ALERT",
                    "工单 " + r.get("woNo") + " 已逾期", "计划完工日期 " + r.get("planEnd") + "，请跟进生产进度或关闭工单", "/mes/wo")
                    .dedup("wo-overdue:" + r.get("id") + ":" + today));
        }
        return "提醒 " + rows.size() + " 张工单";
    }
}
