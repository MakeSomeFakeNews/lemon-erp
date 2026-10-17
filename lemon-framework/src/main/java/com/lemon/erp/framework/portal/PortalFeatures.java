package com.lemon.erp.framework.portal;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.security.LoginUser;

/**
 * 外部门户的访问控制：门户账号只能访问所属门户的接口，内部用户看不到门户菜单（见 {@link PortalKind}）
 */
public final class PortalFeatures {

    private PortalFeatures() {
    }

    /** 门户账号访问不属于其门户的接口时返回提示，允许时返回 null */
    public static String deniedMessage(LoginUser user, String path) {
        PortalKind kind = user.portalKind();
        if (kind == null || kind.allows(path)) {
            return null;
        }
        return Messages.get("framework.text.accountsOnlyUsePortal", Messages.get(kind.getLabel()));
    }
}
