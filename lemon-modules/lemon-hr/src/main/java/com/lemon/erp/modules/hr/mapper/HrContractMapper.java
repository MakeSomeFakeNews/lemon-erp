package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.hr.entity.HrContract;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface HrContractMapper extends BaseMapper<HrContract> {

    Page<HrContract> selectPageList(Page<HrContract> page, @Param("keyword") String keyword, @Param("status") String status,
                                    @Param("contractType") String contractType, @Param("expireBefore") LocalDate expireBefore,
                                    @Param("employeeId") Long employeeId);

    HrContract selectDetail(@Param("id") Long id);

    /** 生效中且在指定日期前到期的合同（到期提醒） */
    List<HrContract> selectExpiring(@Param("before") LocalDate before);
}
