package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_work_center")
public class MesWorkCenter extends BaseEntity {

    @NotBlank(message = "{mes.valid.workCenterCodeCannotEmpty}")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "{mes.valid.workCenterCodeOnlyContainLetters}")
    private String code;

    @NotBlank(message = "{mes.valid.workCenterNameCannotEmpty}")
    @Size(max = 64, message = "{mes.valid.workCenterNameCannotExceedCharacters}")
    private String name;

    /** 人工费率（元 / 工时） */
    @NotNull(message = "{mes.valid.enterLaborRate}")
    @DecimalMin(value = "0", message = "{mes.valid.laborRateCannotLess}")
    private BigDecimal laborRate;

    /** 制造费用率（元 / 工时） */
    @NotNull(message = "{mes.valid.enterOverheadRate}")
    @DecimalMin(value = "0", message = "{mes.valid.overheadRateCannotLess}")
    private BigDecimal overheadRate;

    /** 每个资源每个工作日的可用工时 */
    @DecimalMin(value = "0", message = "{mes.valid.dailyHoursCannotLess}")
    @jakarta.validation.constraints.DecimalMax(value = "24", message = "{mes.valid.dailyHoursCannotExceed}")
    private BigDecimal dailyHours;

    /** 资源数（设备台数 / 班组人数） */
    @jakarta.validation.constraints.Min(value = 1, message = "{mes.valid.resourcesMustLeast}")
    private Integer resources;

    /** 工作日：1 周一 … 7 周日，逗号分隔 */
    @Pattern(regexp = "^([1-7](,[1-7])*)?$", message = "{mes.valid.invalidWorkDaysFormat}")
    private String workDays;

    /** 1 启用 0 停用 */
    private Integer status;

    @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}")
    private String remark;
}
