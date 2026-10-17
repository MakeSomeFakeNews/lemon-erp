package com.lemon.erp.modules.fin.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.modules.fin.entity.FinNote;
import com.lemon.erp.modules.fin.mapper.FinNoteMapper;
import com.lemon.erp.modules.fin.model.NoteKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 票据到期提醒：days 天内（默认 7）到期仍未处理的票据——应收票据提醒托收，应付票据提醒备足资金兑付，通知票据管理人员（fin:note:edit）
 */
@Component
@RequiredArgsConstructor
public class FinNoteDueRemindJob implements JobHandler {

    private final FinNoteMapper noteMapper;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "fin.noteDue";
    }

    @Override
    public String name() {
        return "fin.label.noteDueReminder";
    }

    @Override
    public String defaultCron() {
        return "0 10 9 * * *";
    }

    @Override
    public String defaultParams() {
        return "days=7";
    }

    @Override
    public String run(JobParams params) {
        int days = params.getInt("days", 7);
        LocalDate today = LocalDate.now();
        List<FinNote> due = noteMapper.selectDue(today.plusDays(days));
        if (due.isEmpty()) {
            // 执行结果写入任务日志，不翻译
            return "没有即将到期的票据"; // i18n-ignore
        }
        List<FinNote> receivable = due.stream().filter(n -> n.getKind() == NoteKind.RECEIVABLE).toList();
        List<FinNote> payable = due.stream().filter(n -> n.getKind() == NoteKind.PAYABLE).toList();
        if (!receivable.isEmpty()) {
            messageSender.sendToPermission("fin:note:edit", Message.of("REMIND",
                    receivable.size() + " 张应收票据 " + days + " 天内到期", // i18n-ignore
                    "票面合计 " + total(receivable) + "，请及时办理托收", "/fin/note") // i18n-ignore
                   .dedup("note-in-due:" + today));
        }
        if (!payable.isEmpty()) {
            messageSender.sendToPermission("fin:note:edit", Message.of("ALERT",
                    payable.size() + " 张应付票据 " + days + " 天内到期", // i18n-ignore
                    "票面合计 " + total(payable) + "，请确保账户资金充足", "/fin/note") // i18n-ignore
                   .dedup("note-out-due:" + today));
        }
        return "应收票据 " + receivable.size() + " 张，应付票据 " + payable.size() + " 张"; // i18n-ignore
    }

    private static String total(List<FinNote> notes) {
        return notes.stream().map(FinNote::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString();
    }
}
