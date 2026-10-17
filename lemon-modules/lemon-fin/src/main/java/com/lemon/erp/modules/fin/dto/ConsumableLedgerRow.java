package com.lemon.erp.modules.fin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 低值易耗品领用台账：按部门、物料汇总已确认领用的数量和参考金额（数据库查询结果，为空的列不输出）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsumableLedgerRow {
    private Long deptId;
    private String deptName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String unit;
    private BigDecimal qty;
    private BigDecimal refAmount;
}
