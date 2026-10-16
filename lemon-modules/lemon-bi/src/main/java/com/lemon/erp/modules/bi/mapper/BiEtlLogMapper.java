package com.lemon.erp.modules.bi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.bi.entity.BiEtlLog;
import org.apache.ibatis.annotations.Param;

public interface BiEtlLogMapper extends BaseMapper<BiEtlLog> {

    IPage<BiEtlLog> selectPageList(IPage<BiEtlLog> page, @Param("status") String status);
}
