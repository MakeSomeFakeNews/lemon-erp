package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.hr.entity.HrTaxDeduction;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface HrTaxDeductionMapper extends BaseMapper<HrTaxDeduction> {

    Page<HrTaxDeduction> selectPageList(Page<HrTaxDeduction> page, @Param("keyword") String keyword, @Param("type") String type,
                                        @Param("employeeId") Long employeeId);

    /** 员工某月的专项附加扣除合计：[{employeeId, amount}] */
    List<Map<String, Object>> sumByPeriod(@Param("employeeIds") Collection<Long> employeeIds, @Param("period") int period);
}
