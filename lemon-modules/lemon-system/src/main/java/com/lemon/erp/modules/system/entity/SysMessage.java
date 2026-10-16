package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 站内消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class SysMessage extends BaseEntity {

    private Long userId;

    /** 字典 sys_message_category */
    private String category;

    private String title;

    private String content;

    /** 前端路由，点击跳转 */
    private String link;

    private String dedupKey;

    /** 1 已读 0 未读 */
    private Integer readFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;
}
