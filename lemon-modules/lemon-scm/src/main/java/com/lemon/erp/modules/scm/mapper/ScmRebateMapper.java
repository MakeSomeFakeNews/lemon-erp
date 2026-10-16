package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.entity.ScmRebate;
import com.lemon.erp.modules.scm.entity.ScmRebateTier;
import com.lemon.erp.modules.scm.model.RebateStatus;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ScmRebateMapper extends BaseMapper<ScmRebate> {

    IPage<ScmRebate> selectPageList(IPage<ScmRebate> page, @Param("customerId") Long customerId, @Param("status") RebateStatus status,
                                    @Param("keyword") String keyword);

    ScmRebate selectDetail(@Param("id") Long id);

    List<ScmRebateTier> selectTiers(@Param("rebateId") Long rebateId);

    int deleteTiers(@Param("rebateId") Long rebateId);

    int insertTiers(@Param("rebateId") Long rebateId, @Param("list") List<ScmRebateTier> tiers);

    /** 客户在期间内的销售额：销售出库 - 销售退货（含税，本位币） */
    BigDecimal selectSalesAmount(@Param("customerId") Long customerId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 已结算的「下次订单抵扣」返利合计 */
    BigDecimal selectEarned(@Param("customerId") Long customerId);

    /** 销售订单已使用的返利合计（不含 excludeOrderId） */
    BigDecimal selectUsed(@Param("customerId") Long customerId, @Param("excludeOrderId") Long excludeOrderId);
}
