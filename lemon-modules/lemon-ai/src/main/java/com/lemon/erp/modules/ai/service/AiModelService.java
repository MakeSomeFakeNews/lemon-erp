package com.lemon.erp.modules.ai.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ai.client.LlmClient;
import com.lemon.erp.modules.ai.client.LlmEndpoint;
import com.lemon.erp.modules.ai.client.LlmException;
import com.lemon.erp.modules.ai.client.LlmJsonResult;
import com.lemon.erp.modules.ai.client.LlmRequest;
import com.lemon.erp.modules.ai.client.LlmResponse;
import com.lemon.erp.modules.ai.client.LlmStreamListener;
import com.lemon.erp.modules.ai.entity.AiConfig;
import com.lemon.erp.modules.ai.model.AiFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 业务功能调用大模型的入口（AI 问数、智能录入、数据整理、智能报告都通过它）：
 * <ol>
 *   <li>读取当前租户的「AI 设置」，检查已配置、已启用、该功能的开关已打开、本月用量未超过上限</li>
 *   <li>按请求选择模型：带图片时用识图模型（未配置时提示），否则用对话模型</li>
 *   <li>调用 {@link LlmClient}，成功或失败都记录用量（ai_usage，调用人为当前登录用户）</li>
 * </ol>
 * 不要在数据库事务中调用（见 CLAUDE.md 事务约定）；需要在异步线程中调用时，先传递租户、登录用户和语言上下文。
 */
@Service
@RequiredArgsConstructor
public class AiModelService {

    private final AiConfigService configService;
    private final AiUsageService usageService;
    private final LlmClient client;

    /**
     * 检查功能可用并返回当前租户的配置（含 API Key，不要返回前端）
     *
     * @throws BizException 未配置、未启用、功能未开启、本月用量已达上限
     */
    public AiConfig requireAvailable(AiFeature feature) {
        AiConfig c = configService.find();
        if (!AiConfigService.isConfigured(c)) {
            throw new BizException("ai.error.notConfigured");
        }
        if (!Boolean.TRUE.equals(c.getEnabled())) {
            throw new BizException("ai.error.disabled");
        }
        if (!c.featureEnabled(feature)) {
            throw new BizException("ai.error.featureDisabled", Messages.get(feature.labelKey()));
        }
        usageService.checkQuota(c);
        return c;
    }

    /**
     * 功能对应的连接信息
     *
     * @param vision 需要识图模型
     */
    public LlmEndpoint endpoint(AiFeature feature, boolean vision) {
        AiConfig c = requireAvailable(feature);
        LlmEndpoint endpoint = AiConfigService.endpointOf(c);
        if (vision) {
            if (!StringUtils.hasText(c.getVisionModel())) {
                throw new BizException("ai.error.visionModelRequired");
            }
            return endpoint.withModel(c.getVisionModel());
        }
        return endpoint;
    }

    /** 普通对话 / 带工具的对话（{@link LlmClient#chat}） */
    public LlmResponse chat(AiFeature feature, LlmRequest request) {
        LlmEndpoint endpoint = endpoint(feature, request.hasImages());
        return recorded(feature, endpoint, () -> client.chat(endpoint, request));
    }

    /** 流式对话（{@link LlmClient#stream}） */
    public LlmResponse stream(AiFeature feature, LlmRequest request, LlmStreamListener listener) {
        LlmEndpoint endpoint = endpoint(feature, request.hasImages());
        return recorded(feature, endpoint, () -> client.stream(endpoint, request, listener));
    }

    /** JSON 输出，按 schema 校验，不通过重试一次（{@link LlmClient#chatJson}） */
    public LlmJsonResult chatJson(AiFeature feature, LlmRequest request, Map<String, Object> schema) {
        LlmEndpoint endpoint = endpoint(feature, request.hasImages());
        LlmJsonResult result;
        try {
            result = client.chatJson(endpoint, request, schema);
        } catch (LlmException e) {
            recordFailure(feature, endpoint, e);
            throw e;
        }
        usageService.record(feature, endpoint.model(), result.usage());
        return result;
    }

    private LlmResponse recorded(AiFeature feature, LlmEndpoint endpoint, Supplier<LlmResponse> call) {
        LlmResponse res;
        try {
            res = call.get();
        } catch (LlmException e) {
            recordFailure(feature, endpoint, e);
            throw e;
        }
        usageService.record(feature, endpoint.model(), res.usage());
        return res;
    }

    /** 失败前已消耗的用量（如 JSON 重试后仍不合格）照常记录 */
    private void recordFailure(AiFeature feature, LlmEndpoint endpoint, LlmException e) {
        if (e.usage().total() > 0) {
            usageService.record(feature, endpoint.model(), e.usage());
        }
    }
}
