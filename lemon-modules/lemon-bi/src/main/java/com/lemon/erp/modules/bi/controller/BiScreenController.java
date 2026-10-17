package com.lemon.erp.modules.bi.controller;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.bi.dto.ScreenShareRequest;
import com.lemon.erp.modules.bi.entity.BiScreenShare;
import com.lemon.erp.modules.bi.model.ScreenKey;
import com.lemon.erp.modules.bi.service.BiScreenService;
import com.lemon.erp.modules.bi.service.BiScreenShareService;
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
import java.util.Locale;
import java.util.Map;

/**
 * 数据大屏 /bi/screen：大屏数据、投屏链接管理。大屏汇总全公司数据，与经营驾驶舱一样只对有「全部数据」权限的用户开放；
 * 创建投屏链接相当于把全公司数据授权给电视查看，同样需要「全部数据」
 */
@RestController
@RequestMapping("/bi/screen")
@RequiredArgsConstructor
public class BiScreenController {

    private final BiScreenService screenService;
    private final BiScreenShareService shareService;

    /** 投屏链接 */
    @GetMapping("/share")
    @PreAuthorize("@ss.hasPerm('bi:screen:share')")
    public R<List<BiScreenShare>> shares() {
        requireAllData();
        return R.ok(shareService.list());
    }

    @PostMapping("/share")
    @PreAuthorize("@ss.hasPerm('bi:screen:share')")
    @OperLog(title = "投屏链接", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Long> createShare(@Valid @RequestBody ScreenShareRequest req) {
        requireAllData();
        return R.ok(shareService.create(req));
    }

    @PutMapping("/share/{id}")
    @PreAuthorize("@ss.hasPerm('bi:screen:share')")
    @OperLog(title = "投屏链接", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> updateShare(@PathVariable Long id, @Valid @RequestBody ScreenShareRequest req) {
        requireAllData();
        shareService.update(id, req);
        return R.ok();
    }

    /** 重新生成链接，原链接失效 */
    @PostMapping("/share/{id}/reset")
    @PreAuthorize("@ss.hasPerm('bi:screen:share')")
    @OperLog(title = "投屏链接", type = OperType.UPDATE, saveResult = false)  // i18n-ignore 操作日志存库
    public R<String> resetShare(@PathVariable Long id) {
        requireAllData();
        return R.ok(shareService.resetToken(id));
    }

    @DeleteMapping("/share/{id}")
    @PreAuthorize("@ss.hasPerm('bi:screen:share')")
    @OperLog(title = "投屏链接", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> deleteShare(@PathVariable Long id) {
        requireAllData();
        shareService.delete(id);
        return R.ok();
    }

    /** 大屏数据：key 为 factory / progress / warehouse / sales / quality */
    @GetMapping("/{key}")
    @PreAuthorize("@ss.hasPerm('bi:screen:list')")
    public R<Map<String, Object>> data(@PathVariable String key, @RequestParam(required = false) Long workCenterId) {
        requireAllData();
        ScreenKey screen = parse(key);
        return R.ok(screenService.data(screen, screen.isByWorkCenter() ? workCenterId : null));
    }

    private static void requireAllData() {
        if (!SecurityUtils.getDataScope().all()) {
            throw new BizException("bi.error.dataScreensAggregateCompanyWideData");
        }
    }

    static ScreenKey parse(String key) {
        try {
            return ScreenKey.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BizException("bi.error.dataScreenNotFound", key);
        }
    }
}
