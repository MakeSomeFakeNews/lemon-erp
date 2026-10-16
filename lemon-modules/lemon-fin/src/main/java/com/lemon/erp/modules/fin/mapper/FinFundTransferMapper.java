package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinFundTransfer;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface FinFundTransferMapper extends BaseMapper<FinFundTransfer> {

    IPage<FinFundTransfer> selectPageList(IPage<FinFundTransfer> page, @Param("keyword") String keyword,
                                          @Param("begin") LocalDate begin, @Param("end") LocalDate end);
}
