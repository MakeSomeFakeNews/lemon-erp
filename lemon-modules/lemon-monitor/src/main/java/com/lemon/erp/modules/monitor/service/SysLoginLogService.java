package com.lemon.erp.modules.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.ip.IpLocationService;
import com.lemon.erp.framework.log.LoginLogEvent;
import com.lemon.erp.framework.tenant.TenantProperties;
import com.lemon.erp.modules.monitor.entity.SysLoginLog;
import com.lemon.erp.modules.monitor.mapper.SysLoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysLoginLogService {

    private final SysLoginLogMapper loginLogMapper;
    private final TenantProperties tenantProperties;
    private final IpLocationService ipLocationService;

    /** 监听登录事件，异步落库 */
    @Async
    @EventListener
    public void onLogin(LoginLogEvent e) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(e.username());
        log.setIp(e.ip());
        log.setLocation(ipLocationService.getLocation(e.ip()));
        log.setBrowser(e.browser());
        log.setOs(e.os());
        log.setStatus(e.status());
        log.setMsg(e.msg());
        log.setLoginTime(e.loginTime());
        loginLogMapper.insert(log);
    }

    public PageResult<SysLoginLog> page(long current, long size, String username, String ip, String location, Integer status,
                                        LocalDate beginTime, LocalDate endTime) {
        Page<SysLoginLog> page = loginLogMapper.selectPage(Page.of(current, size), Wrappers.<SysLoginLog>lambdaQuery()
                .like(StringUtils.hasText(username), SysLoginLog::getUsername, username)
                .like(StringUtils.hasText(ip), SysLoginLog::getIp, ip)
                .like(StringUtils.hasText(location), SysLoginLog::getLocation, location)
                .eq(status != null, SysLoginLog::getStatus, status)
                .ge(beginTime != null, SysLoginLog::getLoginTime, beginTime == null ? null : beginTime.atStartOfDay())
                .lt(endTime != null, SysLoginLog::getLoginTime, endTime == null ? null : endTime.plusDays(1).atStartOfDay())
                .orderByDesc(SysLoginLog::getId));
        return PageResult.of(page);
    }

    public void delete(List<Long> ids) {
        loginLogMapper.deleteByIds(ids);
    }

    public void clean() {
        if (tenantProperties.isEnabled()) {
            loginLogMapper.deleteAll();
        } else {
            loginLogMapper.truncate();
        }
    }
}
