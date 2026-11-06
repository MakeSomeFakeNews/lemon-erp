package com.lemon.erp.modules.wms.ai;

import com.lemon.erp.modules.wms.dto.OptionItem;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI 问数工具按模型给出的文字（编码或名称）匹配仓库、物料，匹配不上或有多个时提示模型（见 {@link AiQuerySupport#pick}）
 */
@Component
@RequiredArgsConstructor
public class WmsAiLookup {

    private final WmsWarehouseService warehouseService;
    private final WmsMaterialService materialService;

    /** 仓库 ID，keyword 为空时返回 null（全部仓库） */
    public Long warehouseId(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return AiQuerySupport.pick(warehouseService.options(), keyword, OptionItem::code, OptionItem::name,
                "wms.error.aiWarehouseNotFound", "wms.error.aiWarehouseAmbiguous").id();
    }

    /** 启用的物料（具体 SKU），按编码或名称匹配一个 */
    public WmsMaterial material(String keyword) {
        return AiQuerySupport.pick(materialService.options(keyword.trim()), keyword, WmsMaterial::getCode, WmsMaterial::getName,
                "wms.error.aiMaterialNotFound", "wms.error.aiMaterialAmbiguous");
    }
}
