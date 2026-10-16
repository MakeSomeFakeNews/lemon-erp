package com.lemon.erp.modules.bi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.bi.entity.BiDashboard;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BiDashboardMapper extends BaseMapper<BiDashboard> {

    /** 本人创建的和共享的仪表盘 */
    List<BiDashboard> selectVisible(@Param("userId") Long userId);
}
