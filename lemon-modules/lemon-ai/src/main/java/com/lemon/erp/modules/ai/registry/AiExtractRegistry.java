package com.lemon.erp.modules.ai.registry;

import com.lemon.erp.framework.ai.AiExtractTarget;
import com.lemon.erp.framework.security.PermissionService;
import org.springframework.stereotype.Component;

import java.util.List;

/** 智能录入的目标单据（{@link AiExtractTarget}），按标识查找、按 {@link AiExtractTarget#permission()} 过滤 */
@Component
public class AiExtractRegistry extends AiRegistry<AiExtractTarget> {

    public AiExtractRegistry(List<AiExtractTarget> beans, PermissionService permissionService) {
        super(beans, AiExtractTarget::key, AiExtractTarget.KEY_PATTERN, AiExtractTarget::permission, permissionService);
    }
}
