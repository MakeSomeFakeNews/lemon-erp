package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    /** 系统字典为平台租户；业务字典为所属租户（字典表不由租户插件处理，由 SysDictService 指定） */
    @JsonIgnore
    private Long tenantId;

    @NotBlank(message = "{system.valid.dictionaryTypeRequired}")
    private String dictType;

    /** 字段/列名避开 value 等数据库关键字，JSON 中仍为 label / value */
    @JsonProperty("label")
    @NotBlank(message = "{system.valid.dictionaryLabelRequired}")
    @Size(max = 100, message = "{system.valid.dictionaryLabelCannotExceedCharacters}")
    private String dictLabel;

    @JsonProperty("value")
    @NotBlank(message = "{system.valid.dictionaryValueRequired}")
    @Size(max = 100, message = "{system.valid.dictionaryValueCannotExceedCharacters}")
    private String dictValue;

    /** ant-design-vue Tag 的 color，如 green / red / blue，可空 */
    private String tagColor;

    private Integer sort;

    /** 1 启用 0 停用 */
    private Integer status;

    /** 内置取值（1 是）：程序依赖该取值，不能删除、不能改键值；只读 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer builtin;

    private String remark;
}
