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

    /**
     * 回写已出入库金额、已退款金额（订单币种，在 addDone / addReturn 之后、同一事务中调用，行已加锁）
     *
     * @param doneAmount   已出入库金额的增量（退货补发时为负）
     * @param returnAmount 已退款金额的增量
     * @param doneShare    已出入库部分已分摊的整单优惠的增量
     */
    int addAmounts(@Param("id") Long id, @Param("doneAmount") BigDecimal doneAmount, @Param("returnAmount") BigDecimal returnAmount,
                   @Param("doneShare") BigDecimal doneShare);

    /** 订单关闭时加锁读取明细（关闭前已锁定订单） */
    List<ScmOrderItem> selectForClose(@Param("orderId") Long orderId);

    /** 订单关闭的尾差调整：计入已出入库金额，并记录在 close_adjust */
    int addCloseAdjust(@Param("id") Long id, @Param("amount") BigDecimal amount, @Param("net") BigDecimal net);

    /** 直接退货生成的隐式订单：视为已全部出入库（只用于回写退货数量） */
    void markAllDone(@Param("orderId") Long orderId);
}
