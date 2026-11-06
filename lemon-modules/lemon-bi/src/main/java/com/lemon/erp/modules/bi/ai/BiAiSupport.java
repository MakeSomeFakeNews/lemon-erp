package com.lemon.erp.modules.bi.ai;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;

/**
 * BI 模块 AI 问数工具的公共检查：驾驶舱、决策分析汇总全公司数据，与页面一样只对有「全部数据」权限的用户开放
 */
final class BiAiSupport {

    private BiAiSupport() {
    }

    static void requireAllData() {
        if (!SecurityUtils.getDataScope().all()) {
            throw new BizException("bi.error.cockpitDecisionAnalysisAggregateCompanyWide");
        }
    }
}
