package com.lemon.erp.modules.hr.service;

import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.modules.hr.mapper.HrCandidateMapper;
import com.lemon.erp.modules.hr.mapper.HrEmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 人事的业务记录类型（附件、变更历史）：员工档案（合同、证件）、候选人（简历）
 */
@Component
@RequiredArgsConstructor
public class HrRecordTypes implements RecordTypeProvider {

    private static final DataPermissionRule EMPLOYEE_RULE = new DataPermissionRule("hr_employee", "dept_id", "user_id");

    private final HrEmployeeMapper employeeMapper;
    private final HrCandidateMapper candidateMapper;

    @Override
    public List<RecordType> recordTypes() {
        return List.of(
                new RecordType("hr_employee", "hr.label.employeeProfile", "hr:employee:list", "hr:employee:edit",
                        EMPLOYEE_RULE, employeeMapper::selectById),
                new RecordType("hr_candidate", "hr.label.candidate", "hr:recruit:list", "hr:recruit:candidate",
                        null, candidateMapper::selectById));
    }
}
