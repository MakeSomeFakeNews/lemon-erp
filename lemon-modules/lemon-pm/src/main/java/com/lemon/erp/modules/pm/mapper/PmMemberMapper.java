package com.lemon.erp.modules.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pm.entity.PmMember;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PmMemberMapper extends BaseMapper<PmMember> {

    /** 项目成员（含已审批工时） */
    List<PmMember> selectByProject(@Param("projectId") Long projectId);

    /** 资源负荷：规划中、进行中项目的全部成员 */
    List<PmMember> selectActiveAssignments(@Param("deptId") Long deptId);
}
