package com.lemon.erp.modules.system.dto;

import com.lemon.erp.modules.system.entity.SysUser;

import java.util.List;

public record ProfileInfo(SysUser user, String deptName, List<String> roleNames) {
}
