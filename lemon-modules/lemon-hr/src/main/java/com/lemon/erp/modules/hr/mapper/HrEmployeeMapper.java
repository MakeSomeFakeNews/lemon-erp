package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.hr.dto.EmployeeOption;
import com.lemon.erp.modules.hr.dto.EmployeeQuery;
import com.lemon.erp.modules.hr.entity.HrEmployee;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface HrEmployeeMapper extends BaseMapper<HrEmployee> {

    IPage<HrEmployee> selectPageList(IPage<HrEmployee> page, @Param("q") EmployeeQuery query);

    HrEmployee selectDetail(@Param("id") Long id);

    /** 在职员工，按工号或姓名搜索 */
    List<EmployeeOption> selectOptions(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 在 [from, to] 期间在职过的员工（入职不晚于 to，未离职或离职不早于 from），考勤汇总、工资计算使用
     *
     * @param deptIds 为空不限部门
     */
    List<HrEmployee> selectEmployedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                           @Param("deptIds") Collection<Long> deptIds, @Param("keyword") String keyword);

    /** 在职、试用期、本月入职、本月离职人数 */
    Map<String, Object> selectStats(@Param("monthStart") LocalDate monthStart);

    /** 在职人数按部门 / 学历分布 */
    List<Map<String, Object>> selectCountByDept();

    List<Map<String, Object>> selectCountByEducation();
}
