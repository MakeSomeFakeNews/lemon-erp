package com.lemon.erp.modules.system.service;

import com.lemon.erp.framework.tenant.TenantInitializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 新租户：复制平台的业务字典（之后由租户在「业务字典」中自行维护）
 */
@Component
@RequiredArgsConstructor
public class SysDictTenantInitializer implements TenantInitializer {

    private final SysDictService dictService;

    @Override
    public void initialize(Context context) {
        dictService.copyBusinessData(context.tenantId());
    }
}
