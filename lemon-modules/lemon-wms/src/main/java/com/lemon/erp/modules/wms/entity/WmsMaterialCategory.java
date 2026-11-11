package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物料分类（多级，每个租户一份）。物料、检验方案、促销、提成规则、凭证科目设置等按分类<b>编码</b>关联，编码保存后不能修改
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_material_category")
public class WmsMaterialCategory extends BaseEntity {

    /** 上级分类 ID，0 为一级分类 */
    private Long parentId;

    /** 祖级 ID 路径（逗号分隔，从 0 开始，如 0,3,7），mapper XML 中查询下级分类用；只读 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String ancestors;

    private String code;

    private String name;

    /** 同级排序 */
    private Integer sort;

    private EnableStatus status;

    /** 内置分类（1 是）：程序依赖（如原材料、低值易耗品），不能删除；只读 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer builtin;

    private String remark;
}
