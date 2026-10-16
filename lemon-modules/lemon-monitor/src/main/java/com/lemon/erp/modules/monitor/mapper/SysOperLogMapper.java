package com.lemon.erp.modules.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.monitor.entity.SysOperLog;

public interface SysOperLogMapper extends BaseMapper<SysOperLog> {

    /** 清空日志：TRUNCATE 不逐行删除、不产生大量 undo/binlog，并回收空间 */
    void truncate();

    /** 清空当前租户的日志（SaaS 模式） */
    void deleteAll();
}
