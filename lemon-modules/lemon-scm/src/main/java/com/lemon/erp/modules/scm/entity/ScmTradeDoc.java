package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购入库 / 采购退货 / 销售出库 / 销售退货，按 kind 区分。只作为查询结果返回，新增使用 TradeDocRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_trade_doc")
public class ScmTradeDoc extends BaseEntity {

    private String docNo;

    /** TradeKind */
    private String kind;

    private Long orderId;

    private Long partnerId;

    private Long warehouseId;

    private LocalDate bizDate;

    private BigDecimal totalQty;

    private BigDecimal totalAmount;

    private BigDecimal taxAmount;

    /** 订单币种、汇率；单据金额为本位币 */
    private String currency;

    private BigDecimal exchangeRate;

    /** 价税合计（原币） */
    private BigDecimal amountFc;

    /** 生成的仓库出入库单 */
    private Long wmsDocId;

    private String wmsDocNo;

    private String remark;

    /** 退货是否补发（补收）：1 补发 0 退款 */
    private Integer replenish;

    private Long deptId;

    private Long createBy;

    @TableField(exist = false)
    private String orderNo;

    @TableField(exist = false)
    private String partnerCode;

    @TableField(exist = false)
    private String partnerName;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String createByName;

    /** 明细，仅详情返回 */
    @TableField(exist = false)
    private List<ScmTradeDocItem> items;
}
