package com.lemon.erp.modules.wms.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.wms.model.DocStatus;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockDocType;
import com.lemon.erp.modules.wms.model.TransferFeeSettle;
import com.lemon.erp.modules.wms.model.TransferPricing;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出入库单：入库、出库、调拨共用，按 kind 区分。只作为查询结果返回，新增 / 修改使用 StockDocRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_stock_doc")
public class WmsStockDoc extends BaseEntity {

    private String docNo;

    /** 入库 / 出库 / 调拨 */
    private StockBizType kind;

    /**
     * 入库类型（StockInType）/ 出库类型（StockOutType），调拨为空。两种类型存于同一列，故保留 String，
     * 比较通过 StockDocType#matches 或 {@link #docType()} 完成
     */
    private String bizType;

    /** 收发类别：其他入库为字典 wms_in_category，其他出库、报废出库为字典 wms_out_category（业务字典），可为空；成本凭证按类别取对方科目 */
    private String ioCategory;

    /** 仓库；调拨为调出仓库 */
    private Long warehouseId;

    /** 调入仓库，仅调拨 */
    private Long toWarehouseId;

    private LocalDate bizDate;

    private DocStatus status;

    private BigDecimal totalQty;

    private String remark;

    /** 来源单号：由采购入库、销售出库、工单领料等业务单据自动生成时填写 */
    private String sourceNo;

    /** 在途调拨 1：确认时只调出，调入仓库收货后才入库 */
    private Integer inTransit;

    private Long receiveBy;

    /** 调入收货日期（在途调拨收货的业务日期），调入流水按此日期统计 */
    private LocalDate receiveDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTime;

    /** 红字冲销单：被冲销的原单据 */
    private Long redOf;

    /** 已被冲销 1 */
    private Integer reversed;

    /** 往来单位（业务单据生成时由来源单据传入） */
    private Long partnerId;

    /** 往来单位名称快照 */
    private String partnerName;

    /** 调拨价格（仅调拨）：同价调拨或差价调拨的快速计算依据，见 {@link TransferPricing} */
    private TransferPricing transferPricing;

    /** 调拨价格依据为价格列时的价格列：0 零售价，1-8 批发价1-8 */
    private Integer priceLevel;

    /** 差价调拨的价格系数（调拨价 = 依据价 × 系数），同价调拨为空 */
    private BigDecimal priceFactor;

    /** 调出成本金额（调拨确认时按调出成本价生成，红字为负数） */
    private BigDecimal costAmount;

    /** 调拨金额（调拨价 × 数量合计，确认时生成）：内部结算金额，不影响存货成本 */
    private BigDecimal transferAmount;

    /** 调拨费用（配送费），0 为没有；本公司承担，计入当期费用 */
    private BigDecimal fee;

    /** 调拨费用付给承运商的方式：现付 / 月结 */
    private TransferFeeSettle feeSettle;

    /** 现付调拨费用的付款账户（现金 / 银行末级科目） */
    private Long feeAccountId;

    /** 调拨费用的承运商（供应商），月结时必填 */
    private Long feeCarrierId;

    /** 现付调拨费用生成的费用支出单 */
    private Long feeIoId;

    /** 月结调拨费用生成的承运商应付单 */
    private Long feeBillId;

    /** 审批流程实例（手工单据确认前审批） */
    private String flowInstanceId;

    private Long deptId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    private Long confirmBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    @TableField(exist = false)
    private String warehouseName;

    /** 列表：明细实际涉及的仓库（表头仓库在前），多于一个时显示「多仓」 */
    @TableField(exist = false)
    private List<String> warehouseNames;

    @TableField(exist = false)
    private String toWarehouseName;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private String confirmByName;

    @TableField(exist = false)
    private String receiveByName;

    /** 调拨费用的承运商名称（仅详情） */
    @TableField(exist = false)
    private String feeCarrierName;

    /** 调拨费用付款账户的名称（仅详情） */
    @TableField(exist = false)
    private String feeAccountName;

    /** 红字单据冲销的原单号 */
    @TableField(exist = false)
    private String redOfNo;

    /** 明细，仅详情返回 */
    @TableField(exist = false)
    private List<WmsStockDocItem> items;

    /** 解析后的入库 / 出库类型，调拨或无效取值为 null */
    public StockDocType docType() {
        return kind == null ? null : StockDocType.of(kind, bizType);
    }
}
