package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ScmOrderItemMapper extends BaseMapper<ScmOrderItem> {

    /** 订单明细，带物料信息 */
    List<ScmOrderItem> selectByOrderId(@Param("orderId") Long orderId);

    void insertBatch(@Param("list") List<ScmOrderItem> list);

    /**
     * 回写已入库 / 已出库数量：不超过未完成数量（条件更新，并发安全），返回 0 表示超出
     *
     * @param overRate 允许超过订单数量的比例（%），0 为不允许
     */
    int addDone(@Param("id") Long id, @Param("qty") BigDecimal qty, @Param("overRate") BigDecimal overRate);

    /** 回写已退货数量：不超过实际完成数量 */
    int addReturn(@Param("id") Long id, @Param("qty") BigDecimal qty, @Param("replenish") boolean replenish);
}
