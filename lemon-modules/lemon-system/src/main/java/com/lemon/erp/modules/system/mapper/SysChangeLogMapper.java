package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.system.entity.SysChangeLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysChangeLogMapper extends BaseMapper<SysChangeLog> {

    void insertBatch(@Param("list") List<SysChangeLog> list);

    /** 某条记录的变更历史（含修改人），最新的在前 */
    Page<SysChangeLog> selectByBiz(Page<SysChangeLog> page, @Param("bizType") String bizType, @Param("bizId") Long bizId);
}
