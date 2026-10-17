package com.lemon.erp.modules.hr.service;

import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.modules.hr.model.HrRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 读取当前租户的人力资源计算规则（业务参数按租户缓存；一次计算中只读取一次，传给计算方法）
 */
@Service
@RequiredArgsConstructor
public class HrRuleService {

    private final BizRules bizRules;

    public HrRules current() {
        return HrRules.of(bizRules::get);
    }
}
