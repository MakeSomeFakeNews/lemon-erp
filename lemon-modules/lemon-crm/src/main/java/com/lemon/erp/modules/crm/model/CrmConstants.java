package com.lemon.erp.modules.crm.model;

import java.math.BigDecimal;

/**
 * 客户关系管理的约定：产品保修期、默认税率、程序依赖的业务字典内置取值。
 * 销售阶段的默认赢单概率见 {@link OpportunityStage}，工单优先级的处理时限见 {@link TicketPriority}
 */
public final class CrmConstants {

    /** 产品保修期（月），从销售出库日期起算 */
    public static final int WARRANTY_MONTHS = 12;
    /** 商机产品明细的默认税率（%） */
    public static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(13);
    /** 服务收费的默认税率（%） */
    public static final BigDecimal SERVICE_TAX_RATE = BigDecimal.valueOf(6);
    /** 客户等级（业务字典 scm_customer_level）内置取值：潜在客户，线索转化的客户使用 */
    public static final String CUSTOMER_LEVEL_POTENTIAL = "POTENTIAL";

    private CrmConstants() {
    }
}
