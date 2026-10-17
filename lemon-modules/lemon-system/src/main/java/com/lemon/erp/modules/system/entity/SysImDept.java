package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.system.im.ImPlatform;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 同步通讯录时系统部门与平台部门的对应关系
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_im_dept")
public class SysImDept extends BaseEntity {

    private Long deptId;

    private ImPlatform platform;

    private String imDeptId;
}
