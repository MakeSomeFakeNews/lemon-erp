package com.lemon.erp.modules.bi.service;

import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.modules.bi.dto.BiInsight;
import com.lemon.erp.modules.bi.model.AlertLevel;
import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

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
        return "bi.label.businessAlertPush";
    }

    @Override
    public String defaultCron() {
        return "0 0 9 * * *";
    }

    @Override
    public String run(JobParams params) {
        LocalDate today = LocalDate.now();
        List<BiInsight.AlertResult> triggered = decisionService.alerts().stream()
                .filter(BiInsight.AlertResult::triggered).toList();
        for (BiInsight.AlertResult a : triggered) {
            MessageCategory category = a.level() == AlertLevel.INFO ? MessageCategory.REMIND : MessageCategory.ALERT;
            messageSender.sendToPermission("bi:decision:list", Message.of(category,
                    "经营预警：" + a.name(), "当前值 " + a.value() + (a.remark() == null ? "" : "，" + a.remark()),  // i18n-ignore 站内消息存库
                    "/bi/decision").dedup("bi-alert:" + a.ruleId() + ":" + today));
        }
        return "触发 " + triggered.size() + " 条预警"; // i18n-ignore 任务日志存库
    }
}
