package com.lemon.erp.modules.scm.model;

import com.lemon.erp.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

/**
 * 订单种类：采购订单、销售订单共用 scm_order，按 kind 区分
 */
@Getter
@AllArgsConstructor
public enum OrderKind {

    PURCHASE("CG", "采购订单", "供应商", "scm_purchase_order"),
    SALES("XS", "销售订单", "客户", "scm_sales_order");

    /** 单据号前缀 */
    private final String prefix;
    private final String label;
    /** 往来单位的名称：供应商 / 客户 */
    private final String partnerLabel;
    /** 审批流程标识：在流程中心发布同名流程后，订单审核改为走审批流程 */
    private final String processKey;

    /** 接口路径与权限标识中使用的名称：purchase / sales */
    public String path() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static OrderKind ofPath(String path) {
        for (OrderKind kind : values()) {
            if (kind.path().equals(path)) {
                return kind;
            }
        }
        throw new BizException("不支持的订单类型：{0}", path);
    }

    public static OrderKind ofProcessKey(String processKey) {
        for (OrderKind kind : values()) {
            if (kind.processKey.equals(processKey)) {
                return kind;
            }
        }
        return null;
    }
}
