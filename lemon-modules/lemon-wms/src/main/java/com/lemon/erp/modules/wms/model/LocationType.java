package com.lemon.erp.modules.wms.model;

/**
 * 库位类型，与字典 wms_location_type 对应。上架推荐空库位时只用存储位、拣货位；不良品位的库存不参与拣货建议
 */
public enum LocationType {
    STORAGE,
    PICK,
    RECEIVE,
    SHIP,
    DEFECT
}
