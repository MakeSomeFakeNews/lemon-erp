package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.framework.data.SearchKeys;
import com.lemon.erp.modules.wms.model.StockSign;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 盘点单「选择商品」的查询条件：数据来源是物料资料（不是库存），可只列出一段时间内有过库存变动的物料（动态盘点）
 *
 * @param warehouseId   盘点仓库：当前库存、库存条件、变动、货位都按该仓库；为空时为全部仓库
 * @param warehouseGoods 只看盘点仓库的货品：在该仓库有库存记录（含已出完的）、有过库存流水，或默认库位在该仓库
 * @param keyword       编码、名称、规格、条码或检索列（拼音首字母等）模糊匹配
 * @param category      分类编码，含下级分类
 * @param brand         品牌（业务字典 wms_material_brand）
 * @param locationId    货位（精确）：物料的默认库位是它，或在该库位有库存
 * @param changed       只看有过库存变动的物料：按库存流水的发生时间（确认时间），含出入库、调拨、组装拆卸等，不含盘点本身的盘盈盘亏
 * @param changeBegin   变动日期起（含），可为空
 * @param changeEnd     变动日期止（含），可为空
 * @param purchaseBegin 最新进货日期起：最近一次采购入库（已完成）的业务日期在范围内，全公司
 * @param salesBegin    最新销售日期起：最近一次销售出库（已完成）的业务日期在范围内，全公司
 * @param status        物料状态，为空不限（停用的物料可能还有库存，也可以盘点）
 * @param stock         库存条件：&gt; 0、= 0（含没有库存记录的）、&lt; 0，按盘点仓库的合计；为空不限
 * @param inStock       只看库存 &gt; 0 的物料（兼容旧参数，等同于 stock = POSITIVE）
 */
public record CheckMaterialQuery(Long warehouseId, Boolean warehouseGoods, String keyword, String category, String brand, Long locationId,
                                 Boolean changed,
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeBegin,
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeEnd,
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseBegin,
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseEnd,
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesBegin,
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesEnd,
                                 EnableStatus status, StockSign stock, Boolean inStock) {

    public boolean changedOnly() {
        return Boolean.TRUE.equals(changed);
    }

    /** 只看盘点仓库的货品（未选仓库时不限） */
    public boolean warehouseGoodsOnly() {
        return warehouseId != null && Boolean.TRUE.equals(warehouseGoods);
    }

    /** 库存条件，旧参数 inStock = true 等同于 &gt; 0 */
    public StockSign stockSign() {
        return stock != null ? stock : Boolean.TRUE.equals(inStock) ? StockSign.POSITIVE : null;
    }

    /** 变动时间下限（含）：起始日期的 0 点 */
    public LocalDateTime changeFrom() {
        return changeBegin == null ? null : changeBegin.atStartOfDay();
    }

    /** 变动时间上限（不含）：截止日期次日的 0 点 */
    public LocalDateTime changeTo() {
        return changeEnd == null ? null : changeEnd.plusDays(1).atStartOfDay();
    }

    public boolean purchaseFilter() {
        return purchaseBegin != null || purchaseEnd != null;
    }

    public boolean salesFilter() {
        return salesBegin != null || salesEnd != null;
    }

    public String trimmedKeyword() {
        return keyword == null ? "" : keyword.trim();
    }

    /** 检索列匹配用的规范化关键字（{@link SearchKeys#normalize}） */
    public String searchNorm() {
        return SearchKeys.normalize(keyword);
    }
}
