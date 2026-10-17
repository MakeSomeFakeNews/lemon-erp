package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务参数（按租户）：只保存修改过的参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_biz_rule")
public class SysBizRule extends BaseEntity {

    private String ruleKey;

    private String ruleValue;
}
