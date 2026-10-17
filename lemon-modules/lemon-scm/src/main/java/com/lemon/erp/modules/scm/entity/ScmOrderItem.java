package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.util.Decimals;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_order_item")
public class ScmOrderItem extends BaseEntity {

    private Long orderId;

    private Long materialId;

    private BigDecimal qty;

    /** 含税单价 */
    private BigDecimal price;

    /** 税率（%） */
    private BigDecimal taxRate;

    /** 价税合计 */
    private BigDecimal amount;

    private BigDecimal taxAmount;

    /** 已入库 / 已出库数量 */
    private BigDecimal doneQty;

    /** 已退货数量 */
    private BigDecimal returnQty;

    /** 退货后需要补发（补收）的数量：退货时选择「补发」的计入，订单恢复相应的未完成数量 */
    private BigDecimal replenishQty;

    private LocalDate deliveryDate;

    private String remark;

    /** 录入单位（辅助单位），为空表示基本单位 */
    private String unitName;

    /** 录入单位的换算率：1 录入单位 = unitRatio 基本单位 */
    private BigDecimal unitRatio;

    /** 原价（促销、返利抵扣前的含税单价），没有优惠时为空 */
    private BigDecimal origPrice;

    /** 应用的折扣促销 / 赠品来源的买赠促销 */
    private Long promotionId;

    /** 1 赠品 */
    private Integer gift;

    @TableField(exist = false)
    private String promotionName;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;

    @TableField(exist = false)
    private Integer batchEnabled;

    @TableField(exist = false)
    private Integer serialEnabled;

    /** 实际完成数量 = 已入库 / 已出库 - 已退货（可退货的上限） */
    public BigDecimal netQty() {
        return doneQty.subtract(returnQty);
    }

    /** 未完成数量 = 订购数量 - 已入库 / 已出库 + 需要补发的退货数量，不小于 0；退款的退货不再补发 */
    public BigDecimal remainingQty() {
        BigDecimal remaining = qty.subtract(doneQty).add(Decimals.nz(replenishQty));
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }
}
