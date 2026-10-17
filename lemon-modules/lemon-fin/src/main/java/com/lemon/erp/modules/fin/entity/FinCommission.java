package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.CommissionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售提成表（每个期间一张）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_commission")
public class FinCommission extends BaseEntity {

    private Integer period;

    private CommissionStatus status;

    private BigDecimal totalAmount;

    private String remark;

    private Long createBy;

    private Long confirmBy;

    private LocalDateTime confirmTime;

    @TableField(exist = false)
    private List<FinCommissionLine> lines;
}
