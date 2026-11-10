package com.lemon.erp.modules.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告的接收人与阅读情况（已读 / 未读名单）
 */
@Data
public class NoticeReader {

    private Long userId;

    private String username;

    private String nickname;

    private String deptName;

    /** 阅读时间，未读为空 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;
}
