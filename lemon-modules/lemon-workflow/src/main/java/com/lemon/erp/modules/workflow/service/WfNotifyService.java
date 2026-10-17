package com.lemon.erp.modules.workflow.service;

import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.framework.workflow.FlowTaskAssignedEvent;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 审批通知：任务分配（含转交）时提醒审批人，流程结束时通知发起人。
 * 在审批事务提交前（引擎命令执行完之后）写入站内消息：与审批同一事务，又不在引擎命令执行中途访问数据库
 */
@Service
@RequiredArgsConstructor
public class WfNotifyService {

    private final MessageSender messageSender;
    private final HistoryService historyService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onTaskAssigned(FlowTaskAssignedEvent e) {
        if (e.assigneeId() == null) {
            return;
        }
        messageSender.send(List.of(e.assigneeId()), Message.of("APPROVAL", "待审批：" + e.processName(),  // i18n-ignore 存库或内部异常
                "「" + e.processName() + "」流转到「" + e.taskName() + "」，等待你审批", "/workflow/todo"));  // i18n-ignore 存库或内部异常
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onFinished(FlowFinishedEvent e) {
        // 撤回、终止是发起人或管理员自己的操作，不再通知
        if (e.startUserId() == null || e.status() == FlowStatus.CANCELED) {
            return;
        }
        HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(e.processInstanceId()).singleResult();
        String name = pi == null || pi.getName() == null ? "你的申请" : "「" + pi.getName() + "」";  // i18n-ignore 存库或内部异常
        String result = switch (e.status()) {
            case APPROVED -> "已审批通过";  // i18n-ignore 存库或内部异常
            case REJECTED -> "已被拒绝";  // i18n-ignore 存库或内部异常
            default -> "已结束";  // i18n-ignore 存库或内部异常
        };
        messageSender.send(List.of(e.startUserId()), Message.of("APPROVAL", name + result, name + result, "/workflow/mine"));
    }
}
