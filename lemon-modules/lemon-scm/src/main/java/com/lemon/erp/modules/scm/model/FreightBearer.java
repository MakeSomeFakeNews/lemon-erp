package com.lemon.erp.modules.scm.model;

/**
 * 运费承担方，与字典 scm_freight_bearer 对应。往来单位一方的取值（CUSTOMER / SUPPLIER）都表示运费在本公司与往来单位之间代垫，方向按单据种类：
 * <ul>
 *   <li>发运单（销售）：{@link #COMPANY} 本公司承担，运费计入销售费用；{@link #CUSTOMER} 客户承担，本公司代垫，运费计入客户应收，不是销售收入</li>
 *   <li>采购入库：{@link #SUPPLIER} 供应商代垫，运费计入该供应商的应付；{@link #COMPANY} 本公司承担，运费付给承运商（物流公司）；供应商代垫的计入存货成本，本公司承担的按业务参数 scm.purchaseFreight.cost 计入存货成本（默认）或当期费用</li>
 *   <li>销售出库：{@link #COMPANY} 本公司承担（默认），计入销售费用；{@link #CUSTOMER} 代垫运费（客户承担），另生成客户的代垫运费应收单，应收合计 = 货款 + 代垫运费；
 *       付给承运商的方式见 {@link FreightSettle}</li>
 *   <li>销售退货：只有 {@link #CUSTOMER} 客户代垫（退货运费由客户先付、本公司承担），生成红字应收冲减客户欠款，应收 = −（货款 + 代垫运费）</li>
 *   <li>采购退货：只有 {@link #SUPPLIER} 代垫运费（本公司替供应商垫付退货运费），生成红字应付冲减欠供应商的款，应付 = −（货款 + 代垫运费）；
 *       付给承运商的方式见 {@link FreightSettle}</li>
 * </ul>
 */
public enum FreightBearer {
    /** 本公司承担 */
    COMPANY,
    /** 客户一方：发运单、销售出库为客户承担（本公司代垫），销售退货为客户代垫（本公司承担） */
    CUSTOMER,
    /** 供应商一方：采购入库为供应商代垫（本公司承担），采购退货为本公司代垫（供应商承担） */
    SUPPLIER
}
