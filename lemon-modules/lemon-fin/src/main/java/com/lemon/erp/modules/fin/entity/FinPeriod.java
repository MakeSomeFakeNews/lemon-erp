package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会计期间：凭证日期所在月份，首次使用时自动创建；结账后不能再录入、修改该期间的凭证
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_period")
public class FinPeriod extends BaseEntity {

    /** yyyyMM */
    private Integer period;

    /** OPEN / CLOSED */
    private String status;

    /** 本期已使用的最大凭证号 */
    private Integer voucherSeq;

    private Long closeBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;

    /** 凭证数 */
    @TableField(exist = false)
    private Long voucherCount;

    /** 未记账凭证数 */
    @TableField(exist = false)
    private Long unpostedCount;

    @TableField(exist = false)
    private String closeByName;
}
