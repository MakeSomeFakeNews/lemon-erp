package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 盘点单。只作为查询结果返回，新增 / 修改使用 CheckRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_check")
public class WmsCheck extends BaseEntity {

    private String docNo;

    private Long warehouseId;

    private LocalDate bizDate;

    /** DocStatus */
    private String status;

    private String remark;

    /** 冻结仓库 1：盘点完成前该仓库不能出入库、调拨 */
    private Integer freeze;

    /** 差异审批流程实例 */
    private String flowInstanceId;

    private Long deptId;

    private Long createBy;

    private Long confirmBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private String confirmByName;

    /** 明细行数，仅列表 */
    @TableField(exist = false)
    private Integer itemCount;

    /** 有差异的行数，仅列表 */
    @TableField(exist = false)
    private Integer diffCount;

    /** 明细，仅详情返回 */
    @TableField(exist = false)
    private List<WmsCheckItem> items;
}
