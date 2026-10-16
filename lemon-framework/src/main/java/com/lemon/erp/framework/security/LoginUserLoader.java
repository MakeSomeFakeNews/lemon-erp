package com.lemon.erp.framework.security;

/**
 * 根据用户 ID 加载登录用户（角色 + 权限），由业务模块实现，框架层不依赖具体业务
 */
public interface LoginUserLoader {

    /**
     * @return 用户不存在或已停用时返回 null
     */
    LoginUser loadLoginUser(Long userId);
}
