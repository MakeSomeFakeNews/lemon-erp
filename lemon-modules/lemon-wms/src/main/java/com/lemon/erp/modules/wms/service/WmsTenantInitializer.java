package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.wms.entity.WmsAttribute;
import com.lemon.erp.modules.wms.entity.WmsAttributeValue;
import com.lemon.erp.modules.wms.entity.WmsMaterialCategory;
import com.lemon.erp.modules.wms.mapper.WmsAttributeMapper;
import com.lemon.erp.modules.wms.mapper.WmsAttributeValueMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialCategoryMapper;
import com.lemon.erp.modules.wms.model.MaterialCategoryTree;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新租户复制平台的规格属性及属性值（颜色、尺码）、物料分类（保持层级和编码），按默认设置初始化价格列
 */
@Component
@RequiredArgsConstructor
public class WmsTenantInitializer implements TenantInitializer {

    private final WmsAttributeMapper attributeMapper;
    private final WmsAttributeValueMapper valueMapper;
    private final WmsPriceLevelService priceLevelService;
    private final WmsMaterialCategoryMapper categoryMapper;

    @Override
    public void initialize(Context context) {
        priceLevelService.initDefaults();
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
        copyCategories();
    }

    /** 按树的先序复制平台的物料分类（上级先插入），上级 ID、祖级路径换成新租户中的 ID */
    private void copyCategories() {
        MaterialCategoryTree tree = MaterialCategoryTree.of(TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> categoryMapper.selectList(Wrappers.emptyWrapper())));
        Map<Long, WmsMaterialCategory> copies = new HashMap<>();
        for (WmsMaterialCategory c : tree.ordered()) {
            WmsMaterialCategory parent = copies.get(c.getParentId());
            Long oldId = c.getId();
            WmsMaterialCategory copy = TenantInitializer.asNew(c);
            copy.setParentId(parent == null ? 0L : parent.getId());
            copy.setAncestors(parent == null ? "0" : parent.getAncestors() + "," + parent.getId());
            categoryMapper.insert(copy);
            copies.put(oldId, copy);
        }
    }
}
