package com.lemon.erp.modules.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pm.entity.PmProject;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface PmProjectMapper extends BaseMapper<PmProject> {

    /**
     * 项目及统计：任务数、完成数、逾期数、成员数、人工成本、费用、已开票、已收款
     *
     * @param keyword 编号或名称
     */
    IPage<PmProject> selectPageList(IPage<PmProject> page, @Param("keyword") String keyword, @Param("status") String status,
                                    @Param("type") String type, @Param("managerId") Long managerId,
                                    @Param("customerId") Long customerId, @Param("today") LocalDate today);

    PmProject selectDetail(@Param("id") Long id, @Param("today") LocalDate today);

    /**
     * 项目下拉：指定 ids 时只取这些项目（回显），否则为规划中、审批中、进行中、已暂停的项目，按编码或名称搜索；
     * userId 不为空时只返回该用户参与的项目；分页取一页（不查总数）
     */
    List<PmProject> selectOptions(IPage<PmProject> page, @Param("userId") Long userId, @Param("keyword") String keyword, @Param("ids") List<Long> ids);
}
