package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.dto.OffsetRequests;
import com.lemon.erp.modules.fin.entity.FinOffset;
import org.apache.ibatis.annotations.Param;

public interface FinOffsetMapper extends BaseMapper<FinOffset> {

    IPage<FinOffset> selectPageList(IPage<FinOffset> page, @Param("q") OffsetRequests.Query query);

    FinOffset selectDetail(@Param("id") Long id);
}
