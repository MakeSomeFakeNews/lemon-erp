package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.dto.SrmViews;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmPurchaseContract;
import com.lemon.erp.modules.scm.model.ContractStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScmPurchaseContractMapper extends BaseMapper<ScmPurchaseContract> {

    IPage<ScmPurchaseContract> selectPageList(IPage<ScmPurchaseContract> page, @Param("status") ContractStatus status,
                                              @Param("supplierId") Long supplierId, @Param("keyword") String keyword);

    ScmPurchaseContract selectDetail(@Param("id") Long id);

    /** 供应商在指定日期有效的生效合同（采购订单选择） */
    List<ScmPurchaseContract> selectActive(@Param("supplierId") Long supplierId, @Param("date") LocalDate date);

    /** 合同各物料的执行情况，排除指定订单（修改订单时排除自身） */
    List<SrmViews.ContractUsage> selectUsage(@Param("contractId") Long contractId, @Param("excludeOrderId") Long excludeOrderId);

    /** 引用合同的订单 */
    List<ScmOrder> selectOrders(@Param("contractId") Long contractId);

    /** 出入库单对应订单引用的合同的付款天数 */
    Integer selectPaymentDaysByTradeDoc(@Param("tradeDocId") Long tradeDocId);

    int transit(@Param("id") Long id, @Param("from") ContractStatus from, @Param("to") ContractStatus to);
}
