package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.scm.mapper.ScmRefMapper;
import com.lemon.erp.modules.wms.service.MaterialVirtualGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 切换物料的「虚拟商品」标识前的检查：改为虚拟商品时不能已用于采购订单、请购、询价、供货价格、发货通知；
 * 改为实物时不能已有出入库单或草稿（虚拟商品的出入库没有库存流水和出库成本）
 */
@Component
@RequiredArgsConstructor
public class ScmMaterialVirtualGuard implements MaterialVirtualGuard {

    private final ScmRefMapper refMapper;

    @Override
    public String findBlocker(Long materialId, boolean toVirtual) {
        String key = toVirtual ? refMapper.findStockUse(materialId) : refMapper.findVirtualUse(materialId);
        return key == null ? null : Messages.get(key);
    }
}
