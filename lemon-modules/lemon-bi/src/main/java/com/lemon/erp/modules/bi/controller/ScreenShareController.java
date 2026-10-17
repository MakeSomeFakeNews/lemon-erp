package com.lemon.erp.modules.bi.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.bi.dto.BiInsight;
import com.lemon.erp.modules.bi.dto.BiScreen;
import com.lemon.erp.modules.bi.service.BiScreenShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 投屏链接的免登录访问 /screen-share/{token}（SecurityConfig 中放行 GET），只读、只能访问链接中的大屏
 */
@RestController
@RequestMapping("/screen-share")
@RequiredArgsConstructor
public class ScreenShareController {

    private final BiScreenShareService shareService;

    @GetMapping("/{token}")
    public R<BiInsight.ShareInfo> info(@PathVariable String token) {
        return R.ok(shareService.publicInfo(token));
    }

    @GetMapping("/{token}/{key}")
    public R<BiScreen.ScreenData> data(@PathVariable String token, @PathVariable String key) {
        return R.ok(shareService.publicData(token, BiScreenController.parse(key)));
    }
}
