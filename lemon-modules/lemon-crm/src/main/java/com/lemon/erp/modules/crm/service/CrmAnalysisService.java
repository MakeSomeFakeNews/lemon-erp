package com.lemon.erp.modules.crm.service;

import com.lemon.erp.modules.crm.dto.CrmAnalysis;
import com.lemon.erp.modules.crm.dto.RfmRow;
import com.lemon.erp.modules.crm.mapper.CrmAnalysisMapper;
import com.lemon.erp.modules.crm.model.OpportunityStage;
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

    public CrmAnalysis.Overview overview() {
        LocalDate today = LocalDate.now();
        CrmAnalysis.Overview m = analysisMapper.selectOverview(today.withDayOfMonth(1).atStartOfDay(), LocalDateTime.now());
        long won = num(m.getWonTotal()).longValue();
        long lost = num(m.getLostTotal()).longValue();
        m.setWinRate(won + lost == 0 ? null : BigDecimal.valueOf(won * 100).divide(BigDecimal.valueOf(won + lost), 1, RoundingMode.HALF_UP));
        m.setCustomerStages(analysisMapper.selectCustomerStages());
        return m;
    }

    /**
     * 销售漏斗：各阶段当前的商机数和金额；阶段转化率 = 到达下一阶段的商机数 / 到达本阶段的商机数（按阶段日志）
     */
    public List<CrmAnalysis.FunnelRow> funnel() {
        Map<String, CrmAnalysis.StageCount> current = new HashMap<>();
        analysisMapper.selectFunnel().forEach(r -> current.put(r.getStage(), r));
        Map<String, Long> reached = new HashMap<>();
        analysisMapper.selectStageReached().forEach(r -> reached.put(r.getStage(), num(r.getCount()).longValue()));
        List<OpportunityStage> stages = new ArrayList<>(OpportunityStage.OPEN_STAGES);
        stages.add(OpportunityStage.WON);
        long[] reach = new long[stages.size()];
        for (int i = 0; i < stages.size(); i++) {
            // 跳过中间阶段直接推进的商机也计入前面的阶段
            for (int j = i; j < stages.size(); j++) {
                reach[i] = Math.max(reach[i], reached.getOrDefault(stages.get(j).name(), 0L));
            }
        }
        List<CrmAnalysis.FunnelRow> rows = new ArrayList<>();
        for (int i = 0; i < stages.size(); i++) {
            String stage = stages.get(i).name();
            CrmAnalysis.StageCount cur = current.get(stage);
            BigDecimal conversion = null;
            if (i < stages.size() - 1 && reach[i] != 0) {
                conversion = BigDecimal.valueOf(reach[i + 1] * 100).divide(BigDecimal.valueOf(reach[i]), 1, RoundingMode.HALF_UP);
            }
            rows.add(new CrmAnalysis.FunnelRow(stage, count(cur), amount(cur), reach[i], conversion));
        }
        CrmAnalysis.StageCount lost = current.get(OpportunityStage.LOST.name());
        rows.add(new CrmAnalysis.FunnelRow(OpportunityStage.LOST.name(), count(lost), amount(lost), null, null));
        return rows;
    }

    /**
     * RFM 分析（近一年）：R、F、M 分别与平均值比较，R 天数不高于平均为「近」，F、M 不低于平均为「高」
     */
    public CrmAnalysis.Rfm rfm() {
        LocalDate today = LocalDate.now();
        List<CrmAnalysis.RfmSource> src = analysisMapper.selectRfmSource(today.minusYears(1));
        if (src.isEmpty()) {
            return new CrmAnalysis.Rfm(List.of(), List.of(), null, null, null);
        }
        List<long[]> rf = new ArrayList<>();
        double sumR = 0, sumF = 0;
        BigDecimal sumM = BigDecimal.ZERO;
        for (CrmAnalysis.RfmSource r : src) {
            LocalDate last = LocalDate.parse(r.getLastDate().substring(0, 10));
            long recency = ChronoUnit.DAYS.between(last, today);
            long freq = num(r.getFrequency()).longValue();
            rf.add(new long[]{recency, freq});
            sumR += recency;
            sumF += freq;
            sumM = sumM.add(num(r.getMonetary()));
        }
        double avgR = sumR / src.size();
        double avgF = sumF / src.size();
        BigDecimal avgM = sumM.divide(BigDecimal.valueOf(src.size()), 2, RoundingMode.HALF_UP);
        List<RfmRow> rows = new ArrayList<>();
        Map<String, long[]> segments = new LinkedHashMap<>();
        for (String s : SEGMENTS) segments.put(s, new long[]{0});
        Map<String, BigDecimal> segmentAmount = new HashMap<>();
        for (int i = 0; i < src.size(); i++) {
            CrmAnalysis.RfmSource r = src.get(i);
            BigDecimal m = num(r.getMonetary());
            boolean recent = rf.get(i)[0] <= avgR;
            boolean frequent = rf.get(i)[1] >= avgF;
            boolean valuable = m.compareTo(avgM) >= 0;
            String segment = segment(recent, frequent, valuable);
            segments.get(segment)[0]++;
            segmentAmount.merge(segment, m, BigDecimal::add);
            rows.add(new RfmRow(num(r.getCustomerId()).longValue(), r.getCode(), r.getName(),
                    r.getOwnerName(), (int) rf.get(i)[0], (int) rf.get(i)[1], m, recent, frequent, valuable, segment));
        }
        rows.sort(Comparator.comparing(RfmRow::monetary).reversed());
        List<CrmAnalysis.RfmSegment> segmentRows = new ArrayList<>();
        segments.forEach((name, count) -> segmentRows.add(new CrmAnalysis.RfmSegment(name, count[0],
                segmentAmount.getOrDefault(name, BigDecimal.ZERO))));
        return new CrmAnalysis.Rfm(rows, segmentRows,
                BigDecimal.valueOf(avgR).setScale(1, RoundingMode.HALF_UP),
                BigDecimal.valueOf(avgF).setScale(1, RoundingMode.HALF_UP), avgM);
    }

    /** 近 12 个月的销售额、成交客户数、赢单金额 */
    public List<CrmAnalysis.TrendRow> trend() {
        YearMonth start = YearMonth.now().minusMonths(11);
        Map<Integer, CrmAnalysis.MonthlyRow> sales = new HashMap<>();
        analysisMapper.selectMonthlySales(start.atDay(1)).forEach(r -> sales.put(num(r.getPeriod()).intValue(), r));
        Map<Integer, CrmAnalysis.MonthlyRow> won = new HashMap<>();
        analysisMapper.selectMonthlyWon(start.atDay(1)).forEach(r -> won.put(num(r.getPeriod()).intValue(), r));
        CrmAnalysis.MonthlyRow none = new CrmAnalysis.MonthlyRow();
        List<CrmAnalysis.TrendRow> rows = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth ym = start.plusMonths(i);
            int period = ym.getYear() * 100 + ym.getMonthValue();
            CrmAnalysis.MonthlyRow s = sales.getOrDefault(period, none);
            CrmAnalysis.MonthlyRow w = won.getOrDefault(period, none);
            rows.add(new CrmAnalysis.TrendRow(period, num(s.getAmount()), num(s.getCustomers()).longValue(),
                    num(w.getCount()).longValue(), num(w.getAmount())));
        }
        return rows;
    }

    public List<CrmAnalysis.SourceStat> sources() {
        return analysisMapper.selectSourceStats();
    }

    public List<CrmAnalysis.OwnerStat> owners() {
        LocalDate today = LocalDate.now();
        List<CrmAnalysis.OwnerStat> rows = new ArrayList<>(analysisMapper.selectOwnerStats(today.withDayOfYear(1), today.withDayOfMonth(1).atStartOfDay()));
        rows.sort(Comparator.comparing((CrmAnalysis.OwnerStat r) -> num(r.getWonAmount())).reversed());
        return rows;
    }

    public List<CrmAnalysis.IndustryStat> industries() {
        return analysisMapper.selectIndustryStats(LocalDate.now().minusYears(1));
    }

    public List<CrmAnalysis.TicketStat> tickets() {
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

    private static long count(CrmAnalysis.StageCount r) {
        return r == null || r.getCount() == null ? 0 : r.getCount();
    }

    private static BigDecimal amount(CrmAnalysis.StageCount r) {
        return r == null ? BigDecimal.ZERO : num(r.getAmount());
    }

    private static BigDecimal num(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
