package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.wms.dto.VariantMatrix;
import com.lemon.erp.modules.wms.entity.WmsAttribute;
import com.lemon.erp.modules.wms.entity.WmsAttributeValue;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsVariantValue;
import com.lemon.erp.modules.wms.mapper.WmsAttributeMapper;
import com.lemon.erp.modules.wms.mapper.WmsAttributeValueMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockFlowMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockMapper;
import com.lemon.erp.modules.wms.mapper.WmsVariantValueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 多规格物料：维护规格属性（颜色、尺码等）及属性值；把物料设为款式后，按所选属性值的组合批量生成 SKU（每个 SKU 是一个独立的物料，
 * 库存、成本、单据都按 SKU），SKU 继承款式的单位、分类、批次 / 序列号管理、计价方法等。款式本身不能出入库，订单可按款式的规格矩阵录入数量
 */
@Service
@RequiredArgsConstructor
public class WmsVariantService {

    /** 一个款式最多生成的 SKU 数 */
    private static final int MAX_VARIANTS = 500;

    private final WmsAttributeMapper attributeMapper;
    private final WmsAttributeValueMapper valueMapper;
    private final WmsVariantValueMapper variantValueMapper;
    private final WmsMaterialMapper materialMapper;
    private final WmsStockFlowMapper flowMapper;
    private final WmsStockMapper stockMapper;

    // ---------------------------------------------------------------- 规格属性

    /** 全部规格属性及属性值，按排序 */
    public List<WmsAttribute> attributes() {
        List<WmsAttribute> list = attributeMapper.selectList(Wrappers.<WmsAttribute>lambdaQuery()
                .orderByAsc(WmsAttribute::getSort).orderByAsc(WmsAttribute::getId));
        Map<Long, List<WmsAttributeValue>> values = valueMapper.selectList(Wrappers.<WmsAttributeValue>lambdaQuery()
                        .orderByAsc(WmsAttributeValue::getSort).orderByAsc(WmsAttributeValue::getId)).stream()
                .collect(Collectors.groupingBy(WmsAttributeValue::getAttributeId, LinkedHashMap::new, Collectors.toList()));
        list.forEach(a -> a.setValues(values.getOrDefault(a.getId(), List.of())));
        return list;
    }

    /**
     * 新增或修改规格属性（含属性值，整体保存）：已被 SKU 使用的属性值不能删除，可以改名
     *
     * @return 属性 ID
     */
    @Transactional
    public Long saveAttribute(WmsAttribute attr) {
        List<WmsAttributeValue> values = attr.getValues() == null ? List.of() : attr.getValues();
        Set<String> names = new HashSet<>();
        for (WmsAttributeValue v : values) {
            if (!StringUtils.hasText(v.getAttrValue()) || v.getAttrValue().trim().length() > 32) {
                throw new BizException("wms.error.attributeValueRequiredCannotExceedCharacters");
            }
            if (v.getCode() != null && v.getCode().trim().length() > 16) {
                throw new BizException("wms.error.attributeValueCodeCannotExceedCharacters");
            }
            if (!names.add(v.getAttrValue().trim())) {
                throw new BizException("wms.error.duplicateAttributeValue", v.getAttrValue());
            }
        }
        attr.setName(attr.getName().trim());
        if (attr.getStatus() == null) {
            attr.setStatus(EnableStatus.ENABLED);
        }
        if (attr.getSort() == null) {
            attr.setSort(0);
        }
        try {
            if (attr.getId() == null) {
                attributeMapper.insert(attr);
            } else {
                if (attributeMapper.selectById(attr.getId()) == null) {
                    throw new BizException("wms.error.variantAttributeDoesNotExist");
                }
                attributeMapper.updateById(attr);
            }
        } catch (DuplicateKeyException e) {
            throw new BizException("wms.error.variantAttributeAlreadyExists", attr.getName());
        }
        Map<Long, WmsAttributeValue> existing = valueMapper.selectList(Wrappers.<WmsAttributeValue>lambdaQuery()
                .eq(WmsAttributeValue::getAttributeId, attr.getId())).stream().collect(Collectors.toMap(WmsAttributeValue::getId, Function.identity()));
        Set<Long> kept = values.stream().map(WmsAttributeValue::getId).filter(existing::containsKey).collect(Collectors.toSet());
        List<Long> removed = existing.keySet().stream().filter(id -> !kept.contains(id)).toList();
        if (!removed.isEmpty()) {
            if (variantValueMapper.exists(Wrappers.<WmsVariantValue>lambdaQuery().in(WmsVariantValue::getValueId, removed))) {
                throw new BizException("wms.error.attributeValuesUsedSkusCannotDeleted");
            }
            valueMapper.deleteByIds(removed);
        }
        int sort = 1;
        for (WmsAttributeValue v : values) {
            WmsAttributeValue row = new WmsAttributeValue();
            row.setAttributeId(attr.getId());
            row.setAttrValue(v.getAttrValue().trim());
            row.setCode(StringUtils.hasText(v.getCode()) ? v.getCode().trim() : null);
            row.setSort(sort++);
            if (v.getId() != null && existing.containsKey(v.getId())) {
                row.setId(v.getId());
                valueMapper.updateById(row);
                if (row.getCode() == null) {
                    valueMapper.update(Wrappers.<WmsAttributeValue>lambdaUpdate().set(WmsAttributeValue::getCode, null).eq(WmsAttributeValue::getId, row.getId()));
                }
            } else {
                valueMapper.insert(row);
            }
        }
        return attr.getId();
    }

