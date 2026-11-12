package com.lemon.erp.modules.bi.dto;

import com.lemon.erp.framework.data.SearchKeys;
import com.lemon.erp.modules.bi.model.OverstockBase;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 积压分析的查询条件（GET 参数，issue #111）
 */
@Data
public class OverstockQuery {

    /** 仓库：库存、销量、分仓库的上限都按该仓库；为空时为全部仓库 */
    private Long warehouseId;
    /** 物料分类，包含下级分类 */
    private String category;
    private String brand;
    /** 商品描述：编码、名称、规格包含，或检索列（拼音首字母、去掉分隔符的缩写）包含 */
    private String keyword;
    /** 截至日期，库存按该日期截止计算，不传为今天 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    /** 积压基数方式，不传为最近 N 天销量 */
    private OverstockBase base;
    /** 最近 N 天的 N（1–366），不传为 60 */
    private Integer days;

    // ---------------------------------------------------------------- 由 Service 计算，不从请求接收

    /** 销量的统计期间（积压基数为销量时） */
    @Setter(AccessLevel.NONE)
    private LocalDate salesBegin;
    @Setter(AccessLevel.NONE)
    private LocalDate salesEnd;
    /** 有「查看成本金额」权限：返回资金占用，并按资金占用排序 */
    @Setter(AccessLevel.NONE)
    private boolean costVisible;

    public void resolve(LocalDate salesBegin, LocalDate salesEnd, boolean costVisible) {
        this.salesBegin = salesBegin;
        this.salesEnd = salesEnd;
        this.costVisible = costVisible;
    }

    /** 规范化的关键字，匹配检索列（mapper 中使用） */
    public String getSearchNorm() {
        return SearchKeys.normalize(keyword == null ? null : keyword.trim());
    }
}
