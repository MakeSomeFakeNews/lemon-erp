package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.ShipmentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 物流发运单：由销售出库单生成，跟踪承运商、运单号与物流动态。只作为查询结果返回，新增 / 修改使用 ShipmentRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_shipment")
public class ScmShipment extends BaseEntity {

    private String shipNo;

    /** 销售出库单 */
    private Long tradeDocId;

    private Long customerId;

    /** 承运商，字典 scm_carrier */
    private String carrier;

    private String trackingNo;

    private BigDecimal freight;

    /** 承运商（供应商），有运费时生成应付单 */
    private Long carrierSupplierId;

    /** 运费应付单 */
    private Long freightBillId;

    private String receiver;

    private String phone;

    private String address;

    private LocalDate shipDate;

    private LocalDate signDate;

    /** 发运状态 */
    private ShipmentStatus status;

    private String remark;

    private Long deptId;

    private Long createBy;

    @TableField(exist = false)
    private String tradeDocNo;

    @TableField(exist = false)
    private String orderNo;

    @TableField(exist = false)
    private String customerName;

    @TableField(exist = false)
    private String carrierSupplierCode;

    @TableField(exist = false)
    private String carrierSupplierName;

    @TableField(exist = false)
    private String createByName;

    /** 物流动态，仅详情返回，按时间倒序 */
    @TableField(exist = false)
    private List<ScmShipmentTrack> tracks;
}
