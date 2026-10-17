package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.lemon.erp.common.base.TreeEntity;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.common.util.TreeUtils;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.system.entity.SysDept;
import com.lemon.erp.modules.system.entity.SysRoleDept;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysDeptMapper;
import com.lemon.erp.modules.system.mapper.SysRoleDeptMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDeptService extends ServiceImpl<SysDeptMapper, SysDept> {

    private final SysUserMapper userMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final TokenService tokenService;
    private final ReferenceGuard referenceGuard;

    /** 当前用户数据权限范围内的部门树（部门表的「所属部门」就是自身 id） */
    @DataPermission(table = "sys_dept", deptColumn = "id", userColumn = "")
    public List<SysDept> tree() {
        List<SysDept> depts = list();
        fillLeaderName(depts);
        return TreeUtils.build(depts);
    }

    public void create(SysDept dept) {
        normalize(dept);
        checkLeader(dept.getLeaderUserId());
        dept.setId(null);
        checkParent(dept, true);
        save(dept);
        // 新部门可能落入其他用户「本部门及以下」的数据权限范围
        tokenService.evictAllLoginUsers();
    }

    public void update(SysDept dept) {
        SysDept old = getById(dept.getId());
        if (old == null) {
            throw new BizException("部门不存在");
        }
        checkDataScope(dept.getId());
        normalize(dept);
        if (!Objects.equals(old.getLeaderUserId(), dept.getLeaderUserId())) {
            checkLeader(dept.getLeaderUserId());
        }
        boolean parentChanged = !Objects.equals(old.getParentId(), dept.getParentId());
        if (parentChanged) {
            if (Objects.equals(dept.getId(), dept.getParentId())) {
                throw new BizException("上级部门不能是自己");
            }
            if (selfAndDescendantIds(dept.getId()).contains(dept.getParentId())) {
                throw new BizException("上级部门不能是自己的下级部门");
            }
        }
        checkParent(dept, parentChanged);
        if (dept.getStatus() == 0 && exists(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getParentId, dept.getId()).eq(SysDept::getStatus, 1))) {
            throw new BizException("该部门包含未停用的下级部门，不允许停用");
        }
        updateById(dept);
        tokenService.evictAllLoginUsers();
    }

    @Transactional
    public void delete(Long id) {
        checkDataScope(id);
        if (exists(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, id))) {
            throw new BizException("存在下级部门，不允许删除");
        }
        if (userMapper.exists(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getDeptId, id))) {
            throw new BizException("部门下存在用户，不允许删除");
        }
        referenceGuard.checkDeletable(RefType.DEPT, id, I18n.t("部门"));
        removeById(id);
        roleDeptMapper.delete(Wrappers.<SysRoleDept>lambdaQuery().eq(SysRoleDept::getDeptId, id));
        tokenService.evictAllLoginUsers();
    }

    /** 当前用户没有该部门的数据权限时抛出异常 */
    public void checkDataScope(Long deptId) {
        if (!SecurityUtils.getDataScope().canAccessDept(deptId)) {
            throw new BizException("没有该部门的数据权限");
        }
    }

    /** 当前用户没有其中任一部门的数据权限时抛出异常 */
    public void checkDataScope(Collection<Long> deptIds) {
        deptIds.forEach(this::checkDataScope);
    }

    /** 部门自身及全部下级部门的 ID */
    public Set<Long> selfAndDescendantIds(Long deptId) {
        return TreeUtils.selfAndDescendantIds(
                list(Wrappers.<SysDept>lambdaQuery().select(SysDept::getId, SysDept::getParentId)), deptId);
    }

    /** 用户被删除时，清除其担任的部门负责人 */
    public void clearLeader(Collection<Long> userIds) {
        update(Wrappers.<SysDept>lambdaUpdate()
                .set(SysDept::getLeaderUserId, null)
                .in(SysDept::getLeaderUserId, userIds));
    }

    /** 部门 ID → 名称 */
    public Map<Long, String> nameMap(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return list(Wrappers.<SysDept>lambdaQuery().select(SysDept::getId, SysDept::getName).in(SysDept::getId, ids))
                .stream().collect(Collectors.toMap(SysDept::getId, SysDept::getName));
    }

    /**
     * @param checkScope 是否校验上级部门的数据权限（未修改上级部门时无需校验，
     *                   否则只有「本部门」权限的用户无法编辑自己的部门）
     */
    private void checkParent(SysDept dept, boolean checkScope) {
        if (dept.getParentId() == TreeEntity.ROOT_PARENT_ID) {
            if (checkScope && !SecurityUtils.getDataScope().all()) {
                throw new BizException("没有权限设置顶级部门");
            }
            return;
        }
        SysDept parent = getById(dept.getParentId());
        if (parent == null) {
            throw new BizException("上级部门不存在");
        }
        if (checkScope) {
            checkDataScope(parent.getId());
        }
        if (parent.getStatus() == 0 && dept.getStatus() == 1) {
            throw new BizException("上级部门已停用，不允许启用或新增下级部门");
        }
    }

    /** 负责人必须是存在且启用的用户 */
    private void checkLeader(Long leaderUserId) {
        if (leaderUserId != null && !userMapper.exists(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getId, leaderUserId).eq(SysUser::getStatus, 1))) {
            throw new BizException("负责人不存在或已停用");
        }
    }

    private void fillLeaderName(List<SysDept> depts) {
        Set<Long> userIds = depts.stream().map(SysDept::getLeaderUserId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> names = userMapper.selectDisplayNames(userIds);
        depts.forEach(d -> d.setLeaderName(names.get(d.getLeaderUserId())));
    }

    private void normalize(SysDept dept) {
        if (dept.getParentId() == null) dept.setParentId(TreeEntity.ROOT_PARENT_ID);
        if (dept.getSort() == null) dept.setSort(0);
        if (dept.getStatus() == null) dept.setStatus(1);
    }
}
