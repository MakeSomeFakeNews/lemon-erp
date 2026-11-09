package com.lemon.erp.modules.mes.mapper;

import com.lemon.erp.modules.mes.model.WoDocKind;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.MesWoDoc;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface MesWoDocMapper extends BaseMapper<MesWoDoc> {

    IPage<MesWoDoc> selectPageList(IPage<MesWoDoc> page, @Param("woId") Long woId, @Param("kind") WoDocKind kind);

    MesWoDoc selectDetail(@Param("id") Long id);

    /**
     * 工单各类单据转入产品的成本合计（完工入库的材料 / 人工 / 制造费用，退料冲减的材料成本）
     *
     * @return [{kind, basis（完工入库的成本口径，升级前的单据为空）, material, labor, overhead}]，按种类和口径汇总
     */
    List<Map<String, Object>> selectCostSums(@Param("woId") Long woId);
}
