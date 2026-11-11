package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.dto.IoRequests;
import com.lemon.erp.modules.fin.entity.FinFundTransfer;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface FinFundTransferMapper extends BaseMapper<FinFundTransfer> {

    IPage<FinFundTransfer> selectPageList(IPage<FinFundTransfer> page, @Param("keyword") String keyword,
                                          @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    FinFundTransfer selectDetail(@Param("id") Long id);

    /** 期间内的手续费：笔数（手续费不为 0 的调拨）、金额 */
    IoRequests.SummaryRow selectFeeSummary(@Param("begin") LocalDate begin, @Param("end") LocalDate end);
}
