package com.lemon.erp.modules.ai.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiReportPeriod;
import com.lemon.erp.framework.ai.AiReportSource;
import com.lemon.erp.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * 收集智能报告的数据来源快照（在当前用户的权限、数据权限、语言下调用各来源），组成发给模型的 JSON：
 * {@code {period, previousPeriod, sources: [{key, label, dataScope, data | error}]}}。
 * 一个来源失败不影响其他来源，失败原因写入 error。只查询，不在数据库事务中
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiReportCollector {

    /** 全公司口径 */
    public static final String SCOPE_ALL = "ALL";
    /** 按当前用户的数据权限统计 */
    public static final String SCOPE_LIMITED = "LIMITED";

    private final ObjectMapper objectMapper;

    /**
     * @param sources 当前用户有权限的来源（由注册表过滤）
     */
    public ObjectNode collect(AiReportPeriod period, List<AiReportSource> sources) {
        ObjectNode root = objectMapper.createObjectNode();
        root.putObject("period").put("start", period.start().toString()).put("end", period.end().toString()).put("days", period.days());
        AiReportPeriod prev = period.previous();
        root.putObject("previousPeriod").put("start", prev.start().toString()).put("end", prev.end().toString());
        ArrayNode list = root.putArray("sources");
        boolean allData = SecurityUtils.getDataScope().all();
        for (AiReportSource source : sources) {
            ObjectNode n = list.addObject();
            n.put("key", source.key());
            n.put("label", Messages.get(source.labelKey()));
            n.put("dataScope", source.requiresAllData() || allData ? SCOPE_ALL : SCOPE_LIMITED);
            try {
                n.set("data", objectMapper.valueToTree(source.snapshot(period)));
            } catch (BizException e) {
                n.put("error", e.getLocalizedMessage());
            } catch (RuntimeException e) {
                log.warn("智能报告数据来源 {} 取数失败，期间 {} ~ {}", source.key(), period.start(), period.end(), e);
                n.put("error", Messages.get("ai.text.reportSourceFailed"));
            }
        }
        return root;
    }
}
