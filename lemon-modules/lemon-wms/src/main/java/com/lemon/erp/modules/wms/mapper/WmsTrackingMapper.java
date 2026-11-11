package com.lemon.erp.modules.wms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 切换物料批次 / 序列号管理前检查进行中的仓库单据
 */
@Mapper
public interface WmsTrackingMapper {

    /** 含该物料、在途（未收货）或审批中的出入库 / 调拨单号 */
    List<String> selectOpenStockDocs(@Param("materialId") Long materialId);

    /** 含该物料、草稿或审批中的盘点单号 */
    List<String> selectOpenChecks(@Param("materialId") Long materialId);
}
