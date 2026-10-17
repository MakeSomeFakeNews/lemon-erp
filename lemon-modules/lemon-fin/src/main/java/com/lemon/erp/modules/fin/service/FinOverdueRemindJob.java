package com.lemon.erp.modules.fin.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.fin.mapper.FinJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * 应收逾期提醒：每天汇总逾期未收的应收单，提醒应收人员（fin:ar:list）
 */
@Component
@RequiredArgsConstructor
public class FinOverdueRemindJob implements JobHandler {

    private final FinJobMapper jobMapper;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "fin.arOverdue";
    }

    @Override
    public String name() {
        return "fin.label.overdueReceivableReminder";
    }

    @Override
    public String defaultCron() {
        return "0 0 9 * * *";
    }

    @Override
    public String run(JobParams params) {
        LocalDate today = LocalDate.now();
        Map<String, Object> s = jobMapper.selectOverdueSummary("AR", today);
        long count = s == null ? 0 : ((Number) s.get("count")).longValue();
        if (count == 0) {
            // 执行结果写入任务日志，不翻译
            return "没有逾期应收"; // i18n-ignore
        }
        messageSender.sendToPermission("fin:ar:list", Message.of("ALERT", "有 " + count + " 张应收单已逾期", // i18n-ignore
                "逾期未收金额 " + s.get("amount") + "，请在账龄分析中跟进", "/fin/ar") // i18n-ignore
               .dedup("ar-overdue:" + today));
        return "逾期应收 " + count + " 张"; // i18n-ignore
    }
}