    @Transactional
    public void deleteAttribute(Long id) {
        if (variantValueMapper.exists(Wrappers.<WmsVariantValue>lambdaQuery().eq(WmsVariantValue::getAttributeId, id))) {
            throw new BizException("wms.error.variantAttributeUsedSkusCannotDeleted");
        }
        valueMapper.delete(Wrappers.<WmsAttributeValue>lambdaQuery().eq(WmsAttributeValue::getAttributeId, id));
        attributeMapper.deleteById(id);
    }

    // ---------------------------------------------------------------- 款式与 SKU

    /**
     * 款式的规格矩阵：用到的属性及属性值、已生成的 SKU（含各自的属性值）
     *
     * @return {template, attributes: [{id, name, values: [...]}], variants: [{id, code, name, spec, variantText, status, valueIds}]}
     */
    public VariantMatrix matrix(Long templateId) {
        WmsMaterial template = requireTemplate(templateId, false);
        List<WmsMaterial> variants = materialMapper.selectList(Wrappers.<WmsMaterial>lambdaQuery()
                .eq(WmsMaterial::getTemplateId, templateId).orderByAsc(WmsMaterial::getCode));
        List<WmsVariantValue> vvs = variantValueMapper.selectList(Wrappers.<WmsVariantValue>lambdaQuery().eq(WmsVariantValue::getTemplateId, templateId));
        Map<Long, Map<Long, Long>> valuesByMaterial = new HashMap<>();
        vvs.forEach(v -> valuesByMaterial.computeIfAbsent(v.getMaterialId(), k -> new HashMap<>()).put(v.getAttributeId(), v.getValueId()));
        Set<Long> usedAttrs = vvs.stream().map(WmsVariantValue::getAttributeId).collect(Collectors.toSet());
        Set<Long> usedValues = vvs.stream().map(WmsVariantValue::getValueId).collect(Collectors.toSet());
        List<VariantMatrix.Attribute> attrs = new ArrayList<>();
        for (WmsAttribute a : attributes()) {
            if (!usedAttrs.contains(a.getId())) {
                continue;
            }
            attrs.add(new VariantMatrix.Attribute(a.getId(), a.getName(),
                    a.getValues().stream().filter(v -> usedValues.contains(v.getId())).toList()));
        }
        List<VariantMatrix.Variant> rows = new ArrayList<>();
        for (WmsMaterial v : variants) {
            rows.add(new VariantMatrix.Variant(v.getId(), v.getCode(), v.getName(), v.getSpec(), v.getUnit(), v.getSerialEnabled(),
                    v.getVariantText(), v.getStatus(), valuesByMaterial.getOrDefault(v.getId(), Map.of())));
        }
        return new VariantMatrix(template, attrs, rows);
    }

