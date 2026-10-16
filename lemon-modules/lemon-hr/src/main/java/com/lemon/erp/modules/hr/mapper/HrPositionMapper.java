package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.hr.entity.HrPosition;
import org.apache.ibatis.annotations.Param;

public interface HrPositionMapper extends BaseMapper<HrPosition> {

    /** @param keyword 编码或名称 */
    IPage<HrPosition> selectPageList(IPage<HrPosition> page, @Param("keyword") String keyword,
                                     @Param("category") String category, @Param("status") Integer status);
}
