package com.lemon.erp.modules.system.dto;

/**
 * 公告的阅读进度：应读人数（接收人）、已读人数
 */
public record NoticeReadStat(Long noticeId, Long totalCount, Long readCount) {
}
