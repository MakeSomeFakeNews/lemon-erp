package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.dto.NoticeOptions;
import com.lemon.erp.modules.system.dto.NoticeReader;
import com.lemon.erp.modules.system.dto.NoticeRequest;
import com.lemon.erp.modules.system.entity.SysNotice;
import com.lemon.erp.modules.system.model.NoticeStatus;
import com.lemon.erp.modules.system.service.SysNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知公告：管理端（sys:notice:*）维护、发布、撤回、查看阅读情况；员工端（/my，登录即可）只看发给自己的有效公告
 */
@RestController
@RequestMapping("/system/notice")
@RequiredArgsConstructor
public class SysNoticeController {

    private final SysNoticeService noticeService;

    // ---------------------------------------------------------------- 员工端

    @GetMapping("/my")
    public R<PageResult<SysNotice>> mine(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(defaultValue = "false") boolean unread,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String type) {
        return R.ok(noticeService.mine(current, size, unread, keyword, type));
    }

    @GetMapping("/my/unread-count")
    public R<Long> unreadCount() {
        return R.ok(noticeService.unreadCount());
    }

    @GetMapping("/my/{id}")
    public R<SysNotice> mineDetail(@PathVariable Long id) {
        return R.ok(noticeService.mineDetail(id));
    }

    /** 标为已读（打开详情时调用；演示模式下放行） */
    @PutMapping("/my/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        noticeService.read(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 管理端

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:notice:list')")
    public R<PageResult<SysNotice>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String title,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) NoticeStatus status) {
        return R.ok(noticeService.page(current, size, title, type, status));
    }

    /** 选择发布范围用的部门树和角色 */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasAnyPerm('sys:notice:add', 'sys:notice:edit')")
    public R<NoticeOptions> options() {
        return R.ok(noticeService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:notice:list')")
    public R<SysNotice> detail(@PathVariable Long id) {
        return R.ok(noticeService.detail(id));
    }

    /**
     * 接收人名单
     *
     * @param read true 已读，false 未读，不传为全部
     */
    @GetMapping("/{id}/readers")
    @PreAuthorize("@ss.hasPerm('sys:notice:list')")
    public R<PageResult<NoticeReader>> readers(@PathVariable Long id,
                                               @RequestParam(required = false) Boolean read,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size) {
        return R.ok(noticeService.readers(id, read, keyword, current, size));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:notice:add')")
    @OperLog(title = "通知公告", type = OperType.INSERT) // i18n-ignore 操作日志存库
    public R<Long> create(@Valid @RequestBody NoticeRequest req) {
        return R.ok(noticeService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:notice:edit')")
    @OperLog(title = "通知公告", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeRequest req) {
        noticeService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:notice:remove')")
    @OperLog(title = "通知公告", type = OperType.DELETE) // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        noticeService.delete(ids);
        return R.ok();
    }

    /** 发布，返回接收人数 */
    @PutMapping("/{id}/publish")
    @PreAuthorize("@ss.hasPerm('sys:notice:publish')")
    @OperLog(title = "发布公告", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Integer> publish(@PathVariable Long id) {
        return R.ok(noticeService.publish(id));
    }

    @PutMapping("/{id}/withdraw")
    @PreAuthorize("@ss.hasPerm('sys:notice:publish')")
    @OperLog(title = "撤回公告", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> withdraw(@PathVariable Long id) {
        noticeService.withdraw(id);
        return R.ok();
    }
}
