package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.modules.system.entity.SysDictData;
import com.lemon.erp.modules.system.entity.SysDictType;
import com.lemon.erp.modules.system.mapper.SysDictDataMapper;
import com.lemon.erp.modules.system.mapper.SysDictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 字典类型与字典数据；按类型查询的结果走缓存 dict:{type}，变更时清除
 */
@Service
@RequiredArgsConstructor
public class SysDictService {

    public static final String CACHE_KEY = "dict:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final SysDictTypeMapper typeMapper;
    private final SysDictDataMapper dataMapper;
    private final CacheService cache;

    // ---------------- 字典类型 ----------------

    public PageResult<SysDictType> pageType(long current, long size, String name, String type, Integer status) {
        Page<SysDictType> page = typeMapper.selectPage(Page.of(current, size), Wrappers.<SysDictType>lambdaQuery()
                .like(StringUtils.hasText(name), SysDictType::getName, name)
                .like(StringUtils.hasText(type), SysDictType::getType, type)
                .eq(status != null, SysDictType::getStatus, status)
                .orderByDesc(SysDictType::getId));
        return PageResult.of(page);
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
        if (type.getStatus() == null) type.setStatus(1);
        typeMapper.insert(type);
    }

    /** 修改字典类型编码时，同步更新其下字典数据的 dictType */
    @Transactional
    public void updateType(SysDictType type) {
        SysDictType old = getType(type.getId());
        checkTypeUnique(type);
        typeMapper.updateById(type);
        if (!old.getType().equals(type.getType())) {
            dataMapper.update(Wrappers.<SysDictData>lambdaUpdate()
                    .set(SysDictData::getDictType, type.getType())
                    .eq(SysDictData::getDictType, old.getType()));
        }
        evict(old.getType());
        evict(type.getType());
    }

    @Transactional
    public void deleteTypes(List<Long> ids) {
        List<String> types = typeMapper.selectByIds(ids).stream().map(SysDictType::getType).toList();
        typeMapper.deleteByIds(ids);
        if (!types.isEmpty()) {
            dataMapper.delete(Wrappers.<SysDictData>lambdaQuery().in(SysDictData::getDictType, types));
        }
        types.forEach(this::evict);
    }

    private void checkTypeUnique(SysDictType type) {
        SysDictType exist = typeMapper.selectOne(Wrappers.<SysDictType>lambdaQuery().eq(SysDictType::getType, type.getType()));
        if (exist != null && !Objects.equals(exist.getId(), type.getId())) {
            throw new BizException("system.error.dictionaryTypeAlreadyExists");
        }
    }

    // ---------------- 字典数据 ----------------

    public PageResult<SysDictData> pageData(long current, long size, String dictType, String label) {
        Page<SysDictData> page = dataMapper.selectPage(Page.of(current, size), Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, dictType)
                .like(StringUtils.hasText(label), SysDictData::getDictLabel, label)
                .orderByAsc(SysDictData::getSort)
                .orderByAsc(SysDictData::getId));
        return PageResult.of(page);
    }

    /** 某类型下启用的字典数据，供前端渲染下拉框、标签 */
    public List<SysDictData> listDataByType(String dictType) {
        String key = CACHE_KEY + dictType;
        SysDictData[] cached = cache.get(key, SysDictData[].class);
        if (cached != null) {
            return Arrays.asList(cached);
        }
        List<SysDictData> list = dataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSort)
                .orderByAsc(SysDictData::getId));
        cache.set(key, list, CACHE_TTL);
        return list;
    }

    public void createData(SysDictData data) {
        if (!typeMapper.exists(Wrappers.<SysDictType>lambdaQuery().eq(SysDictType::getType, data.getDictType()))) {
            throw new BizException("system.error.dictionaryTypeDoesNotExist");
        }
        checkValueUnique(data);
        data.setId(null);
        if (data.getStatus() == null) data.setStatus(1);
        if (data.getSort() == null) data.setSort(0);
        dataMapper.insert(data);
        evict(data.getDictType());
    }

    public void updateData(SysDictData data) {
        SysDictData old = dataMapper.selectById(data.getId());
        if (old == null) {
            throw new BizException("system.error.dictionaryDataDoesNotExist");
        }
        // 字典数据不允许跨类型移动
        data.setDictType(old.getDictType());
        checkValueUnique(data);
        dataMapper.updateById(data);
        evict(old.getDictType());
    }

    public void deleteData(List<Long> ids) {
        List<SysDictData> list = dataMapper.selectByIds(ids);
        dataMapper.deleteByIds(ids);
        list.stream().map(SysDictData::getDictType).distinct().forEach(this::evict);
    }

    private void checkValueUnique(SysDictData data) {
        SysDictData exist = dataMapper.selectOne(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, data.getDictType())
                .eq(SysDictData::getDictValue, data.getDictValue()));
        if (exist != null && !Objects.equals(exist.getId(), data.getId())) {
            throw new BizException("system.error.dictionaryValueAlreadyExists");
        }
    }

    private void evict(String dictType) {
        cache.delete(CACHE_KEY + dictType);
    }
}
