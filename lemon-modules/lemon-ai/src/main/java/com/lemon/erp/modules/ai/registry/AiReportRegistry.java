package com.lemon.erp.modules.ai.registry;

import com.lemon.erp.framework.ai.AiReportSource;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 智能报告的数据来源（{@link AiReportSource}），按 {@link AiReportSource#permission()} 过滤；
 * {@link AiReportSource#requiresAllData()} 的来源还要求当前用户有「全部数据」权限
 */
@Component
public class AiReportRegistry extends AiRegistry<AiReportSource> {

    public AiReportRegistry(List<AiReportSource> beans, PermissionService permissionService) {
        super(beans, AiReportSource::key, AiReportSource.KEY_PATTERN, AiReportSource::permission, permissionService);
    }

    /** 当前用户有权限的来源，按 {@link AiReportSource#order()} 排序（报告中板块的顺序） */
    @Override
    public List<AiReportSource> available() {
        return super.available().stream().sorted(Comparator.comparingInt(AiReportSource::order)).toList();
    }

    @Override
    protected boolean permitted(AiReportSource source) {
        return super.permitted(source) && (!source.requiresAllData() || SecurityUtils.getDataScope().all());
    }
}
