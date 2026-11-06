package com.lemon.erp.framework.ai;

/**
 * 数据整理可以修改的主数据字段（{@link AiCleanseSource#editableFields()}）。
 * 名称、规格、分类、单位对应 {@link AiCleanseRecord} 的同名字段，联系人、地址放在 {@link AiCleanseRecord#extra()} 中
 * （键为 {@link #extraKey()}），取值统一用 {@link AiCleanseRecord#value(AiCleanseField)}
 */
public enum AiCleanseField {
    /** 名称 */
    NAME(128, null),
    /** 规格型号 */
    SPEC(128, null),
    /** 分类 */
    CATEGORY(64, null),
    /** 计量单位 */
    UNIT(16, null),
    /** 联系人（客户、供应商） */
    CONTACT(64, "contact"),
    /** 地址（客户、供应商） */
    ADDRESS(255, "address");

    private final int maxLength;
    private final String extraKey;

    AiCleanseField(int maxLength, String extraKey) {
        this.maxLength = maxLength;
        this.extraKey = extraKey;
    }

    /** 建议值的最大长度（与业务表的列宽一致，超出的建议直接丢弃） */
    public int maxLength() {
        return maxLength;
    }

    /** 放在 {@link AiCleanseRecord#extra()} 中的字段的键，名称等固定字段为 null */
    public String extraKey() {
        return extraKey;
    }
}
