package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.wms.dto.WarehouseAlertView;
import com.lemon.erp.modules.wms.entity.WmsMaterialWarehouseLimit;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分仓库的库存上下限。XML 中的 warehouseAlerts、materialAlerts 片段是库存预警的统一口径，供各模块的预警查询只读引用
 */
public interface WmsMaterialWarehouseLimitMapper extends BaseMapper<WmsMaterialWarehouseLimit> {

    /** 物料各仓库的上下限与当前库存（按仓库排序），只含设置了上下限的仓库 */
    List<WarehouseAlertView> selectWarehouseAlerts(@Param("materialId") Long materialId);

    /** 单条多值 INSERT */
    void insertBatch(@Param("list") List<WmsMaterialWarehouseLimit> list);
}
