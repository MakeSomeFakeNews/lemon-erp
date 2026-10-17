package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.dto.OrderQuery;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ScmOrderMapper extends BaseMapper<ScmOrder> {

    /** 分页查询某种订单，带往来单位、仓库、制单人名称及完成数量 */
    IPage<ScmOrder> selectPageList(IPage<ScmOrder> page, @Param("kind") OrderKind kind, @Param("q") OrderQuery query);

    ScmOrder selectDetail(@Param("id") Long id);

    /** 反审核：已审核且没有入库 / 出库过的订单回到草稿，返回 0 表示不满足条件 */
    int unapprove(@Param("id") Long id);

    /** 客户未完成订单（未发货部分）的金额，排除指定订单 */
    BigDecimal selectOpenSalesAmount(@Param("customerId") Long customerId, @Param("excludeId") Long excludeId,
                                     @Param("statuses") Collection<OrderStatus> statuses);

    /**
     * 未结束订单的未完成数量，按物料汇总（MRP：采购在途 / 销售待发）
     *
     * @return [{materialId, qty}]
     */
    List<Map<String, Object>> selectOpenQty(@Param("kind") OrderKind kind, @Param("statuses") Collection<OrderStatus> statuses,
                                            @Param("materialIds") Collection<Long> materialIds);

    /**
     * 供应商门户：本供应商已审核及以后的采购订单
     *
     * @param pending 只看未确认的
     */
    IPage<ScmOrder> selectPortalPage(IPage<ScmOrder> page, @Param("supplierId") Long supplierId, @Param("pending") Boolean pending,
                                     @Param("keyword") String keyword);
}
