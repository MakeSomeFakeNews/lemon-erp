package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义字段的值
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_custom_value")
public class SysCustomValue extends BaseEntity {

    private String bizType;

    private Long bizId;

    private String fieldKey;

    private String fieldValue;
}
