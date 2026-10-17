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

    @NotBlank(message = "{bi.valid.ruleNameRequired}")
    @Size(max = 64, message = "{bi.valid.ruleNameCannotExceedCharacters}")
    private String name;

    /** 监控指标（BiAlertMetric） */
    @NotBlank(message = "{bi.valid.selectMetric}")
    private String metric;

    /** GT 大于 / GE 不小于 / LT 小于 / LE 不大于 */
    @NotBlank(message = "{bi.valid.selectComparison}")
    @Pattern(regexp = "GT|GE|LT|LE", message = "{bi.valid.invalidComparison}")
    private String operator;

    @NotNull(message = "{bi.valid.enterThreshold}")
    private BigDecimal threshold;

    /** INFO / WARN / DANGER */
    @NotBlank(message = "{bi.valid.selectLevel}")
    @Pattern(regexp = "INFO|WARN|DANGER", message = "{bi.valid.invalidLevel}")
    private String level;

    private Integer enabled;

    @Size(max = 255, message = "{bi.valid.suggestionCannotExceedCharacters}")
    private String remark;
}
