package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.fin.model.FundDirection;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收支类别（每个租户一份，可自行增加）：其他收入单、费用支出单明细的分类，标明收 / 支，关联生成凭证用的会计科目
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_io_category")
public class FinIoCategory extends BaseEntity {

    private String name;

    /** IN 收入 / OUT 支出 */
    private FundDirection direction;

    /** 凭证科目（末级）：收入记贷方、支出记借方 */
    private Long accountId;

    private Integer sort;

    private EnableStatus status;

    private String remark;

    @TableField(exist = false)
    private String accountCode;

    @TableField(exist = false)
    private String accountName;

    /** 是否已被收支单使用（已使用的不能删除，可停用） */
    @TableField(exist = false)
    private Boolean used;
}
