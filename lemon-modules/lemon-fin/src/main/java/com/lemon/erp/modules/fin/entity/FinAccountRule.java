package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.AccountRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 凭证科目设置：某个用途（可再按维度取值）使用的科目
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_account_rule")
public class FinAccountRule extends BaseEntity {

    private AccountRole role;

    /** 物料分类 / 职位类别，空串为该用途的默认科目 */
    private String dimValue;

    private Long accountId;
}
