package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.rule.BizRule;
import com.lemon.erp.framework.rule.BizRuleProvider;
import com.lemon.erp.modules.scm.model.RfmMode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 客户 RFM 分群的默认参数（业务参数，按租户；模块、名称、说明为消息键）。
 * 进销存报表和客户关系的「客户分析」页面可以临时修改，有业务参数编辑权限的用户可在页面上保存为默认值；读取见 {@link ScmRfmService}
 */
@Component
public class ScmRfmRules implements BizRuleProvider {

    private static final String MODULE = "scm.label.rfmModule";

    public static final String MODE = "scm.rfm.mode";
    public static final String RECENCY_DAYS = "scm.rfm.recencyDays";
    public static final String FREQUENCY = "scm.rfm.frequency";
    public static final String MONETARY = "scm.rfm.monetary";

    static final int MAX_RECENCY_DAYS = 3650;
    static final int MAX_FREQUENCY = 1000;
    static final int MAX_MONETARY = 100_000_000;
    static final int SCALE = 2;

    @Override
    public List<BizRule> rules() {
        return List.of(
                BizRule.select(MODE, MODULE, "scm.label.rfmMode", RfmMode.THRESHOLD, RfmMode.class, "scm.label.rfmModeRemark"),
                BizRule.number(RECENCY_DAYS, MODULE, "scm.label.rfmRecencyDays", 30, 1, MAX_RECENCY_DAYS,
                        "scm.label.rfmRecencyDaysRemark"),
                BizRule.decimal(FREQUENCY, MODULE, "scm.label.rfmFrequency", "1", SCALE, 0, MAX_FREQUENCY,
                        "scm.label.rfmFrequencyRemark"),
                BizRule.decimal(MONETARY, MODULE, "scm.label.rfmMonetary", "1000", SCALE, 0, MAX_MONETARY,
                        "scm.label.rfmMonetaryRemark"));
    }
}
