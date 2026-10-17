package com.lemon.erp.modules.monitor.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.monitor.dto.CacheInfo;
import com.lemon.erp.modules.monitor.dto.CacheName;
import com.lemon.erp.modules.monitor.dto.CacheValue;
import com.lemon.erp.modules.monitor.service.CacheMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/monitor/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheMonitorService cacheMonitorService;

    /** 缓存服务运行信息（Redis INFO、key 数量、命令统计） */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('monitor:cache:list')")
    public R<CacheInfo> info() {
        return R.ok(cacheMonitorService.getInfo());
    }

    @GetMapping("/names")
    @PreAuthorize("@ss.hasPerm('monitor:cache:list')")
    public R<List<CacheName>> names() {
        return R.ok(cacheMonitorService.listNames());
    }

    @GetMapping("/keys")
    @PreAuthorize("@ss.hasPerm('monitor:cache:list')")
    public R<List<String>> keys(@RequestParam String prefix) {
        return R.ok(cacheMonitorService.listKeys(prefix));
    }

    @GetMapping("/value")
    @PreAuthorize("@ss.hasPerm('monitor:cache:list')")
    public R<CacheValue> value(@RequestParam String key) {
        return R.ok(cacheMonitorService.getValue(key));
    }

    @DeleteMapping("/names")
    @PreAuthorize("@ss.hasPerm('monitor:cache:remove')")
    @OperLog(title = "缓存监控-清空缓存", type = OperType.CLEAN) // i18n-ignore 操作日志存库
    public R<Void> clear(@RequestParam String prefix) {
        cacheMonitorService.clearByPrefix(prefix);
        return R.ok();
    }

    @DeleteMapping("/keys")
    @PreAuthorize("@ss.hasPerm('monitor:cache:remove')")
    @OperLog(title = "缓存监控-删除缓存", type = OperType.DELETE) // i18n-ignore 操作日志存库
    public R<Void> deleteKey(@RequestParam String key) {
        cacheMonitorService.deleteKey(key);
        return R.ok();
    }
}
