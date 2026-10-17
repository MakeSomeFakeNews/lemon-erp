package com.lemon.erp.modules.pm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 项目按月成本：已审核工时的人工成本、已审核费用（数据库查询结果，为空的列不输出）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonthlyCost {
    /** yyyyMM */
    private Integer period;
    private BigDecimal labor;
    private BigDecimal expense;
}
