package com.lemon.erp.modules.scm.model;

/**
 * 运费承担方，与字典 scm_freight_bearer 对应。
 * <ul>
 *   <li>发运单（销售）：{@link #COMPANY} 本公司承担，运费计入销售费用；{@link #CUSTOMER} 客户承担，本公司代垫，运费计入客户应收，不是销售收入</li>
 *   <li>采购入库：{@link #SUPPLIER} 供应商代垫，运费计入该供应商的应付；{@link #COMPANY} 本公司承担，运费付给承运商；两种都计入存货成本</li>
 * </ul>
 */
public enum FreightBearer {
    /** 本公司承担 */
    COMPANY,
    /** 客户承担（本公司代垫，仅发运单） */
    CUSTOMER,
    /** 供应商代垫（仅采购入库） */
    SUPPLIER
}
