package com.lemon.erp.modules.mes.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.mes.mapper.MesRefMapper;
import com.lemon.erp.modules.wms.service.MaterialVirtualGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 切换物料的「虚拟商品」标识前的检查：已用于 BOM、替代料、生产计划、委外、工单的物料不能改为虚拟商品；
 * 虚拟商品不能用于生产，改为实物没有限制
 */
@Component
@RequiredArgsConstructor
public class MesMaterialVirtualGuard implements MaterialVirtualGuard {

    private final MesRefMapper refMapper;

    @Override
    public String findBlocker(Long materialId, boolean toVirtual) {
        if (!toVirtual) {
            return null;
        }
        String key = refMapper.findStockUse(materialId);
        return key == null ? null : Messages.get(key);
    }
}
