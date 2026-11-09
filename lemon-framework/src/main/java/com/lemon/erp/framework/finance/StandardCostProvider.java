package com.lemon.erp.framework.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

/**
 * 标准成本：生产完工入库的实时库存成本优先取产品的标准成本，由财务模块实现（{@code FinStandardCostProvider}）
 */
public interface StandardCostProvider {

    /**
     * 单位标准成本及其料工费构成（采购件只有材料）
     *
     * @param unitCost 单位标准成本
     * @param material 其中材料
     * @param labor    其中直接人工
     * @param overhead 其中制造费用
     */
    record StandardCost(BigDecimal unitCost, BigDecimal material, BigDecimal labor, BigDecimal overhead) {
    }

    /**
     * 业务日期所在期间生效的标准成本版本（生效期间不晚于该期间的最近一个已发布版本）中各物料的单位标准成本
     *
     * @return 物料 ID → 标准成本；没有生效版本、物料不在版本中或标准成本为 0 的不在结果中
     */
    Map<Long, StandardCost> costs(LocalDate bizDate, Collection<Long> materialIds);
}
