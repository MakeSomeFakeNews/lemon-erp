package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.entity.FinCostLayer;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinCostLayerMapper extends BaseMapper<FinCostLayer> {

    void insertBatch(@Param("list") List<FinCostLayer> list);
}
