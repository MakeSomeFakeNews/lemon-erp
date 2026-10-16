package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 会计科目：一级 4 位编码，下级在上级编码后加 2 位；凭证只能使用末级科目。新增 / 修改使用 AccountRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_account")
public class FinAccount extends BaseEntity {

    private String code;

    private String name;

    /** 上级科目，0 为一级科目 */
    private Long parentId;

    /** AccountCategory */
    private String category;

    /** 余额方向 DEBIT / CREDIT */
    private String direction;

    /** 1 启用 0 停用 */
    private Integer status;

    private String remark;

    /** 是否末级科目 */
    @TableField(exist = false)
    private Boolean leaf;

    /** 下级科目（树形列表） */
    @TableField(exist = false)
    private List<FinAccount> children;
}
