package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.system.model.DictKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    @NotBlank(message = "{system.valid.dictionaryNameRequired}")
    @Size(max = 64, message = "{system.valid.dictionaryNameCannotExceedCharacters}")
    private String name;

    @NotBlank(message = "{system.valid.dictionaryTypeRequired}")
    @Pattern(regexp = "^[a-z][a-z0-9_]{0,99}$", message = "{system.valid.dictionaryTypeMayOnlyContainLowercase}")
    private String type;

    /** 新增时必填，之后不能修改（系统字典的取值由程序内置） */
    private DictKind kind;

    /** 1 启用 0 停用 */
    private EnableStatus status;

    private String remark;
}
