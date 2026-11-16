package com.lemon.erp.modules.bi.dto;

import com.lemon.erp.framework.data.SearchKeys;
import com.lemon.erp.modules.bi.model.SalesRateBase;
import com.lemon.erp.modules.bi.model.SalesRateMode;
import com.lemon.erp.modules.bi.model.StockAlertFilter;
import com.lemon.erp.modules.bi.model.StockAlertTab;
import com.lemon.erp.modules.bi.model.StockQtyFilter;
import com.lemon.erp.modules.bi.model.TurnoverBasis;
import com.lemon.erp.modules.bi.model.TurnoverOutFilter;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存分析（周转分析、滞销 / 畅销分析、库存预警，issue #116）的查询条件（GET 参数）。
 * 商品条件与积压分析相同；周转、滞销 / 畅销用日期范围，库存预警用截至日期
 */
@Data
public class StockAnalysisQuery {

    /** 仓库：库存、销量、进货、分仓库的上下限都按该仓库；为空时为全部仓库 */
    private Long warehouseId;
    /** 物料分类，包含下级分类 */
    private String category;
    private String brand;
    /** 商品描述：编码、名称、规格包含，或检索列（拼音首字母、去掉分隔符的缩写）包含 */
    private String keyword;

    /** 周转、滞销 / 畅销：日期范围（必填，不超过 366 天） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate begin;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate end;

    // ---------------------------------------------------------------- 周转分析

    /** 计算维度，不传为按数量 */
    private TurnoverBasis basis;
    /** 计算维度为批发价时的价格列（1–8，须已启用） */
    private Integer priceLevel;
    /** 剔除期初库存为 0 且本期出库为 0 的商品，不传为剔除 */
    private Boolean excludeIdle;
    /** 按本期出库筛选，不传为全部 */
    private TurnoverOutFilter outFilter;

    // ---------------------------------------------------------------- 滞销 / 畅销分析

    /** 不传为滞销 */
    private SalesRateMode mode;
    /** 销售基数，不传为本期进货量 */
    private SalesRateBase base;
    /** 销售比例阈值（%），不传时滞销为 80、畅销为 10 */
    private BigDecimal ratio;

    // ---------------------------------------------------------------- 库存预警

    /** 截至日期，库存按该日期截止计算，不传为今天 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    /** 标签页，不传为仓库 */
    private StockAlertTab tab;
    private StockQtyFilter stockFilter;
    /** 预警筛选，只用于「仓库」标签页 */
    private StockAlertFilter alertFilter;

    // ---------------------------------------------------------------- 由 Service 计算，不从请求接收

    /** 库存预警的月销量期间：截至日往前一个月（截至日 − 1 个月 + 1 天 ~ 截至日） */
    @Setter(AccessLevel.NONE)
    private LocalDate monthBegin;
    /** 有「查看成本金额」权限：返回库存均价、库存金额、资金占用 */
    @Setter(AccessLevel.NONE)
    private boolean costVisible;

    public void resolve(LocalDate monthBegin, boolean costVisible) {
        this.monthBegin = monthBegin;
        this.costVisible = costVisible;
    }

    /** 规范化的关键字，匹配检索列（mapper 中使用） */
    public String getSearchNorm() {
        return SearchKeys.normalize(keyword == null ? null : keyword.trim());
    }
}
