package com.lemon.erp.modules.mes.service;

import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import com.lemon.erp.modules.mes.mapper.MesEcnMapper;
import com.lemon.erp.modules.mes.mapper.MesTechDocMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.mes.model.MesConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 生产可挂附件、记录变更历史的单据：生产工单（图纸、作业指导书）
 */
@Component
@RequiredArgsConstructor
public class MesRecordTypes implements RecordTypeProvider {

    private final MesWorkOrderMapper workOrderMapper;
    private final MesTechDocMapper docMapper;
    private final MesEcnMapper ecnMapper;

    @Override
    public List<RecordType> recordTypes() {
        return List.of(new RecordType("mes_wo", "mes.label.workOrder", "mes:wo:list", "mes:wo:edit",
                        MesConstants.WO_RULE, workOrderMapper::selectById),
                new RecordType("mes_doc", "mes.label.technicalDocument", "mes:doc:list", "mes:doc:edit", null, docMapper::selectById),
                new RecordType("mes_ecn", "mes.label.ecn", "mes:ecn:list", "mes:ecn:edit", null, ecnMapper::selectById));
    }
}
