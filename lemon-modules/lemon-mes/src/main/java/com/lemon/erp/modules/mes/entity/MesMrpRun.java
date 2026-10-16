package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_mrp_run")
public class MesMrpRun extends BaseEntity {

    private String runNo;

    /** 参与运算的计划号，逗号分隔 */
    private String planNos;

    /** 需求来源：生产计划、销售订单、销售预测 */
    private String sources;

    private Integer resultCount;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String createByName;

    /** 待处理（未下达）的建议行数 */
    @TableField(exist = false)
    private Integer pendingCount;
}
