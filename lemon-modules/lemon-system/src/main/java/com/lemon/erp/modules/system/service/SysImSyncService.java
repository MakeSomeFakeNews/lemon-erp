package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.modules.system.dto.ImBindingQuery;
import com.lemon.erp.modules.system.dto.ImBindingRow;
import com.lemon.erp.modules.system.dto.ImSyncResult;
import com.lemon.erp.modules.system.entity.SysDept;
import com.lemon.erp.modules.system.entity.SysImConfig;
import com.lemon.erp.modules.system.entity.SysImDept;
import com.lemon.erp.modules.system.entity.SysImUser;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.im.ImContacts;
import com.lemon.erp.modules.system.im.ImPlatform;
import com.lemon.erp.modules.system.mapper.SysDeptMapper;
import com.lemon.erp.modules.system.mapper.SysImDeptMapper;
import com.lemon.erp.modules.system.mapper.SysImUserMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通讯录同步与成员绑定：按手机号把平台成员绑定到已有账号；开启「自动创建」时按平台的部门树建立部门（同一上级下同名的部门直接对应），
 * 为没有账号的成员开通账号（初始密码见参数设置，首次登录要求修改）。只增加和改名，不删除部门、不停用账号（离职由人事处理）
 */
@Service
@RequiredArgsConstructor
public class SysImSyncService {

    private static final int UNMATCHED_LIMIT = 50;
    private static final int USERNAME_MAX = 64;

    private final SysImConfigService configService;
    private final SysImAuthService authService;
    private final SysImUserMapper imUserMapper;
    private final SysImDeptMapper imDeptMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final SysOrgSyncService orgSyncService;
    private final TokenService tokenService;
    private final TransactionTemplate tx;

    public ImSyncResult sync(ImPlatform platform) {
        SysImConfig c = configService.requireEnabled(platform);
        // 先读取平台通讯录（网络调用不放在事务中）
        ImContacts contacts = configService.client(platform).contacts(c);
        ImSyncResult result = Objects.requireNonNull(tx.execute(s -> apply(c, contacts)));
        configService.markSynced(c.getId(), result.summary());
        if (result.deptCreated() > 0 || result.created() > 0) {
            tokenService.evictAllLoginUsers();
        }
        return result;
    }

