package com.lemon.erp.framework.rule;

import java.util.List;
import java.util.Set;

/**
 * 业务模块登记自己的业务参数（声明为 Bean）
 */
public interface BizRuleProvider {

    List<BizRule> rules();

    /**
     * 登录用户都可以读取的参数标识（前端按参数显示界面，如是否允许直接开单），经 {@code GET /system/biz-rule/values} 返回。
     * 只登记界面需要的开关，不要登记金额阈值等不应公开的参数
     */
    default Set<String> publicKeys() {
        return Set.of();
    }

    /**
     * 保存前的额外校验（类型、范围等通用校验通过之后调用），不合法时抛 {@code BizException}，如表格各行的上限必须递增
     *
     * @param key   参数标识
     * @param value 规范化之后的值
     */
    default void validate(String key, String value) {
    }
}
