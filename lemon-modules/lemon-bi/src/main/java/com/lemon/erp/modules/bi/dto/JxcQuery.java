package com.lemon.erp.modules.bi.dto;

import com.lemon.erp.modules.bi.model.GoodsRankBy;
import com.lemon.erp.modules.bi.model.JxcDimension;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 进销存报表的查询条件（GET 参数）。日期范围必填；dim + key 表示从汇总行穿透（key 为空表示「未设置」的那一行），
 * 由 Service 转换为对应的筛选条件，汇总与明细使用同一套条件
 */
@Data
public class JxcQuery {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate begin;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate end;

    /** 客户（销售报表）/ 供应商（进货报表） */
    private Long partnerId;
    /** 业务员（用户 ID） */
    private Long salesmanId;
    private Long materialId;
    private String category;
    private String brand;
    private String region;
    /** 仓库：明细行的仓库，为空时取单据表头的仓库 */
    private Long warehouseId;
    /** 物料编码 / 名称 */
    private String keyword;

    /** 穿透：汇总维度和汇总行的键 */
    private JxcDimension dim;
    private String key;

    /** 商品分析：畅销排序、滞销的销量上限、积压的可销天数阈值 */
    private GoodsRankBy rankBy;
    private BigDecimal slowQty;
    private Integer overstockDays;

    // ---------------------------------------------------------------- 由穿透生成，不从请求接收

    @Setter(AccessLevel.NONE)
    private boolean noCategory;
    @Setter(AccessLevel.NONE)
    private boolean noBrand;
    @Setter(AccessLevel.NONE)
    private boolean noRegion;
    @Setter(AccessLevel.NONE)
    private boolean noSalesman;

    /** 筛选「未设置」的取值：只能由穿透设置 */
    public void markNone(JxcDimension d) {
        switch (d) {
            case CATEGORY -> noCategory = true;
            case BRAND -> noBrand = true;
            case REGION -> noRegion = true;
            case SALESMAN -> noSalesman = true;
            default -> {
            }
        }
    }
}
