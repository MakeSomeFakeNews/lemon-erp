package com.lemon.erp.modules.fin.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 销售提成的业绩数据：按销售订单的业务员统计
 */
public interface FinCommissionQueryMapper {

    /** 销售出库 / 退货（退货为负）按业务员、物料汇总：[{salesmanId, materialId, category, qty, net}] */
    List<Map<String, Object>> selectSales(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 物料的成本单价：当期核算单价，没有时取参考成本价：[{materialId, unitCost}] */
    List<Map<String, Object>> selectUnitCosts(@Param("period") int period, @Param("materialIds") Collection<Long> materialIds);

    /** 收款核销的销售出库 / 退货应收单，按业务员汇总：[{salesmanId, amount}] */
    List<Map<String, Object>> selectReceipts(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 提成表明细，带业务员名称 */
    List<com.lemon.erp.modules.fin.entity.FinCommissionLine> selectLines(@Param("commissionId") Long commissionId);

    /** 规则列表，带业务员名称 */
    List<com.lemon.erp.modules.fin.entity.FinCommissionRule> selectRules();
}
