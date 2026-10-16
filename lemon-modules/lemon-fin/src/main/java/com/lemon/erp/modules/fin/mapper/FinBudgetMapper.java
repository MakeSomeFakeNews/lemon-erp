package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.entity.FinBudget;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinBudgetMapper extends BaseMapper<FinBudget> {

    List<FinBudget> selectListView(@Param("year") int year, @Param("deptId") Long deptId);
}
