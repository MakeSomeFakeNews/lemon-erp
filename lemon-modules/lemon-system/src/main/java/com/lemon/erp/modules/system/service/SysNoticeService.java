package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.portal.PortalKind;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.system.dto.NoticeOptions;
import com.lemon.erp.modules.system.dto.NoticeReadStat;
import com.lemon.erp.modules.system.dto.NoticeReader;
import com.lemon.erp.modules.system.dto.NoticeRequest;
import com.lemon.erp.modules.system.entity.SysDept;
import com.lemon.erp.modules.system.entity.SysNotice;
import com.lemon.erp.modules.system.entity.SysNoticeRecipient;
import com.lemon.erp.modules.system.entity.SysNoticeTarget;
import com.lemon.erp.modules.system.entity.SysRole;
import com.lemon.erp.modules.system.mapper.SysNoticeMapper;
import com.lemon.erp.modules.system.mapper.SysNoticeRecipientMapper;
import com.lemon.erp.modules.system.mapper.SysNoticeTargetMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.modules.system.model.NoticeScope;
import com.lemon.erp.modules.system.model.NoticeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知公告：草稿 → 已发布 → 已撤回（撤回后可修改、再次发布）。
 * <ul>
 *   <li>发布时按范围（全员 / 指定部门含下级 / 指定角色）确定接收人写入 {@code sys_notice_recipient}，作为可见范围和阅读统计的依据；
 *       再次发布时按当时的范围增减接收人（已读记录保留）；站内消息按去重键每人一条，撤回时删除未读的，再次发布时补发</li>
 *   <li>员工只能看到发给自己、已发布且未过期（有效期含当天）的公告；阅读时记录阅读时间，并把对应的站内消息标为已读</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SysNoticeService {

    /** 公告类型（业务字典） */
    public static final String TYPE_DICT = "sys_notice_type";
    /** 管理端查看权限：有此权限的用户可以查看全部公告（含草稿）和附件 */
    public static final String PERM_LIST = "sys:notice:list";
    /** 站内消息的去重键前缀：同一公告每人只发一次，阅读公告时按此标记消息已读 */
    private static final String DEDUP_PREFIX = "notice:";
    /** 站内消息的链接：员工端「全部公告」打开该公告 */
    private static final String LINK_PREFIX = "/notices?id=";
    private static final int BATCH = 500;
    private static final int EXCERPT_MAX = 100;
    private static final List<NoticeStatus> EDITABLE = List.of(NoticeStatus.DRAFT, NoticeStatus.WITHDRAWN);
    private static final Set<String> PORTAL_ROLES = Arrays.stream(PortalKind.values()).map(PortalKind::getRole).collect(Collectors.toSet());

    private final SysNoticeMapper noticeMapper;
    private final SysNoticeTargetMapper targetMapper;
    private final SysNoticeRecipientMapper recipientMapper;
    private final SysUserMapper userMapper;
    private final SysDeptService deptService;
    private final SysRoleService roleService;
    private final SysMessageService messageService;
    private final DictLookup dictLookup;
    private final PermissionService permissionService;

    // ---------------------------------------------------------------- 管理端

    /** 公告列表（不含正文），附阅读进度 */
    public PageResult<SysNotice> page(long current, long size, String title, String type, NoticeStatus status) {
        Page<SysNotice> page = noticeMapper.selectPage(Page.of(current, size), Wrappers.<SysNotice>lambdaQuery()
                .select(SysNotice.class, f -> !"content".equals(f.getColumn()))
                .like(StringUtils.hasText(title), SysNotice::getTitle, title)
                .eq(StringUtils.hasText(type), SysNotice::getType, type)
                .eq(status != null, SysNotice::getStatus, status)
                .orderByDesc(SysNotice::getId));
        List<SysNotice> list = page.getRecords();
        if (!list.isEmpty()) {
            Map<Long, NoticeReadStat> stats = recipientMapper.selectStats(list.stream().map(SysNotice::getId).toList()).stream()
                    .collect(Collectors.toMap(NoticeReadStat::noticeId, Function.identity()));
            fillNames(list);
            list.forEach(n -> {
                NoticeReadStat s = stats.get(n.getId());
                n.setTotalCount(s == null ? 0L : s.totalCount());
                n.setReadCount(s == null ? 0L : s.readCount());
            });
        }
        return PageResult.of(page);
    }

    public SysNotice detail(Long id) {
        SysNotice n = getExisting(id);
        List<Long> targets = targetIds(id);
        n.setTargetIds(targets);
        n.setTargetNames(targetNames(n.getScopeType(), targets));
        fillNames(List.of(n));
        NoticeReadStat stat = recipientMapper.selectStats(List.of(id)).stream().findFirst().orElse(null);
        n.setTotalCount(stat == null ? 0L : stat.totalCount());
        n.setReadCount(stat == null ? 0L : stat.readCount());
        return n;
    }

    /** 新建草稿，返回 ID */
    @Transactional
    public Long create(NoticeRequest req) {
        List<Long> targets = validate(req);
        SysNotice n = new SysNotice();
        n.setTitle(req.title().trim());
        n.setType(req.type());
        n.setContent(req.content());
        n.setScopeType(req.scopeType());
        n.setPinned(Boolean.TRUE.equals(req.pinned()));
        n.setExpireDate(req.expireDate());
        n.setStatus(NoticeStatus.DRAFT);
        noticeMapper.insert(n);
        saveTargets(n.getId(), targets);
        return n.getId();
    }

    /** 修改草稿或已撤回的公告 */
    @Transactional
    public void update(Long id, NoticeRequest req) {
        getExisting(id);
        List<Long> targets = validate(req);
        LambdaUpdateWrapper<SysNotice> w = Wrappers.<SysNotice>lambdaUpdate()
                .set(SysNotice::getTitle, req.title().trim())
                .set(SysNotice::getType, req.type())
                .set(SysNotice::getContent, req.content())
                .set(SysNotice::getScopeType, req.scopeType())
                .set(SysNotice::getPinned, Boolean.TRUE.equals(req.pinned()))
                .set(SysNotice::getExpireDate, req.expireDate())
                .set(SysNotice::getUpdateTime, LocalDateTime.now())
                .eq(SysNotice::getId, id)
                .in(SysNotice::getStatus, EDITABLE);
        if (noticeMapper.update(w) == 0) {
            throw new BizException("system.error.noticeNotEditable");
        }
        targetMapper.delete(Wrappers.<SysNoticeTarget>lambdaQuery().eq(SysNoticeTarget::getNoticeId, id));
        saveTargets(id, targets);
    }

    /** 删除草稿或已撤回的公告（已发布的先撤回） */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysNotice n = getExisting(id);
            if (!EDITABLE.contains(n.getStatus())) {
                throw new BizException("system.error.noticePublishedCannotDelete", n.getTitle());
            }
            targetMapper.delete(Wrappers.<SysNoticeTarget>lambdaQuery().eq(SysNoticeTarget::getNoticeId, id));
            recipientMapper.delete(Wrappers.<SysNoticeRecipient>lambdaQuery().eq(SysNoticeRecipient::getNoticeId, id));
            noticeMapper.deleteById(id);
        }
    }

    /**
     * 发布：按当前的范围确定接收人（再次发布时增减，已读记录保留），给还没有该公告消息的接收人发站内消息，返回接收人数
     */
    @Transactional
    public int publish(Long id) {
        SysNotice n = getExisting(id);
        if (!EDITABLE.contains(n.getStatus())) {
            throw new BizException("system.error.noticeAlreadyPublished");
        }
        LocalDate today = LocalDate.now();
        if (n.getExpireDate() != null && n.getExpireDate().isBefore(today)) {
            throw new BizException("system.error.noticeExpireBeforeToday");
        }
        Set<Long> users = new LinkedHashSet<>(scopeUsers(n.getScopeType(), targetIds(id)));
        if (users.isEmpty()) {
            throw new BizException("system.error.noticeNoRecipients");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!StatusTransitions.tryChangeFromAny(noticeMapper, SysNotice::getId, id, SysNotice::getStatus, EDITABLE, NoticeStatus.PUBLISHED,
                w -> w.set(SysNotice::getPublishTime, now).set(SysNotice::getPublishBy, SecurityUtils.getUserId())
                        .set(SysNotice::getUpdateTime, now))) {
            throw new BizException("system.error.noticeStatusChanged");
        }

        Set<Long> existing = recipientMapper.selectList(Wrappers.<SysNoticeRecipient>lambdaQuery()
                        .select(SysNoticeRecipient::getUserId).eq(SysNoticeRecipient::getNoticeId, id))
                .stream().map(SysNoticeRecipient::getUserId).collect(Collectors.toSet());
        List<Long> removed = existing.stream().filter(u -> !users.contains(u)).toList();
        for (List<Long> chunk : chunks(removed)) {
            recipientMapper.delete(Wrappers.<SysNoticeRecipient>lambdaQuery()
                    .eq(SysNoticeRecipient::getNoticeId, id).in(SysNoticeRecipient::getUserId, chunk));
        }
        List<Long> added = users.stream().filter(u -> !existing.contains(u)).toList();
        for (List<Long> chunk : chunks(added)) {
            recipientMapper.insertBatch(id, chunk);
        }
        // 站内消息发给全部接收人，按去重键跳过已经有该公告消息的人（再次发布时只提醒新增的接收人和撤回时还没看的人）；
        // 存库内容保持原文（标题、正文摘要），不按请求语言生成
        messageService.send(users, MessageSender.Message.of(MessageCategory.NOTICE, n.getTitle(), excerpt(n.getContent()),
                LINK_PREFIX + id).dedup(DEDUP_PREFIX + id));
        return users.size();
    }

    /**
     * 撤回：员工不再看到，可以修改后再次发布；删除还没看的站内消息（再次发布时这些人会重新收到提醒）
     */
    @Transactional
    public void withdraw(Long id) {
        getExisting(id);
        StatusTransitions.change(noticeMapper, SysNotice::getId, id, SysNotice::getStatus, NoticeStatus.PUBLISHED, NoticeStatus.WITHDRAWN,
                w -> w.set(SysNotice::getUpdateTime, LocalDateTime.now()), "system.error.noticeNotPublished");
        messageService.deleteUnreadByDedupKey(DEDUP_PREFIX + id);
    }

    /**
     * 接收人名单
     *
     * @param read true 已读，false 未读，null 全部
     */
    public PageResult<NoticeReader> readers(Long id, Boolean read, String keyword, long current, long size) {
        getExisting(id);
        return PageResult.of(recipientMapper.selectReaders(Page.of(current, size), id, read, keyword));
    }

    /** 选择发布范围用的部门树和角色（门户角色除外） */
    public NoticeOptions options() {
        List<SysDept> depts = deptService.tree();
        List<NoticeOptions.RoleOption> roles = roleService.listEnabled().stream()
                .filter(r -> !PORTAL_ROLES.contains(r.getCode()))
                .map(r -> new NoticeOptions.RoleOption(r.getId(), r.getName(), r.getCode())).toList();
        return new NoticeOptions(depts, roles);
    }

    // ---------------------------------------------------------------- 员工端

    /** 发给本人、已发布且未过期的公告（不含正文），置顶在前 */
    public PageResult<SysNotice> mine(long current, long size, boolean unread, String keyword, String type) {
        Page<SysNotice> page = Page.of(current, size);
        noticeMapper.selectMine(page, SecurityUtils.getUserId(), LocalDate.now(), unread, keyword, type);
        page.getRecords().forEach(n -> n.setRead(n.getReadTime() != null));
        return PageResult.of(page);
    }

    public long unreadCount() {
        return noticeMapper.countMineUnread(SecurityUtils.getUserId(), LocalDate.now());
    }

    /** 本人可见的公告详情；不可见（不在范围内、未发布、已撤回、已过期）时提示不存在 */
    public SysNotice mineDetail(Long id) {
        SysNotice n = noticeMapper.selectMineById(id, SecurityUtils.getUserId(), LocalDate.now());
        if (n == null) {
            throw new BizException("system.error.noticeNotAvailable");
        }
        n.setRead(n.getReadTime() != null);
        return n;
    }

    /** 标为已读（打开详情时调用），对应的站内消息一并标为已读 */
    @Transactional
    public void read(Long id) {
        Long userId = SecurityUtils.getUserId();
        if (recipientMapper.markRead(id, userId, LocalDateTime.now()) > 0) {
            messageService.readByDedupKey(userId, DEDUP_PREFIX + id);
        }
    }

    /**
     * 附件的访问校验（{@link SysNoticeRecordTypes}）：有管理端查看权限的看全部，其他人只能看发给自己的有效公告
     */
    Object loadForAttachment(long id) {
        if (permissionService.hasPerm(PERM_LIST)) {
            return noticeMapper.selectById(id);
        }
        return noticeMapper.selectMineById(id, SecurityUtils.getUserId(), LocalDate.now());
    }

    // ---------------------------------------------------------------- 内部

    private SysNotice getExisting(Long id) {
        SysNotice n = noticeMapper.selectById(id);
        if (n == null) {
            throw new BizException("system.error.noticeDoesNotExist");
        }
        return n;
    }

    /** 校验字段，返回去重后的发布范围（全员时为空） */
    private List<Long> validate(NoticeRequest req) {
        dictLookup.check(TYPE_DICT, req.type(), "system.label.noticeType");
        if (req.expireDate() != null && req.expireDate().isBefore(LocalDate.now())) {
            throw new BizException("system.error.noticeExpireBeforeToday");
        }
        if (req.scopeType() == NoticeScope.ALL) {
            return List.of();
        }
        List<Long> ids = req.targetIds() == null ? List.of()
                : req.targetIds().stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            throw new BizException(req.scopeType() == NoticeScope.DEPT ? "system.error.noticeSelectDept" : "system.error.noticeSelectRole");
        }
        if (req.scopeType() == NoticeScope.DEPT) {
            if (deptService.count(Wrappers.<SysDept>lambdaQuery().in(SysDept::getId, ids)) != ids.size()) {
                throw new BizException("system.error.departmentDoesNotExist");
            }
        } else {
            List<SysRole> roles = roleService.listByIds(ids);
            if (roles.size() != ids.size() || roles.stream().anyMatch(r -> PORTAL_ROLES.contains(r.getCode()))) {
                throw new BizException("system.error.noticeRoleInvalid");
            }
        }
        return ids;
    }

    private void saveTargets(Long noticeId, List<Long> targets) {
        if (targets.isEmpty()) {
            return;
        }
        targetMapper.insertBatch(targets.stream().map(t -> {
            SysNoticeTarget x = new SysNoticeTarget();
            x.setNoticeId(noticeId);
            x.setTargetId(t);
            return x;
        }).toList());
    }

    private List<Long> targetIds(Long noticeId) {
        return targetMapper.selectList(Wrappers.<SysNoticeTarget>lambdaQuery().eq(SysNoticeTarget::getNoticeId, noticeId)
                        .orderByAsc(SysNoticeTarget::getId))
                .stream().map(SysNoticeTarget::getTargetId).toList();
    }

    private List<String> targetNames(NoticeScope scope, List<Long> ids) {
        if (ids.isEmpty() || scope == NoticeScope.ALL) {
            return List.of();
        }
        Map<Long, String> names = scope == NoticeScope.DEPT ? deptService.nameMap(ids)
                : roleService.listByIds(ids).stream().collect(Collectors.toMap(SysRole::getId, SysRole::getName));
        return ids.stream().map(names::get).filter(Objects::nonNull).toList();
    }

    /** 发布范围内的用户：指定部门含全部下级部门 */
    private List<Long> scopeUsers(NoticeScope scope, List<Long> targets) {
        return switch (scope) {
            case ALL -> recipientMapper.selectScopeUsers(null, null);
            case DEPT -> {
                Set<Long> depts = deptService.selfAndDescendantIds(targets);
                yield depts.isEmpty() ? List.of() : recipientMapper.selectScopeUsers(depts, null);
            }
            case ROLE -> targets.isEmpty() ? List.of() : recipientMapper.selectScopeUsers(null, targets);
        };
    }

    private void fillNames(List<SysNotice> list) {
        Set<Long> userIds = new HashSet<>();
        list.forEach(n -> {
            userIds.add(n.getPublishBy());
            userIds.add(n.getCreateBy());
        });
        userIds.remove(null);
        Map<Long, String> names = userMapper.selectDisplayNames(userIds);
        list.forEach(n -> {
            n.setPublishByName(names.get(n.getPublishBy()));
            n.setCreateByName(names.get(n.getCreateBy()));
        });
    }

    private static List<List<Long>> chunks(List<Long> ids) {
        List<List<Long>> result = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += BATCH) {
            result.add(ids.subList(i, Math.min(ids.size(), i + BATCH)));
        }
        return result;
    }

    /** 站内消息的内容：正文去掉常见的 Markdown 标记后的前 100 个字符 */
    static String excerpt(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return null;
        }
        String text = markdown
                .replaceAll("!\\[[^\\]]*]\\([^)]*\\)", "")
                .replaceAll("\\[([^\\]]*)]\\([^)]*\\)", "$1")
                .replaceAll("(?m)^\\s{0,3}(#{1,6}|>|[-*+]|\\d+\\.)\\s+", "")
                .replaceAll("[*_`~|]", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.isEmpty()) {
            return null;
        }
        return text.length() > EXCERPT_MAX ? text.substring(0, EXCERPT_MAX) + "…" : text;
    }
}
