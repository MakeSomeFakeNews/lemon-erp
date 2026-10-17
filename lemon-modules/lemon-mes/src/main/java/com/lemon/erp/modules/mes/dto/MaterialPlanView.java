package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.mes.model.SupplyType;
import com.lemon.erp.modules.mes.entity.MesMaterialSubstitute;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 生产物料：仓库物料主数据 + 计划属性 + 默认 BOM、默认供应商、库存
 */
@Data
public class MaterialPlanView {
    private Long id;
    private String code;
    private String name;
    private String spec;
    private String unit;
    private String category;
    private Integer batchEnabled;
    private Integer serialEnabled;
    private BigDecimal safetyStock;
    private EnableStatus status;
    private String remark;

    /** 设置的物料属性，为空表示自动 */
    private SupplyType supplyType;
    /** 生效的物料属性：设置的，或有默认 BOM 为 MAKE、否则为 BUY */
    private SupplyType effectiveSupplyType;
    private Integer leadDays;
    private BigDecimal minLot;
    private BigDecimal lotMultiple;
    private BigDecimal overIssueRate;
    private String planner;
    private Integer fqcRequired;

    private Long bomId;
    private String bomVersion;
    private Integer bomLeadDays;
    private Long supplierId;
    private String supplierName;
    private Integer supplierLeadDays;
    private BigDecimal supplierMinQty;

    private BigDecimal onHandQty;
    private Integer substituteCount;

    /** 详情：替代料、被哪些 BOM 使用 */
    private List<MesMaterialSubstitute> substitutes;
    private List<WhereUsed> whereUsed;

    @Data
    public static class WhereUsed {
        private Long bomId;
        private String version;
        private Integer isDefault;
        private EnableStatus bomStatus;
        private Long productId;
        private String productCode;
        private String productName;
        private BigDecimal qty;
        private BigDecimal lossRate;
    }
}
