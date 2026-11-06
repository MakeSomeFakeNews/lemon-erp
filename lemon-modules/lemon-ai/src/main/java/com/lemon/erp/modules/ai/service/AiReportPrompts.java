package com.lemon.erp.modules.ai.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiReportPeriod;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.ai.mapper.AiConfigMapper;
import com.lemon.erp.modules.ai.model.AiReportPeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;

/**
 * 智能报告、预警分析给模型的提示词（消息键在 ai.properties 中，按当前语言生成；定时任务用默认语言）
 * 与报告标题。提示词明确要求数字只能来自快照、不得编造
 */
@Component
@RequiredArgsConstructor
public class AiReportPrompts {

    private final AiConfigMapper configMapper;

    /** 撰写报告的系统提示：身份、期间、结构（概览、各板块要点、异常与风险、建议）、只用快照中的数字、语言 */
    public String reportSystem(AiReportPeriodType type, AiReportPeriod period) {
        return Messages.get("ai.prompt.reportSystem", Messages.get(type.labelKey()), range(period), range(period.previous()), tenant(),
                Messages.get("ai.prompt.language"));
    }

    /** 报告的数据（快照 JSON） */
    public String reportData(String json) {
        return Messages.get("ai.prompt.reportData", json);
    }

    /** 预警分析的系统提示 */
    public String alertSystem() {
        return Messages.get("ai.prompt.alertSystem", tenant(), LocalDate.now().toString(), Messages.get("ai.prompt.language"));
    }

    /** 预警分析的数据（JSON） */
    public String alertData(String json) {
        return Messages.get("ai.prompt.alertData", json);
    }

    /** 报告标题（存库，用默认语言）：周报（2026-10-26 ~ 2026-11-01）、月报（2026-10） */
    public static String title(AiReportPeriodType type, AiReportPeriod period) {
        Locale locale = Messages.DEFAULT_LOCALE;
        return type == AiReportPeriodType.MONTH
                ? Messages.get(locale, "ai.text.reportTitle.MONTH", period.start().toString().substring(0, 7))
                : Messages.get(locale, "ai.text.reportTitle.WEEK", period.start().toString(), period.end().toString());
    }

    private static String range(AiReportPeriod p) {
        return p.start() + " ~ " + p.end();
    }

    private String tenant() {
        String name = configMapper.selectTenantName(TenantContext.currentOrPlatform());
        return name == null ? "" : name;
    }
}
