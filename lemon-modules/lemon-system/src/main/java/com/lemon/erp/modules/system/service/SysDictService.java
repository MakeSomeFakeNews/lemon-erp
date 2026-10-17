package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.system.entity.SysDictData;
import com.lemon.erp.modules.system.entity.SysDictType;
import com.lemon.erp.modules.system.mapper.SysDictDataMapper;
import com.lemon.erp.modules.system.mapper.SysDictTypeMapper;
import com.lemon.erp.modules.system.model.DictKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 字典类型与字典数据。
 * <ul>
 *   <li>系统字典：取值由程序内置，全平台共用一份（平台租户），只能修改名称、颜色、排序、备注，不能新增、删除取值或修改键值</li>
 *   <li>业务字典：每个租户各自一份（新租户开通时从平台复制），可新增、修改、停用、删除取值；内置取值（程序依赖）不能删除、不能改键值</li>
 * </ul>
 * 字典表不由租户插件处理（平台共用的系统字典要跨租户读取），按种类在这里指定租户。
 * 按类型查询的结果走缓存：系统字典 dict:{type}，业务字典 dict:{租户}:{type}，变更时清除
 */
@Service
@RequiredArgsConstructor
public class SysDictService implements DictLookup {

    public static final String CACHE_KEY = "dict:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Pattern BUSINESS_VALUE = Pattern.compile("^[A-Z][A-Z0-9_]{0,15}$");

    private final SysDictTypeMapper typeMapper;
    private final SysDictDataMapper dataMapper;
    private final CacheService cache;

    // ---------------- 字典类型 ----------------

    public PageResult<SysDictType> pageType(long current, long size, String name, String type, Integer status, DictKind kind) {
        Page<SysDictType> page = typeMapper.selectPage(Page.of(current, size), Wrappers.<SysDictType>lambdaQuery()
                .like(StringUtils.hasText(name), SysDictType::getName, name)
                .like(StringUtils.hasText(type), SysDictType::getType, type)
                .eq(status != null, SysDictType::getStatus, status)
                .eq(kind != null, SysDictType::getKind, kind)
                .orderByDesc(SysDictType::getId));
        return PageResult.of(page);
    }

    /** 全部业务字典类型（业务字典页面左侧列表） */
    public List<SysDictType> businessTypes() {
        return typeMapper.selectList(Wrappers.<SysDictType>lambdaQuery().eq(SysDictType::getKind, DictKind.BUSINESS)
                .orderByAsc(SysDictType::getType));
    }

    public SysDictType getType(Long id) {
        SysDictType type = typeMapper.selectById(id);
        if (type == null) {
            throw new BizException("system.error.dictionaryTypeDoesNotExist");
        }
        return type;
    }

    public void createType(SysDictType type) {
        checkTypeUnique(type);
        type.setId(null);
        if (type.getKind() == null) type.setKind(DictKind.BUSINESS);
        if (type.getStatus() == null) type.setStatus(1);
        typeMapper.insert(type);
    }

    /** 字典类型编码和种类不能修改：程序按编码读取，种类决定数据归属的租户 */
    public void updateType(SysDictType type) {
        SysDictType old = getType(type.getId());
        type.setType(old.getType());
        type.setKind(old.getKind());
        typeMapper.updateById(type);
    }

    /** 只能删除没有内置取值的字典类型（全部租户的数据一并删除） */
    @Transactional
    public void deleteTypes(List<Long> ids) {
        List<SysDictType> types = typeMapper.selectByIds(ids);
        for (SysDictType t : types) {
            if (t.getKind() == DictKind.SYSTEM || dataMapper.exists(Wrappers.<SysDictData>lambdaQuery()
                    .eq(SysDictData::getDictType, t.getType()).eq(SysDictData::getBuiltin, 1))) {
                throw new BizException("system.error.dictTypeBuiltinCannotDelete", t.getName());
            }
        }
        typeMapper.deleteByIds(ids);
        for (SysDictType t : types) {
            dataMapper.delete(Wrappers.<SysDictData>lambdaQuery().eq(SysDictData::getDictType, t.getType()));
        }
        evictNowAndAfterCommit(() -> cache.deleteByPrefix(CACHE_KEY));
    }

