package com.lemon.erp.modules.wms.model;

import com.lemon.erp.modules.wms.entity.WmsMaterial;

import java.math.BigDecimal;

/**
 * 物料的价格列：0 零售价，1-8 批发价1-8（wms_material.retail_price / wholesale_price1-8，含税、基本单位、本位币）
 */
public final class PriceLevels {

    public static final int RETAIL = 0;
    public static final int MAX = 8;

    /** 各价格列在物料（JSON）中的字段名，下标为价格列 */
    public static final java.util.List<String> FIELDS = java.util.List.of("retailPrice", "wholesalePrice1", "wholesalePrice2",
            "wholesalePrice3", "wholesalePrice4", "wholesalePrice5", "wholesalePrice6", "wholesalePrice7", "wholesalePrice8");

    private PriceLevels() {
    }

    public static boolean valid(Integer level) {
        return level != null && level >= RETAIL && level <= MAX;
    }

    /** 物料在某价格列的价格，没有设置时为 null */
    public static BigDecimal price(WmsMaterial m, int level) {
        return switch (level) {
            case 0 -> m.getRetailPrice();
            case 1 -> m.getWholesalePrice1();
            case 2 -> m.getWholesalePrice2();
            case 3 -> m.getWholesalePrice3();
            case 4 -> m.getWholesalePrice4();
            case 5 -> m.getWholesalePrice5();
            case 6 -> m.getWholesalePrice6();
            case 7 -> m.getWholesalePrice7();
            case 8 -> m.getWholesalePrice8();
            default -> null;
        };
    }

    public static void setPrice(WmsMaterial m, int level, BigDecimal price) {
        switch (level) {
            case 0 -> m.setRetailPrice(price);
            case 1 -> m.setWholesalePrice1(price);
            case 2 -> m.setWholesalePrice2(price);
            case 3 -> m.setWholesalePrice3(price);
            case 4 -> m.setWholesalePrice4(price);
            case 5 -> m.setWholesalePrice5(price);
            case 6 -> m.setWholesalePrice6(price);
            case 7 -> m.setWholesalePrice7(price);
            case 8 -> m.setWholesalePrice8(price);
            default -> throw new IllegalArgumentException("price level " + level);
        }
    }
}
