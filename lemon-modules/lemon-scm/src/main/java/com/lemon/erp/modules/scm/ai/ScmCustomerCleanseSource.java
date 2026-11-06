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
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * 数据整理：客户（名称、联系人、地址）。候选记录不含电话、信用额度；税号只用于查重。
 * 应用建议调用 {@link ScmCustomerService#update}（编码校验与变更历史），另外按实体注解校验长度，不允许改成其他启用客户的名称；
 * 疑似重复的客户可停用
 */
@Component
@RequiredArgsConstructor
public class ScmCustomerCleanseSource implements AiCleanseSource {

    private final ScmCustomerService customerService;
    private final Validator validator;

    @Override
    public String key() {
        return "scm.customer";
    }

    @Override
    public String labelKey() {
        return "scm.label.customer";
    }

    @Override
    public String viewPermission() {
        return "scm:customer:list";
    }

    @Override
    public String applyPermission() {
        return "scm:customer:edit";
    }

    @Override
    public Set<AiCleanseField> editableFields() {
        return EnumSet.of(AiCleanseField.NAME, AiCleanseField.CONTACT, AiCleanseField.ADDRESS);
    }

    @Override
    public PageResult<AiCleanseRecord> page(long current, long size, String keyword) {
        PageResult<ScmCustomer> page = customerService.pageForCleanse(current, size, keyword);
        return new PageResult<>(page.records().stream().map(ScmCustomerCleanseSource::toRecord).toList(), page.total());
    }

    @Override
    public AiCleanseRecord get(Long id) {
        return toRecord(customerService.get(id));
    }

    @Override
    public void apply(AiCleanseChange change) {
        ScmCustomer c = customerService.get(change.id());
        change.changes().forEach((field, value) -> {
            String v = AiCleanseSupport.trimToNull(value);
            switch (field) {
                case NAME -> c.setName(v);
                case CONTACT -> c.setContact(v);
                case ADDRESS -> c.setAddress(v);
                default -> throw new BizException("scm.error.cleanseFieldNotSupported", field.name());
            }
        });
        AiCleanseSupport.validate(validator, c, Default.class, Update.class);
        if (change.changes().containsKey(AiCleanseField.NAME) && customerService.existsEnabledName(c.getName(), c.getId())) {
            throw new BizException("scm.error.cleanseCustomerNameExists", c.getName());
        }
        customerService.update(c.getId(), c);
    }

    @Override
    public boolean supportsDisable() {
        return true;
    }

    @Override
    public void disable(Long id) {
        customerService.changeStatus(id, EnableStatus.DISABLED);
    }

    private static AiCleanseRecord toRecord(ScmCustomer c) {
        return new AiCleanseRecord(c.getId(), c.getCode(), c.getName(), null, null, null,
                AiCleanseSupport.extra(AiCleanseField.CONTACT.extraKey(), c.getContact(), AiCleanseField.ADDRESS.extraKey(), c.getAddress(),
                        AiCleanseRecord.TAX_NO, c.getTaxNo()));
    }
}
