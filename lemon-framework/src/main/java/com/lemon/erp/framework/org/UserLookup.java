package com.lemon.erp.framework.org;

import com.lemon.erp.common.exception.BizException;

import java.util.Optional;

/**
 * 按 ID 查询登录账号的简要信息，由系统模块实现（{@code SysUserLookup}）。
 * 业务模块（客户关系、项目等）校验负责人、成员时使用，不直接访问 sys_user 表
 */
public interface UserLookup {

    /**
     * @param userId 账号 ID，为空时返回空
     * @return 当前租户下的账号，不存在时为空
     */
    Optional<UserBrief> find(Long userId);

    /**
     * 账号必须存在（不检查是否停用）
     *
     * @param errorKey 不存在时抛出的 {@link BizException} 消息键
     */
    default UserBrief require(Long userId, String errorKey) {
        return find(userId).orElseThrow(() -> new BizException(errorKey));
    }

    /**
     * 账号必须存在且为正常状态
     *
     * @param errorKey 不存在或已停用时抛出的 {@link BizException} 消息键
     */
    default UserBrief requireActive(Long userId, String errorKey) {
        return find(userId).filter(UserBrief::active).orElseThrow(() -> new BizException(errorKey));
    }

    /**
     * @return 账号昵称，账号不存在时返回 null
     */
    default String nameOf(Long userId) {
        return find(userId).map(UserBrief::nickname).orElse(null);
    }

    /**
     * @param active 账号状态为正常（未停用）
     */
    record UserBrief(Long id, String username, String nickname, Long deptId, boolean active) {
    }
}
