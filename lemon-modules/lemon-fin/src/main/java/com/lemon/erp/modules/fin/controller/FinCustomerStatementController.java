package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.StatementRequests;
import com.lemon.erp.modules.fin.entity.FinCustomerStatement;
import com.lemon.erp.modules.fin.entity.FinCustomerStatementLine;
import com.lemon.erp.modules.fin.model.StatementStatus;
import com.lemon.erp.modules.fin.service.FinCustomerStatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户对账（issue #134）：财务（/fin/customer-statement）与经销商门户（/dealer/statement）。
 * 明细接口传 size 时分页，不传时返回全部（打印、导出）
 */
@RestController
@RequiredArgsConstructor
public class FinCustomerStatementController {

    private final FinCustomerStatementService statementService;

    @GetMapping("/fin/customer-statement")
    @PreAuthorize("@ss.hasPerm('fin:customerStatement:list')")
    public R<PageResult<FinCustomerStatement>> page(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Long customerId,
                                                    @RequestParam(required = false) StatementStatus status,
                                                    @RequestParam(required = false) String keyword) {
        return R.ok(statementService.page(current, size, customerId, status, keyword, false));
    }

    @GetMapping("/fin/customer-statement/{id}")
    @PreAuthorize("@ss.hasPerm('fin:customerStatement:list')")
    public R<FinCustomerStatement> detail(@PathVariable Long id) {
        return R.ok(statementService.detail(id, null));
    }

    @GetMapping("/fin/customer-statement/{id}/lines")
    @PreAuthorize("@ss.hasPerm('fin:customerStatement:list')")
    public R<PageResult<FinCustomerStatementLine>> lines(@PathVariable Long id, @RequestParam(required = false) Long current,
                                                         @RequestParam(required = false) Long size) {
        return R.ok(statementService.lines(id, null, current, size));
    }

    @PostMapping("/fin/customer-statement")
    @PreAuthorize("@ss.hasPerm('fin:customerStatement:add')")
    @OperLog(title = "客户对账单", type = OperType.INSERT) // i18n-ignore
    public R<Long> generate(@Valid @RequestBody StatementRequests.CustomerGenerate req) {
        return R.ok(statementService.generate(req));
    }

    @PostMapping("/fin/customer-statement/{id}/send")
    @PreAuthorize("@ss.hasPerm('fin:customerStatement:edit')")
    @OperLog(title = "客户对账单-发送", type = OperType.UPDATE) // i18n-ignore
    public R<Void> send(@PathVariable Long id) {
        statementService.send(id);
        return R.ok();
    }

    /** 登记客户签回的结果（没有经销商门户账号的客户） */
    @PostMapping("/fin/customer-statement/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('fin:customerStatement:edit')")
    @OperLog(title = "客户对账单-登记确认结果", type = OperType.UPDATE) // i18n-ignore
    public R<Void> register(@PathVariable Long id, @Valid @RequestBody StatementRequests.Respond req) {
        statementService.confirm(id, null, req);
        return R.ok();
    }

    @DeleteMapping("/fin/customer-statement/{id}")
    @PreAuthorize("@ss.hasPerm('fin:customerStatement:remove')")
    @OperLog(title = "客户对账单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable Long id) {
        statementService.delete(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 经销商门户

    @GetMapping("/dealer/statement")
    @PreAuthorize("@ss.hasPerm('dealer:statement:list')")
    public R<PageResult<FinCustomerStatement>> dealerPage(@RequestParam(defaultValue = "1") long current,
                                                          @RequestParam(defaultValue = "10") long size,
                                                          @RequestParam(required = false) StatementStatus status) {
        return R.ok(statementService.page(current, size, SecurityUtils.requireCustomerId(), status, null, true));
    }

    @GetMapping("/dealer/statement/{id}")
    @PreAuthorize("@ss.hasPerm('dealer:statement:list')")
    public R<FinCustomerStatement> dealerDetail(@PathVariable Long id) {
        return R.ok(statementService.detail(id, SecurityUtils.requireCustomerId()));
    }

    @GetMapping("/dealer/statement/{id}/lines")
    @PreAuthorize("@ss.hasPerm('dealer:statement:list')")
    public R<PageResult<FinCustomerStatementLine>> dealerLines(@PathVariable Long id, @RequestParam(required = false) Long current,
                                                               @RequestParam(required = false) Long size) {
        return R.ok(statementService.lines(id, SecurityUtils.requireCustomerId(), current, size));
    }

    @PostMapping("/dealer/statement/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('dealer:statement:confirm')")
    @OperLog(title = "经销商门户-对账确认", type = OperType.UPDATE) // i18n-ignore
    public R<Void> dealerConfirm(@PathVariable Long id, @Valid @RequestBody StatementRequests.Respond req) {
        statementService.confirm(id, SecurityUtils.requireCustomerId(), req);
        return R.ok();
    }
}
