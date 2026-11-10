package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.util.TreeUtils;
import com.lemon.erp.framework.org.DeptLookup;
import com.lemon.erp.modules.system.entity.SysDept;
import com.lemon.erp.modules.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link DeptLookup} 的实现：读取当前租户的部门树（租户条件由插件自动追加），不做操作人的数据权限校验
 */
@Service
@RequiredArgsConstructor
public class SysDeptLookup implements DeptLookup {

    private final SysDeptMapper deptMapper;

    @Override
    public Map<Long, Set<Long>> selfAndDescendantIds(Collection<Long> deptIds) {
        Map<Long, Set<Long>> result = new LinkedHashMap<>();
        if (deptIds.isEmpty()) {
            return result;
        }
        List<SysDept> all = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery().select(SysDept::getId, SysDept::getParentId));
        deptIds.forEach(id -> result.computeIfAbsent(id, k -> TreeUtils.selfAndDescendantIds(all, k)));
        return result;
    }
}
