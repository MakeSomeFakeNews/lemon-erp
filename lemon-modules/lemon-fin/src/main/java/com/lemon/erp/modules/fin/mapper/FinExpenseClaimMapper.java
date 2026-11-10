package com.lemon.erp.modules.fin.mapper;

import com.lemon.erp.modules.fin.model.ExpenseClaimStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.fin.dto.ClaimSummaryRow;
import com.lemon.erp.modules.fin.entity.FinExpenseClaim;
import com.lemon.erp.modules.fin.entity.FinExpenseClaimItem;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface FinExpenseClaimMapper extends BaseMapper<FinExpenseClaim> {

    Page<FinExpenseClaim> selectPageList(Page<FinExpenseClaim> page, @Param("keyword") String keyword, @Param("status") ExpenseClaimStatus status,
                                         @Param("userId") Long userId);

    FinExpenseClaim selectDetail(@Param("id") Long id);

    List<FinExpenseClaimItem> selectItems(@Param("claimId") Long claimId);

    /** 在途（审批中、待付款）报销金额按部门、科目汇总，不含指定的报销单：[{deptId, accountId, amount}] */
    List<Map<String, Object>> selectCommitted(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("excludeId") Long excludeId);

    /**
     * 费用报销汇总（报销日期范围，不含草稿）：byAccount 为 true 时按费用科目汇总明细金额，否则按部门汇总报销金额
     *
     * @param statuses 计入的状态
     */
    List<ClaimSummaryRow> selectSummary(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("byAccount") boolean byAccount,
                                        @Param("statuses") List<ExpenseClaimStatus> statuses);
}
