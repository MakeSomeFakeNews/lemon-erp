package com.lemon.erp.modules.crm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.crm.dto.LeadConvertRequest;
import com.lemon.erp.modules.crm.dto.LeadRequest;
import com.lemon.erp.modules.crm.entity.CrmLead;
import com.lemon.erp.modules.crm.service.CrmLeadService;
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
import java.util.Map;

/**
 * 线索管理 /crm/lead
 */
@RestController
@RequestMapping("/crm/lead")
@RequiredArgsConstructor
public class CrmLeadController {

    private final CrmLeadService leadService;

    /** @param mine 只看我负责的 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('crm:lead:list')")
    public R<PageResult<CrmLead>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String source,
                                       @RequestParam(required = false) Long campaignId,
                                       @RequestParam(defaultValue = "false") boolean mine) {
        return R.ok(leadService.page(current, size, keyword, status, source, campaignId, mine ? SecurityUtils.getUserId() : null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('crm:lead:list')")
    public R<CrmLead> get(@PathVariable Long id) {
        return R.ok(leadService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('crm:lead:add')")
    @OperLog(title = "线索管理", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody LeadRequest req) {
        return R.ok(leadService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('crm:lead:edit')")
    @OperLog(title = "线索管理", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody LeadRequest req) {
        leadService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('crm:lead:remove')")
    @OperLog(title = "线索管理", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        leadService.delete(ids);
        return R.ok();
    }

    /** 标记无效，body：{ reason } */
    @PostMapping("/{id}/invalid")
    @PreAuthorize("@ss.hasPerm('crm:lead:edit')")
    @OperLog(title = "线索管理-无效", type = OperType.UPDATE)
    public R<Void> invalidate(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        leadService.invalidate(id, body == null ? null : body.get("reason"));
        return R.ok();
    }

    /** 转化为客户（可同时生成商机），返回 { customerId, opportunityId } */
    @PostMapping("/{id}/convert")
    @PreAuthorize("@ss.hasPerm('crm:lead:convert')")
    @OperLog(title = "线索管理-转化", type = OperType.INSERT)
    public R<Map<String, Long>> convert(@PathVariable Long id, @Valid @RequestBody LeadConvertRequest req) {
        return R.ok(leadService.convert(id, req));
    }
}
