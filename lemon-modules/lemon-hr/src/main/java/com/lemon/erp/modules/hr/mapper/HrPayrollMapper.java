package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.hr.entity.HrPayroll;
import org.apache.ibatis.annotations.Param;

public interface HrPayrollMapper extends BaseMapper<HrPayroll> {

    IPage<HrPayroll> selectPageList(IPage<HrPayroll> page, @Param("periodFrom") Integer periodFrom, @Param("periodTo") Integer periodTo,
                                    @Param("status") String status);

    HrPayroll selectDetail(@Param("id") Long id);

    /** 按明细重新汇总人数和金额 */
    void updateTotals(@Param("id") Long id);
}
