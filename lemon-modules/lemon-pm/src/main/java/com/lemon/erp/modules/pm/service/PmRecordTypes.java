package com.lemon.erp.modules.pm.service;

import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.modules.pm.mapper.PmProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 项目业务记录类型（附件、变更历史）：项目（合同、交付文档）
 */
@Component
@RequiredArgsConstructor
public class PmRecordTypes implements RecordTypeProvider {

    private final PmProjectMapper projectMapper;

    @Override
    public List<RecordType> recordTypes() {
        return List.of(new RecordType("pm_project", "pm.label.project", "pm:project:list", "pm:project:edit",
                new DataPermissionRule("pm_project", "dept_id", "manager_id"), projectMapper::selectById));
    }
}
