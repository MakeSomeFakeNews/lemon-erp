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

    @NotBlank(message = "工作中心编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "工作中心编码只能包含字母、数字、下划线和短横线，最多32个字符")
    private String code;

    @NotBlank(message = "工作中心名称不能为空")
    @Size(max = 64, message = "工作中心名称不能超过64个字符")
    private String name;

    /** 人工费率（元 / 工时） */
    @NotNull(message = "请填写人工费率")
    @DecimalMin(value = "0", message = "人工费率不能小于0")
    private BigDecimal laborRate;

    /** 制造费用率（元 / 工时） */
    @NotNull(message = "请填写制造费用率")
    @DecimalMin(value = "0", message = "制造费用率不能小于0")
    private BigDecimal overheadRate;

    /** 每个资源每个工作日的可用工时 */
    @DecimalMin(value = "0", message = "每日工时不能小于0")
    @jakarta.validation.constraints.DecimalMax(value = "24", message = "每日工时不能超过24")
    private BigDecimal dailyHours;

    /** 资源数（设备台数 / 班组人数） */
    @jakarta.validation.constraints.Min(value = 1, message = "资源数至少为1")
    private Integer resources;

    /** 工作日：1 周一 … 7 周日，逗号分隔 */
    @Pattern(regexp = "^([1-7](,[1-7])*)?$", message = "工作日格式不正确")
    private String workDays;

    /** 1 启用 0 停用 */
    private Integer status;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
