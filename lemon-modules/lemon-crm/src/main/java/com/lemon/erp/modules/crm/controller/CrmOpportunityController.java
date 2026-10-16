package com.lemon.erp.modules.crm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.crm.dto.OppOrderRequest;
import com.lemon.erp.modules.crm.dto.OpportunityRequest;
import com.lemon.erp.modules.crm.dto.StageRequest;
import com.lemon.erp.modules.crm.entity.CrmOpportunity;
import com.lemon.erp.modules.crm.entity.CrmStageLog;
import com.lemon.erp.modules.crm.service.CrmOpportunityService;
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
 * 销售机会 /crm/opportunity
 */
@RestController
@RequestMapping("/crm/opportunity")
@RequiredArgsConstructor
public class CrmOpportunityController {

    private final CrmOpportunityService opportunityService;

    /** @param status OPEN 进行中 / WON / LOST；mine 只看我负责的 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('crm:opp:list')")
    public R<PageResult<CrmOpportunity>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String stage,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) Long customerId,
                                              @RequestParam(required = false) Long campaignId,
                                              @RequestParam(defaultValue = "false") boolean mine) {
        return R.ok(opportunityService.page(current, size, keyword, stage, status, customerId,
                mine ? SecurityUtils.getUserId() : null, campaignId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('crm:opp:list')")
    public R<CrmOpportunity> get(@PathVariable Long id) {
        return R.ok(opportunityService.detail(id));
    }

    @GetMapping("/{id}/stages")
    @PreAuthorize("@ss.hasPerm('crm:opp:list')")
    public R<List<CrmStageLog>> stages(@PathVariable Long id) {
        return R.ok(opportunityService.stageLogs(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('crm:opp:add')")
    @OperLog(title = "销售机会", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody OpportunityRequest req) {
        return R.ok(opportunityService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('crm:opp:edit')")
    @OperLog(title = "销售机会", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody OpportunityRequest req) {
        opportunityService.update(id, req);
        return R.ok();
    }

    @PutMapping("/{id}/stage")
    @PreAuthorize("@ss.hasPerm('crm:opp:edit')")
    @OperLog(title = "销售机会-推进阶段", type = OperType.UPDATE)
    public R<Void> changeStage(@PathVariable Long id, @Valid @RequestBody StageRequest req) {
        opportunityService.changeStage(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('crm:opp:remove')")
    @OperLog(title = "销售机会", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        opportunityService.delete(ids);
        return R.ok();
    }

    /** 赢单商机生成销售订单（草稿），返回订单 ID */
    @PostMapping("/{id}/order")
    @PreAuthorize("@ss.hasPerm('crm:opp:order')")
    @OperLog(title = "销售机会-生成销售订单", type = OperType.INSERT)
    public R<Long> createOrder(@PathVariable Long id, @RequestBody OppOrderRequest req) {
        return R.ok(opportunityService.createOrder(id, req));
    }
}
