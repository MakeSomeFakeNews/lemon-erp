package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
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

    private String remark;
}