    private ImSyncResult apply(SysImConfig c, ImContacts contacts) {
        ImPlatform p = c.getPlatform();
        boolean create = Boolean.TRUE.equals(c.getSyncCreate());
        Map<String, Long> deptMap = new HashMap<>();
        int deptCreated = 0;
        if (create) {
            deptCreated = syncDepts(p, contacts.depts(), deptMap);
        }

        Map<String, Long> bound = imUserMapper.selectList(Wrappers.<SysImUser>lambdaQuery().eq(SysImUser::getPlatform, p)).stream()
                .collect(Collectors.toMap(SysImUser::getImUserId, SysImUser::getUserId));
        Set<Long> boundUsers = new HashSet<>(bound.values());
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getPhone)
                .isNull(SysUser::getSupplierId).isNull(SysUser::getCustomerId));
        // 手机号重复的账号无法确定对应关系，不自动绑定
        Map<String, List<SysUser>> byPhone = users.stream().filter(u -> normalizePhone(u.getPhone()) != null)
                .collect(Collectors.groupingBy(u -> normalizePhone(u.getPhone())));
        Set<String> usernames = users.stream().map(SysUser::getUsername).collect(Collectors.toCollection(HashSet::new));
        Long root = create ? rootDept() : null;

        int alreadyBound = 0;
        int matched = 0;
        int created = 0;
        List<String> unmatched = new ArrayList<>();
        int unmatchedCount = 0;
        String warning = null;
        for (ImContacts.Member m : contacts.members()) {
            if (bound.containsKey(m.userId())) {
                alreadyBound++;
                continue;
            }
            String phone = normalizePhone(m.mobile());
            List<SysUser> same = phone == null ? List.of() : byPhone.getOrDefault(phone, List.of());
            if (same.size() == 1 && !boundUsers.contains(same.get(0).getId())) {
                authService.bind(same.get(0).getId(), p, m.userId(), m.name());
                boundUsers.add(same.get(0).getId());
                matched++;
                continue;
            }
            if (create && same.isEmpty() && warning == null) {
                String username = username(p, phone, m.userId(), usernames);
                try {
                    Long userId = orgSyncService.create(username, m.name(), deptMap.getOrDefault(m.deptId(), root), phone, m.email());
                    usernames.add(username);
                    authService.bind(userId, p, m.userId(), m.name());
                    boundUsers.add(userId);
                    created++;
                    continue;
                } catch (BizException e) {
                    // 账号数达到套餐上限等：停止开通，其余成员计入未绑定
                    warning = "未能继续开通账号：" + e.getMessage(); // i18n-ignore 同时存入同步结果
                }
            }
            unmatchedCount++;
            if (unmatched.size() < UNMATCHED_LIMIT) {
                unmatched.add(m.name());
            }
        }
        return new ImSyncResult(contacts.depts().size(), deptCreated, contacts.members().size(), alreadyBound, matched, created,
                unmatchedCount, unmatched, warning);
    }

    /** 按平台的部门树建立部门（平台部门已按上级在前排列），返回新建数量；deptMap 返回平台部门 → 系统部门 */
    private int syncDepts(ImPlatform p, List<ImContacts.Dept> depts, Map<String, Long> deptMap) {
        List<SysDept> all = deptMapper.selectList(Wrappers.lambdaQuery());
        Map<Long, SysDept> byId = all.stream().collect(Collectors.toMap(SysDept::getId, d -> d));
        Map<String, Long> byParentAndName = new HashMap<>();
        all.forEach(d -> byParentAndName.putIfAbsent(d.getParentId() + "/" + d.getName(), d.getId()));
        // 已删除的系统部门不再对应
        imDeptMapper.selectList(Wrappers.<SysImDept>lambdaQuery().eq(SysImDept::getPlatform, p)).forEach(m -> {
            if (byId.containsKey(m.getDeptId())) {
                deptMap.put(m.getImDeptId(), m.getDeptId());
            } else {
                imDeptMapper.deleteById(m.getId());
            }
        });
        Long root = rootDept();
        int created = 0;
        for (ImContacts.Dept d : depts) {
            if (deptMap.containsKey(d.id())) {
                SysDept mapped = byId.get(deptMap.get(d.id()));
                if (d.parentId() != null && StringUtils.hasText(d.name()) && !d.name().equals(mapped.getName())) {
                    deptMapper.update(Wrappers.<SysDept>lambdaUpdate().set(SysDept::getName, truncate(d.name(), 64)).eq(SysDept::getId, mapped.getId()));
                }
                continue;
            }
            Long deptId;
            if (d.parentId() == null) {
                deptId = root;
            } else {
                Long parent = deptMap.get(d.parentId());
                if (parent == null) {
                    continue;
                }
                String name = truncate(StringUtils.hasText(d.name()) ? d.name() : d.id(), 64);
                deptId = byParentAndName.get(parent + "/" + name);
                if (deptId == null) {
                    SysDept dept = new SysDept();
                    dept.setParentId(parent);
                    dept.setName(name);
                    dept.setSort(d.sort());
                    dept.setStatus(1);
                    deptMapper.insert(dept);
                    deptId = dept.getId();
                    byParentAndName.put(parent + "/" + name, deptId);
                    created++;
                }
            }
            SysImDept m = new SysImDept();
            m.setPlatform(p);
            m.setImDeptId(d.id());
            m.setDeptId(deptId);
            imDeptMapper.insert(m);
            deptMap.put(d.id(), deptId);
        }
        return created;
    }

    /** 顶级部门（企业本身），平台的根部门对应到这里 */
    private Long rootDept() {
        return deptMapper.selectList(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, 0L)).stream()
                .min(Comparator.comparing(SysDept::getSort, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(SysDept::getId))
                .map(SysDept::getId)
                .orElseThrow(() -> new BizException("system.error.createTopLevelDepartmentDepartmentsFirst"));
    }

    /** 新账号的登录名：优先用手机号，已被占用时用「平台_成员 userid」 */
    private static String username(ImPlatform p, String phone, String imUserId, Set<String> taken) {
        if (phone != null && !taken.contains(phone)) {
            return phone;
        }
        String base = truncate(p.name().toLowerCase() + "_" + imUserId.replaceAll("[^A-Za-z0-9_.@-]", "_"), USERNAME_MAX - 4);
        String name = base;
        for (int i = 2; taken.contains(name); i++) {
            name = base + "_" + i;
        }
        return name;
    }

    /** 统一手机号格式：只保留数字，去掉中国区号 86 */
    static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 13 && digits.startsWith("86")) {
            digits = digits.substring(2);
        }
        return digits.length() >= 5 ? digits : null;
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    // ---------------------------------------------------------------- 成员绑定（管理员）

    public PageResult<ImBindingRow> bindings(long current, long size, ImBindingQuery query) {
        if (query.platform() == null) {
            throw new BizException("system.error.selectPlatform");
        }
        Page<ImBindingRow> page = Page.of(current, size);
        imUserMapper.selectBindingPage(page, query);
        return PageResult.of(page);
    }

    /** 手工绑定：填写成员在平台中的 userid（手机号不一致或平台不返回手机号时） */
    public void bind(Long userId, ImPlatform platform, String imUserId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getSupplierId() != null || user.getCustomerId() != null) {
            throw new BizException("system.error.userNotFound");
        }
        if (!StringUtils.hasText(imUserId) || imUserId.trim().length() > 64) {
            throw new BizException("system.error.enterMemberUseridUpCharacters");
        }
        configService.requireEnabled(platform);
        authService.bind(userId, platform, imUserId.trim(), null);
    }

    public void unbind(Long bindingId) {
        imUserMapper.deleteById(bindingId);
    }
}
