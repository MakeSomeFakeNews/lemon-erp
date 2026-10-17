package com.lemon.erp.modules.crm.service;

import com.lemon.erp.modules.crm.dto.RfmRow;
import com.lemon.erp.modules.crm.mapper.CrmAnalysisMapper;
import com.lemon.erp.modules.crm.model.CrmConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户分析：经营概览、销售漏斗与阶段转化率、RFM 客户价值分层、销售趋势、来源与销售人员分析、服务质量
 */
@Service
@RequiredArgsConstructor
public class CrmAnalysisService {

    private final CrmAnalysisMapper analysisMapper;

    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        Map<String, Object> m = new LinkedHashMap<>(analysisMapper.selectOverview(today.withDayOfMonth(1).atStartOfDay(),
                LocalDateTime.now()));
        long won = num(m.get("wonTotal")).longValue();
        long lost = num(m.get("lostTotal")).longValue();
        m.put("winRate", won + lost == 0 ? null : BigDecimal.valueOf(won * 100).divide(BigDecimal.valueOf(won + lost), 1, RoundingMode.HALF_UP));
        m.put("customerStages", analysisMapper.selectCustomerStages());
        return m;
    }

    /**
     * 销售漏斗：各阶段当前的商机数和金额；阶段转化率 = 到达下一阶段的商机数 / 到达本阶段的商机数（按阶段日志）
     */
    public List<Map<String, Object>> funnel() {
        Map<String, Map<String, Object>> current = new HashMap<>();
        analysisMapper.selectFunnel().forEach(r -> current.put((String) r.get("stage"), r));
        Map<String, Long> reached = new HashMap<>();
        analysisMapper.selectStageReached().forEach(r -> reached.put((String) r.get("stage"), num(r.get("count")).longValue()));
        List<String> stages = new ArrayList<>(CrmConstants.OPEN_STAGES);
        stages.add("WON");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < stages.size(); i++) {
            String stage = stages.get(i);
            Map<String, Object> cur = current.getOrDefault(stage, Map.of());
            // 跳过中间阶段直接推进的商机也计入前面的阶段
            long reach = 0;
            for (int j = i; j < stages.size(); j++) {
                reach = Math.max(reach, reached.getOrDefault(stages.get(j), 0L));
            }
            long reachedHere = reach;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stage", stage);
            row.put("count", num(cur.get("count")));
            row.put("amount", num(cur.get("amount")));
            row.put("reached", reachedHere);
            rows.add(row);
        }
        for (int i = 0; i < rows.size() - 1; i++) {
            long here = (long) rows.get(i).get("reached");
            long next = (long) rows.get(i + 1).get("reached");
            rows.get(i).put("conversion", here == 0 ? null : BigDecimal.valueOf(next * 100).divide(BigDecimal.valueOf(here), 1, RoundingMode.HALF_UP));
        }
        Map<String, Object> lost = current.getOrDefault("LOST", Map.of());
        Map<String, Object> lostRow = new LinkedHashMap<>();
        lostRow.put("stage", "LOST");
        lostRow.put("count", num(lost.get("count")));
        lostRow.put("amount", num(lost.get("amount")));
        rows.add(lostRow);
        return rows;
    }

    /**
     * RFM 分析（近一年）：R、F、M 分别与平均值比较，R 天数不高于平均为「近」，F、M 不低于平均为「高」
     */
    public Map<String, Object> rfm() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> src = analysisMapper.selectRfmSource(today.minusYears(1));
        if (src.isEmpty()) {
            return Map.of("rows", List.of(), "segments", List.of());
        }
        List<long[]> rf = new ArrayList<>();
        double sumR = 0, sumF = 0;
        BigDecimal sumM = BigDecimal.ZERO;
        for (Map<String, Object> r : src) {
            LocalDate last = LocalDate.parse(r.get("lastDate").toString().substring(0, 10));
            long recency = ChronoUnit.DAYS.between(last, today);
            long freq = num(r.get("frequency")).longValue();
            rf.add(new long[]{recency, freq});
            sumR += recency;
            sumF += freq;
            sumM = sumM.add(num(r.get("monetary")));
        }
        double avgR = sumR / src.size();
        double avgF = sumF / src.size();
        BigDecimal avgM = sumM.divide(BigDecimal.valueOf(src.size()), 2, RoundingMode.HALF_UP);
        List<RfmRow> rows = new ArrayList<>();
        Map<String, long[]> segments = new LinkedHashMap<>();
        for (String s : SEGMENTS) segments.put(s, new long[]{0});
        Map<String, BigDecimal> segmentAmount = new HashMap<>();
        for (int i = 0; i < src.size(); i++) {
            Map<String, Object> r = src.get(i);
            BigDecimal m = num(r.get("monetary"));
            boolean recent = rf.get(i)[0] <= avgR;
            boolean frequent = rf.get(i)[1] >= avgF;
            boolean valuable = m.compareTo(avgM) >= 0;
            String segment = segment(recent, frequent, valuable);
            segments.get(segment)[0]++;
            segmentAmount.merge(segment, m, BigDecimal::add);
            rows.add(new RfmRow(num(r.get("customerId")).longValue(), (String) r.get("code"), (String) r.get("name"),
                    (String) r.get("ownerName"), (int) rf.get(i)[0], (int) rf.get(i)[1], m, recent, frequent, valuable, segment));
        }
        rows.sort(Comparator.comparing(RfmRow::monetary).reversed());
        List<Map<String, Object>> segmentRows = new ArrayList<>();
        segments.forEach((name, count) -> segmentRows.add(Map.of("segment", name, "count", count[0],
                "amount", segmentAmount.getOrDefault(name, BigDecimal.ZERO))));
        return Map.of("rows", rows, "segments", segmentRows,
                "avgRecency", BigDecimal.valueOf(avgR).setScale(1, RoundingMode.HALF_UP),
                "avgFrequency", BigDecimal.valueOf(avgF).setScale(1, RoundingMode.HALF_UP), "avgMonetary", avgM);
    }

    /** 近 12 个月的销售额、成交客户数、赢单金额 */
    public List<Map<String, Object>> trend() {
        YearMonth start = YearMonth.now().minusMonths(11);
        Map<Integer, Map<String, Object>> sales = new HashMap<>();
        analysisMapper.selectMonthlySales(start.atDay(1)).forEach(r -> sales.put(num(r.get("period")).intValue(), r));
        Map<Integer, Map<String, Object>> won = new HashMap<>();
        analysisMapper.selectMonthlyWon(start.atDay(1)).forEach(r -> won.put(num(r.get("period")).intValue(), r));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth ym = start.plusMonths(i);
            int period = ym.getYear() * 100 + ym.getMonthValue();
            Map<String, Object> s = sales.getOrDefault(period, Map.of());
            Map<String, Object> w = won.getOrDefault(period, Map.of());
            rows.add(Map.of("period", period, "sales", num(s.get("amount")), "customers", num(s.get("customers")),
                    "wonCount", num(w.get("count")), "wonAmount", num(w.get("amount"))));
        }
        return rows;
    }

    public List<Map<String, Object>> sources() {
        return analysisMapper.selectSourceStats();
    }

    public List<Map<String, Object>> owners() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = new ArrayList<>(analysisMapper.selectOwnerStats(today.withDayOfYear(1), today.withDayOfMonth(1).atStartOfDay()));
        rows.sort(Comparator.comparing((Map<String, Object> r) -> num(r.get("wonAmount"))).reversed());
        return rows;
    }

    public List<Map<String, Object>> industries() {
        return analysisMapper.selectIndustryStats(LocalDate.now().minusYears(1));
    }

    public List<Map<String, Object>> tickets() {
        return analysisMapper.selectTicketStats();
    }

    // ---------------------------------------------------------------- 内部

    private static final List<String> SEGMENTS = List.of("重要价值客户", "重要发展客户", "重要保持客户", "重要挽留客户", // i18n-ignore 分层名称由前端按中文名翻译
            "一般价值客户", "一般发展客户", "一般保持客户", "一般挽留客户"); // i18n-ignore

    static String segment(boolean recent, boolean frequent, boolean valuable) {
        String level = valuable ? "重要" : "一般"; // i18n-ignore
        String kind = recent ? (frequent ? "价值" : "发展") : (frequent ? "保持" : "挽留"); // i18n-ignore
        return level + kind + "客户"; // i18n-ignore 分层名称是标识，由前端按中文名翻译
    }

    private static BigDecimal num(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
