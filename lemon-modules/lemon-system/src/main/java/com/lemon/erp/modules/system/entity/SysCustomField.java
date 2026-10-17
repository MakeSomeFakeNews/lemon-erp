package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
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

    @NotBlank(message = "{system.valid.selectBusinessType}")
    private String bizType;

    @NotBlank(message = "{system.valid.enterFieldKey}")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,31}$", message = "{system.valid.fieldKeyMustStartLetterContain}")
    private String fieldKey;

    @NotBlank(message = "{system.valid.enterDisplayName}")
    @Size(max = 32, message = "{system.valid.displayNameCannotExceedCharacters}")
    private String label;

    @NotNull(message = "{system.valid.selectType}")
    private CustomFieldType fieldType;

    @Size(max = 1000, message = "{system.valid.optionsCannotExceedCharacters}")
    private String options;

    private Integer required;

    private Integer showInList;

    private Integer sort;

    private EnableStatus status;

    @Size(max = 255, message = "{system.valid.descriptionCannotExceedCharacters}")
    private String remark;
}
