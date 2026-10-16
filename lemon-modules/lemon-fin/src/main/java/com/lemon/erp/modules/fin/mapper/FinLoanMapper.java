package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.fin.entity.FinLoan;
import com.lemon.erp.modules.fin.entity.FinLoanRepay;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface FinLoanMapper extends BaseMapper<FinLoan> {

    Page<FinLoan> selectPageList(Page<FinLoan> page, @Param("keyword") String keyword, @Param("status") String status,
                                 @Param("userId") Long userId);

    FinLoan selectDetail(@Param("id") Long id);

    List<FinLoanRepay> selectRepays(@Param("loanId") Long loanId);

    /** 还款：累计不超过借款金额，还清时状态变为已还清；返回 0 表示超出 */
    int addRepaid(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
