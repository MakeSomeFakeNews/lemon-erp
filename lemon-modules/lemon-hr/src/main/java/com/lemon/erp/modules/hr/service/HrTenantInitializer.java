package com.lemon.erp.modules.hr.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.hr.mapper.HrShiftMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 新租户复制平台的班次（考勤、请假折算工时依赖默认班次）
 */
@Component
@RequiredArgsConstructor
public class HrTenantInitializer implements TenantInitializer {

    private final HrShiftMapper shiftMapper;

    @Override
    public void initialize(Context context) {
        TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID, () -> shiftMapper.selectList(Wrappers.emptyWrapper()))
                .forEach(s -> shiftMapper.insert(TenantInitializer.asNew(s)));
    }
}
