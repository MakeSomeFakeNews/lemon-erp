package com.lemon.erp.modules.wms.model;

import com.lemon.erp.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

/**
 * 库存业务类型，与字典 wms_biz_type 对应。IN / OUT / TRANSFER 共用出入库单（wms_stock_doc），CHECK 为盘点单
 */
@Getter
@AllArgsConstructor
public enum StockBizType {

    IN("RK", "入库单"),
    OUT("CK", "出库单"),
    TRANSFER("DB", "调拨单"),
    CHECK("PD", "盘点单");

    /** 单据号前缀 */
    private final String prefix;
    private final String label;

    /** 接口路径与权限标识中使用的名称：in / out / transfer / check */
    public String path() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** 出入库单的种类，按接口路径解析 */
    public static StockBizType ofDocPath(String path) {
        for (StockBizType type : values()) {
            if (type != CHECK && type.path().equals(path)) {
                return type;
            }
        }
        throw new BizException("不支持的单据类型：" + path);
    }
}
