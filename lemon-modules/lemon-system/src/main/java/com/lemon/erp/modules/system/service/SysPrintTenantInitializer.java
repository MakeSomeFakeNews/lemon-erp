package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.system.entity.SysPrintTemplate;
import com.lemon.erp.modules.system.mapper.SysPrintTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 新租户：复制平台预置的打印格式示例（销售出库单的「发货单（不含价格）」）及其所在单据的标准格式。
 * 只复制名称、配置、排序、状态，不复制平台上传的 JRXML、套打底图；平台已删除示例时不复制。
 * 平台自己修改过的其他模板不复制（新租户按默认版式）
 */
@Component
@RequiredArgsConstructor
public class SysPrintTenantInitializer implements TenantInitializer {

    /** 预置了多份格式的单据 */
    static final String PRESET_KEY = "scm.salesOut";

    private final SysPrintTemplateMapper templateMapper;

    @Override
    public void initialize(Context context) {
        String presetName = Messages.get(Messages.DEFAULT_LOCALE, "system.text.printFormatDeliveryNoPrice");
        List<SysPrintTemplate> platform = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> templateMapper.selectList(Wrappers.<SysPrintTemplate>lambdaQuery()
                        .eq(SysPrintTemplate::getTemplateKey, PRESET_KEY)));
        SysPrintTemplate preset = platform.stream().filter(t -> presetName.equals(t.getName())).findFirst().orElse(null);
        if (preset == null) {
            return;
        }
        SysPrintTemplate standard = new SysPrintTemplate();
        standard.setTemplateKey(PRESET_KEY);
        standard.setName(Messages.get(Messages.DEFAULT_LOCALE, "system.text.printFormatStandard"));
        standard.setIsDefault(true);
        standard.setSort(0);
        standard.setStatus(EnableStatus.ENABLED);
        standard.setConfig("{}");
        templateMapper.insert(standard);
        // 示例格式不作为默认：复制时清空 ID，JRXML、底图未读取（列表查询不返回）
        SysPrintTemplate copy = TenantInitializer.asNew(preset);
        copy.setIsDefault(false);
        templateMapper.insert(copy);
    }
}
