package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.dto.PasswordRequest;
import com.lemon.erp.modules.system.dto.ProfileInfo;
import com.lemon.erp.modules.system.dto.ProfileRequest;
import com.lemon.erp.modules.system.service.ProfileService;
import com.lemon.erp.modules.system.service.UserSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 个人中心，登录即可访问
 */
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserSettingService userSettingService;

    @GetMapping
    public R<ProfileInfo> profile() {
        return R.ok(profileService.profile());
    }

    @PutMapping
    @OperLog(title = "个人中心", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> update(@Valid @RequestBody ProfileRequest req) {
        profileService.update(req);
        return R.ok();
    }

    @PutMapping("/password")
    @OperLog(title = "个人中心-修改密码", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> changePassword(@Valid @RequestBody PasswordRequest req) {
        profileService.changePassword(req);
        return R.ok();
    }

    @PostMapping("/avatar")
    @OperLog(title = "个人中心-上传头像", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return R.ok(profileService.uploadAvatar(file));
    }

    /** 当前用户的个人设置（如 ui 界面设置），没有保存过时 data 为 null */
    @GetMapping("/settings/{key}")
    public R<Map<String, Object>> setting(@PathVariable String key) {
        return R.ok(userSettingService.get(key));
    }

    /** 保存当前用户的个人设置（整体替换）。界面设置修改后防抖保存，较频繁，不记操作日志 */
    @PutMapping("/settings/{key}")
    public R<Void> saveSetting(@PathVariable String key, @RequestBody(required = false) Map<String, Object> value) {
        userSettingService.save(key, value);
        return R.ok();
    }
}
