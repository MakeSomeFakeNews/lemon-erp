package com.lemon.erp.modules.system.dto;

import com.lemon.erp.modules.system.model.NoticeScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改通知公告（草稿或已撤回的公告）
 *
 * @param targetIds 发布范围为指定部门 / 指定角色时的部门或角色 ID
 * @param expireDate 有效期至（含当天），为空长期有效
 */
public record NoticeRequest(
        @NotBlank(message = "{system.valid.noticeTitleRequired}")
        @Size(max = 128, message = "{system.valid.noticeTitleTooLong}") String title,
        @NotBlank(message = "{system.valid.noticeTypeRequired}") String type,
        @Size(max = 50000, message = "{system.valid.noticeContentTooLong}") String content,
        @NotNull(message = "{system.valid.noticeScopeRequired}") NoticeScope scopeType,
        @Size(max = 500, message = "{system.valid.noticeTargetsTooMany}") List<Long> targetIds,
        Boolean pinned,
        LocalDate expireDate) {
}
