package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.system.dto.ImBindingQuery;
import com.lemon.erp.modules.system.dto.ImBindingRow;
import com.lemon.erp.modules.system.entity.SysImUser;
import org.apache.ibatis.annotations.Param;

public interface SysImUserMapper extends BaseMapper<SysImUser> {

    /** 内部用户（不含门户账号）及其在指定平台上的绑定 */
    IPage<ImBindingRow> selectBindingPage(IPage<ImBindingRow> page, @Param("q") ImBindingQuery query);
}
