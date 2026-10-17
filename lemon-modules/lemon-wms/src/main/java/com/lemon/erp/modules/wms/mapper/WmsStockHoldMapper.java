package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.entity.WmsStockHold;
import com.lemon.erp.modules.wms.model.HoldStatus;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface WmsStockHoldMapper extends BaseMapper<WmsStockHold> {

    IPage<WmsStockHold> selectPageList(IPage<WmsStockHold> page, @Param("status") String status,
                                       @Param("keyword") String keyword, @Param("sourceNo") String sourceNo);

    /** 扣减冻结中的数量，数量不足时返回 0；减到 0 时变为 doneStatus */
    int consume(@Param("id") Long id, @Param("qty") BigDecimal qty, @Param("doneStatus") HoldStatus doneStatus);

    /**
     * 待检的数量（MRP 视为将要可用）
     *
     * @return [{materialId, qty}]
     */
    List<Map<String, Object>> selectPendingQty(@Param("materialIds") Collection<Long> materialIds);
}
