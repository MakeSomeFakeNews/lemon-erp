package com.lemon.erp.modules.fin.model;

/**
 * 毛利明细中调整项（整单优惠、本公司承担运费、收付款折让 / 抹零、坏账核销）的显示方式（issue #93）。
 * 两种方式的合计相同，都等于单据口径的销售金额与毛利
 */
public enum AdjustMode {
    /** 汇总到底部：明细行为优惠前的货款与毛利，调整项按类别在底部各占一行（金额为负、成本 0、毛利为负，退货方向相反） */
    BOTTOM,
    /** 分摊到各行：调整项按行金额比例分摊到每一行明细 */
    ALLOCATE;

    /** 未指定时汇总到底部 */
    public static AdjustMode orDefault(AdjustMode mode) {
        return mode == null ? BOTTOM : mode;
    }
}
