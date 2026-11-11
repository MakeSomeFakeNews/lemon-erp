package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.wms.entity.WmsMaterialCategory;

import java.util.List;
import java.util.Map;

public interface WmsMaterialCategoryMapper extends BaseMapper<WmsMaterialCategory> {

    /** 各分类编码直接包含的物料数（不含下级分类，与物料列表一致含多规格的款式和 SKU）：[{code, count}] */
    List<Map<String, Object>> countMaterials();
}
