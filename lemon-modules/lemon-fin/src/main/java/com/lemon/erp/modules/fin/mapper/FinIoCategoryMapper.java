package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.fin.entity.FinIoCategory;
import com.lemon.erp.modules.fin.model.FundDirection;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinIoCategoryMapper extends BaseMapper<FinIoCategory> {

    /** 收支类别（含科目名称、是否已使用），按方向、排序 */
    List<FinIoCategory> selectViewList(@Param("direction") FundDirection direction, @Param("status") EnableStatus status);
}
