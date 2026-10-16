package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.QmsSupplierClaim;
import com.lemon.erp.modules.mes.model.ClaimStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface QmsSupplierClaimMapper extends BaseMapper<QmsSupplierClaim> {

    /** @param portal 供应商门户：不显示待确认的 */
    IPage<QmsSupplierClaim> selectPageList(IPage<QmsSupplierClaim> page, @Param("status") ClaimStatus status,
                                           @Param("supplierId") Long supplierId, @Param("keyword") String keyword,
                                           @Param("portal") boolean portal);

    QmsSupplierClaim selectDetail(@Param("id") Long id);

    /**
     * 已确认的索赔按供应商统计
     *
     * @return [{supplierId, claims, amount}]
     */
    List<Map<String, Object>> selectStats(@Param("supplierIds") Collection<Long> supplierIds,
                                          @Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate);
}
