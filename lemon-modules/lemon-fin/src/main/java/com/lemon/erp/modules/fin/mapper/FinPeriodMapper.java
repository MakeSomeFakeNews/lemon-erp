package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinPeriod;
import org.apache.ibatis.annotations.Param;

public interface FinPeriodMapper extends BaseMapper<FinPeriod> {

    /** 期间列表，带凭证数、未记账凭证数 */
    IPage<FinPeriod> selectPageList(IPage<FinPeriod> page);

    /** 取下一个凭证号：期间未结账时流水 + 1（行锁到事务结束），返回 0 表示期间已结账 */
    int nextSeq(@Param("period") int period);
}
