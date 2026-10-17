package com.lemon.erp.modules.mes.mapper;

import com.lemon.erp.modules.mes.model.PlanStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.MesPlan;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MesPlanMapper extends BaseMapper<MesPlan> {

    /** @param keyword 计划号、产品编码或名称 */
    IPage<MesPlan> selectPageList(IPage<MesPlan> page, @Param("keyword") String keyword, @Param("status") PlanStatus status);

    /** @return [{materialId, qty}] 销售订单已生成计划的数量 */
    List<Map<String, Object>> selectPlannedBySalesOrder(@Param("orderId") Long orderId);

    BigDecimal selectWoQty(@Param("planId") Long planId);

    BigDecimal selectFinishedQty(@Param("planId") Long planId);
}
