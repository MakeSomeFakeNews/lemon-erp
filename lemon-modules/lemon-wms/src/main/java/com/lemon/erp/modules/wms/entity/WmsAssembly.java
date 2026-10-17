package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.wms.model.AssemblyKind;
import com.lemon.erp.modules.wms.model.DocStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 组装拆卸单：组装消耗子件得到成套件，拆卸把成套件拆为子件；确认后生成出库、入库单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_assembly")
public class WmsAssembly extends BaseEntity {

    private String assemblyNo;

    private AssemblyKind kind;

    private Long warehouseId;

    private LocalDate bizDate;

    /** 成套件 */
    private Long materialId;

    private Long locationId;

    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expiryDate;

    private String serialNos;

    private BigDecimal qty;

    private DocStatus status;

    private Long outDocId;

    private String outDocNo;

    private Long inDocId;

    private String inDocNo;

    private String remark;

    private Long deptId;

    private Long createBy;

    private Long confirmBy;

    private LocalDateTime confirmTime;

    @TableField(exist = false)
    private String warehouseName;
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
    @TableField(exist = false)
    private String locationCode;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private List<WmsAssemblyItem> items;
}
