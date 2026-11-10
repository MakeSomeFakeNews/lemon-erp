package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.modules.scm.dto.RfmSettings;
import com.lemon.erp.modules.scm.model.RfmMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 客户 RFM 分群参数：页面临时传入的值优先，没有传入的取当前租户的默认值（{@link ScmRfmRules}）
 */
@Service
@RequiredArgsConstructor
public class ScmRfmService {

    private final BizRules bizRules;

    /**
     * @param overrides 页面传入的参数，可为空，为空的项取默认值
     * @return 实际使用的参数（四项都不为空）
     */
    public RfmSettings settings(RfmSettings overrides) {
        RfmSettings o = overrides == null ? new RfmSettings() : overrides;
        RfmSettings s = new RfmSettings(
                o.getMode() != null ? o.getMode() : bizRules.option(ScmRfmRules.MODE, RfmMode.class),
                o.getRecencyDays() != null ? o.getRecencyDays() : bizRules.number(ScmRfmRules.RECENCY_DAYS),
                o.getFrequency() != null ? o.getFrequency() : bizRules.decimal(ScmRfmRules.FREQUENCY),
                o.getMonetary() != null ? o.getMonetary() : bizRules.decimal(ScmRfmRules.MONETARY));
        check(s);
        return s;
    }

    static void check(RfmSettings s) {
        if (s.getRecencyDays() < 1 || s.getRecencyDays() > ScmRfmRules.MAX_RECENCY_DAYS) {
            throw new BizException("scm.error.rfmRecencyDays", String.valueOf(ScmRfmRules.MAX_RECENCY_DAYS));
        }
        if (!inRange(s.getFrequency(), ScmRfmRules.MAX_FREQUENCY)) {
            throw new BizException("scm.error.rfmFrequency", String.valueOf(ScmRfmRules.MAX_FREQUENCY));
        }
        if (!inRange(s.getMonetary(), ScmRfmRules.MAX_MONETARY)) {
            throw new BizException("scm.error.rfmMonetary", String.valueOf(ScmRfmRules.MAX_MONETARY));
        }
    }

    private static boolean inRange(BigDecimal v, int max) {
        return v.signum() >= 0 && v.compareTo(BigDecimal.valueOf(max)) <= 0 && v.stripTrailingZeros().scale() <= ScmRfmRules.SCALE;
    }
}
