package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinWoCost;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinWoCostMapper extends BaseMapper<FinWoCost> {

    IPage<FinWoCost> selectPageList(IPage<FinWoCost> page, @Param("period") int period);

    void insertBatch(@Param("list") List<FinWoCost> list);
}
