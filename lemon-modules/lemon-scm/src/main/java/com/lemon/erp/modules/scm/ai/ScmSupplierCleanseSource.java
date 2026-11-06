package com.lemon.erp.modules.scm.ai;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.ai.AiCleanseChange;
import com.lemon.erp.framework.ai.AiCleanseField;
import com.lemon.erp.framework.ai.AiCleanseRecord;
import com.lemon.erp.framework.ai.AiCleanseSource;
import com.lemon.erp.framework.ai.AiCleanseSupport;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * 数据整理：供应商（名称、联系人、地址）。候选记录不含电话、邮箱、开户行和银行账号；税号只用于查重。
 * 应用建议调用 {@link ScmSupplierService#update}（编码校验与变更历史），另外按实体注解校验长度，不允许改成其他启用供应商的名称；
 * 疑似重复的供应商可停用
 */
@Component
@RequiredArgsConstructor
public class ScmSupplierCleanseSource implements AiCleanseSource {

    private final ScmSupplierService supplierService;
    private final Validator validator;

    @Override
    public String key() {
        return "scm.supplier";
    }

    @Override
    public String labelKey() {
        return "scm.label.supplier";
    }

    @Override
    public String viewPermission() {
        return "scm:supplier:list";
    }

    @Override
    public String applyPermission() {
        return "scm:supplier:edit";
    }

    @Override
    public Set<AiCleanseField> editableFields() {
        return EnumSet.of(AiCleanseField.NAME, AiCleanseField.CONTACT, AiCleanseField.ADDRESS);
    }

    @Override
    public PageResult<AiCleanseRecord> page(long current, long size, String keyword) {
        PageResult<ScmSupplier> page = supplierService.pageForCleanse(current, size, keyword);
        return new PageResult<>(page.records().stream().map(ScmSupplierCleanseSource::toRecord).toList(), page.total());
    }

    @Override
    public AiCleanseRecord get(Long id) {
        return toRecord(supplierService.get(id));
    }

    @Override
    public void apply(AiCleanseChange change) {
        ScmSupplier s = supplierService.get(change.id());
        change.changes().forEach((field, value) -> {
            String v = AiCleanseSupport.trimToNull(value);
            switch (field) {
                case NAME -> s.setName(v);
                case CONTACT -> s.setContact(v);
                case ADDRESS -> s.setAddress(v);
                default -> throw new BizException("scm.error.cleanseFieldNotSupported", field.name());
            }
        });
        AiCleanseSupport.validate(validator, s, Default.class, Update.class);
        if (change.changes().containsKey(AiCleanseField.NAME) && supplierService.existsEnabledName(s.getName(), s.getId())) {
            throw new BizException("scm.error.cleanseSupplierNameExists", s.getName());
        }
        supplierService.update(s.getId(), s);
    }

    @Override
    public boolean supportsDisable() {
        return true;
    }

    @Override
    public void disable(Long id) {
        supplierService.changeStatus(id, EnableStatus.DISABLED);
    }

    private static AiCleanseRecord toRecord(ScmSupplier s) {
        return new AiCleanseRecord(s.getId(), s.getCode(), s.getName(), null, null, null,
                AiCleanseSupport.extra(AiCleanseField.CONTACT.extraKey(), s.getContact(), AiCleanseField.ADDRESS.extraKey(), s.getAddress(),
                        AiCleanseRecord.TAX_NO, s.getTaxNo()));
    }
}
