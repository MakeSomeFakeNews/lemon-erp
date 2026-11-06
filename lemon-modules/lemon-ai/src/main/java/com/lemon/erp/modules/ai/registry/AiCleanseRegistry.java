package com.lemon.erp.modules.ai.registry;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.ai.AiCleanseChange;
import com.lemon.erp.framework.ai.AiCleanseSource;
import com.lemon.erp.framework.security.PermissionService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据整理的主数据来源（{@link AiCleanseSource}）：{@link #available()} / {@link #require} 按查看权限过滤，
 * 应用建议用 {@link #apply}，另外检查应用权限和可修改的字段
 */
@Component
public class AiCleanseRegistry extends AiRegistry<AiCleanseSource> {

    public AiCleanseRegistry(List<AiCleanseSource> beans, PermissionService permissionService) {
        super(beans, AiCleanseSource::key, AiCleanseSource.KEY_PATTERN, AiCleanseSource::viewPermission, permissionService);
    }

    /** 当前用户能否应用该来源的建议 */
    public boolean canApply(AiCleanseSource source) {
        return permitted(source) && hasPerm(source.applyPermission());
    }

    /**
     * 应用一条用户确认过的建议
     *
     * @throws BizException 没有应用权限，或包含不允许修改的字段
     */
    public void apply(String key, AiCleanseChange change) {
        AiCleanseSource source = require(key);
        if (!canApply(source)) {
            throw new BizException(R.FORBIDDEN, "ai.error.capabilityForbidden");
        }
        if (change.changes().isEmpty() || !source.editableFields().containsAll(change.changes().keySet())) {
            throw new BizException("ai.error.cleanseFieldNotEditable");
        }
        source.apply(change);
    }

    /**
     * 停用一条记录（疑似重复的建议）
     *
     * @throws BizException 没有应用权限，或该来源不支持停用
     */
    public void disable(String key, Long id) {
        AiCleanseSource source = require(key);
        if (!canApply(source)) {
            throw new BizException(R.FORBIDDEN, "ai.error.capabilityForbidden");
        }
        if (!source.supportsDisable()) {
            throw new BizException("ai.error.cleanseDisableNotSupported");
        }
        source.disable(id);
    }
}
