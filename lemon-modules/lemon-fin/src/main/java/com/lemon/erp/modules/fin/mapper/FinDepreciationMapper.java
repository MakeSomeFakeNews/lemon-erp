package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinDepreciation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinDepreciationMapper extends BaseMapper<FinDepreciation> {

    IPage<FinDepreciation> selectPageList(IPage<FinDepreciation> page, @Param("period") Integer period, @Param("assetId") Long assetId);

    void insertBatch(@Param("list") List<FinDepreciation> list);
}
