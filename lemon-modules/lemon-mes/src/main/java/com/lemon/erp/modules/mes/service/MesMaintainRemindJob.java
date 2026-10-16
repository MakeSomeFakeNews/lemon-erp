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
 * 设备保养提醒：3 天内（参数 days）到期的保养，提醒设备维护人员（mes:maintain:handle，每台每个保养日期一次）
 */
@Component
@RequiredArgsConstructor
public class MesMaintainRemindJob implements JobHandler {

    private final MesJobMapper jobMapper;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "mes.maintainRemind";
    }

    @Override
    public String name() {
        return "设备保养提醒";
    }

    @Override
    public String defaultCron() {
        return "0 30 8 * * *";
    }

    @Override
    public String defaultParams() {
        return "days=3";
    }

    @Override
    public String run(JobParams params) {
        List<Map<String, Object>> rows = jobMapper.selectMaintainDue(LocalDate.now().plusDays(params.getInt("days", 3)));
        for (Map<String, Object> r : rows) {
            messageSender.sendToPermission("mes:maintain:handle", Message.of("REMIND",
                    "设备「" + r.get("code") + " " + r.get("name") + "」需要保养", "计划保养日期 " + r.get("nextDate"), "/mes/maintenance")
                    .dedup("maintain:" + r.get("id") + ":" + r.get("nextDate")));
        }
        return "提醒 " + rows.size() + " 台设备";
    }
}
