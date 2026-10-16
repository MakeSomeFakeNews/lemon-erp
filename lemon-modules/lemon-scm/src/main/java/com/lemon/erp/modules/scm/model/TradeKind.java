package com.lemon.erp.modules.scm.model;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.wms.model.StockBizType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 采购入库 / 采购退货 / 销售出库 / 销售退货，与字典 scm_trade_kind 对应。
 * 保存即确认：同一事务内生成并确认仓库的出入库单，回写订单的已入库 / 已出库、已退货数量
 */
@Getter
@AllArgsConstructor
public enum TradeKind {

    PURCHASE_IN("purchaseIn", "CR", "采购入库", OrderKind.PURCHASE, false, StockBizType.IN, "PURCHASE"),
    PURCHASE_RETURN("purchaseReturn", "CT", "采购退货", OrderKind.PURCHASE, true, StockBizType.OUT, "PURCHASE_RETURN"),
    SALES_OUT("salesOut", "XC", "销售出库", OrderKind.SALES, false, StockBizType.OUT, "SALES"),
    SALES_RETURN("salesReturn", "XT", "销售退货", OrderKind.SALES, true, StockBizType.IN, "SALES_RETURN");

    /** 接口路径与权限标识中使用的名称 */
    private final String path;
    private final String prefix;
    private final String label;
    private final OrderKind orderKind;
    /** 是否退货：回写订单的已退货数量，否则回写已入库 / 已出库数量 */
    private final boolean returns;
    /** 生成的仓库单据种类 */
    private final StockBizType stockKind;
    /** 仓库单据的入库类型（字典 wms_in_type）/ 出库类型（字典 wms_out_type） */
    private final String stockBizType;

    public boolean inbound() {
        return stockKind == StockBizType.IN;
    }

    /** 金额方向：退货为负，应收应付据此冲减 */
    public int sign() {
        return returns ? -1 : 1;
    }

    public static TradeKind ofPath(String path) {
        for (TradeKind kind : values()) {
            if (kind.path.equals(path)) {
                return kind;
            }
        }
        throw new BizException("不支持的单据类型：" + path);
    }
}
