package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.wms.entity.WmsAttribute;
import com.lemon.erp.modules.wms.entity.WmsAttributeValue;
import com.lemon.erp.modules.wms.mapper.WmsAttributeMapper;
import com.lemon.erp.modules.wms.mapper.WmsAttributeValueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 新租户复制平台的规格属性及属性值（颜色、尺码）
 */
@Component
@RequiredArgsConstructor
public class WmsTenantInitializer implements TenantInitializer {

    private final WmsAttributeMapper attributeMapper;
    private final WmsAttributeValueMapper valueMapper;

    @Override
    public void initialize(Context context) {
        List<WmsAttribute> attrs = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID, () -> attributeMapper.selectList(Wrappers.emptyWrapper()));
        List<WmsAttributeValue> values = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID, () -> valueMapper.selectList(Wrappers.emptyWrapper()));
        for (WmsAttribute a : attrs) {
            Long oldId = a.getId();
            attributeMapper.insert(TenantInitializer.asNew(a));
            values.stream().filter(v -> v.getAttributeId().equals(oldId)).forEach(v -> {
                v.setAttributeId(a.getId());
                valueMapper.insert(TenantInitializer.asNew(v));
            });
        }
    }
}
