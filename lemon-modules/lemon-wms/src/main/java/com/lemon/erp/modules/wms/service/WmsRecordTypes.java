package com.lemon.erp.modules.wms.service;

import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 仓库的业务记录类型（附件、变更历史）：物料（图纸、规格书）
 */
@Component
@RequiredArgsConstructor
public class WmsRecordTypes implements RecordTypeProvider {

    private final WmsMaterialMapper materialMapper;

    @Override
    public List<RecordType> recordTypes() {
        return List.of(new RecordType("wms_material", "wms.label.material", "wms:material:list", "wms:material:edit",
                null, materialMapper::selectById));
    }
}
