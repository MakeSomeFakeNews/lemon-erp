package com.lemon.erp.framework.rule;

import java.util.List;

/**
 * 业务模块登记自己的业务参数（声明为 Bean）
 */
public interface BizRuleProvider {

    List<BizRule> rules();
}
