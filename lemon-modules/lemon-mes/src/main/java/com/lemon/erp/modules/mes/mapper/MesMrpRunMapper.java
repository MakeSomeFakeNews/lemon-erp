package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.MesMrpRun;

public interface MesMrpRunMapper extends BaseMapper<MesMrpRun> {

    IPage<MesMrpRun> selectPageList(IPage<MesMrpRun> page);
}
