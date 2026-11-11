package com.lemon.erp.framework.ref;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 合并往来单位的影响范围：一类数据中引用被合并资料的行数。mapper XML 按列 label_key / total / conflicts 映射
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MergeImpact {

    /** 数据名称的消息键（如「销售订单」），由各模块登记，合并服务按当前语言翻译后返回 */
    private String labelKey;
    /** 引用被合并资料的行数 */
    private long total;
    /** 其中与保留资料重复、按规则合并或舍弃而不是直接改指向的行数（如同一物料的客户执行价），没有为 0 */
    private long conflicts;
    /** 返回前端时的名称（已翻译） */
    private String label;

    public MergeImpact(String labelKey, long total, long conflicts) {
        this(labelKey, total, conflicts, null);
    }
}
