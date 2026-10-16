package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.hr.entity.HrPerfAppraisal;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface HrPerfAppraisalMapper extends BaseMapper<HrPerfAppraisal> {

    /**
     * @param reviewerId 评分人（我评分的）
     * @param employeeId 被考核人（我的考核）
     * @param keyword    工号或姓名
     */
    IPage<HrPerfAppraisal> selectPageList(IPage<HrPerfAppraisal> page, @Param("planId") Long planId,
                                          @Param("status") String status, @Param("deptId") Long deptId,
                                          @Param("reviewerId") Long reviewerId, @Param("employeeId") Long employeeId,
                                          @Param("keyword") String keyword);

    HrPerfAppraisal selectDetail(@Param("id") Long id);

    void insertBatch(@Param("items") List<HrPerfAppraisal> items);

    /** 绩效奖金：发放月份为 period 的已归档考核中已完成的考核表（员工、等级） */
    List<HrPerfAppraisal> selectPayGrades(@Param("period") int period);

    /** 等级分布 */
    List<Map<String, Object>> selectGradeStats(@Param("planId") Long planId);
}
