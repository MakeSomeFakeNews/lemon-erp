package com.lemon.erp.framework.portal;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门户账号：供应链在供应商 / 客户资料中管理，由系统模块实现（{@code SysPortalAccountService}）。
 * 门户账号是关联了供应商（供应商门户）或客户（经销商门户）的用户，只分配对应的内置门户角色，登录后只能使用该门户
 *
 * @see PortalKind
 */
public interface PortalAccounts {

    /** @param partnerId 供应商 ID（供应商门户）/ 客户 ID（经销商门户） */
    List<Account> list(PortalKind kind, Long partnerId);

    /** @return 用户 ID */
    Long create(PortalKind kind, Long partnerId, NewAccount account);

    void changeStatus(PortalKind kind, Long partnerId, Long userId, Integer status);

    void resetPassword(PortalKind kind, Long partnerId, Long userId, String password);

    /** 往来单位的启用账号（发送门户消息） */
    List<Long> enabledUserIds(PortalKind kind, Long partnerId);

    record Account(Long userId, String username, String nickname, String phone, Integer status, LocalDateTime createTime) {
    }

    record NewAccount(String username, String nickname, String phone, String password) {
    }
}
