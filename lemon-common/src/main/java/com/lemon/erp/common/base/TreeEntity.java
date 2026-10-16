package com.lemon.erp.common.base;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 树形实体（菜单、部门等）：通过 parentId 自关联，0 表示顶级节点
 *
 * @param <T> 实体自身类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class TreeEntity<T extends TreeEntity<T>> extends BaseEntity {

    /** 顶级节点的 parentId */
    public static final long ROOT_PARENT_ID = 0L;

    private Long parentId;

    private Integer sort;

    /** 叶子节点不输出 children，方便前端树组件判断 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private List<T> children = new ArrayList<>();
}
