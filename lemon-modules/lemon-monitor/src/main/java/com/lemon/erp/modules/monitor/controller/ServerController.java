package com.lemon.erp.modules.monitor.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.monitor.dto.ServerInfo;
import com.lemon.erp.modules.monitor.service.ServerMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor/server")
@RequiredArgsConstructor
public class ServerController {

    private final ServerMonitorService serverMonitorService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('monitor:server:list')")
    public R<ServerInfo> info() {
        return R.ok(serverMonitorService.getServerInfo());
    }
}
