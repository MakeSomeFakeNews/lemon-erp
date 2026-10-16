package com.lemon.erp.modules.bi.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 经营预警推送：评估全部启用的预警规则，触发的推送给决策支持用户（bi:decision:list，每条规则每天一次）
 */
@Component
@RequiredArgsConstructor
public class BiAlertJob implements JobHandler {

    private final BiDecisionService decisionService;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "bi.alertNotify";
    }

    @Override
    public String name() {
        return "经营预警推送";
    }

    @Override
    public String defaultCron() {
        return "0 0 9 * * *";
    }

    @Override
    public String run(JobParams params) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> triggered = decisionService.alerts().stream()
                .filter(a -> Boolean.TRUE.equals(a.get("triggered"))).toList();
        for (Map<String, Object> a : triggered) {
            String level = String.valueOf(a.get("level"));
            messageSender.sendToPermission("bi:decision:list", Message.of("INFO".equals(level) ? "REMIND" : "ALERT",
                    "经营预警：" + a.get("name"), "当前值 " + a.get("value") + (a.get("remark") == null ? "" : "，" + a.get("remark")),
                    "/bi/decision").dedup("bi-alert:" + a.get("ruleId") + ":" + today));
        }
        return "触发 " + triggered.size() + " 条预警";
    }
}
