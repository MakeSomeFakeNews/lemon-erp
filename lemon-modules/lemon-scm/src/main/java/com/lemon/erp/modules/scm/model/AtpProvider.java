package com.lemon.erp.modules.scm.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 其他模块提供的未来供需（生产：在制工单产出、工单待领料），参与可承诺量、补货建议计算
 */
public interface AtpProvider {

    /** @param warehouseId 为空表示全部仓库 */
    List<AtpLine> lines(Long materialId, Long warehouseId);

    /** 各物料的未来净供需合计（全部仓库）：供应为正、需求为负，没有的物料不在结果中 */
    Map<Long, BigDecimal> netQty(Collection<Long> materialIds);

    /** 物料的供应方式（MAKE 自制 / BUY 采购 / OUTSOURCE 委外），没有设置的不在结果中 */
    default Map<Long, String> supplyTypes(Collection<Long> materialIds) {
        return Map.of();
    }
}
