package com.lemon.erp.modules.workflow.service;

import com.lemon.erp.framework.org.EmployeeLeftEvent;
import com.lemon.erp.modules.workflow.engine.FlowableUserContext;
import com.lemon.erp.modules.workflow.model.ApprovalAction;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 离职交接：离职人员名下未处理的审批任务转交给交接人（记录转交意见）；没有交接人时保留，由管理员在流程监控中处理
 */
@Service
@RequiredArgsConstructor
public class WfHandoverService {

    private final TaskService taskService;

    @EventListener
    public void onEmployeeLeft(EmployeeLeftEvent e) {
        if (e.userId() == null || e.handoverUserId() == null) {
            return;
        }
        String to = String.valueOf(e.handoverUserId());
        for (Task task : taskService.createTaskQuery().taskAssignee(String.valueOf(e.userId())).list()) {
            FlowableUserContext.runAs(e.handoverUserId(), () -> {
                taskService.addComment(task.getId(), task.getProcessInstanceId(), ApprovalAction.TRANSFER.name(), "离职交接，转交给新的负责人");  // i18n-ignore 存库或内部异常
                taskService.setAssignee(task.getId(), to);
            });
        }
    }
}
