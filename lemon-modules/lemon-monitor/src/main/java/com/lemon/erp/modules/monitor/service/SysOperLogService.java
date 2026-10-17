package com.lemon.erp.modules.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.ip.IpLocationService;
import com.lemon.erp.framework.log.OperLogEvent;
import com.lemon.erp.framework.tenant.TenantProperties;
import com.lemon.erp.modules.monitor.entity.SysOperLog;
import com.lemon.erp.modules.monitor.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysOperLogService {

    private static final Set<String> LIST_EXCLUDED = Set.of("params", "result", "errorMsg");

    private final SysOperLogMapper operLogMapper;
    private final TenantProperties tenantProperties;
    private final IpLocationService ipLocationService;

    /** 监听切面发布的操作日志事件，异步落库 */
    @Async
    @EventListener
    public void onOperLog(OperLogEvent e) {
        SysOperLog log = new SysOperLog();
        log.setTitle(e.title());
        log.setOperType(e.operType());
        log.setMethod(e.method());
        log.setRequestMethod(e.requestMethod());
        log.setUrl(e.url());
        log.setIp(e.ip());
        log.setLocation(ipLocationService.getLocation(e.ip()));
        log.setOperator(e.operator());
        log.setParams(e.params());
        log.setResult(e.result());
        log.setStatus(e.status());
        log.setErrorMsg(e.errorMsg());
        log.setCostTime(e.costTime());
        log.setOperTime(e.operTime());
        operLogMapper.insert(log);
    }

    public PageResult<SysOperLog> page(long current, long size, String title, String operator, Integer operType,
                                       Integer status, LocalDate beginTime, LocalDate endTime) {
        Page<SysOperLog> page = operLogMapper.selectPage(Page.of(current, size), Wrappers.<SysOperLog>lambdaQuery()
                // 列表不需要请求参数/返回结果/错误信息等大字段，详情接口再按主键查询
                .select(SysOperLog.class, f -> !LIST_EXCLUDED.contains(f.getProperty()))
                .like(StringUtils.hasText(title), SysOperLog::getTitle, title)
                .like(StringUtils.hasText(operator), SysOperLog::getOperator, operator)
                .eq(operType != null, SysOperLog::getOperType, operType)
                .eq(status != null, SysOperLog::getStatus, status)
                .ge(beginTime != null, SysOperLog::getOperTime, beginTime == null ? null : beginTime.atStartOfDay())
                .lt(endTime != null, SysOperLog::getOperTime, endTime == null ? null : endTime.plusDays(1).atStartOfDay())
                .orderByDesc(SysOperLog::getId));
        return PageResult.of(page);
    }

    /**
     * 最近的操作日志（首页「最近操作」使用）：只查列表需要的列，按主键倒序取前 limit 条，不做 count
     */
    public List<SysOperLog> recent(int limit) {
        return operLogMapper.selectPage(Page.of(1, limit, false), Wrappers.<SysOperLog>lambdaQuery()
                .select(SysOperLog::getId, SysOperLog::getTitle, SysOperLog::getOperType, SysOperLog::getOperator,
                        SysOperLog::getStatus, SysOperLog::getOperTime, SysOperLog::getCostTime)
                .orderByDesc(SysOperLog::getId)).getRecords();
    }

    public SysOperLog detail(Long id) {
        SysOperLog log = operLogMapper.selectById(id);
        if (log == null) {
            throw new BizException("monitor.error.logDoesNotExist");
        }
        return log;
    }

    public void delete(List<Long> ids) {
        operLogMapper.deleteByIds(ids);
    }

    public void clean() {
        if (tenantProperties.isEnabled()) {
            operLogMapper.deleteAll();
        } else {
            operLogMapper.truncate();
        }
    }
}
