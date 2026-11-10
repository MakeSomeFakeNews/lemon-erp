package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.system.model.NoticeScope;
import com.lemon.erp.modules.system.model.NoticeStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知公告：正文为 Markdown；发布时按范围确定接收人（{@link SysNoticeRecipient}），并给接收人发站内消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseEntity {

    private String title;

    /** 类型（业务字典 sys_notice_type） */
    private String type;

    /** 正文（Markdown），列表中不返回 */
    private String content;

    private NoticeScope scopeType;

    /** 置顶：工作台、公告列表排在前面 */
    private Boolean pinned;

    /** 有效期至（含当天），为空长期有效；过期后不再显示给员工 */
    private LocalDate expireDate;

    private NoticeStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    private Long publishBy;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 发布范围：部门或角色 ID */
    @TableField(exist = false)
    private List<Long> targetIds;

    /** 发布范围的名称（部门、角色），管理端显示 */
    @TableField(exist = false)
    private List<String> targetNames;

    @TableField(exist = false)
    private String publishByName;

    @TableField(exist = false)
    private String createByName;

    /** 应读人数（发布时确定的接收人） */
    @TableField(exist = false)
    private Long totalCount;

    /** 已读人数 */
    @TableField(exist = false)
    private Long readCount;

    /** 员工端：本人的阅读时间，未读为空 */
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    /** 员工端：是否已读 */
    @TableField(exist = false)
    private Boolean read;
}
