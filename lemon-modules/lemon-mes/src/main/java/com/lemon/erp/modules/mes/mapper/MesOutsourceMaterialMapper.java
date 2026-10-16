package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.mes.entity.MesOutsourceMaterial;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MesOutsourceMaterialMapper extends BaseMapper<MesOutsourceMaterial> {

    List<MesOutsourceMaterial> selectByOrder(@Param("orderId") Long orderId);

    void insertBatch(@Param("list") List<MesOutsourceMaterial> list);

    int addIssued(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /** 退料不能超过加工商处结存（已发 - 已退 - 已核销） */
    int addReturned(@Param("id") Long id, @Param("qty") BigDecimal qty);

    int addConsumed(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /**
     * 采购订单中委外件的明细（订单数量）
     *
     * @return [{orderItemId, materialId, qty}]
     */
    List<Map<String, Object>> selectOrderItems(@Param("orderId") Long orderId);

    /**
     * 采购入库 / 退货单明细（核销用料）
     *
     * @return [{orderId, orderItemId, materialId, qty}]
     */
    List<Map<String, Object>> selectTradeItems(@Param("docId") Long docId);
}
