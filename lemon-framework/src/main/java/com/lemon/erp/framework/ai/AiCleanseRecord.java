package com.lemon.erp.framework.ai;

import java.util.Map;

/**
 * 数据整理的候选记录：发送给模型做查重、规范化分析的主数据字段。只放分析需要的字段，不放价格、电话、银行账号等敏感信息。
 *
 * @param id       记录 ID
 * @param code     编码
 * @param name     名称
 * @param spec     规格型号（没有时为 null）
 * @param category 分类的取值（字典值或分类名称，没有时为 null）
 * @param unit     计量单位（没有时为 null）
 * @param extra    其他有助于判断的字段（如品牌、税号，以及 {@link AiCleanseField#CONTACT} 等字段的
 *                 {@link AiCleanseField#extraKey()}），字段名用英文驼峰，值为 null 的不放；没有时为空 Map
 */
public record AiCleanseRecord(Long id, String code, String name, String spec, String category, String unit,
                              Map<String, Object> extra) {

    /** 查重时按税号判断重复的 extra 键（客户、供应商） */
    public static final String TAX_NO = "taxNo";

    public AiCleanseRecord {
        extra = extra == null ? Map.of() : Map.copyOf(extra);
    }

    /** 字段的当前值（没有时为 null） */
    public String value(AiCleanseField field) {
        return switch (field) {
            case NAME -> name;
            case SPEC -> spec;
            case CATEGORY -> category;
            case UNIT -> unit;
            default -> {
                Object v = extra.get(field.extraKey());
                yield v == null ? null : v.toString();
            }
        };
    }
}
