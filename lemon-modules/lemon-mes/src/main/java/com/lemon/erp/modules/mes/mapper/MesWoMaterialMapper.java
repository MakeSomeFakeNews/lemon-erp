package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.mes.dto.KitLineView;
import com.lemon.erp.modules.mes.entity.MesWoMaterial;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MesWoMaterialMapper extends BaseMapper<MesWoMaterial> {

    List<MesWoMaterial> selectByWo(@Param("woId") Long woId);

    void insertBatch(@Param("list") List<MesWoMaterial> list);

    /**
     * 领料（正数）/ 退料（负数），退料不能超过已领数量
     *
     * @param overIssueRate 领料时传允许超领比例（%）：累计已领不能超过 需求 × (1 + 比例) + 已补料；退料传 null
     */
    int addIssued(@Param("id") Long id, @Param("qty") BigDecimal qty, @Param("overIssueRate") BigDecimal overIssueRate);

    /** 补料：已领、已补料同时增加 */
    int addSupplement(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /**
     * 未结束工单的全部用料（含工单、产品信息），按齐套分配的优先级排序：已下达 / 生产中在前，再按计划开工日期、工单 ID
     *
     * @param materialIds 只查这些物料，为空查全部（同一物料的分配与其他物料无关）
     */
    List<KitLineView> selectOpenLines(@Param("statuses") Collection<String> statuses,
                                      @Param("materialIds") Collection<Long> materialIds);

    /**
     * 未结束工单尚未领用的数量，按物料（MRP 已分配）
     *
     * @return [{materialId, qty}]
     */
    List<Map<String, Object>> selectOpenAllocated(@Param("statuses") Collection<String> statuses,
                                                  @Param("materialIds") Collection<Long> materialIds);
}
