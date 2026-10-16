package com.lemon.erp.modules.fin.mapper;

import com.lemon.erp.modules.fin.dto.CostVariance;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 成本差异分析：读取工单成本、工单领退料与标准成本（直接关联生产模块的表）
 */
public interface FinCostVarianceMapper {

    /**
     * 期间内完工（最后一次完工入库的业务日期在期间内）或关闭的非返工工单，实际成本为截至该期间的各期工单成本合计
     *
     * @param next 期间结束的次日
     */
    List<CostVariance> selectOrders(@Param("period") int period, @Param("begin") LocalDate begin, @Param("next") LocalDate next,
                                    @Param("versionId") Long versionId);

    /** 工单用料：materialId、unitQty */
    List<Map<String, Object>> selectWoMaterials(@Param("woId") Long woId);

    /** 工单在 next 之前的净领料（领料 + 补料 - 退料），按领料当期的加权平均单价计算实际金额：materialId、qty、amount */
    List<Map<String, Object>> selectIssues(@Param("woId") Long woId, @Param("next") LocalDate next);
}
