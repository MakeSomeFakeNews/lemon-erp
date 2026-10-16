package com.lemon.erp.modules.crm.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.crm.entity.CrmCollectionPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 合同回款提醒：执行中合同的逾期回款计划，提醒合同负责人（每期一次）
 */
@Component
@RequiredArgsConstructor
public class CrmCollectionRemindJob implements JobHandler {

    private final CrmContractService contractService;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "crm.collectionRemind";
    }

    @Override
    public String name() {
        return "合同回款提醒";
    }

    @Override
    public String defaultCron() {
        return "0 50 8 * * *";
    }

    @Override
    public String run(JobParams params) {
        List<CrmCollectionPlan> list = contractService.overduePlans();
        for (CrmCollectionPlan p : list) {
            if (p.getOwnerId() == null) {
                continue;
            }
            messageSender.send(List.of(p.getOwnerId()), Message.of("REMIND", "合同回款逾期：" + p.getContractNo() + " 第 " + p.getSeq() + " 期",
                    p.getCustomerName() + " 计划 " + p.getPlanDate() + " 回款 " + p.getAmount().toPlainString() + "，已回 "
                            + p.getReceivedAmount().toPlainString(), "/crm/contract").dedup("collection-overdue:" + p.getId()));
        }
        return "逾期 " + list.size() + " 期";
    }
}
