package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 64, message = "字典名称不能超过64个字符")
    private String name;

    @NotBlank(message = "字典类型不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]{0,99}$", message = "字典类型只能包含小写字母、数字和下划线，且以字母开头")
    private String type;

    /** 1 启用 0 停用 */
    private Integer status;

    private String remark;
}
