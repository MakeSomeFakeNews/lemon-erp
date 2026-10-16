package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.scm.entity.ScmDelivery;
import com.lemon.erp.modules.scm.entity.ScmDeliveryItem;
import com.lemon.erp.modules.scm.entity.ScmDeliveryPick;
import com.lemon.erp.modules.scm.model.DeliveryStatus;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ScmDeliveryMapper extends BaseMapper<ScmDelivery> {

    /** 波次中的发货通知 */
    List<ScmDelivery> selectByWave(@Param("waveId") Long waveId);

    Page<ScmDelivery> selectPageList(Page<ScmDelivery> page, @Param("keyword") String keyword, @Param("status") DeliveryStatus status,
                                     @Param("warehouseId") Long warehouseId, @Param("orderId") Long orderId);

    ScmDelivery selectDetail(@Param("id") Long id);

    List<ScmDeliveryItem> selectItems(@Param("deliveryId") Long deliveryId);

    List<ScmDeliveryPick> selectPicks(@Param("deliveryId") Long deliveryId);

    /** 订单明细在未完成（待拣货、已拣货）的发货通知中的数量：[{orderItemId, qty}] */
    List<Map<String, Object>> selectNoticedQty(@Param("orderId") Long orderId);
}
