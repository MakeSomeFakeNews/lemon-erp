package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.wms.entity.WmsReservation;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface WmsReservationMapper extends BaseMapper<WmsReservation> {

    Page<WmsReservation> selectPageList(Page<WmsReservation> page, @Param("warehouseId") Long warehouseId,
                                        @Param("keyword") String keyword, @Param("sourceNo") String sourceNo);

    List<WmsReservation> selectBySource(@Param("sourceType") String sourceType, @Param("sourceId") Long sourceId);

    /** 仓库中各物料的可用库存（库存 - 冻结）与预留：[{materialId, available, reserved}] */
    List<Map<String, Object>> selectFree(@Param("warehouseId") Long warehouseId, @Param("materialIds") Collection<Long> materialIds);

    /** 核销：剩余预留不足时只扣到 0 */
    int consume(@Param("id") Long id, @Param("qty") BigDecimal qty);
}
