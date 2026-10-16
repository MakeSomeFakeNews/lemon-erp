package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.dto.PasswordRequest;
import com.lemon.erp.modules.system.dto.ProfileInfo;
import com.lemon.erp.modules.system.dto.ProfileRequest;
import com.lemon.erp.modules.system.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人中心，登录即可访问
 */
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public R<ProfileInfo> profile() {
        return R.ok(profileService.profile());
    }

    @PutMapping
    @OperLog(title = "个人中心", type = OperType.UPDATE)
    public R<Void> update(@Valid @RequestBody ProfileRequest req) {
        profileService.update(req);
        return R.ok();
    }

    @PutMapping("/password")
    @OperLog(title = "个人中心-修改密码", type = OperType.UPDATE)
    public R<Void> changePassword(@Valid @RequestBody PasswordRequest req) {
        profileService.changePassword(req);
        return R.ok();
    }

    @PostMapping("/avatar")
    @OperLog(title = "个人中心-上传头像", type = OperType.UPDATE)
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return R.ok(profileService.uploadAvatar(file));
    }
}
