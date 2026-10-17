package com.lemon.erp.modules.scm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.scm.model.SupplierConfirm;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购 / 销售订单，按 kind 区分。只作为查询结果返回，新增 / 修改使用 OrderRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_order")
public class ScmOrder extends BaseEntity {

    private String orderNo;

    /** 订单种类：采购 / 销售 */
    private OrderKind kind;

    /** 供应商 / 客户 */
    private Long partnerId;

    private LocalDate orderDate;

    private LocalDate deliveryDate;

    /** 默认收货 / 发货仓库 */
    private Long warehouseId;

    /** 订单状态 */
    private OrderStatus status;

    private BigDecimal totalQty;

    /** 价税合计 */
    private BigDecimal totalAmount;

    private BigDecimal taxAmount;

    /** 订单来源 */
    private OrderSource source;

    private String flowInstanceId;

    /** 引用的采购合同（按合同价下单） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long contractId;

    /** 供应商在门户的确认，为空表示未确认 */
    private SupplierConfirm supplierConfirm;

    /** 供应商承诺交期 */
    private LocalDate promisedDate;

    private String confirmRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    /** 促销优惠合计（原价 - 成交价） */
    private BigDecimal discountAmount;

    /** 使用返利抵扣的金额 */
    private BigDecimal rebateAmount;

    /** 外部单号（电商 / 门店订单导入） */
    private String externalNo;

    private String remark;

    /** 版本：每次变更单生效后加 1 */
    private Integer version;

    /** 币种，单价、金额按此币种 */
    private String currency;

    /** 汇率：1 外币 = 汇率 × 本位币 */
    private java.math.BigDecimal exchangeRate;

    private Long deptId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 业务员（销售订单）：默认客户的负责销售，没有时为制单人 */
    private Long salesmanId;

    private Long approveBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    @TableField(exist = false)
    private String contractNo;
    @TableField(exist = false)
    private String partnerCode;

    @TableField(exist = false)
    private String partnerName;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private String salesmanName;

    @TableField(exist = false)
    private String approveByName;

    /** 已入库 / 已出库数量合计（扣除退货），列表展示进度 */
    @TableField(exist = false)
    private BigDecimal doneQty;

    /** 明细，仅详情返回 */
    @TableField(exist = false)
    private List<ScmOrderItem> items;
}
