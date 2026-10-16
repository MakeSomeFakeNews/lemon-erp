package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinMaterialCost;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinMaterialCostMapper extends BaseMapper<FinMaterialCost> {

    /** @param keyword 物料编码或名称 */
    IPage<FinMaterialCost> selectPageList(IPage<FinMaterialCost> page, @Param("period") int period, @Param("keyword") String keyword);

    void insertBatch(@Param("list") List<FinMaterialCost> list);
}
