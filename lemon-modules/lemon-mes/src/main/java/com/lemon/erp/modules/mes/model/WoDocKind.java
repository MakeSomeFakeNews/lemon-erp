package com.lemon.erp.modules.mes.model;

import com.lemon.erp.modules.wms.model.StockOutType;
import com.lemon.erp.modules.wms.model.StockInType;
import com.lemon.erp.modules.wms.model.StockDocType;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.wms.model.StockBizType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

/**
 * 工单领料 / 退料 / 完工入库 / 补料，与字典 mes_wo_doc_kind 对应
 */
@Getter
@AllArgsConstructor
public enum WoDocKind {

    /** 领料 */
    ISSUE("LL", "mes.label.materialIssue", StockOutType.PRODUCTION),
    /** 退料 */
    RETURN("TL", "mes.label.materialReturn", StockInType.PRODUCTION_RETURN),
    /** 完工入库 */
    FINISH("WG", "mes.label.completionReceipt", StockInType.PRODUCTION),
    /** 补料：审批通过的补料申请发料，不受领料上限限制，计入工单已补料数量 */
    SUPPLEMENT("BL", "mes.label.supplementaryIssue", StockOutType.PRODUCTION),
    /** 报废入库：报工报废的产品入不良品仓，不计入完工数量 */
    SCRAP("BF", "mes.label.scrapReceipt", StockInType.SCRAP_IN);

    private final String prefix;
    /** 单据名称的消息键（编码规则目录） */
    private final String docName;
    /** 仓库单据的入库类型（字典 wms_in_type）/ 出库类型（字典 wms_out_type） */
    private final StockDocType stockBizType;

    /** 生成的仓库单据种类 */
    public StockBizType getStockKind() {
        return stockBizType.kind();
    }

    public String path() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static WoDocKind ofPath(String path) {
        for (WoDocKind kind : values()) {
            if (kind.path().equals(path)) {
                return kind;
            }
        }
        throw new BizException("mes.error.unsupportedDocumentType", path);
    }
}
