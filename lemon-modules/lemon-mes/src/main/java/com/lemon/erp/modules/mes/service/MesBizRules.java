package com.lemon.erp.modules.mes.service;

import com.lemon.erp.framework.rule.BizRule;
import com.lemon.erp.framework.rule.BizRuleProvider;
import com.lemon.erp.modules.mes.model.FinishKitCheck;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 生产制造的业务参数（模块、名称、说明为消息键）
 */
@Component
public class MesBizRules implements BizRuleProvider {

    private static final String MODULE = "mes.label.manufacturing";

    /**
     * 完工入库的领料校验：累计完工入库超过按净领料（领料 + 补料 − 退料）计算的齐套数量时 不校验 / 提示 / 禁止。
     * 默认「提示」：已有客户的流程不被拦截，界面上能看到超出的情况
     */
    public static final String FINISH_KIT_CHECK = "mes.finish.kitCheck";

    @Override
    public List<BizRule> rules() {
        return List.of(BizRule.select(FINISH_KIT_CHECK, MODULE, "mes.label.finishKitCheck", FinishKitCheck.WARN, FinishKitCheck.class,
                "mes.label.finishKitCheckRemark"));
    }

    @Override
    public Set<String> publicKeys() {
        return Set.of(FINISH_KIT_CHECK);
    }
}
