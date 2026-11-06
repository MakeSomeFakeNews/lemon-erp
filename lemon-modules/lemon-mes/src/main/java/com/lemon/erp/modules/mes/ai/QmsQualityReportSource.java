package com.lemon.erp.modules.mes.ai;

import com.lemon.erp.framework.ai.AiReportMetrics;
import com.lemon.erp.framework.ai.AiReportPeriod;
import com.lemon.erp.framework.ai.AiReportSource;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.mes.dto.QmsViews;
import com.lemon.erp.modules.mes.service.QmsAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能报告「质量」：质量分析（{@link QmsAnalysisService} 的合格率趋势、不良柏拉图）在本期与上期的检验批数、合格率，
 * 按检验类型的合格率，不良类型排行。检验数据不按部门区分，只对有「全部数据」权限的用户开放
 */
@Component
@RequiredArgsConstructor
public class QmsQualityReportSource implements AiReportSource {

    private static final int TOP = 5;

    private final QmsAnalysisService analysisService;
    private final DictLookup dictLookup;

    @Override
    public String key() {
        return "mes.quality";
    }

    @Override
    public String labelKey() {
        return "mes.ai.report.quality";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public String permission() {
        return "qms:analysis:list";
    }

    @Override
    public boolean requiresAllData() {
        return true;
    }

    @Override
    public QualitySnapshot snapshot(AiReportPeriod period) {
        Map<String, String> types = dictLookup.options("mes_qc_type");
        Map<String, String> defects = dictLookup.options("mes_defect_type");
        PassRate current = passRate(analysisService.trend(period.start(), period.end()), types);
        AiReportPeriod prev = period.previous();
        PassRate previous = passRate(analysisService.trend(prev.start(), prev.end()), types);
        List<QmsViews.ParetoRow> pareto = analysisService.pareto(period.start(), period.end());
        BigDecimal defectTotal = pareto.stream().map(r -> AiReportMetrics.nz(r.qty())).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Defect> topDefects = pareto.stream().limit(TOP)
                .map(r -> new Defect(defects.getOrDefault(r.defectType(), r.defectType()), r.qty(), r.count(),
                        AiReportMetrics.percent(r.qty(), defectTotal)))
                .toList();
        return new QualitySnapshot(current, previous,
                current.passRatePct() == null || previous.passRatePct() == null ? null
                        : current.passRatePct().subtract(previous.passRatePct()),
                topDefects);
    }

    /** 汇总质量分析的合格率趋势（按月、检验类型）为期间合计和按类型的合格率 */
    static PassRate passRate(List<QmsViews.TrendRow> rows, Map<String, String> typeNames) {
        long total = 0;
        long pass = 0;
        long fail = 0;
        Map<String, long[]> byType = new LinkedHashMap<>();
        for (QmsViews.TrendRow r : rows) {
            total += r.total();
            pass += r.pass();
            fail += r.fail();
            long[] t = byType.computeIfAbsent(r.type(), k -> new long[2]);
            t[0] += r.total();
            t[1] += r.pass();
        }
        List<TypeRate> types = byType.entrySet().stream()
                .map(e -> new TypeRate(typeNames.getOrDefault(e.getKey(), e.getKey()), e.getValue()[0], e.getValue()[1],
                        AiReportMetrics.percent(e.getValue()[1], e.getValue()[0])))
                .toList();
        return new PassRate(total, pass, fail, AiReportMetrics.percent(pass, total), types);
    }

    /**
     * 检验合格率（已确认的检验单，让步接收计为合格）
     *
     * @param inspections 检验批数
     * @param passRatePct 合格率（%）
     * @param byType      按检验类型（来料、过程、成品、首件）
     */
    public record PassRate(long inspections, long passed, long failed, BigDecimal passRatePct, List<TypeRate> byType) {
    }

    public record TypeRate(String type, long inspections, long passed, BigDecimal passRatePct) {
    }

    /**
     * 不良类型
     *
     * @param defectQty 不良数量
     * @param batches   不合格批数
     * @param sharePct  占不良数量的百分比
     */
    public record Defect(String type, BigDecimal defectQty, long batches, BigDecimal sharePct) {
    }

    /**
     * 质量快照
     *
     * @param passRateChangePoints 合格率比上期变化（百分点）
     * @param topDefects           不良数量前 5 的不良类型（本期）
     */
    public record QualitySnapshot(PassRate current, PassRate previous, BigDecimal passRateChangePoints, List<Defect> topDefects) {
    }
}