    /**
     * 按所选属性值的组合生成 SKU（已有的组合跳过）。第一次生成时把物料设为款式：已发生库存业务的物料不能设为款式
     *
     * @param valueIds 属性 ID → 选中的属性值 ID
     * @return 新生成的 SKU 数
     */
    @Transactional
    public int generate(Long templateId, Map<Long, List<Long>> valueIds) {
        WmsMaterial template = requireTemplate(templateId, true);
        if (valueIds == null || valueIds.isEmpty() || valueIds.values().stream().anyMatch(l -> l == null || l.isEmpty())) {
            throw new BizException("wms.error.selectLeastOneValueEachAttribute");
        }
        Map<Long, WmsAttribute> attrs = attributeMapper.selectByIds(valueIds.keySet()).stream()
                .collect(Collectors.toMap(WmsAttribute::getId, Function.identity()));
        if (attrs.size() != valueIds.size()) {
            throw new BizException("wms.error.variantAttributeDoesNotExist");
        }
        Set<Long> allValueIds = valueIds.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, WmsAttributeValue> values = valueMapper.selectByIds(allValueIds).stream()
                .collect(Collectors.toMap(WmsAttributeValue::getId, Function.identity()));
        for (Map.Entry<Long, List<Long>> e : valueIds.entrySet()) {
            for (Long v : e.getValue()) {
                WmsAttributeValue value = values.get(v);
                if (value == null || !value.getAttributeId().equals(e.getKey())) {
                    throw new BizException("wms.error.valueDoesNotBelongVariantAttribute", attrs.get(e.getKey()).getName());
                }
            }
        }
        // 已有 SKU 的属性必须一致（不能中途增减属性）
        List<WmsVariantValue> existingValues = variantValueMapper.selectList(Wrappers.<WmsVariantValue>lambdaQuery()
                .eq(WmsVariantValue::getTemplateId, templateId));
        Set<Long> existingAttrs = existingValues.stream().map(WmsVariantValue::getAttributeId).collect(Collectors.toSet());
        if (!existingAttrs.isEmpty() && !existingAttrs.equals(valueIds.keySet())) {
            throw new BizException("wms.error.skusStyleGeneratedOtherAttributesAttributes");
        }
        Set<String> existingKeys = existingValues.stream().collect(Collectors.groupingBy(WmsVariantValue::getMaterialId,
                        Collectors.mapping(WmsVariantValue::getValueId, Collectors.toSet())))
                .values().stream().map(WmsVariantService::key).collect(Collectors.toSet());

        // 属性按排序组合，属性值按排序
        List<WmsAttribute> orderedAttrs = attrs.values().stream()
                .sorted(Comparator.comparing((WmsAttribute a) -> a.getSort() == null ? 0 : a.getSort()).thenComparing(WmsAttribute::getId)).toList();
        List<List<WmsAttributeValue>> combos = new ArrayList<>();
        combos.add(new ArrayList<>());
        for (WmsAttribute a : orderedAttrs) {
            List<WmsAttributeValue> vs = valueIds.get(a.getId()).stream().distinct().map(values::get)
                    .sorted(Comparator.comparing((WmsAttributeValue v) -> v.getSort() == null ? 0 : v.getSort()).thenComparing(WmsAttributeValue::getId)).toList();
            List<List<WmsAttributeValue>> next = new ArrayList<>();
            for (List<WmsAttributeValue> c : combos) {
                for (WmsAttributeValue v : vs) {
                    List<WmsAttributeValue> n = new ArrayList<>(c);
                    n.add(v);
                    next.add(n);
                }
            }
            combos = next;
        }
        List<List<WmsAttributeValue>> todo = combos.stream()
                .filter(c -> !existingKeys.contains(key(c.stream().map(WmsAttributeValue::getId).collect(Collectors.toSet())))).toList();
        if (existingKeys.size() + todo.size() > MAX_VARIANTS) {
            throw new BizException("wms.error.styleHaveMostSkus", MAX_VARIANTS);
        }
        if (!template.isTemplateMaterial()) {
            materialMapper.update(Wrappers.<WmsMaterial>lambdaUpdate().set(WmsMaterial::getIsTemplate, 1).eq(WmsMaterial::getId, templateId));
        }
        for (List<WmsAttributeValue> combo : todo) {
            String suffix = combo.stream().map(v -> StringUtils.hasText(v.getCode()) ? v.getCode() : v.getAttrValue()).collect(Collectors.joining("-"));
            String text = combo.stream().map(WmsAttributeValue::getAttrValue).collect(Collectors.joining(" / "));
            String code = template.getCode() + "-" + suffix;
            if (code.length() > 64) {
                throw new BizException("wms.error.skuCodeExceedsCharactersShortenStyle", code);
            }
            if (materialMapper.exists(Wrappers.<WmsMaterial>lambdaQuery().eq(WmsMaterial::getCode, code))) {
                throw new BizException("wms.error.materialCodeAlreadyExists2", code);
            }
            WmsMaterial m = new WmsMaterial();
            m.setCode(code);
            m.setName(template.getName());
            String spec = StringUtils.hasText(template.getSpec()) ? template.getSpec() + " " + text : text;
            m.setSpec(spec.length() > 128 ? spec.substring(0, 128) : spec);
            m.setUnit(template.getUnit());
            m.setCategory(template.getCategory());
            m.setBatchEnabled(template.getBatchEnabled());
            m.setSerialEnabled(template.getSerialEnabled());
            m.setIqcRequired(template.getIqcRequired());
            m.setDefaultWarehouseId(template.getDefaultWarehouseId());
            m.setDefaultLocationId(template.getDefaultLocationId());
            m.setRefCost(template.getRefCost());
            m.setCostMethod(template.getCostMethod());
            m.setIsTemplate(0);
            m.setTemplateId(templateId);
            m.setVariantText(text);
            m.setStatus(EnableStatus.ENABLED);
            materialMapper.insert(m);
            for (WmsAttributeValue v : combo) {
                WmsVariantValue vv = new WmsVariantValue();
                vv.setTemplateId(templateId);
                vv.setMaterialId(m.getId());
                vv.setAttributeId(v.getAttributeId());
                vv.setValueId(v.getId());
                variantValueMapper.insert(vv);
            }
        }
        return todo.size();
    }

