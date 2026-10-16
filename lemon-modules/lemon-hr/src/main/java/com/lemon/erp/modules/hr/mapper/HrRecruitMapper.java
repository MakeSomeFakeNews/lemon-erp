package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.hr.entity.HrRecruit;
import org.apache.ibatis.annotations.Param;

public interface HrRecruitMapper extends BaseMapper<HrRecruit> {

    /** @param keyword 单号或岗位名称 */
    IPage<HrRecruit> selectPageList(IPage<HrRecruit> page, @Param("keyword") String keyword,
                                    @Param("status") String status, @Param("deptId") Long deptId);

    HrRecruit selectDetail(@Param("id") Long id);

    /** 录用一人：招聘中的需求已录用人数 + 1，招满后自动关闭；不在招聘中返回 0 */
    int addHired(@Param("id") Long id);
}
