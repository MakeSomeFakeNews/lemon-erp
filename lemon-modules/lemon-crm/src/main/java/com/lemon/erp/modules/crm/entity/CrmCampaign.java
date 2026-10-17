package com.lemon.erp.modules.crm.entity;

import com.lemon.erp.modules.crm.model.CampaignStatus;
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

    @NotBlank(message = "{crm.valid.campaignNameRequired}")
    @Size(max = 128, message = "{crm.valid.campaignNameCannotExceedCharacters}")
    private String name;

    /** 类型(字典 crm_campaign_type) */
    @NotBlank(message = "{crm.valid.selectCampaignType}")
    private String type;

    @NotNull(message = "{crm.valid.selectStartDate}")
    private LocalDate startDate;

    @NotNull(message = "{crm.valid.selectEndDate}")
    private LocalDate endDate;

    @DecimalMin(value = "0", message = "{crm.valid.budgetCannotNegative}")
    @Digits(integer = 16, fraction = 2, message = "{crm.valid.amountAllowsMostDecimalPlaces}")
    private BigDecimal budget;

    @DecimalMin(value = "0", message = "{crm.valid.actualCostCannotLess}")
    @Digits(integer = 16, fraction = 2, message = "{crm.valid.amountAllowsMostDecimalPlaces}")
    private BigDecimal actualCost;

    @Min(value = 0, message = "{crm.valid.targetLeadsCannotLess}")
    private Integer targetLeads;

    /** PLANNED / ACTIVE / FINISHED / CANCELED */
    private CampaignStatus status;

    private Long ownerId;

    @Size(max = 1000, message = "{crm.valid.campaignDescriptionCannotExceedCharacters}")
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
