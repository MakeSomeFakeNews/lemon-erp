package com.lemon.erp.modules.hr.dto;

import java.util.List;

/**
 * 发起考核：deptIds 为空时考核全部在职员工，否则考核这些部门（含下级部门）的员工
 */
public record PerfStartRequest(List<Long> deptIds) {
}
