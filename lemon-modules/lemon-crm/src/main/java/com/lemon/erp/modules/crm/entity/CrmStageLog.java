package com.lemon.erp.modules.crm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商机阶段变更记录（转化率分析）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_stage_log")
public class CrmStageLog extends BaseEntity {

    /** 商机 */
    private Long opportunityId;

    /** 原阶段，新建时为空 */
    private String fromStage;

    /** 新阶段 */
    private String toStage;

    /** 操作人 */
    private Long userId;

    @TableField(exist = false)
    private String userName;
}
