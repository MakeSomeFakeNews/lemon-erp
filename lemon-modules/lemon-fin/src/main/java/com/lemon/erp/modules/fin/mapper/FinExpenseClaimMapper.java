package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.fin.entity.FinExpenseClaim;
import com.lemon.erp.modules.fin.entity.FinExpenseClaimItem;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface FinExpenseClaimMapper extends BaseMapper<FinExpenseClaim> {

    Page<FinExpenseClaim> selectPageList(Page<FinExpenseClaim> page, @Param("keyword") String keyword, @Param("status") String status,
                                         @Param("userId") Long userId);

    FinExpenseClaim selectDetail(@Param("id") Long id);

    List<FinExpenseClaimItem> selectItems(@Param("claimId") Long claimId);

    /** 部门在途（审批中、待付款）报销金额按科目汇总，不含指定的报销单：[{accountId, amount}] */
    List<Map<String, Object>> selectCommitted(@Param("deptId") Long deptId, @Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                              @Param("excludeId") Long excludeId);
}
