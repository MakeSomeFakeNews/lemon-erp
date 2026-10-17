package com.lemon.erp.modules.fin.entity;

import com.lemon.erp.modules.fin.model.AccountDirection;
import com.lemon.erp.modules.fin.model.AccountCategory;
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
    private AccountCategory category;

    /** 余额方向 DEBIT / CREDIT */
    private AccountDirection direction;

    /** 1 启用 0 停用 */
    private Integer status;

    private String remark;

    /** 辅助核算项目（AuxItem 名称，逗号分隔），手工凭证使用该科目时必填 */
    private String auxItems;

    /** 是否末级科目 */
    @TableField(exist = false)
    private Boolean leaf;

    /** 下级科目（树形列表） */
    @TableField(exist = false)
    private List<FinAccount> children;
}
