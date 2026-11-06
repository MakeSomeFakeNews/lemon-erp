package com.lemon.erp.modules.ai.registry;

import com.lemon.erp.framework.ai.AiExtractTarget;
import com.lemon.erp.framework.security.PermissionService;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 智能录入的目标单据（{@link AiExtractTarget}），按标识查找、按 {@link AiExtractTarget#permission()} 过滤。
 * 列表按实现类上的 {@code @Order} 排序（页面中目标单据的显示顺序），没有 {@code @Order} 的排在最后
 */
@Component
public class AiExtractRegistry extends AiRegistry<AiExtractTarget> {

    public AiExtractRegistry(List<AiExtractTarget> beans, PermissionService permissionService) {
        super(beans, AiExtractTarget::key, AiExtractTarget.KEY_PATTERN, AiExtractTarget::permission, permissionService);
    }

    @Override
    public List<AiExtractTarget> all() {
        return sorted(super.all());
    }

    @Override
    public List<AiExtractTarget> available() {
        return sorted(super.available());
    }

    private static List<AiExtractTarget> sorted(List<AiExtractTarget> list) {
        return list.stream().sorted(AnnotationAwareOrderComparator.INSTANCE).toList();
    }
}
