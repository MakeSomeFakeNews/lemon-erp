package com.lemon.erp.framework.rule;

import java.util.List;

/**
 * 业务模块登记自己的业务参数（声明为 Bean）
 */
public interface BizRuleProvider {

    List<BizRule> rules();

    /**
     * 保存前的额外校验（类型、范围等通用校验通过之后调用），不合法时抛 {@code BizException}，如表格各行的上限必须递增
     *
     * @param key   参数标识
     * @param value 规范化之后的值
     */
    default void validate(String key, String value) {
    }
}
