package com.lemon.erp.modules.wms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.enums.EnableStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 盘点单「选择商品」的一行：物料资料 + 盘点仓库的当前库存、最近变动时间，以及全公司最新进货 / 销售日期
 */
@Data
public class CheckMaterialView {
    private Long id;
    private String code;
    private String name;
    private String spec;
    private String unit;
    private String category;
    private String brand;
    private Integer batchEnabled;
    private Integer serialEnabled;
    private EnableStatus status;
    /** 盘点仓库的当前库存（未选仓库时为全部仓库合计），没有库存为 0 */
    private BigDecimal qty;
    /** 盘点仓库最近一次库存变动（不含盘点）的时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastChangeTime;
    /** 最近一次采购入库的业务日期（全公司） */
    private LocalDate lastPurchaseDate;
    /** 最近一次销售出库的业务日期（全公司） */
    private LocalDate lastSalesDate;
}
