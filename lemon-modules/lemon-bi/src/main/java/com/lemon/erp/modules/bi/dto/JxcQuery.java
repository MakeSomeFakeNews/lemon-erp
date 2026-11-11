package com.lemon.erp.modules.bi.dto;

import com.lemon.erp.modules.bi.model.GoodsRankBy;
import com.lemon.erp.modules.bi.model.JxcDimension;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

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
    /** 物料分类：筛选时包含下级分类；按分类汇总后穿透时只取该分类（见 {@link #drillCategory}） */
    private String category;
    private String brand;
    private String region;
    /** 仓库：明细行的仓库，为空时取单据表头的仓库 */
    private Long warehouseId;
    /** 物料编码 / 名称 */
    private String keyword;
    /** 只取某张出入库单（单据列表穿透到该单的明细，与其他条件同时生效） */
    private Long docId;

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
    /** 只取该分类本身（不含下级分类）：按分类汇总的行穿透时，与汇总口径一致 */
    @Setter(AccessLevel.NONE)
    private boolean categoryExact;
    @Setter(AccessLevel.NONE)
    private boolean noBrand;
    @Setter(AccessLevel.NONE)
    private boolean noRegion;
    @Setter(AccessLevel.NONE)
    private boolean noSalesman;

    /**
     * 明细是否按整张单据列出：没有按商品、分类、品牌、仓库、商品关键字筛选（只按日期、往来单位、业务员、区域等单据级条件）。
     * 此时收付款折让作为单据的最后一行列出（issue #89），否则只列出单据的部分明细，折让按行金额比例分摊到各行
     */
    public boolean isWholeDocs() {
        return materialId == null && !StringUtils.hasText(category) && !noCategory && !StringUtils.hasText(brand) && !noBrand
                && warehouseId == null && !StringUtils.hasText(keyword);
    }

    /** 按分类汇总的行穿透：只取该分类本身的物料（汇总按物料的分类编码分组，下级分类单独成行） */
    public void drillCategory(String code) {
        category = code;
        categoryExact = true;
    }

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
