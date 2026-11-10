package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告的接收人与阅读记录：发布时按范围写入（再次发布时增减），阅读时记录时间
 */
@Data
@TableName("sys_notice_recipient")
public class SysNoticeRecipient {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noticeId;

    private Long userId;

    /** 阅读时间，未读为空 */
    private LocalDateTime readTime;
}
