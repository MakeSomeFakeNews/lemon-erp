package com.lemon.erp.modules.hr.service;

import com.lemon.erp.framework.ai.AiReportMetrics;
import com.lemon.erp.modules.hr.mapper.HrWorkforceMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 人员变动汇总（智能报告的人事来源使用）：期初、期末在职人数，入职、离职人数和离职率。只有人数，不含薪资等个人信息；
 * 汇总全公司员工，不按数据权限过滤
 */
@Service
@RequiredArgsConstructor
public class HrWorkforceSummaryService {

    private final HrWorkforceMapper workforceMapper;

    /** 期间（含首尾两天）的人员变动 */
    public PeriodSummary summary(LocalDate begin, LocalDate end) {
        Counts c = workforceMapper.selectCounts(begin, end);
        if (c == null) {
            c = new Counts();
        }
        return new PeriodSummary(c.getHeadcountStart(), c.getHeadcountEnd(), c.getHires(), c.getLeaves(),
                c.getHeadcountEnd() - c.getHeadcountStart(), turnover(c));
    }

    /** 离职率（%）= 离职人数 / 平均在职人数（期初、期末的平均） */
    static BigDecimal turnover(Counts c) {
        return AiReportMetrics.percent(BigDecimal.valueOf(c.getLeaves()),
                BigDecimal.valueOf(c.getHeadcountStart() + c.getHeadcountEnd()).divide(BigDecimal.valueOf(2)));
    }

    /**
     * 期间的人员变动
     *
     * @param headcountStart  期初在职人数
     * @param headcountEnd    期末在职人数
     * @param hires           入职人数
     * @param leaves          离职人数
     * @param netChange       净增人数
     * @param turnoverRatePct 离职率（%）
     */
    public record PeriodSummary(long headcountStart, long headcountEnd, long hires, long leaves, long netChange,
                                BigDecimal turnoverRatePct) {
    }

    /** 数据库查询结果 */
    @Data
    public static class Counts {
        private long headcountStart;
        private long headcountEnd;
        private long hires;
        private long leaves;
    }
}
