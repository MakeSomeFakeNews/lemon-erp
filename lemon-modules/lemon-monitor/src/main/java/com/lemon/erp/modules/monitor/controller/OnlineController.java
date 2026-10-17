package com.lemon.erp.modules.monitor.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.OnlineSession;
import com.lemon.erp.framework.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/monitor/online")
@RequiredArgsConstructor
public class OnlineController {

    private final TokenService tokenService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('monitor:online:list')")
    public R<List<OnlineSession>> list(@RequestParam(required = false) String username) {
        return R.ok(tokenService.listSessions().stream()
                .filter(s -> !StringUtils.hasText(username) || s.username().contains(username))
                .toList());
    }

    @DeleteMapping("/{tokenId}")
    @PreAuthorize("@ss.hasPerm('monitor:online:kick')")
    @OperLog(title = "在线用户", type = OperType.FORCE) // i18n-ignore 操作日志存库
    public R<Void> kick(@PathVariable String tokenId) {
        // 只能强退自己能看到的会话（SaaS 模式下为本租户的）
        tokenService.listSessions().stream()
                .filter(s -> s.tokenId().equals(tokenId))
                .findFirst()
                .ifPresent(s -> tokenService.removeSession(tokenId));
        return R.ok();
    }
}
