package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.hr.entity.HrSalary;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface HrSalaryMapper extends BaseMapper<HrSalary> {

    /**
     * 在职员工及其薪资档案（没有档案的员工 id 为空）
     *
     * @param missing true 只看没有薪资档案的员工
     */
    IPage<HrSalary> selectPageList(IPage<HrSalary> page, @Param("keyword") String keyword,
                                   @Param("deptIds") Collection<Long> deptIds, @Param("missing") Boolean missing);

    /** 生产类岗位在职员工的薪资档案 */
    List<HrSalary> selectProductionSalaries();
}
