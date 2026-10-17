package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.system.model.CustomFieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义字段定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_custom_field")
public class SysCustomField extends BaseEntity {

    @NotBlank(message = "请选择业务类型")
    private String bizType;

    @NotBlank(message = "请填写字段标识")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,31}$", message = "字段标识为字母开头的字母、数字、下划线，最多32个字符")
    private String fieldKey;

    @NotBlank(message = "请填写显示名称")
    @Size(max = 32, message = "显示名称不能超过32个字符")
    private String label;

    @NotNull(message = "请选择类型")
    private CustomFieldType fieldType;

    @Size(max = 1000, message = "选项不能超过1000个字符")
    private String options;

    private Integer required;

    private Integer showInList;

    private Integer sort;

    private Integer status;

    @Size(max = 255, message = "说明不能超过255个字符")
    private String remark;
}
