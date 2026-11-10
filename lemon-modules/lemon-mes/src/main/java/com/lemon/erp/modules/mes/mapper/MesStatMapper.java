package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.dto.MesStatQuery;
import com.lemon.erp.modules.mes.dto.MesStatViews;
import com.lemon.erp.modules.mes.model.MesStatDimension;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产统计报表（只读，按工单做数据权限），见 MesStatService
 */
public interface MesStatMapper {

    /** 报工汇总（人员计件、工序 / 工作中心产量），最多 limit 行 */
    List<MesStatViews.ReportRow> selectReportSummary(@Param("dim") MesStatDimension dim, @Param("q") MesStatQuery q,
                                                     @Param("limit") int limit);

    /** 报工汇总的合计 */
    MesStatViews.ReportRow selectReportTotal(@Param("q") MesStatQuery q);

    /** 报工明细 */
    IPage<MesStatViews.ReportDetail> selectReportDetail(IPage<MesStatViews.ReportDetail> page, @Param("q") MesStatQuery q);

    /** 工单完成情况：按工单列出（计划完工日期在期间内） */
    List<MesStatViews.WoRow> selectWoList(@Param("q") MesStatQuery q, @Param("limit") int limit);

    /** 工单完成情况：按产品 / 车间 / 月份汇总；dim 为空时为合计 */
    List<MesStatViews.WoRow> selectWoSummary(@Param("dim") MesStatDimension dim, @Param("q") MesStatQuery q,
                                             @Param("today") LocalDate today, @Param("limit") int limit);

    /** 产品产量（完工入库、报废入库）汇总；dim 为空时为合计 */
    List<MesStatViews.OutputRow> selectOutputSummary(@Param("dim") MesStatDimension dim, @Param("q") MesStatQuery q,
                                                     @Param("limit") int limit);

    /** 用料差异：期间内结束（完工 / 关闭）的工单 */
    List<MesStatViews.UsageRow> selectUsage(@Param("dim") MesStatDimension dim, @Param("q") MesStatQuery q,
                                            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                            @Param("limit") int limit);
}
