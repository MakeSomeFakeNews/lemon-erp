package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 通知公告的发布范围：指定部门（含下级部门）或指定角色时的部门 / 角色 ID
 */
@Data
@TableName("sys_notice_target")
public class SysNoticeTarget {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noticeId;

    private Long targetId;
}
