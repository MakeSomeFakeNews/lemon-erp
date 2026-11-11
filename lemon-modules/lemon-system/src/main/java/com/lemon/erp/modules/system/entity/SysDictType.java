package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.system.model.DictKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

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

    /** 取值的扩展值定义（业务模块声明的 DictExtension，名称已按界面语言翻译），没有时为空；只读 */
    @TableField(exist = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Ext ext;

    /**
     * 扩展值定义：字典页面多显示一列，保存取值时必填
     *
     * @param label 名称，如 税率(%)
     * @param scale 最多小数位数
     * @param min   最小值
     * @param max   最大值
     */
    public record Ext(String label, int scale, BigDecimal min, BigDecimal max) {
    }
}
