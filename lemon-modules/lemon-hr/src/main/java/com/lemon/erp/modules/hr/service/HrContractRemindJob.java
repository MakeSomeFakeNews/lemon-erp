package com.lemon.erp.modules.hr.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.hr.entity.HrContract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 劳动合同到期提醒：到期日已过的合同改为已到期；days 天内（默认 30）到期的合同提醒人事（hr:contract:list，每份合同一次）
 */
@Component
@RequiredArgsConstructor
public class HrContractRemindJob implements JobHandler {

    private final HrContractService contractService;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "hr.contractExpiry";
    }

    @Override
    public String name() {
        return "hr.label.employmentContractExpiryReminder";
    }

    @Override
    public String defaultCron() {
        return "0 40 8 * * *";
    }

    @Override
    public String defaultParams() {
        return "days=30";
    }

    @Override
    public String run(JobParams params) {
        int expired = contractService.markExpired();
        List<HrContract> list = contractService.expiring(params.getInt("days", 30));
        for (HrContract c : list) {
            messageSender.sendToPermission("hr:contract:list", Message.of("REMIND",
                    "劳动合同即将到期：" + c.getEmployeeName() + "（" + c.getEmpNo() + "）",  // i18n-ignore 站内消息存库
                    "合同 " + c.getContractNo() + " 将于 " + c.getEndDate() + " 到期，请及时续签或办理终止", "/hr/contract")  // i18n-ignore 站内消息存库
                    .dedup("contract-expiry:" + c.getId()));
        }
        return "已到期 " + expired + " 份，提醒 " + list.size() + " 份";  // i18n-ignore 任务日志存库
    }
}
