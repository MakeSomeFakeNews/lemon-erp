package com.lemon.erp.modules.bi.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 预警规则：指标当前值与阈值比较，满足条件时在决策支持中提示
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bi_alert_rule")
public class BiAlertRule extends BaseEntity {

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 64, message = "规则名称不能超过64个字符")
    private String name;

    /** 监控指标（BiAlertMetric） */
    @NotBlank(message = "请选择监控指标")
    private String metric;

    /** GT 大于 / GE 不小于 / LT 小于 / LE 不大于 */
    @NotBlank(message = "请选择比较方式")
    @Pattern(regexp = "GT|GE|LT|LE", message = "比较方式不正确")
    private String operator;

    @NotNull(message = "请填写阈值")
    private BigDecimal threshold;

    /** INFO / WARN / DANGER */
    @NotBlank(message = "请选择级别")
    @Pattern(regexp = "INFO|WARN|DANGER", message = "级别不正确")
    private String level;

    private Integer enabled;

    @Size(max = 255, message = "处理建议不能超过255个字符")
    private String remark;
}
