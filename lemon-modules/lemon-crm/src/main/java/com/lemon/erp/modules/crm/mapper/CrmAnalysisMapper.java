package com.lemon.erp.modules.crm.mapper;

import com.lemon.erp.modules.crm.dto.CrmAnalysis;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户分析：只读汇总客户、线索、商机、工单，以及供应链的销售出库数据
 */
public interface CrmAnalysisMapper {

    /** 关键指标（本月新增客户 / 线索 / 转化、在途商机金额与加权金额、本月赢单、赢单率、工单） */
    CrmAnalysis.Overview selectOverview(@Param("monthStart") LocalDateTime monthStart, @Param("now") LocalDateTime now);

    /** 客户阶段分布（含公海中还没有经营信息的客户，计为潜在客户） */
    List<CrmAnalysis.StageCount> selectCustomerStages();

    /** 商机按阶段：数量、金额 */
    List<CrmAnalysis.StageCount> selectFunnel();

    /** 到达过各阶段的商机数（阶段日志），用于阶段转化率 */
    List<CrmAnalysis.StageCount> selectStageReached();

    /** 客户近一年销售：最近出库日期、出库次数、净销售额（扣除退货） */
    List<CrmAnalysis.RfmSource> selectRfmSource(@Param("since") LocalDate since);

    /** 按月销售额（净额）与赢单金额 */
    List<CrmAnalysis.MonthlyRow> selectMonthlySales(@Param("since") LocalDate since);

    List<CrmAnalysis.MonthlyRow> selectMonthlyWon(@Param("since") LocalDate since);

    /** 线索来源：线索数、已转化数；商机来源：赢单金额 */
    List<CrmAnalysis.SourceStat> selectSourceStats();

    /** 销售人员：负责客户数、在途商机、本年赢单、本月跟进次数 */
    List<CrmAnalysis.OwnerStat> selectOwnerStats(@Param("yearStart") LocalDate yearStart, @Param("monthStart") LocalDateTime monthStart);

    /** 行业分布：客户数、近一年销售额 */
    List<CrmAnalysis.IndustryStat> selectIndustryStats(@Param("since") LocalDate since);

    /** 工单：按类型统计数量、平均解决时长（分钟）、按时解决数、平均满意度 */
    List<CrmAnalysis.TicketStat> selectTicketStats();
}
