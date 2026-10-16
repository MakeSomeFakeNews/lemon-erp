package com.lemon.erp.modules.mes.mapper;

import com.lemon.erp.modules.mes.dto.QmsViews;
import com.lemon.erp.modules.mes.entity.QmsInspectionItem;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 质量分析统计
 */
public interface QmsAnalysisMapper {

    /** 已确认检验单按月份、类型统计批次合格情况（让步接收计为合格批） */
    List<QmsViews.TrendRow> selectTrend(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 不良类型柏拉图：已确认的不合格检验单的不良数量 */
    List<QmsViews.ParetoRow> selectPareto(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 有实测值的定量检验项目（已确认的检验单） */
    List<QmsViews.SpcItem> selectSpcItems();

    /** 方案项目在期间内已确认检验单的实测值（按检验日期） */
    List<SpcRow> selectSpcRows(@Param("planItemId") Long planItemId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** SPC 原始数据 */
    record SpcRow(Long inspectionId, String inspNo, LocalDate inspectDate, String measured) {
    }

    /** 方案项目（取规格限） */
    QmsInspectionItem selectPlanItemSpec(@Param("planItemId") Long planItemId);
}
