package com.lemon.erp.modules.fin.model;

/**
 * 凭证来源，与字典 fin_voucher_source 对应。业务生成的凭证删除后，来源单据可以重新生成
 */
public enum VoucherSource {
    MANUAL,
    /** 应收单 */
    AR,
    /** 应付单 */
    AP,
    /** 收款单 */
    RECEIPT,
    /** 付款单 */
    PAYMENT,
    /** 折旧计提 */
    DEPRECIATION,
    /** 资产处置 */
    ASSET,
    /** 成本结转 */
    COST,
    /** 期末结转损益（结账时自动生成并记账） */
    CLOSE,
    /** 工资计提、发放（人力资源） */
    SALARY,
    PROJECT,
    EXPENSE,
    /** 预收预付及冲抵 */
    ADVANCE,
    /** 坏账核销 */
    BAD_DEBT,
    /** 红字冲销 */
    REVERSAL,
    /** 增值税结转、附加税计提 */
    TAX,
    /** 费用报销 */
    CLAIM,
    /** 员工借款、还款 */
    LOAN,
    /** 期末调汇 */
    FX,
    /** 票据贴现、托收、拒付、兑付 */
    NOTE,
    /** 账户调拨 */
    TRANSFER
}
