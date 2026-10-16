package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.mes.entity.MesMrpResult;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MesMrpResultMapper extends BaseMapper<MesMrpResult> {

    /** 运算结果，按低层码、物料编码排序 */
    List<MesMrpResult> selectByRun(@Param("runId") Long runId);

    void insertBatch(@Param("list") List<MesMrpResult> list);
}
