package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.CreditUsage;
import com.lemon.erp.modules.scm.dto.PartnerOption;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.model.SalesChannel;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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

@RestController
@RequestMapping("/scm/customer")
@RequiredArgsConstructor
public class ScmCustomerController {

    private final ScmCustomerService customerService;
    private final ScmOrderService orderService;
    private final ImportExecutor importExecutor;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:customer:list')")
    public R<PageResult<ScmCustomer>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String level,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) SalesChannel channel) {
        return R.ok(customerService.page(current, size, keyword, level, status, channel));
    }

    /** 登录即可访问：订单等页面按编码或名称搜索 */
    @GetMapping("/options")
    public R<List<PartnerOption>> options(@RequestParam(required = false) String keyword) {
        return R.ok(customerService.options(keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:customer:list')")
    public R<ScmCustomer> get(@PathVariable Long id) {
        return R.ok(customerService.get(id));
    }

    /** 信用占用：额度、应收余额、未完成订单金额（客户管理、CRM 客户详情） */
    @GetMapping("/{id}/credit")
    @PreAuthorize("@ss.hasAnyPerm('scm:customer:list', 'crm:customer:list')")
    public R<CreditUsage> credit(@PathVariable Long id) {
        return R.ok(orderService.creditUsage(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:customer:add')")
    @OperLog(title = "客户管理", type = OperType.INSERT)
    public R<Void> create(@Validated(Create.class) @RequestBody ScmCustomer customer) {
        customerService.create(customer);
        return R.ok();
    }


    /** Excel 导入：逐行按新增校验，任何一行出错整批不导入 */
    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('scm:customer:add')")
    @OperLog(title = "客户管理-导入", type = OperType.IMPORT)
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<ScmCustomer> req) {
        return R.ok(importExecutor.run(req, customerService::create, Create.class));
    }
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:customer:edit')")
    @OperLog(title = "客户管理", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody ScmCustomer customer) {
        customerService.update(id, customer);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('scm:customer:edit')")
    @OperLog(title = "客户管理-修改状态", type = OperType.UPDATE)
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        customerService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:customer:remove')")
    @OperLog(title = "客户管理", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        customerService.delete(ids);
        return R.ok();
    }
}
