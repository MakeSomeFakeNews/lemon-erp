package com.lemon.erp.modules.wms.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * 物料的最低销售价（含税、基本单位、本位币），调拨价格「最低售价」使用。价格控制在供应链模块，由其实现为 Bean；没有实现时没有最低售价
 */
public interface MinSalePriceProvider {

    /** @return 物料 ID → 最低销售价，没有设置的物料不在结果中 */
    Map<Long, BigDecimal> minSalePrices(Collection<Long> materialIds);
}
