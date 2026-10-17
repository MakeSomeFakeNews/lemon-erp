package com.lemon.erp.modules.hr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 考核计划的等级分布：已归档考核按等级的人数、平均分（数据库查询结果，为空的列不输出）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GradeStat {
    private String grade;
    private Long count;
    private BigDecimal avgScore;
}
