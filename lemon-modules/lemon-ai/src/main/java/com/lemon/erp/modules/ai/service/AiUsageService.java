package com.lemon.erp.modules.ai.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ai.client.LlmUsage;
import com.lemon.erp.modules.ai.dto.AiUsageSummary;
import com.lemon.erp.modules.ai.entity.AiConfig;
import com.lemon.erp.modules.ai.entity.AiUsage;
import com.lemon.erp.modules.ai.mapper.AiUsageMapper;
import com.lemon.erp.modules.ai.model.AiFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;

/** 大模型用量：每次调用记录一条，调用前检查本月是否超过上限 */
@Service
@RequiredArgsConstructor
public class AiUsageService {

    private static final int TOP_USERS = 20;

    private final AiUsageMapper usageMapper;

    private static LocalDateTime monthStart() {
        return YearMonth.now().atDay(1).atStartOfDay();
    }

    /** 本月已用 token（输入 + 输出） */
    public long usedThisMonth() {
        return usageMapper.sumTokensSince(monthStart());
    }

    /**
     * 本月用量达到上限时拒绝调用
     *
     * @throws BizException ai.error.monthlyLimitReached
     */
    public void checkQuota(AiConfig config) {
        Long limit = config.getMonthlyTokenLimit();
        if (limit == null) {
            return;
        }
        long used = usedThisMonth();
        if (used >= limit) {
            throw new BizException("ai.error.monthlyLimitReached", used, limit);
        }
    }

    /** 记录一次调用的用量（调用人为当前登录用户） */
    public void record(AiFeature feature, String model, LlmUsage usage) {
        AiUsage u = new AiUsage();
        u.setUserId(SecurityUtils.getUserIdOrNull());
        u.setFeature(feature);
        u.setModel(model == null || model.length() <= 64 ? model : model.substring(0, 64));
        u.setPromptTokens(usage == null ? 0 : usage.promptTokens());
        u.setCompletionTokens(usage == null ? 0 : usage.completionTokens());
        usageMapper.insert(u);
    }

    public AiUsageSummary summary(AiConfig config) {
        LocalDateTime since = monthStart();
        return new AiUsageSummary(YearMonth.now().toString(), usageMapper.sumTokensSince(since),
                config == null ? null : config.getMonthlyTokenLimit(),
                usageMapper.sumByFeature(since), usageMapper.sumByUser(since, TOP_USERS));
    }
}
