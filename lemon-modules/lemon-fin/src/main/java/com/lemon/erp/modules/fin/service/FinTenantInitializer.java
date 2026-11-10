package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinCurrency;
import com.lemon.erp.modules.fin.entity.FinIoCategory;
import com.lemon.erp.modules.fin.mapper.FinAccountMapper;
import com.lemon.erp.modules.fin.mapper.FinCurrencyMapper;
import com.lemon.erp.modules.fin.mapper.FinIoCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新租户复制平台的币种、会计科目表（科目按编码层级复制，上级科目 ID 换成新租户中的）和收支类别（科目换成新租户中的）
 */
@Component
@RequiredArgsConstructor
public class FinTenantInitializer implements TenantInitializer {

    private final FinCurrencyMapper currencyMapper;
    private final FinAccountMapper accountMapper;
    private final FinIoCategoryMapper ioCategoryMapper;

    @Override
    public void initialize(Context context) {
        List<FinCurrency> currencies = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> currencyMapper.selectList(Wrappers.emptyWrapper()));
        currencies.forEach(c -> currencyMapper.insert(TenantInitializer.asNew(c)));

        List<FinAccount> accounts = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> accountMapper.selectList(Wrappers.emptyWrapper()));
        // 上级科目的编码更短，先插入
        accounts.sort(Comparator.comparing((FinAccount a) -> a.getCode().length()).thenComparing(FinAccount::getCode));
        Map<Long, Long> idMap = new HashMap<>();
        for (FinAccount a : accounts) {
            Long platformId = a.getId();
            TenantInitializer.asNew(a);
            a.setParentId(a.getParentId() == null || a.getParentId() == 0 ? 0L : idMap.getOrDefault(a.getParentId(), 0L));
            accountMapper.insert(a);
            idMap.put(platformId, a.getId());
        }

        List<FinIoCategory> categories = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> ioCategoryMapper.selectList(Wrappers.emptyWrapper()));
        for (FinIoCategory c : categories) {
            Long accountId = c.getAccountId();
            TenantInitializer.asNew(c);
            c.setAccountId(accountId == null ? null : idMap.get(accountId));
            ioCategoryMapper.insert(c);
        }
    }
}
