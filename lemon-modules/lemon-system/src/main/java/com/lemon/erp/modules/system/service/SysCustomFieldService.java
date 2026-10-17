package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.system.entity.SysCustomField;
import com.lemon.erp.modules.system.entity.SysCustomValue;
import com.lemon.erp.modules.system.mapper.SysCustomFieldMapper;
import com.lemon.erp.modules.system.mapper.SysCustomValueMapper;
import com.lemon.erp.modules.system.model.CustomFieldType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自定义字段：为业务记录（与附件、变更历史相同的业务类型，如客户、物料、销售订单）扩展字段，值按 业务类型 + 记录 ID + 字段标识 保存，
 * 不修改业务表。读写值按业务类型登记的功能权限和记录的数据权限校验；列表批量取值只校验查看权限
 */
@Service
@RequiredArgsConstructor
public class SysCustomFieldService {

    private static final int MAX_FIELDS = 50;
    private static final int MAX_VALUE = 1000;
    private static final int MAX_BATCH = 500;

    private final SysCustomFieldMapper fieldMapper;
    private final SysCustomValueMapper valueMapper;
    private final SysRecordAccessService recordAccess;
    private final PermissionService permissionService;

    /** 可扩展字段的业务类型：[{code, name}] */
    public List<Map<String, String>> types() {
        return recordAccess.types().stream().map(t -> Map.of("code", t.code(), "name", Messages.get(t.name()))).toList();
    }

    /** 全部定义（管理页面），按业务类型、排序 */
    public List<SysCustomField> all(String bizType) {
        return fieldMapper.selectList(Wrappers.<SysCustomField>lambdaQuery()
                .eq(bizType != null && !bizType.isBlank(), SysCustomField::getBizType, bizType)
                .orderByAsc(SysCustomField::getBizType).orderByAsc(SysCustomField::getSort).orderByAsc(SysCustomField::getId));
    }

    /** 业务类型启用的字段（表单、列表使用） */
    public List<SysCustomField> enabled(String bizType) {
        recordAccess.type(bizType);
        return fieldMapper.selectList(Wrappers.<SysCustomField>lambdaQuery()
                .eq(SysCustomField::getBizType, bizType).eq(SysCustomField::getStatus, 1)
                .orderByAsc(SysCustomField::getSort).orderByAsc(SysCustomField::getId));
    }

    public Long save(SysCustomField f) {
        recordAccess.type(f.getBizType());
        if (f.getFieldType() == CustomFieldType.SELECT && options(f).isEmpty()) {
            throw new BizException("system.error.enterOptionsDropDown");
        }
        f.setLabel(f.getLabel().trim());
        f.setRequired(Integer.valueOf(1).equals(f.getRequired()) ? 1 : 0);
        f.setShowInList(Integer.valueOf(1).equals(f.getShowInList()) ? 1 : 0);
        f.setStatus(f.getStatus() == null ? 1 : f.getStatus());
        f.setSort(f.getSort() == null ? 0 : f.getSort());
        try {
            if (f.getId() == null) {
                if (fieldMapper.selectCount(Wrappers.<SysCustomField>lambdaQuery().eq(SysCustomField::getBizType, f.getBizType())) >= MAX_FIELDS) {
                    throw new BizException("system.error.upCustomFieldsPerBusinessType", MAX_FIELDS);
                }
                fieldMapper.insert(f);
            } else {
                SysCustomField old = fieldMapper.selectById(f.getId());
                if (old == null) {
                    throw new BizException("system.error.customFieldDoesNotExist");
                }
                if (!old.getBizType().equals(f.getBizType()) || !old.getFieldKey().equals(f.getFieldKey())) {
                    throw new BizException("system.error.businessTypeFieldKeyCannotChanged");
                }
                fieldMapper.updateById(f);
            }
        } catch (DuplicateKeyException e) {
            throw new BizException("system.error.fieldKeyAlreadyExists", f.getFieldKey());
        }
        return f.getId();
    }

    /** 删除定义及已填的值 */
    @Transactional
    public void delete(Long id) {
        SysCustomField f = fieldMapper.selectById(id);
        if (f == null) {
            return;
        }
        valueMapper.delete(Wrappers.<SysCustomValue>lambdaQuery().eq(SysCustomValue::getBizType, f.getBizType())
                .eq(SysCustomValue::getFieldKey, f.getFieldKey()));
        fieldMapper.deleteById(id);
    }

