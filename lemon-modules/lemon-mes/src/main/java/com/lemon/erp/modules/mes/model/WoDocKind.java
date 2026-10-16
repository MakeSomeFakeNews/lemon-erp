package com.lemon.erp.modules.mes.model;

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

    ISSUE("LL", "领料", StockBizType.OUT, "PRODUCTION"),
    RETURN("TL", "退料", StockBizType.IN, "PRODUCTION_RETURN"),
    FINISH("WG", "完工入库", StockBizType.IN, "PRODUCTION"),
    /** 补料：审批通过的补料申请发料，不受领料上限限制，计入工单已补料数量 */
    SUPPLEMENT("BL", "补料", StockBizType.OUT, "PRODUCTION"),
    /** 报废入库：报工报废的产品入不良品仓，不计入完工数量 */
    SCRAP("BF", "报废入库", StockBizType.IN, "SCRAP_IN");

    private final String prefix;
    private final String label;
    /** 生成的仓库单据种类 */
    private final StockBizType stockKind;
    /** 仓库单据的入库类型（字典 wms_in_type）/ 出库类型（字典 wms_out_type） */
    private final String stockBizType;

    public String path() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static WoDocKind ofPath(String path) {
        for (WoDocKind kind : values()) {
            if (kind.path().equals(path)) {
                return kind;
            }
        }
        throw new BizException("不支持的单据类型：" + path);
    }
}
