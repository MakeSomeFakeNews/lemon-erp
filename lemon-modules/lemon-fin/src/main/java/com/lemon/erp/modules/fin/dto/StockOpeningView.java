package com.lemon.erp.modules.fin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 存货期初：已保存的期初，或按库存流水推算的启用期间之前的结存数量（金额待填）
 */
@Data
public class StockOpeningView {
    /** 启用期间，未设置时为空 */
    private Integer startPeriod;
    /** 已有之后期间的成本核算时不能修改 */
    private boolean locked;
    private List<Row> rows;
    /** 存货期初金额按科目汇总 与 科目期初余额的对比 */
    private List<AccountCheck> checks;

    @Data
    public static class Row {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String spec;
        private String unit;
        private String category;
        /** 按库存流水推算的结存数量 */
        private BigDecimal bookQty;
        private BigDecimal qty;
        private BigDecimal amount;
    }

    @Data
    public static class AccountCheck {
        private String accountCode;
        private String accountName;
        private BigDecimal stockAmount;
        private BigDecimal ledgerAmount;
    }
}
