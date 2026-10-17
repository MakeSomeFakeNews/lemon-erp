package com.lemon.erp.modules.scm.model;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockDocType;
import com.lemon.erp.modules.wms.model.StockInType;
import com.lemon.erp.modules.wms.model.StockOutType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 采购入库 / 采购退货 / 销售出库 / 销售退货，与字典 scm_trade_kind 对应。
 * 保存即确认：同一事务内生成并确认仓库的出入库单，回写订单的已入库 / 已出库、已退货数量
 */
@Getter
@AllArgsConstructor
public enum TradeKind {

    PURCHASE_IN("purchaseIn", "CR", "scm.label.purchaseReceipt", OrderKind.PURCHASE, false, StockBizType.IN, StockInType.PURCHASE),
    PURCHASE_RETURN("purchaseReturn", "CT", "scm.label.purchaseReturn", OrderKind.PURCHASE, true, StockBizType.OUT, StockOutType.PURCHASE_RETURN),
    SALES_OUT("salesOut", "XC", "scm.label.salesDelivery", OrderKind.SALES, false, StockBizType.OUT, StockOutType.SALES),
    SALES_RETURN("salesReturn", "XT", "scm.label.salesReturn", OrderKind.SALES, true, StockBizType.IN, StockInType.SALES_RETURN);

    /** 接口路径与权限标识中使用的名称 */
    private final String path;
    private final String prefix;
    /** 名称的消息键 */
    private final String labelKey;
    private final OrderKind orderKind;
    /** 是否退货：回写订单的已退货数量，否则回写已入库 / 已出库数量 */
    private final boolean returns;
    /** 生成的仓库单据种类 */
    private final StockBizType stockKind;
    /** 仓库单据的入库类型（字典 wms_in_type）/ 出库类型（字典 wms_out_type） */
    private final StockDocType stockBizType;

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
        throw new BizException("scm.error.unsupportedDocumentType", path);
    }
}
