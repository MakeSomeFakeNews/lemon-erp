package com.lemon.erp.modules.crm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.crm.dto.FollowupRequest;
import com.lemon.erp.modules.crm.entity.CrmFollowup;
import com.lemon.erp.modules.crm.service.CrmFollowupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 跟进记录 /crm/followup
 */
@RestController
@RequestMapping("/crm/followup")
@RequiredArgsConstructor
public class CrmFollowupController {

    private final CrmFollowupService followupService;

    /** 客户、线索、商机详情中的跟进记录也使用该接口 */
    @GetMapping
    @PreAuthorize("@ss.hasAnyPerm('crm:followup:list', 'crm:customer:list', 'crm:lead:list', 'crm:opp:list')")
    public R<PageResult<CrmFollowup>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String targetType,
                                           @RequestParam(required = false) Long targetId,
                                           @RequestParam(required = false) Long customerId,
                                           @RequestParam(defaultValue = "false") boolean mine,
                                           @RequestParam(required = false) String method,
                                           @RequestParam(required = false) LocalDate beginDate,
                                           @RequestParam(required = false) LocalDate endDate) {
        return R.ok(followupService.page(current, size, targetType, targetId, customerId, mine, method, beginDate, endDate));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('crm:followup:add')")
    @OperLog(title = "跟进记录", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody FollowupRequest req) {
        return R.ok(followupService.create(req));
    }

    /** 本人的跟进或有删除权限 */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('crm:followup:list')")
    @OperLog(title = "跟进记录", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable Long id) {
        followupService.delete(id);
        return R.ok();
    }
}
