package com.lemon.erp.modules.bi.model;

/**
 * 进销存报表的汇总维度；销售报表、进货报表可用的维度不同
 */
public enum JxcDimension {
    /** 物料分类（业务字典 wms_material_category） */
    CATEGORY(true, true),
    /** 品牌（业务字典 wms_material_brand） */
    BRAND(true, true),
    /** 客户区域 */
    REGION(true, false),
    /** 客户 */
    CUSTOMER(true, false),
    /** 业务员（订单的 salesman_id） */
    SALESMAN(true, false),
    /** 供应商 */
    SUPPLIER(false, true),
    /** 商品 */
    MATERIAL(true, true),
    /** 日 */
    DAY(true, true),
    /** 月 */
    MONTH(true, true);

    private final boolean sales;
    private final boolean purchase;

    JxcDimension(boolean sales, boolean purchase) {
        this.sales = sales;
        this.purchase = purchase;
    }

    public boolean supports(boolean salesReport) {
        return salesReport ? sales : purchase;
    }

    /** 按日期的维度按日期升序排列，其他按净额降序 */
    public boolean isDate() {
        return this == DAY || this == MONTH;
    }
}
