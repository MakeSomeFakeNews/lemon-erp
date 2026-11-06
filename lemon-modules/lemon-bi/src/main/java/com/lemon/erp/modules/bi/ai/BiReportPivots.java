package com.lemon.erp.modules.bi.ai;

import com.lemon.erp.framework.ai.AiReportMetrics;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.bi.dto.PivotRequest;
import com.lemon.erp.modules.bi.dto.PivotResult;
import com.lemon.erp.modules.bi.model.BiCube;
import com.lemon.erp.modules.bi.service.BiEtlService;
import com.lemon.erp.modules.bi.service.BiOlapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 智能报告的商务智能来源共用：按日期范围调用多维分析（{@link BiOlapService#pivot}，数据权限与页面一致）取汇总和排行
 */
@Component
@RequiredArgsConstructor
class BiReportPivots {

    /** 排行的条数 */
    static final int TOP = 5;

    private final BiOlapService olapService;
    private final BiEtlService etlService;
    private final DictLookup dictLookup;

    /** 期间内的多维分析结果，row 为空时只有总计 */
    PivotResult pivot(BiCube cube, String row, LocalDate begin, LocalDate end) {
        return olapService.pivot(new PivotRequest(cube.name(), row, null, null, null, begin, end, null, null));
    }

    /** 期间内的总计（各指标） */
    Map<String, Object> total(BiCube cube, LocalDate begin, LocalDate end) {
        return pivot(cube, null, begin, end).total();
    }

    /** 数据仓库最近一次成功同步的时间（分析数据截至此时） */
    LocalDateTime syncedAt() {
        return etlService.lastSuccess();
    }

    /**
     * 按指标降序的前几名
     *
     * @param dictType 维度值是字典值时的字典类型（名称由字典转换），为空时用多维分析返回的名称
     */
    List<RankItem> top(PivotResult result, String measure, String secondMeasure, String dictType) {
        Map<String, String> names = dictType != null ? dictLookup.options(dictType) : result.labels().getOrDefault("rows", Map.of());
        BigDecimal total = dec(result.total(), measure);
        return result.rowTotals().stream()
                .sorted(Comparator.comparing((Map<String, Object> m) -> dec(m, measure)).reversed())
                .limit(TOP)
                .map(m -> {
                    String key = String.valueOf(m.get("r"));
                    return new RankItem(names.getOrDefault(key, key), dec(m, measure),
                            secondMeasure == null ? null : dec(m, secondMeasure), AiReportMetrics.percent(dec(m, measure), total));
                })
                .toList();
    }

    static BigDecimal dec(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    /** 比率指标（多维分析在汇总后计算，分母为 0 时为空） */
    static BigDecimal ratio(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        return v == null ? null : new BigDecimal(v.toString());
    }

    static long count(Map<String, Object> m, String key) {
        return dec(m, key).longValue();
    }

    /**
     * 排行项
     *
     * @param name     名称
     * @param amount   金额（元）或排行依据的值
     * @param profit   毛利（元），采购等没有时为空
     * @param sharePct 占期间合计的百分比
     */
    record RankItem(String name, BigDecimal amount, BigDecimal profit, BigDecimal sharePct) {
    }
}
