package com.lemon.erp.modules.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.ai.dto.AiReportView;
import com.lemon.erp.modules.ai.entity.AiReport;
import com.lemon.erp.modules.ai.model.AiReportPeriodType;
import org.apache.ibatis.annotations.Param;

public interface AiReportMapper extends BaseMapper<AiReport> {

    /**
     * 当前用户能看到的报告（不含正文和快照）：本人生成的；有全部数据权限时另加自动生成的
     *
     * @param userId     当前用户
     * @param allData    是否有「全部数据」权限
     * @param periodType 期间类型，为空不限
     */
    Page<AiReportView> selectVisible(Page<AiReportView> page, @Param("userId") Long userId, @Param("allData") boolean allData,
                                     @Param("periodType") AiReportPeriodType periodType);

    /** 一份报告的列表信息（不判断可见性） */
    AiReportView selectView(@Param("id") Long id);

    String selectContent(@Param("id") Long id);

    String selectSnapshot(@Param("id") Long id);
}
