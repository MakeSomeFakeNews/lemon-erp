package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.CashAccountRequests;
import com.lemon.erp.modules.fin.entity.FinBankOpeningItem;
import com.lemon.erp.modules.fin.entity.FinCashAccount;
import com.lemon.erp.modules.fin.service.FinCashAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 资金账户：现金、银行、微信、支付宝等出纳账户，绑定入账科目、录入期初后启用
 */
@RestController
@RequestMapping("/fin/cash-account")
@RequiredArgsConstructor
public class FinCashAccountController {

    private final FinCashAccountService cashAccountService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:cashAccount:list')")
    public R<List<FinCashAccount>> list() {
        return R.ok(cashAccountService.list());
    }

    /** 登录即可访问：单据的账户下拉（全部资金账户，usable 为 true 的可选） */
    @GetMapping("/options")
    public R<List<CashAccountRequests.Option>> options() {
        return R.ok(cashAccountService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:cashAccount:list')")
    public R<FinCashAccount> get(@PathVariable Long id) {
        return R.ok(cashAccountService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:cashAccount:add')")
    @OperLog(title = "资金账户", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody CashAccountRequests.Save req) {
        return R.ok(cashAccountService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:cashAccount:edit')")
    @OperLog(title = "资金账户", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CashAccountRequests.Save req) {
        cashAccountService.update(id, req);
        return R.ok();
    }

    /** 启用（期初须平衡）/ 停用 */
    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('fin:cashAccount:edit')")
    @OperLog(title = "资金账户-修改状态", type = OperType.UPDATE) // i18n-ignore
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        if (req.status() == EnableStatus.ENABLED) {
            cashAccountService.enable(id);
        } else {
            cashAccountService.disable(id);
        }
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:cashAccount:remove')")
    @OperLog(title = "资金账户", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        cashAccountService.delete(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 期初

    /** 期初平衡检查（含入账科目在启用日期之前的账面余额） */
    @GetMapping("/{id}/opening-check")
    @PreAuthorize("@ss.hasPerm('fin:cashAccount:list')")
    public R<CashAccountRequests.OpeningCheck> openingCheck(@PathVariable Long id) {
        return R.ok(cashAccountService.openingCheck(id));
    }

    /** 科目在日期之前的账面余额（科目期初 + 凭证，含未记账），新增账户时参考日记账期初 */
    @GetMapping("/ledger-balance")
    @PreAuthorize("@ss.hasPerm('fin:cashAccount:list')")
    public R<BigDecimal> ledgerBalance(@RequestParam Long accountId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(cashAccountService.ledgerBalance(accountId, date));
    }

    @GetMapping("/{id}/opening-items")
    @PreAuthorize("@ss.hasPerm('fin:cashAccount:list')")
    public R<List<FinBankOpeningItem>> openingItems(@PathVariable Long id) {
        return R.ok(cashAccountService.openingItems(id));
    }

    @PostMapping("/{id}/opening-items")
    @PreAuthorize("@ss.hasPerm('fin:cashAccount:edit')")
    @OperLog(title = "资金账户-期初未达账", type = OperType.INSERT) // i18n-ignore
    public R<Long> addOpeningItem(@PathVariable Long id, @Valid @RequestBody CashAccountRequests.OpeningItemSave req) {
        return R.ok(cashAccountService.addOpeningItem(id, req));
    }

    @DeleteMapping("/opening-items/{itemId}")
    @PreAuthorize("@ss.hasPerm('fin:cashAccount:edit')")
    @OperLog(title = "资金账户-期初未达账", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteOpeningItem(@PathVariable Long itemId) {
        cashAccountService.deleteOpeningItem(itemId);
        return R.ok();
    }
}
