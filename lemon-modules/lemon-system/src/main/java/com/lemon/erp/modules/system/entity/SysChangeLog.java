package com.lemon.erp.modules.system.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字段变更历史
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_change_log")
public class SysChangeLog extends BaseEntity {

    /** 业务类型 RecordType.code */
    private String bizType;

    private Long bizId;

    private String fieldName;

    private String fieldLabel;

    private String oldValue;

    private String newValue;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String createByName;
}
