package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
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

    /** StockBizType：IN / OUT / TRANSFER */
    private String kind;

    /** 入库类型（字典 wms_in_type）/ 出库类型（字典 wms_out_type），调拨为空 */
    private String bizType;

    /** 仓库；调拨为调出仓库 */
    private Long warehouseId;

    /** 调入仓库，仅调拨 */
    private Long toWarehouseId;

    private LocalDate bizDate;

    /** DocStatus */
    private String status;

    private BigDecimal totalQty;

    private String remark;

    /** 来源单号：由采购入库、销售出库、工单领料等业务单据自动生成时填写 */
    private String sourceNo;

    /** 在途调拨 1：确认时只调出，调入仓库收货后才入库 */
    private Integer inTransit;

    private Long receiveBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTime;

    /** 红字冲销单：被冲销的原单据 */
    private Long redOf;

    /** 已被冲销 1 */
    private Integer reversed;

    /** 审批流程实例（手工单据确认前审批） */
    private String flowInstanceId;

    private Long deptId;

    private Long createBy;

    private Long confirmBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String toWarehouseName;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private String confirmByName;

    @TableField(exist = false)
    private String receiveByName;

    /** 红字单据冲销的原单号 */
    @TableField(exist = false)
    private String redOfNo;

    /** 明细，仅详情返回 */
    @TableField(exist = false)
    private List<WmsStockDocItem> items;
}
