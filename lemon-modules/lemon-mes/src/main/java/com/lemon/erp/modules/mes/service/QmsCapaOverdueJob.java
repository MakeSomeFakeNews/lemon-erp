package com.lemon.erp.modules.mes.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.modules.mes.entity.QmsCapa;
import com.lemon.erp.modules.mes.mapper.QmsCapaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 纠正预防逾期提醒：超过要求完成日期仍未关闭的，通知负责人
 */
@Component
@RequiredArgsConstructor
public class QmsCapaOverdueJob implements JobHandler {

    private final QmsCapaMapper capaMapper;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "qms.capaOverdue";
    }

    @Override
    public String name() {
        return "纠正预防逾期提醒";
    }

    @Override
    public String defaultCron() {
        return "0 20 9 * * *";
    }

    @Override
    public String run(JobParams params) {
        LocalDate today = LocalDate.now();
        List<QmsCapa> overdue = capaMapper.selectOverdue(today);
        for (QmsCapa c : overdue) {
            messageSender.send(List.of(c.getOwnerId()), Message.of("ALERT", "纠正预防已逾期：" + c.getCapaNo(), // i18n-ignore
                    c.getTitle() + "，要求完成日期 " + c.getDueDate(), "/qms/capa").dedup("capa-overdue:" + c.getId() + ":" + today)); // i18n-ignore
        }
        return overdue.isEmpty() ? "没有逾期的纠正预防" : "逾期 " + overdue.size() + " 项"; // i18n-ignore
    }
}
