package com.lemon.erp.modules.mes.service;

import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceChecker;
import com.lemon.erp.modules.mes.mapper.MesRefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 删除主数据前检查本模块的单据是否引用
 */
@Component
@RequiredArgsConstructor
public class MesReferenceChecker implements ReferenceChecker {

    private final MesRefMapper refMapper;

    @Override
    public String findReference(RefType type, Long id) {
        // 引用单据名称按请求语言翻译（译文在 i18n/mes.en-US.json）
        return I18n.t(switch (type) {
            case MATERIAL -> refMapper.findMaterial(id);
            case WAREHOUSE -> refMapper.findWarehouse(id);
            case SUPPLIER -> refMapper.findSupplier(id);
            case USER -> refMapper.findUser(id);
            case DEPT -> refMapper.findDept(id);
            default -> null;
        });
    }
}
