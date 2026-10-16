package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 招标邀请的供应商
 */
@Data
@TableName("scm_tender_supplier")
public class ScmTenderSupplier {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenderId;

    private Long supplierId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String supplierCode;
    @TableField(exist = false)
    private String supplierName;
    /** 是否已投标 */
    @TableField(exist = false)
    private Boolean bid;
}
