package com.lemon.erp.modules.wms.service;

import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceChecker;
import com.lemon.erp.modules.wms.mapper.WmsRefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 删除主数据前检查本模块的单据是否引用
 */
@Component
@RequiredArgsConstructor
public class WmsReferenceChecker implements ReferenceChecker {

    private final WmsRefMapper refMapper;

    @Override
    public String findReference(RefType type, Long id) {
        return switch (type) {
            case USER -> refMapper.findUser(id);
            default -> null;
        };
    }
}