    /** 款式各 SKU 在全部仓库（不含不良品仓）的库存：SKU ID → 数量 */
    public Map<Long, BigDecimal> stock(Long templateId) {
        requireTemplate(templateId, false);
        List<Long> ids = materialMapper.selectList(Wrappers.<WmsMaterial>lambdaQuery().select(WmsMaterial::getId)
                .eq(WmsMaterial::getTemplateId, templateId)).stream().map(WmsMaterial::getId).toList();
        Map<Long, BigDecimal> result = new HashMap<>();
        if (!ids.isEmpty()) {
            stockMapper.selectQtyByMaterial(ids).forEach(r -> result.put(((Number) r.get("materialId")).longValue(), new BigDecimal(r.get("qty").toString())));
        }
        return result;
    }

    /** 款式（启用），按编码或名称搜索，订单按规格矩阵录入时选择 */
    public List<WmsMaterial> templateOptions(String keyword) {
        return Queries.limit(materialMapper, Wrappers.<WmsMaterial>lambdaQuery()
                .select(WmsMaterial::getId, WmsMaterial::getCode, WmsMaterial::getName, WmsMaterial::getSpec, WmsMaterial::getUnit)
                .eq(WmsMaterial::getIsTemplate, 1).eq(WmsMaterial::getStatus, EnableStatus.ENABLED)
                .and(StringUtils.hasText(keyword), w -> w.like(WmsMaterial::getCode, keyword).or().like(WmsMaterial::getName, keyword))
                .orderByAsc(WmsMaterial::getCode), 50);
    }

    /**
     * @param forGenerate 生成 SKU：SKU 本身不能再作为款式，已发生库存业务的普通物料不能设为款式
     */
    private WmsMaterial requireTemplate(Long id, boolean forGenerate) {
        WmsMaterial m = id == null ? null : materialMapper.selectById(id);
        if (m == null) {
            throw new BizException("wms.error.materialNotFound");
        }
        if (m.getTemplateId() != null) {
            throw new BizException("wms.error.skuCannotStyle", m.getCode());
        }
        if (!m.isTemplateMaterial()) {
            if (!forGenerate) {
                throw new BizException("wms.error.notMultiVariantStyle", m.getCode());
            }
            if (flowMapper.existsByMaterialId(id)) {
                throw new BizException("wms.error.hasStockTransactionsCannotBecomeMulti", m.getCode());
            }
        }
        return m;
    }

    private static String key(Set<Long> valueIds) {
        return valueIds.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
    }
}
