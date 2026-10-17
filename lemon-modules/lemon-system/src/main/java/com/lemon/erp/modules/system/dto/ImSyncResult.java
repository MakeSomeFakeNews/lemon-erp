package com.lemon.erp.modules.system.dto;

import java.util.List;

/**
 * 通讯录同步结果
 *
 * @param alreadyBound 之前已绑定的成员
 * @param matched      按手机号绑定到已有账号
 * @param created      新建账号并绑定
 * @param unmatched    未绑定的成员姓名（最多 50 个）
 * @param warning      未全部完成的原因（如账号数达到套餐上限）
 */
public record ImSyncResult(int deptTotal, int deptCreated, int memberTotal, int alreadyBound, int matched, int created,
                           int unmatchedCount, List<String> unmatched, String warning) {

    /** 同步结果摘要，存入集成配置（保持中文） */
    public String summary() {
        String s = "成员 " + memberTotal + " 人：之前已绑定 " + alreadyBound + "，按手机号绑定 " + matched + "，新建账号 " + created // i18n-ignore
                + "，未绑定 " + unmatchedCount + (deptCreated > 0 ? "；新建部门 " + deptCreated : ""); // i18n-ignore
        return warning == null ? s : s + "。" + warning; // i18n-ignore
    }
}
