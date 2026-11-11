package com.lemon.erp.modules.fin.model;

/**
 * 内置收支类别的标识（fin_io_category.code）：业务单据自动生成费用支出单时按标识取类别，这些类别不能删除、不能改方向，名称和科目可以修改
 */
public enum IoCategoryCode {
    /** 销售运费：销售出库本公司承担、现付的运费（默认科目销售费用 6601） */
    SALES_FREIGHT,
    /** 代垫运费：销售出库、采购退货替往来单位垫付、现付给承运商的运费（默认科目其他应收款 1221，由往来单位的代垫运费单据冲平） */
    FREIGHT_ADVANCE
}
