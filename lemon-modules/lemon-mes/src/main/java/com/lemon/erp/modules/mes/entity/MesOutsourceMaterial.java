package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 委外用料：委外采购订单中委外件按默认 BOM 展开的子件，记录发给加工商、加工商退回和收货核销的数量。
 * 加工商处结存 = 已发 - 已退 - 已核销
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_outsource_material")
public class MesOutsourceMaterial extends BaseEntity {

    /** 采购订单（委外） */
    private Long orderId;

    private Long orderItemId;

    /** 委外件 */
    private Long productId;

    /** 子件 */
    private Long materialId;

    /** 单位用量（含损耗） */
    private BigDecimal unitQty;

    /** 需求数量 = 委外数量 × 单位用量 */
    private BigDecimal requiredQty;

    private BigDecimal issuedQty;

    private BigDecimal returnedQty;

    /** 收货核销的数量 = 收货数量 × 单位用量 */
    private BigDecimal consumedQty;

    @TableField(exist = false)
    private String productCode;
    @TableField(exist = false)
    private String productName;
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
}
