package com.lemon.erp.modules.scm.model;

import com.lemon.erp.framework.setting.OptionEnum;

/**
 * 本公司承担的采购运费的入账方式（业务参数 scm.purchaseFreight.cost），label 为消息键。
 * 供应商代垫的运费是采购成本的一部分，始终计入存货成本，不受此参数影响
 */
public enum PurchaseFreightCost implements OptionEnum {
    /** 计入存货成本：按各行不含税金额分摊到入库成本，凭证 借 存货 / 贷 应付账款-承运商（企业会计准则的一般做法） */
    INVENTORY("scm.label.freightCostInventory"),
    /** 计入当期费用：不分摊到存货，凭证 借 采购运费科目（默认销售费用）/ 贷 应付账款-承运商（商品流通企业进货费用金额较小时的简化做法） */
    EXPENSE("scm.label.freightCostExpense");

    private final String label;

    PurchaseFreightCost(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
