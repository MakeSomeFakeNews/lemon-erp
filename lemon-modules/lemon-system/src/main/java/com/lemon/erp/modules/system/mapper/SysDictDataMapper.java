package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysDictData;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    /** 批量插入（新租户复制业务字典） */
    void insertBatch(@Param("list") List<SysDictData> list);
}
