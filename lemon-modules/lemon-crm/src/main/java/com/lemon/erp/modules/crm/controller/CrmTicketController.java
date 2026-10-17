package com.lemon.erp.modules.crm.controller;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.crm.dto.TicketActionRequest;
import com.lemon.erp.modules.crm.dto.TicketChargeRequest;
import com.lemon.erp.modules.crm.dto.TicketPartsRequest;
import com.lemon.erp.modules.crm.dto.TicketRequest;
import com.lemon.erp.modules.crm.dto.WarrantyInfo;
import com.lemon.erp.modules.crm.entity.CrmTicket;
import com.lemon.erp.modules.crm.entity.CrmTicketLog;
import com.lemon.erp.modules.crm.entity.CrmTicketPart;
import com.lemon.erp.modules.crm.service.CrmTicketService;
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

/**
 * 服务工单 /crm/ticket
 */
@RestController
@RequestMapping("/crm/ticket")
@RequiredArgsConstructor
public class CrmTicketController {

    private final CrmTicketService ticketService;

    /** @param mine 只看指派给我的；overdue 只看超时未解决的 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('crm:ticket:list')")
    public R<PageResult<CrmTicket>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) String priority,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) Long customerId,
                                         @RequestParam(defaultValue = "false") boolean mine,
                                         @RequestParam(required = false) Boolean overdue) {
        return R.ok(ticketService.page(current, size, keyword, type, priority, status, customerId,
                mine ? SecurityUtils.getUserId() : null, overdue));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('crm:ticket:list')")
    public R<CrmTicket> get(@PathVariable Long id) {
        return R.ok(ticketService.detail(id));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("@ss.hasPerm('crm:ticket:list')")
    public R<List<CrmTicketLog>> logs(@PathVariable Long id) {
        return R.ok(ticketService.logs(id));
    }

    /** 按序列号查询销售出库与保修信息，没有记录时 data 为空 */
    @GetMapping("/warranty")
    @PreAuthorize("@ss.hasPerm('crm:ticket:list')")
    public R<WarrantyInfo> warranty(@RequestParam String serialNo) {
        return R.ok(ticketService.warranty(serialNo));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('crm:ticket:add')")
    @OperLog(title = "服务工单", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody TicketRequest req) {
        return R.ok(ticketService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('crm:ticket:handle')")
    @OperLog(title = "服务工单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TicketRequest req) {
        ticketService.update(id, req);
        return R.ok();
    }

    /** 处理动作：assign 指派 / process 处理记录 / resolve 解决 / close 关闭 / reopen 重新打开 */
    @GetMapping("/{id}/parts")
    @PreAuthorize("@ss.hasPerm('crm:ticket:list')")
    public R<List<CrmTicketPart>> parts(@PathVariable Long id) {
        return R.ok(ticketService.parts(id));
    }

    /** 领用备件：生成服务领用出库单 */
    @PostMapping("/{id}/parts")
    @PreAuthorize("@ss.hasPerm('crm:ticket:handle')")
    @OperLog(title = "服务工单-领用备件", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> issueParts(@PathVariable Long id, @Valid @RequestBody TicketPartsRequest req) {
        return R.ok(ticketService.issueParts(id, req));
    }

    /** 服务收费：生成应收单 */
    @PostMapping("/{id}/charge")
    @PreAuthorize("@ss.hasPerm('crm:ticket:handle')")
    @OperLog(title = "服务工单-收费", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Void> charge(@PathVariable Long id, @Valid @RequestBody TicketChargeRequest req) {
        ticketService.charge(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/{action}")
    @PreAuthorize("@ss.hasPerm('crm:ticket:handle')")
    @OperLog(title = "服务工单-处理", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> action(@PathVariable Long id, @PathVariable String action, @Valid @RequestBody TicketActionRequest req) {
        switch (action) {
            case "assign" -> ticketService.assign(id, req);
            case "process" -> ticketService.process(id, req);
            case "resolve" -> ticketService.resolve(id, req);
            case "close" -> ticketService.close(id, req);
            case "reopen" -> ticketService.reopen(id, req);
            default -> throw new BizException("不支持的操作：{0}", action);
        }
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('crm:ticket:remove')")
    @OperLog(title = "服务工单", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        ticketService.delete(ids);
        return R.ok();
    }
}
