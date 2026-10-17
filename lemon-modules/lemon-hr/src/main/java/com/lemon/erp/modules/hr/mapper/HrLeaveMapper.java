package com.lemon.erp.modules.hr.mapper;

import com.lemon.erp.modules.hr.model.LeaveKind;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.hr.dto.LeaveQuery;
import com.lemon.erp.modules.hr.entity.HrLeave;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface HrLeaveMapper extends BaseMapper<HrLeave> {

    IPage<HrLeave> selectPageList(IPage<HrLeave> page, @Param("q") LeaveQuery query);

    /** 已通过且与 [from, to) 有交集的请假 / 加班 */
    List<HrLeave> selectApprovedOverlapping(@Param("employeeIds") Collection<Long> employeeIds,
                                            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 时间段与 [start, end) 重叠的审批中 / 已通过的同种申请数 */
    long countOverlapping(@Param("employeeId") Long employeeId, @Param("kind") LeaveKind kind, @Param("excludeId") Long excludeId,
                          @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 开始时间在 [from, to) 内的审批中 / 已通过的某类请假小时数 */
    BigDecimal sumHours(@Param("employeeId") Long employeeId, @Param("type") String type,
                        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 调休余额：已批准的转调休加班 - 审批中、已通过的调休 */
    BigDecimal compensatoryBalance(@Param("employeeId") Long employeeId);
}
