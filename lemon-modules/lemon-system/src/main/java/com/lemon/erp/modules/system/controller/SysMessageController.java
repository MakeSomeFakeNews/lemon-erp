package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.modules.system.entity.SysMessage;
import com.lemon.erp.modules.system.service.SysMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 我的消息：登录即可访问，只能操作自己的消息
 */
@RestController
@RequestMapping("/system/message")
@RequiredArgsConstructor
public class SysMessageController {

    private final SysMessageService messageService;

    @GetMapping
    public R<PageResult<SysMessage>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) Boolean unread,
                                          @RequestParam(required = false) MessageCategory category) {
        return R.ok(messageService.mine(current, size, unread, category));
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(messageService.unreadCount());
    }

    /** 标为已读，body 为消息 ID 列表；空列表表示全部已读 */
    @PutMapping("/read")
    public R<Void> read(@RequestBody(required = false) List<Long> ids) {
        messageService.read(ids);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> delete(@PathVariable List<Long> ids) {
        messageService.delete(ids);
        return R.ok();
    }
}
