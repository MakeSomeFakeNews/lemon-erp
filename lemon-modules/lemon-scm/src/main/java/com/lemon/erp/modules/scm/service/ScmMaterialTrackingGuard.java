package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.scm.mapper.ScmDeliveryMapper;
import com.lemon.erp.modules.wms.service.MaterialTrackingGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 切换物料批次 / 序列号管理前的检查：尚未出库的发货通知（拣货明细已确定库位、批次和序列号）
 */
@Component
@RequiredArgsConstructor
public class ScmMaterialTrackingGuard implements MaterialTrackingGuard {

    private final ScmDeliveryMapper deliveryMapper;

    @Override
    public List<String> openDocs(Long materialId) {
        return deliveryMapper.selectOpenNosByMaterial(materialId).stream()
                .map(no -> Messages.get("scm.text.openDelivery", no)).toList();
    }
}
