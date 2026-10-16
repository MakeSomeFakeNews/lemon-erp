package com.lemon.erp.modules.mes.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工程变更前后 BOM 的差异：子件按物料、工序按工序号对比，只列出有变化的
 */
public record EcnDiff(List<ItemDiff> items, List<OpDiff> ops) {

    public enum Action {
        ADDED,
        REMOVED,
        CHANGED
    }

    public record ItemDiff(Action action, Long materialId, String materialCode, String materialName, String unit,
                           BigDecimal oldQty, BigDecimal newQty, BigDecimal oldLossRate, BigDecimal newLossRate,
                           String oldRefDes, String newRefDes) {
    }

    public record OpDiff(Action action, Integer seq, String oldName, String newName, String oldWorkCenter, String newWorkCenter,
                         BigDecimal oldStdHours, BigDecimal newStdHours, BigDecimal oldPieceRate, BigDecimal newPieceRate,
                         Integer oldFirstArticle, Integer newFirstArticle) {
    }
}
