package com.lemon.erp.modules.pm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.org.EmployeeLeftEvent;
import com.lemon.erp.modules.pm.entity.PmMember;
import com.lemon.erp.modules.pm.entity.PmProject;
import com.lemon.erp.modules.pm.entity.PmTask;
import com.lemon.erp.modules.pm.mapper.PmMemberMapper;
import com.lemon.erp.modules.pm.mapper.PmProjectMapper;
import com.lemon.erp.modules.pm.mapper.PmTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 离职交接：担任项目经理的项目、负责的未完成任务移交给交接人；离职人员在项目中的投入比例清零（资源分配不再计入）
 */
@Service
@RequiredArgsConstructor
public class PmHandoverService {

    private final PmProjectMapper projectMapper;
    private final PmTaskMapper taskMapper;
    private final PmMemberMapper memberMapper;

    @EventListener
    public void onEmployeeLeft(EmployeeLeftEvent e) {
        if (e.userId() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        memberMapper.update(Wrappers.<PmMember>lambdaUpdate().set(PmMember::getAllocation, 0).set(PmMember::getUpdateTime, now)
                .eq(PmMember::getUserId, e.userId()));
        Long to = e.handoverUserId();
        if (to == null) {
            return;
        }
        projectMapper.update(Wrappers.<PmProject>lambdaUpdate().set(PmProject::getManagerId, to).set(PmProject::getUpdateTime, now)
                .eq(PmProject::getManagerId, e.userId()));
        taskMapper.update(Wrappers.<PmTask>lambdaUpdate().set(PmTask::getAssigneeId, to).set(PmTask::getUpdateTime, now)
                .eq(PmTask::getAssigneeId, e.userId()).ne(PmTask::getStatus, "DONE"));
    }
}
