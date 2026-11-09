package com.lemon.erp.modules.mes.dto;

import java.math.BigDecimal;

/**
 * 关闭工单前预览尚未转入产品的实时成本，金额需要「查看成本金额」权限（{@code costVisible} 为 false 时为空）
 *
 * @param standard    已按标准成本完工入库：实际与标准的差异不调整库存成本
 * @param finishedQty 已完工数量；为 0 时未转入的成本是在制品损失，不进入库存
 * @param material    未转入的净领料成本
 * @param conversion  未转入的工费（人工 + 制造费用）
 * @param total       合计
 * @param toStock     其中追加到已完工产品库存的部分（按产品当前库存比例预计）
 * @param toExpense   不进入库存的部分：没有完工时为在制品损失，有完工时对应已出库的产品
 */
public record WoCloseCostView(boolean standard, BigDecimal finishedQty, boolean costVisible, BigDecimal material,
                              BigDecimal conversion, BigDecimal total, BigDecimal toStock, BigDecimal toExpense) {
}
