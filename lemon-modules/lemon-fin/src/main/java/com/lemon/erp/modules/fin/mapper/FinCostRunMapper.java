package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinCostRun;

public interface FinCostRunMapper extends BaseMapper<FinCostRun> {

    IPage<FinCostRun> selectPageList(IPage<FinCostRun> page);
}
