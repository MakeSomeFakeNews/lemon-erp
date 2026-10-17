package com.lemon.erp.modules.crm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.crm.dto.AssignRequest;
import com.lemon.erp.modules.crm.dto.CrmAnalysis;
import com.lemon.erp.modules.crm.dto.CrmCustomerRequest;
import com.lemon.erp.modules.crm.entity.CrmContact;
import com.lemon.erp.modules.crm.entity.CrmCustomer;
import com.lemon.erp.modules.crm.service.CrmCustomerService;
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
 * 客户管理 /crm/customer、联系人 /crm/contact
 */
@RestController
@RequestMapping("/crm")
@RequiredArgsConstructor
public class CrmCustomerController {

    private final CrmCustomerService customerService;

    /**
     * @param scope    mine 我的客户 / team 团队客户（数据权限）/ pool 公海
     * @param dueToday 只看今天需要跟进的
     */
    @GetMapping("/customer")
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    public R<PageResult<CrmCustomer>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(defaultValue = "mine") String scope,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String stage,
                                           @RequestParam(required = false) String industry,
                                           @RequestParam(required = false) Long ownerId,
                                           @RequestParam(defaultValue = "false") boolean dueToday) {
        if ("pool".equals(scope)) {
            return R.ok(customerService.pool(current, size, keyword, stage, industry));
        }
        return R.ok(customerService.page(current, size, "mine".equals(scope), keyword, stage, industry, ownerId, dueToday));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    public R<CrmCustomer> get(@PathVariable Long customerId) {
        return R.ok(customerService.detail(customerId));
    }

    @GetMapping("/customer/{customerId}/summary")
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    public R<CrmAnalysis.CustomerSummary> summary(@PathVariable Long customerId) {
        return R.ok(customerService.summary(customerId));
    }

    @PostMapping("/customer")
    @PreAuthorize("@ss.hasPerm('crm:customer:add')")
    @OperLog(title = "客户管理", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody CrmCustomerRequest req) {
        return R.ok(customerService.create(req));
    }

    /** 负责人本人或有修改权限 */
    @PutMapping("/customer/{customerId}")
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    @OperLog(title = "客户管理", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long customerId, @Valid @RequestBody CrmCustomerRequest req) {
        customerService.update(customerId, req);
        return R.ok();
    }

    @PostMapping("/customer/assign")
    @PreAuthorize("@ss.hasPerm('crm:customer:assign')")
    @OperLog(title = "客户管理-分配", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> assign(@Valid @RequestBody AssignRequest req) {
        customerService.assign(req.customerIds(), req.ownerId());
        return R.ok();
    }

    /** 领取公海客户，返回领取成功的数量 */
    @PostMapping("/customer/claim")
    @PreAuthorize("@ss.hasPerm('crm:customer:claim')")
    @OperLog(title = "客户管理-领取", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Integer> claim(@RequestBody List<Long> customerIds) {
        return R.ok(customerService.claim(customerIds));
    }

    /** 放回公海：负责人本人或有分配权限 */
    @PostMapping("/customer/release")
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    @OperLog(title = "客户管理-放回公海", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> release(@RequestBody List<Long> customerIds) {
        customerService.release(customerIds);
        return R.ok();
    }

    // ---------------------------------------------------------------- 联系人（负责人本人或有客户修改权限）

    @GetMapping("/contact")
    @PreAuthorize("@ss.hasAnyPerm('crm:customer:list', 'crm:opp:list', 'crm:ticket:list')")
    public R<List<CrmContact>> contacts(@RequestParam Long customerId) {
        return R.ok(customerService.contacts(customerId));
    }

    @PostMapping("/contact")
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    @OperLog(title = "联系人", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Void> createContact(@Valid @RequestBody CrmContact contact) {
        customerService.saveContact(null, contact);
        return R.ok();
    }

    @PutMapping("/contact/{id}")
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    @OperLog(title = "联系人", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> updateContact(@PathVariable Long id, @Valid @RequestBody CrmContact contact) {
        customerService.saveContact(id, contact);
        return R.ok();
    }

    @DeleteMapping("/contact/{id}")
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    @OperLog(title = "联系人", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> deleteContact(@PathVariable Long id) {
        customerService.deleteContact(id);
        return R.ok();
    }
}
