package com.lemon.erp.modules.bi.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.bi.dto.BiInsight;
import com.lemon.erp.modules.bi.model.AbcClass;
import com.lemon.erp.modules.bi.service.BiDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ABC 分析：近一年各物料的销售成本（没有成本时用销售额）累计占比分类，复用 {@link BiDecisionService#abc}（需要全部数据权限）
 */
@Component
@RequiredArgsConstructor
public class BiAbcTool implements AiTool {

    /** 物料的分类结果，金额单位元；sharePercent 占比、cumulativePercent 累计占比（%） */
    record AbcItem(String code, String name, String category, BigDecimal qty, BigDecimal value, BigDecimal sharePercent,
                   BigDecimal cumulativePercent, AbcClass abcClass) {
    }

    /** 各类的物料数、金额、占比（%） */
    record AbcGroup(AbcClass abcClass, long count, BigDecimal value, BigDecimal sharePercent) {
    }

    /**
     * @param basis     分类依据（销售成本或销售额）
     * @param materials 物料总数（满足 abcClass 条件的）
     * @param rows      按金额降序的前 top 个物料
     */
    record Abc(String basis, BigDecimal total, List<AbcGroup> summary, long materials, List<AbcItem> rows) {
    }

    private final BiDecisionService decisionService;

    @Override
    public String name() {
        return "bi_abc_analysis";
    }

    @Override
    public String labelKey() {
        return "bi.ai.tool.abc";
    }

    @Override
    public String descriptionKey() {
        return "bi.ai.tool.abc.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object()
                .prop("abcClass", AiSchema.enumOf("bi.ai.param.abcClass", AbcClass.A.name(), AbcClass.B.name(), AbcClass.C.name()), false)
                .prop("top", AiSchema.integer("bi.ai.param.top").min(1).max(AiTool.MAX_ROWS), false)
                .build();
    }

    @Override
    public String permission() {
        return "bi:decision:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        BiAiSupport.requireAllData();
        BiInsight.Abc abc = decisionService.abc();
        AbcClass only = args.has("abcClass") ? AbcClass.valueOf(args.getString("abcClass")) : null;
        List<BiInsight.AbcRow> matched = abc.rows().stream().filter(r -> only == null || r.getAbcClass() == only).toList();
        List<AbcItem> rows = matched.stream().limit(args.getInt("top", 20))
                .map(r -> new AbcItem(r.getCode(), r.getName(), r.getCategory(), r.getQty(), r.getValue(), r.getShare(), r.getCumulative(), r.getAbcClass()))
                .toList();
        List<AbcGroup> summary = abc.summary().stream().map(s -> new AbcGroup(s.abcClass(), s.count(), s.value(), s.share())).toList();
        return AiToolResult.of(new Abc(abc.basis(), abc.total(), summary, matched.size(), rows));
    }
}
