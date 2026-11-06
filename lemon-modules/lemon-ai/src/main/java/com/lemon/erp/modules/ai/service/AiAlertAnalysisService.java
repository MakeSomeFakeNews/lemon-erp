package com.lemon.erp.modules.ai.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.ai.AiAlertSource;
import com.lemon.erp.framework.ai.AiReportPeriod;
import com.lemon.erp.framework.ai.AiReportSource;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.ai.client.LlmMessage;
import com.lemon.erp.modules.ai.client.LlmRequest;
import com.lemon.erp.modules.ai.client.LlmResponse;
import com.lemon.erp.modules.ai.dto.AiAlertAnalysis;
import com.lemon.erp.modules.ai.model.AiFeature;
import com.lemon.erp.modules.ai.registry.AiReportRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 经营预警的 AI 分析：预警规则的当前值、阈值、近期趋势和相关明细（{@link AiAlertSource}），加上当前用户有权限的相关报告来源
 * 近 30 天（与前 30 天对比）的快照，交给模型分析可能的原因和建议。结果不入库也不缓存：分析依据的数据随查看人的权限和实时数据变化，
 * 由页面在打开期间保留。属于「智能报告」功能（开关、用量记为 REPORT）
 */
@Service
@RequiredArgsConstructor
public class AiAlertAnalysisService {

    /** 相关来源的快照期间：近 30 天 */
    private static final int RECENT_DAYS = 30;

    private final ObjectProvider<AiAlertSource> alertSources;
    private final AiReportRegistry registry;
    private final AiReportCollector collector;
    private final AiReportPrompts prompts;
    private final AiModelService modelService;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    /**
     * @throws BizException 没有查看预警的权限、AI 不可用（报告功能未开启等）、规则不存在
     */
    public AiAlertAnalysis analyze(Long ruleId) {
        AiAlertSource source = alertSources.stream().findFirst().orElseThrow(() -> new BizException("ai.error.alertNotFound"));
        if (!permissionService.hasAnyPerm(source.permissions())) {
            throw new BizException(R.FORBIDDEN, "ai.error.capabilityForbidden");
        }
        modelService.requireAvailable(AiFeature.REPORT);
        AiAlertSource.AiAlert alert = source.alert(ruleId);
        if (alert == null) {
            throw new BizException("ai.error.alertNotFound");
        }
        LocalDate today = LocalDate.now();
        AiReportPeriod recent = new AiReportPeriod(today.minusDays(RECENT_DAYS - 1L), today);
        List<AiReportSource> related = alert.reportSources().stream().map(registry::find).flatMap(Optional::stream).toList();
        ObjectNode data = objectMapper.createObjectNode();
        data.set("alert", objectMapper.valueToTree(alert));
        ObjectNode snapshots = collector.collect(recent, related);
        data.set("related", snapshots);
        LlmResponse res = modelService.chat(AiFeature.REPORT, LlmRequest.of(LlmMessage.system(prompts.alertSystem()),
                LlmMessage.user(prompts.alertData(objectMapper.writeValueAsString(data)))));
        return new AiAlertAnalysis(ruleId, res.content(), AiReportService.labels(snapshots), res.usage().total());
    }
}
