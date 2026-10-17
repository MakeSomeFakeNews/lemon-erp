package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.dto.ImBindingQuery;
import com.lemon.erp.modules.system.dto.ImBindingRow;
import com.lemon.erp.modules.system.dto.ImMyBinding;
import com.lemon.erp.modules.system.dto.ImSyncResult;
import com.lemon.erp.modules.system.entity.SysImConfig;
import com.lemon.erp.modules.system.im.ImPlatform;
import com.lemon.erp.modules.system.service.SysImAuthService;
import com.lemon.erp.modules.system.service.SysImConfigService;
import com.lemon.erp.modules.system.service.SysImSyncService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
 * 第三方集成（钉钉、企业微信、飞书）：应用配置、通讯录同步、成员绑定，以及个人中心的账号绑定
 */
@RestController
@RequestMapping("/system/im")
@RequiredArgsConstructor
public class SysImController {

    private final SysImConfigService configService;
    private final SysImSyncService syncService;
    private final SysImAuthService authService;

    @GetMapping("/config")
    @PreAuthorize("@ss.hasPerm('sys:im:list')")
    public R<List<SysImConfig>> configs() {
        return R.ok(configService.list());
    }

    @PutMapping("/config")
    @PreAuthorize("@ss.hasPerm('sys:im:edit')")
    @OperLog(title = "第三方集成", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> save(@Valid @RequestBody SysImConfig config) {
        configService.save(config);
        return R.ok();
    }

    /** 校验应用凭证，已绑定时给自己发一条测试消息 */
    @PostMapping("/{platform}/verify")
    @PreAuthorize("@ss.hasPerm('sys:im:edit')")
    public R<String> verify(@PathVariable ImPlatform platform) {
        return R.ok(configService.verify(platform));
    }

    @PostMapping("/{platform}/robot-test")
    @PreAuthorize("@ss.hasPerm('sys:im:edit')")
    public R<Void> robotTest(@PathVariable ImPlatform platform) {
        configService.robotTest(platform);
        return R.ok();
    }

    @PostMapping("/{platform}/sync")
    @PreAuthorize("@ss.hasPerm('sys:im:sync')")
    @OperLog(title = "同步通讯录", type = OperType.IMPORT) // i18n-ignore 操作日志存库
    public R<ImSyncResult> sync(@PathVariable ImPlatform platform) {
        return R.ok(syncService.sync(platform));
    }

    @GetMapping("/bindings")
    @PreAuthorize("@ss.hasPerm('sys:im:list')")
    public R<PageResult<ImBindingRow>> bindings(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                ImBindingQuery query) {
        return R.ok(syncService.bindings(current, size, query));
    }

    public record BindRequest(@NotNull(message = "{system.valid.userRequired}") Long userId, @NotNull(message = "{system.valid.platformRequired}") ImPlatform platform,
                              String imUserId) {
    }

    @PostMapping("/bindings")
    @PreAuthorize("@ss.hasPerm('sys:im:sync')")
    @OperLog(title = "成员绑定", type = OperType.INSERT) // i18n-ignore 操作日志存库
    public R<Void> bind(@Valid @RequestBody BindRequest req) {
        syncService.bind(req.userId(), req.platform(), req.imUserId());
        return R.ok();
    }

    @DeleteMapping("/bindings/{id}")
    @PreAuthorize("@ss.hasPerm('sys:im:sync')")
    @OperLog(title = "成员绑定", type = OperType.DELETE) // i18n-ignore 操作日志存库
    public R<Void> unbind(@PathVariable Long id) {
        syncService.unbind(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 个人中心：本人的账号绑定（登录即可）

    @GetMapping("/mine")
    public R<List<ImMyBinding>> mine() {
        return R.ok(authService.myBindings());
    }

    /** 绑定的授权页地址：授权后回到回调页完成绑定 */
    @GetMapping("/mine/{platform}/bind-url")
    public R<String> bindUrl(@PathVariable ImPlatform platform) {
        return R.ok(authService.bindUrl(platform));
    }

    @DeleteMapping("/mine/{platform}")
    public R<Void> unbindMine(@PathVariable ImPlatform platform) {
        authService.unbindMine(platform);
        return R.ok();
    }
}
