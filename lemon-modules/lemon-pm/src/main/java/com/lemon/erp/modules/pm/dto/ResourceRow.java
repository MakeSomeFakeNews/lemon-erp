package com.lemon.erp.modules.pm.dto;

import com.lemon.erp.modules.pm.entity.PmMember;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资源负荷：成员在进行中、规划中项目的投入比例合计（超过 100% 为超负荷），以及未完成任务的计划工时
 */
public record ResourceRow(Long userId, String userName, String deptName, int allocation, BigDecimal openTaskHours,
                          List<PmMember> projects) {
}
