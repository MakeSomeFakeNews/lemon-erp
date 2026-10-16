package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.mes.dto.MaterialPlanView;
import com.lemon.erp.modules.mes.entity.MesMaterialPlan;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface MesMaterialPlanMapper extends BaseMapper<MesMaterialPlan> {

    /** 物料（含未设置计划属性的）的计划属性摘要、默认 BOM、默认供应商、库存、替代料数量 */
    List<MaterialPlanView> selectViews(@Param("ids") Collection<Long> ids);

    List<MesMaterialPlan> selectByMaterialIds(@Param("materialIds") Collection<Long> materialIds);

    List<MaterialPlanView.WhereUsed> selectWhereUsed(@Param("materialId") Long materialId);
}
