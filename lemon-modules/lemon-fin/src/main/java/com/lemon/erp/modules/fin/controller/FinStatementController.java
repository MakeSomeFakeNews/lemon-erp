package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.StatementRequests;
import com.lemon.erp.modules.fin.entity.FinSupplierStatement;
import com.lemon.erp.modules.fin.model.StatementStatus;
import com.lemon.erp.modules.fin.service.FinStatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 供应商对账：财务（/fin/statement）与供应商门户（/portal/statement）
 */
@RestController
@RequiredArgsConstructor
public class FinStatementController {

    private final FinStatementService statementService;

    @GetMapping("/fin/statement")
    @PreAuthorize("@ss.hasPerm('fin:statement:list')")
    public R<PageResult<FinSupplierStatement>> page(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Long supplierId,
                                                    @RequestParam(required = false) StatementStatus status,
                                                    @RequestParam(required = false) String keyword) {
        return R.ok(statementService.page(current, size, supplierId, status, keyword, false));
    }

    @GetMapping("/fin/statement/{id}")
    @PreAuthorize("@ss.hasPerm('fin:statement:list')")
    public R<FinSupplierStatement> detail(@PathVariable Long id) {
        return R.ok(statementService.detail(id, null));
    }

    @PostMapping("/fin/statement")
    @PreAuthorize("@ss.hasPerm('fin:statement:add')")
    @OperLog(title = "供应商对账单", type = OperType.INSERT) // i18n-ignore
    public R<Long> generate(@Valid @RequestBody StatementRequests.Generate req) {
        return R.ok(statementService.generate(req));
    }

    @PostMapping("/fin/statement/{id}/send")
    @PreAuthorize("@ss.hasPerm('fin:statement:edit')")
    @OperLog(title = "供应商对账单-发送", type = OperType.UPDATE) // i18n-ignore
    public R<Void> send(@PathVariable Long id) {
        statementService.send(id);
        return R.ok();
    }

    @DeleteMapping("/fin/statement/{id}")
    @PreAuthorize("@ss.hasPerm('fin:statement:remove')")
    @OperLog(title = "供应商对账单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable Long id) {
        statementService.delete(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 供应商门户

    @GetMapping("/portal/statement")
    @PreAuthorize("@ss.hasPerm('portal:statement:list')")
    public R<PageResult<FinSupplierStatement>> portalPage(@RequestParam(defaultValue = "1") long current,
                                                          @RequestParam(defaultValue = "10") long size,
                                                          @RequestParam(required = false) StatementStatus status) {
        return R.ok(statementService.page(current, size, SecurityUtils.requireSupplierId(), status, null, true));
    }

    @GetMapping("/portal/statement/{id}")
    @PreAuthorize("@ss.hasPerm('portal:statement:list')")
    public R<FinSupplierStatement> portalDetail(@PathVariable Long id) {
        return R.ok(statementService.detail(id, SecurityUtils.requireSupplierId()));
    }

    @PostMapping("/portal/statement/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('portal:statement:confirm')")
    @OperLog(title = "门户-对账确认", type = OperType.UPDATE) // i18n-ignore
    public R<Void> confirm(@PathVariable Long id, @Valid @RequestBody StatementRequests.Confirm req) {
        statementService.confirm(id, SecurityUtils.requireSupplierId(), req);
        return R.ok();
    }

    @PutMapping("/portal/statement/{id}/invoice")
    @PreAuthorize("@ss.hasPerm('portal:statement:confirm')")
    @OperLog(title = "门户-对账单发票", type = OperType.UPDATE) // i18n-ignore
    public R<Void> invoice(@PathVariable Long id, @Valid @RequestBody StatementRequests.Invoice req) {
        statementService.invoice(id, SecurityUtils.requireSupplierId(), req);
        return R.ok();
    }
}
