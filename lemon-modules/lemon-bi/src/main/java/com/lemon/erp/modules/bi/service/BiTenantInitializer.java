package com.lemon.erp.modules.bi.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.bi.mapper.BiAlertRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 新租户复制平台的预警规则
 */
@Component
@RequiredArgsConstructor
public class BiTenantInitializer implements TenantInitializer {

    private final BiAlertRuleMapper alertRuleMapper;

    @Override
    public void initialize(Context context) {
        TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID, () -> alertRuleMapper.selectList(Wrappers.emptyWrapper()))
                .forEach(r -> alertRuleMapper.insert(TenantInitializer.asNew(r)));
    }
}