    /** 一条记录的值：字段标识 → 值 */
    public Map<String, String> values(String bizType, Long bizId) {
        recordAccess.check(bizType, bizId, false, "system.text.customFields");
        Map<String, String> result = new LinkedHashMap<>();
        valueMapper.selectList(Wrappers.<SysCustomValue>lambdaQuery().eq(SysCustomValue::getBizType, bizType).eq(SysCustomValue::getBizId, bizId))
                .forEach(v -> result.put(v.getFieldKey(), v.getFieldValue()));
        return result;
    }

    /** 列表显示：多条记录的值（记录 ID → 字段标识 → 值），只校验查看权限 */
    public Map<Long, Map<String, String>> batchValues(String bizType, Collection<Long> bizIds) {
        RecordType type = recordAccess.type(bizType);
        if (Arrays.stream(type.viewPerm().split(",")).map(String::trim).noneMatch(permissionService::hasPerm)) {
            throw new BizException("system.error.noPermissionView", Messages.get(type.name()), Messages.get("system.text.customFields"));
        }
        if (bizIds == null || bizIds.isEmpty()) {
            return Map.of();
        }
        if (bizIds.size() > MAX_BATCH) {
            throw new BizException("system.error.upRecordsTime", MAX_BATCH);
        }
        Map<Long, Map<String, String>> result = new HashMap<>();
        valueMapper.selectList(Wrappers.<SysCustomValue>lambdaQuery().eq(SysCustomValue::getBizType, bizType).in(SysCustomValue::getBizId, bizIds))
                .forEach(v -> result.computeIfAbsent(v.getBizId(), k -> new LinkedHashMap<>()).put(v.getFieldKey(), v.getFieldValue()));
        return result;
    }

    /**
     * 保存一条记录的值（整体替换）：按类型校验，必填项不能为空，空值删除
     */
    @Transactional
    public void saveValues(String bizType, Long bizId, Map<String, String> values) {
        recordAccess.check(bizType, bizId, true, "system.text.customFields");
        Map<String, SysCustomField> fields = enabled(bizType).stream().collect(Collectors.toMap(SysCustomField::getFieldKey, f -> f));
        Map<String, String> input = values == null ? Map.of() : values;
        Map<String, String> clean = new LinkedHashMap<>();
        for (SysCustomField f : fields.values()) {
            String v = input.get(f.getFieldKey());
            v = v == null ? null : v.trim();
            if (v == null || v.isEmpty()) {
                if (Integer.valueOf(1).equals(f.getRequired())) {
                    throw new BizException("system.error.fill", f.getLabel());
                }
                continue;
            }
            clean.put(f.getFieldKey(), validate(f, v));
        }
        if (fields.isEmpty()) {
            return;
        }
        // 只替换已启用字段的值，停用字段的值保留
        valueMapper.delete(Wrappers.<SysCustomValue>lambdaQuery().eq(SysCustomValue::getBizType, bizType).eq(SysCustomValue::getBizId, bizId)
                .in(SysCustomValue::getFieldKey, fields.keySet()));
        clean.forEach((k, v) -> {
            SysCustomValue row = new SysCustomValue();
            row.setBizType(bizType);
            row.setBizId(bizId);
            row.setFieldKey(k);
            row.setFieldValue(v);
            valueMapper.insert(row);
        });
    }

    private static String validate(SysCustomField f, String v) {
        if (v.length() > MAX_VALUE) {
            throw new BizException("system.error.cannotExceedCharacters", f.getLabel(), MAX_VALUE);
        }
        switch (f.getFieldType()) {
            case NUMBER -> {
                try {
                    return new BigDecimal(v).stripTrailingZeros().toPlainString();
                } catch (NumberFormatException e) {
                    throw new BizException("system.error.mustNumber", f.getLabel());
                }
            }
            case DATE -> {
                try {
                    return LocalDate.parse(v.length() > 10 ? v.substring(0, 10) : v).toString();
                } catch (DateTimeParseException e) {
                    throw new BizException("system.error.mustDate", f.getLabel());
                }
            }
            case BOOL -> {
                return Boolean.toString(Boolean.parseBoolean(v));
            }
            case SELECT -> {
                if (!options(f).contains(v)) {
                    throw new BizException("system.error.mustOneOptions", f.getLabel());
                }
                return v;
            }
            default -> {
                return v;
            }
        }
    }

    private static Set<String> options(SysCustomField f) {
        if (f.getOptions() == null) {
            return Set.of();
        }
        return Arrays.stream(f.getOptions().split("[,，\\n]")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
