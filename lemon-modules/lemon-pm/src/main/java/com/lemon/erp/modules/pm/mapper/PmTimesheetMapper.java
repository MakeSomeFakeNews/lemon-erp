package com.lemon.erp.modules.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pm.dto.TimesheetSummaryRow;
import com.lemon.erp.modules.pm.entity.PmTimesheet;
import com.lemon.erp.modules.pm.model.TimesheetStatus;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PmTimesheetMapper extends BaseMapper<PmTimesheet> {

    /** @param excludeUserId 不含该账号的工时（审批列表排除审批人本人的），可为空 */
    IPage<PmTimesheet> selectPageList(IPage<PmTimesheet> page, @Param("projectId") Long projectId, @Param("userId") Long userId,
                                      @Param("status") TimesheetStatus status, @Param("beginDate") LocalDate beginDate,
                                      @Param("endDate") LocalDate endDate, @Param("excludeUserId") Long excludeUserId);

    /** 本人某段时间的工时 */
    List<PmTimesheet> selectMine(@Param("userId") Long userId, @Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate);

    /** 本人某天已填报的工时合计（不含驳回的、不含 excludeId） */
    BigDecimal sumDay(@Param("userId") Long userId, @Param("workDate") LocalDate workDate, @Param("excludeId") Long excludeId);

    /**
     * 工时汇总（不含驳回的）：工作日期在 [beginDate, endDate] 内，按项目或成员汇总小时数（调用方加数据权限）
     *
     * @param byUser true 按成员，false 按项目
     */
    List<TimesheetSummaryRow> selectSummary(@Param("byUser") boolean byUser, @Param("projectId") Long projectId,
                                            @Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate);
}
