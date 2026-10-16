package com.lemon.erp.framework.org;

import java.math.BigDecimal;

/**
 * 人工成本参考：由人力资源模块按薪资档案计算，生产的工作中心人工费率参考使用
 */
public interface LaborCostProvider {

    /**
     * 生产类岗位在职员工的平均小时人工成本 =（基本工资 + 津贴 + 单位社保公积金）/ 174
     *
     * @return 没有生产岗位员工或没有薪资档案时返回 null
     */
    BigDecimal productionHourlyCost();
}
