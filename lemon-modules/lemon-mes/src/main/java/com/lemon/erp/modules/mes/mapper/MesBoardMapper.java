package com.lemon.erp.modules.mes.mapper;

import com.lemon.erp.modules.mes.dto.BoardHourRow;
import com.lemon.erp.modules.mes.dto.BoardOrderRow;
import com.lemon.erp.modules.mes.dto.BoardReportSum;
import com.lemon.erp.modules.mes.entity.MesWoOp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 生产看板：在制工单、工序和当天的报工
 */
@Mapper
public interface MesBoardMapper {

    /** 已下达、生产中的工单 */
    List<BoardOrderRow> selectOrders();

    /** 在制工单的工序（含工作中心名称） */
    List<MesWoOp> selectOps();

    /** 某天各工序的报工 */
    List<BoardReportSum> sumByOp(@Param("day") LocalDate day);

    /** 某天各工作中心的报工（含已完工的工单） */
    List<BoardReportSum> sumByWorkCenter(@Param("day") LocalDate day);

    /** 某天按小时的产出 */
    List<BoardHourRow> sumByHour(@Param("day") LocalDate day, @Param("wc") Long workCenterId);

    /** 某天完工的工单数 */
    long countFinished(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("wc") Long workCenterId);
}
