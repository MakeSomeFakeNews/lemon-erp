package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysNoticeTarget;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysNoticeTargetMapper extends BaseMapper<SysNoticeTarget> {

    void insertBatch(@Param("list") List<SysNoticeTarget> list);
}
