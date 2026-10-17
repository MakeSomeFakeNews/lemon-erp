package com.lemon.erp.modules.mes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 生产看板：当天按小时（报工录入时间）的产出
 */
@Data
public class BoardHourRow {
    /** 小时 0 ~ 23（列名避开 H2 的关键字 HOUR） */
    @JsonProperty("hour")
    private Integer reportHour;
    private BigDecimal good;
    private BigDecimal scrap;
}