    private void checkTypeUnique(SysDictType type) {
        SysDictType exist = typeMapper.selectOne(Wrappers.<SysDictType>lambdaQuery().eq(SysDictType::getType, type.getType()));
        if (exist != null && !Objects.equals(exist.getId(), type.getId())) {
            throw new BizException("system.error.dictionaryTypeAlreadyExists");
        }
    }

    // ---------------- 字典数据 ----------------

    /** 字典数据管理：系统字典为平台的数据，业务字典为当前租户的数据 */
    public PageResult<SysDictData> pageData(long current, long size, String dictType, String label) {
        SysDictType type = requireType(dictType);
        Page<SysDictData> page = dataMapper.selectPage(Page.of(current, size), Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getTenantId, tenantOf(type))
                .like(StringUtils.hasText(label), SysDictData::getDictLabel, label)
                .orderByAsc(SysDictData::getSort)
                .orderByAsc(SysDictData::getId));
        return PageResult.of(page);
    }

    /** 某类型下启用的字典数据，供前端渲染下拉框、标签 */
    public List<SysDictData> listDataByType(String dictType) {
        SysDictType type = typeMapper.selectOne(Wrappers.<SysDictType>lambdaQuery().eq(SysDictType::getType, dictType));
        if (type == null) {
            return List.of();
        }
        Long tenantId = tenantOf(type);
        String key = cacheKey(type, tenantId);
        SysDictData[] cached = cache.get(key, SysDictData[].class);
        if (cached != null) {
            return Arrays.asList(cached);
        }
        List<SysDictData> list = dataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getTenantId, tenantId)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSort)
                .orderByAsc(SysDictData::getId));
        cache.set(key, list, CACHE_TTL);
        return list;
    }

    @Override
    public boolean contains(String dictType, String value) {
        return listDataByType(dictType).stream().anyMatch(d -> d.getDictValue().equals(value));
    }

    /** 只有业务字典可以新增取值 */
    public void createData(SysDictData data) {
        SysDictType type = requireType(data.getDictType());
        if (type.getKind() == DictKind.SYSTEM) {
            throw new BizException("system.error.systemDictCannotAddValue");
        }
        checkBusinessValue(data.getDictValue());
        data.setId(null);
        data.setTenantId(tenantOf(type));
        data.setBuiltin(0);
        checkValueUnique(data);
        if (data.getStatus() == null) data.setStatus(1);
        if (data.getSort() == null) data.setSort(0);
        dataMapper.insert(data);
        evict(type);
    }

    /** 系统字典只能改名称、颜色、排序、备注；内置取值不能改键值 */
    public void updateData(SysDictData data) {
        SysDictData old = getData(data.getId());
        SysDictType type = requireType(old.getDictType());
        data.setDictType(old.getDictType());
        data.setTenantId(old.getTenantId());
        data.setBuiltin(old.getBuiltin());
        if (type.getKind() == DictKind.SYSTEM) {
            data.setStatus(old.getStatus());
        }
        if (Integer.valueOf(1).equals(old.getBuiltin())) {
            data.setDictValue(old.getDictValue());
        } else if (type.getKind() == DictKind.BUSINESS && !old.getDictValue().equals(data.getDictValue())) {
            checkBusinessValue(data.getDictValue());
        }
        checkValueUnique(data);
        dataMapper.updateById(data);
        evict(type);
    }

    public void deleteData(List<Long> ids) {
        List<SysDictData> list = ids.stream().map(this::getData).toList();
        for (SysDictData d : list) {
            if (Integer.valueOf(1).equals(d.getBuiltin())) {
                throw new BizException("system.error.dictValueBuiltinCannotDelete", d.getDictLabel());
            }
        }
        dataMapper.deleteByIds(ids);
        list.stream().map(SysDictData::getDictType).distinct().map(this::requireType).forEach(this::evict);
    }

    // ---------------- 业务字典（租户维护，只能访问业务字典） ----------------

    public PageResult<SysDictData> pageBusinessData(long current, long size, String dictType, String label) {
        requireBusiness(dictType);
        return pageData(current, size, dictType, label);
    }

    public void createBusinessData(SysDictData data) {
        requireBusiness(data.getDictType());
        createData(data);
    }

    public void updateBusinessData(SysDictData data) {
        requireBusiness(getData(data.getId()).getDictType());
        updateData(data);
    }

    public void deleteBusinessData(List<Long> ids) {
        ids.forEach(id -> requireBusiness(getData(id).getDictType()));
        deleteData(ids);
    }

    private void requireBusiness(String dictType) {
        if (requireType(dictType).getKind() != DictKind.BUSINESS) {
            throw new BizException("system.error.dictionaryTypeDoesNotExist");
        }
    }

    /** 新租户：复制平台的业务字典 */
    public void copyBusinessData(Long tenantId) {
        List<String> types = businessTypes().stream().map(SysDictType::getType).toList();
        if (types.isEmpty()) {
            return;
        }
        List<SysDictData> list = dataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                .in(SysDictData::getDictType, types).eq(SysDictData::getTenantId, TenantContext.PLATFORM_TENANT_ID));
        list.forEach(d -> {
            d.setId(null);
            d.setTenantId(tenantId);
        });
        if (!list.isEmpty()) {
            dataMapper.insertBatch(list);
        }
    }

    /** 业务字典的键值存入各业务表的编码字段（最短 VARCHAR(16)）：大写字母开头，大写字母、数字、下划线，不超过 16 位 */
    private static void checkBusinessValue(String value) {
        if (value == null || !BUSINESS_VALUE.matcher(value).matches()) {
            throw new BizException("system.error.bizDictValueFormat");
        }
    }

    /** 只能访问系统字典（平台）和当前租户的业务字典 */
    private SysDictData getData(Long id) {
        SysDictData data = id == null ? null : dataMapper.selectById(id);
        if (data == null || !Objects.equals(data.getTenantId(), tenantOf(requireType(data.getDictType())))) {
            throw new BizException("system.error.dictionaryDataDoesNotExist");
        }
        return data;
    }

    private SysDictType requireType(String dictType) {
        SysDictType type = typeMapper.selectOne(Wrappers.<SysDictType>lambdaQuery().eq(SysDictType::getType, dictType));
        if (type == null) {
            throw new BizException("system.error.dictionaryTypeDoesNotExist");
        }
        return type;
    }

    private void checkValueUnique(SysDictData data) {
        SysDictData exist = dataMapper.selectOne(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, data.getDictType())
                .eq(SysDictData::getTenantId, data.getTenantId())
                .eq(SysDictData::getDictValue, data.getDictValue()));
        if (exist != null && !Objects.equals(exist.getId(), data.getId())) {
            throw new BizException("system.error.dictionaryValueAlreadyExists");
        }
    }

    /** 系统字典的数据属于平台，业务字典属于当前租户 */
    private static Long tenantOf(SysDictType type) {
        return type.getKind() == DictKind.BUSINESS ? TenantContext.currentOrPlatform() : TenantContext.PLATFORM_TENANT_ID;
    }

    private static String cacheKey(SysDictType type, Long tenantId) {
        return type.getKind() == DictKind.BUSINESS ? CACHE_KEY + tenantId + ":" + type.getType() : CACHE_KEY + type.getType();
    }

    private void evict(SysDictType type) {
        cache.delete(cacheKey(type, tenantOf(type)));
    }

    /** 立即清理一次，有事务时提交后再清理一次，避免并发请求把未提交前的旧值重新加载进缓存 */
    private static void evictNowAndAfterCommit(Runnable evict) {
        evict.run();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict.run();
                }
            });
        }
    }
}
