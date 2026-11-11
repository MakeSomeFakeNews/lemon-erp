package com.lemon.erp.modules.wms.ai;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.ai.AiCleanseChange;
import com.lemon.erp.framework.ai.AiCleanseField;
import com.lemon.erp.framework.ai.AiCleanseRecord;
import com.lemon.erp.framework.ai.AiCleanseSource;
import com.lemon.erp.framework.ai.AiCleanseSupport;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialCategoryService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 数据整理：物料（名称、规格、分类、计量单位）。应用建议调用 {@link WmsMaterialService#update}（编码、条码等校验与变更历史），
 * 另外按实体注解校验长度、按物料分类校验分类（可选用的分类，候选为编码 → 完整路径），不允许改成与其他启用物料同名同规格；疑似重复的物料可停用
 */
@Component
@RequiredArgsConstructor
public class WmsMaterialCleanseSource implements AiCleanseSource {

    private final WmsMaterialService materialService;
    private final WmsMaterialCategoryService categoryService;
    private final Validator validator;

    @Override
    public String key() {
        return "wms.material";
    }

    @Override
    public String labelKey() {
        return "wms.label.material";
    }

    @Override
    public String viewPermission() {
        return "wms:material:list";
    }

    @Override
    public String applyPermission() {
        return "wms:material:edit";
    }

    @Override
    public Set<AiCleanseField> editableFields() {
        return EnumSet.of(AiCleanseField.NAME, AiCleanseField.SPEC, AiCleanseField.CATEGORY, AiCleanseField.UNIT);
    }

    @Override
    public PageResult<AiCleanseRecord> page(long current, long size, String keyword) {
        PageResult<WmsMaterial> page = materialService.pageForCleanse(current, size, keyword);
        return new PageResult<>(page.records().stream().map(WmsMaterialCleanseSource::toRecord).toList(), page.total());
    }

    @Override
    public AiCleanseRecord get(Long id) {
        return toRecord(materialService.get(id));
    }

    @Override
    public Map<String, String> categoryOptions() {
        return categoryService.options();
    }

    @Override
    public void apply(AiCleanseChange change) {
        WmsMaterial m = materialService.get(change.id());
        change.changes().forEach((field, value) -> {
            String v = AiCleanseSupport.trimToNull(value);
            switch (field) {
                case NAME -> m.setName(v);
                case SPEC -> m.setSpec(v);
                case CATEGORY -> {
                    categoryService.checkUsable(v, null);
                    m.setCategory(v);
                }
                case UNIT -> m.setUnit(v);
                default -> throw new BizException("wms.error.cleanseFieldNotSupported", field.name());
            }
        });
        AiCleanseSupport.validate(validator, m, Default.class, Update.class);
        if (materialService.existsSameNameSpec(m.getName(), m.getSpec(), m.getId())) {
            throw new BizException("wms.error.cleanseSameNameSpec", m.getName(), m.getSpec() == null ? "" : m.getSpec());
        }
        materialService.update(m.getId(), m);
    }

    @Override
    public boolean supportsDisable() {
        return true;
    }

    @Override
    public void disable(Long id) {
        materialService.changeStatus(id, EnableStatus.DISABLED);
    }

    private static AiCleanseRecord toRecord(WmsMaterial m) {
        return new AiCleanseRecord(m.getId(), m.getCode(), m.getName(), m.getSpec(), m.getCategory(), m.getUnit(),
                AiCleanseSupport.extra("barcode", m.getBarcode()));
    }
}
