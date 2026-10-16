package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.entity.FinAccountOpening;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinAccountOpeningMapper extends BaseMapper<FinAccountOpening> {

    void insertBatch(@Param("list") List<FinAccountOpening> list);
}
