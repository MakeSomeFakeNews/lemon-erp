package com.lemon.erp.modules.crm.service;

import com.lemon.erp.modules.crm.dto.CrmAnalysis;
import com.lemon.erp.modules.crm.dto.RfmRow;
import com.lemon.erp.modules.crm.mapper.CrmAnalysisMapper;
import com.lemon.erp.modules.crm.model.OpportunityStage;
import com.lemon.erp.modules.scm.dto.RfmSettings;
import com.lemon.erp.modules.scm.model.RfmSegment;
import com.lemon.erp.modules.scm.service.RfmCalculator;
import com.lemon.erp.modules.scm.service.ScmRfmService;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户分析：经营概览、销售漏斗与阶段转化率、RFM 客户价值分层、销售趋势、来源与销售人员分析、服务质量
 */
@Service
@RequiredArgsConstructor
public class CrmAnalysisService {

    private final CrmAnalysisMapper analysisMapper;
    private final ScmRfmService rfmService;

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
     * RFM 分析（近一年）：按分群参数（页面传入，为空的项取租户默认值）判断 R、F、M 的高低，分为八类（{@link RfmCalculator}）；
     * 金额为不含税净销售额，与进销存报表的客户 RFM 口径一致（两处共用 M 阈值）
     */
    public CrmAnalysis.Rfm rfm(RfmSettings params) {
        RfmSettings settings = rfmService.settings(params);
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusYears(1).plusDays(1);
        BigDecimal months = RfmCalculator.months(since, today);
        List<CrmAnalysis.RfmSource> src = analysisMapper.selectRfmSource(since);
        List<RfmCalculator.Metrics> metrics = new ArrayList<>(src.size());
        for (CrmAnalysis.RfmSource r : src) {
            LocalDate last = LocalDate.parse(r.getLastDate().substring(0, 10));
            metrics.add(new RfmCalculator.Metrics(Math.max(0, ChronoUnit.DAYS.between(last, today)),
                    num(r.getFrequency()).longValue(), num(r.getMonetary())));
        }
        RfmCalculator.Result result = RfmCalculator.calculate(metrics, settings, months);
        List<RfmRow> rows = new ArrayList<>();
        Map<RfmSegment, long[]> counts = new EnumMap<>(RfmSegment.class);
        Map<RfmSegment, BigDecimal> amounts = new EnumMap<>(RfmSegment.class);
        for (int i = 0; i < src.size(); i++) {
            CrmAnalysis.RfmSource r = src.get(i);
            RfmCalculator.Metrics x = metrics.get(i);
            RfmCalculator.Score s = result.scores().get(i);
            counts.computeIfAbsent(s.segment(), k -> new long[1])[0]++;
            amounts.merge(s.segment(), x.monetary(), BigDecimal::add);
            rows.add(new RfmRow(num(r.getCustomerId()).longValue(), r.getCode(), r.getName(), r.getOwnerName(),
                    (int) x.recency(), (int) x.frequency(), x.monetary(), s.monthlyFrequency(), s.avgAmount(),
                    s.r(), s.f(), s.m(), s.recent(), s.frequent(), s.valuable(), s.segment()));
        }
        rows.sort(Comparator.comparing(RfmRow::monetary).reversed());
        List<CrmAnalysis.SegmentStat> segments = new ArrayList<>();
        for (RfmSegment seg : RfmSegment.values()) {
            segments.add(new CrmAnalysis.SegmentStat(seg, counts.getOrDefault(seg, new long[1])[0],
                    amounts.getOrDefault(seg, BigDecimal.ZERO)));
        }
        return new CrmAnalysis.Rfm(rows, segments, months.setScale(1, RoundingMode.HALF_UP), settings,
                result.avgR(), result.avgF(), result.avgM());
    }

    /** 近 12 个月的销售额（不含税净销售额，与 RFM 同一口径）、成交客户数、赢单金额 */
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

    /** 行业分布：近一年（与 RFM 相同的期间）的不含税净销售额 */
    public List<CrmAnalysis.IndustryStat> industries() {
        return analysisMapper.selectIndustryStats(LocalDate.now().minusYears(1).plusDays(1));
    }

    public List<CrmAnalysis.TicketStat> tickets() {
        return analysisMapper.selectTicketStats();
    }

    // ---------------------------------------------------------------- 内部

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
