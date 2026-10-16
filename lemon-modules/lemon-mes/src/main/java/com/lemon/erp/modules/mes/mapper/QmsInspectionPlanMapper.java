package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.QmsInspectionPlan;
import com.lemon.erp.modules.mes.model.QcType;
import org.apache.ibatis.annotations.Param;

public interface QmsInspectionPlanMapper extends BaseMapper<QmsInspectionPlan> {

    IPage<QmsInspectionPlan> selectPageList(IPage<QmsInspectionPlan> page, @Param("type") QcType type,
                                            @Param("keyword") String keyword);

    QmsInspectionPlan selectDetail(@Param("id") Long id);

    /** 匹配的启用方案：物料方案优先，其次物料分类方案 */
    QmsInspectionPlan selectMatch(@Param("type") QcType type, @Param("materialId") Long materialId);
}
