package com.lemon.erp.modules.wms.model;

/**
 * 出入库单的业务类型：入库类型（{@link StockInType}）或出库类型（{@link StockOutType}）。
 * 两者存于同一列 wms_stock_doc.biz_type，因此实体字段保留 String，比较统一通过本接口的方法完成
 */
public interface StockDocType {

    /** 枚举名称，即存库的取值 */
    String name();

    /** 所属单据种类：入库或出库 */
    StockBizType kind();

    /** 可以手工新增：其余类型由业务单据生成，手工做会绕过供应链、生产的应收应付和成本归集 */
    boolean isManual();

    /** 收发类别字典（业务字典），不能填写收发类别时为空 */
    String ioCategoryDict();

    /** 与存库的取值相同 */
    default boolean matches(String bizType) {
        return name().equals(bizType);
    }

    /** 按单据种类解析业务类型；调拨、空值或无效取值返回 null */
    static StockDocType of(StockBizType kind, String bizType) {
        if (bizType == null) {
            return null;
        }
        Class<? extends Enum<?>> type = switch (kind) {
            case IN -> StockInType.class;
            case OUT -> StockOutType.class;
            default -> null;
        };
        if (type == null) {
            return null;
        }
        for (Enum<?> value : type.getEnumConstants()) {
            if (value.name().equals(bizType)) {
                return (StockDocType) value;
            }
        }
        return null;
    }
}
