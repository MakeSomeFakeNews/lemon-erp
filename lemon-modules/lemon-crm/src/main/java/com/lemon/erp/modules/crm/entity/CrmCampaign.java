package com.lemon.erp.modules.crm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 市场活动：线索、商机可以关联来源活动，按活动统计线索数、转化数、赢单金额和投入产出比
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_campaign")
public class CrmCampaign extends BaseEntity {

    @NotBlank(message = "活动名称不能为空")
    @Size(max = 128, message = "活动名称不能超过128个字符")
    private String name;

    /** 类型(字典 crm_campaign_type) */
    @NotBlank(message = "请选择活动类型")
    private String type;

    @NotNull(message = "请选择开始日期")
    private LocalDate startDate;

    @NotNull(message = "请选择结束日期")
    private LocalDate endDate;

    @DecimalMin(value = "0", message = "预算不能小于0")
    @Digits(integer = 16, fraction = 2, message = "金额最多2位小数")
    private BigDecimal budget;

    @DecimalMin(value = "0", message = "实际花费不能小于0")
    @Digits(integer = 16, fraction = 2, message = "金额最多2位小数")
    private BigDecimal actualCost;

    @Min(value = 0, message = "目标线索数不能小于0")
    private Integer targetLeads;

    /** PLANNED / ACTIVE / FINISHED / CANCELED */
    private String status;

    private Long ownerId;

    @Size(max = 1000, message = "活动说明不能超过1000个字符")
    private String description;

    @TableField(exist = false)
    private String ownerName;
    /** 线索数、已转化线索数、商机数、赢单金额 */
    @TableField(exist = false)
    private Integer leadCount;
    @TableField(exist = false)
    private Integer convertedCount;
    @TableField(exist = false)
    private Integer oppCount;
    @TableField(exist = false)
    private BigDecimal wonAmount;

    /** 实际销售：活动商机生成的销售订单金额（已审核及以后） */
    @TableField(exist = false)
    private BigDecimal orderAmount;

    /** 回款：这些订单出库应收的已核销金额 */
    @TableField(exist = false)
    private BigDecimal receivedAmount;
}
