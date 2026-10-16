package com.lemon.erp.framework.security;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.security.datascope.DataScope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser user) {
            return user;
        }
        throw new BizException(R.UNAUTHORIZED, "未登录");
    }

    public static Long getUserId() {
        return getLoginUser().userId();
    }

    /** 当前登录用户 ID，未登录（如异步任务中）返回 null */
    public static Long getUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof LoginUser user ? user.userId() : null;
    }

    public static DataScope getDataScope() {
        return getLoginUser().dataScope();
    }

    /** 当前登录用户名，未登录返回 null（不抛异常，供日志等场景使用） */
    public static String getUsernameOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof LoginUser user ? user.username() : null;
    }

    /** 供应商门户接口：当前用户关联的供应商，不是供应商账号时拒绝访问 */
    public static Long requireSupplierId() {
        Long supplierId = getLoginUser().supplierId();
        if (supplierId == null) {
            throw new BizException(R.FORBIDDEN, "供应商门户仅供供应商账号使用");
        }
        return supplierId;
    }

    /** 经销商门户接口：当前用户关联的客户，不是经销商账号时拒绝访问 */
    public static Long requireCustomerId() {
        Long customerId = getLoginUser().customerId();
        if (customerId == null) {
            throw new BizException(R.FORBIDDEN, "经销商门户仅供经销商账号使用");
        }
        return customerId;
    }
}
