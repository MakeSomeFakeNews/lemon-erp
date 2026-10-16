package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.dto.VoucherQuery;
import com.lemon.erp.modules.fin.entity.FinVoucher;
import org.apache.ibatis.annotations.Param;

public interface FinVoucherMapper extends BaseMapper<FinVoucher> {

    IPage<FinVoucher> selectPageList(IPage<FinVoucher> page, @Param("q") VoucherQuery query);

    FinVoucher selectDetail(@Param("id") Long id);
}
