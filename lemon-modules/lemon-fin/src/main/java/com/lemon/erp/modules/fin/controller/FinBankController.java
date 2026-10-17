package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.BankStatementImport;
import com.lemon.erp.modules.fin.entity.FinBankStatement;
import com.lemon.erp.modules.fin.service.FinBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 银行对账：对账单导入、与日记账勾对、余额调节表
 */
@RestController
@RequestMapping("/fin")
@RequiredArgsConstructor
public class FinBankController {

    private final FinBankService bankService;

    @GetMapping("/bank/statement")
    @PreAuthorize("@ss.hasPerm('fin:bank:list')")
    public R<List<FinBankStatement>> statements(@RequestParam Long accountId,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(bankService.statements(accountId, begin, end));
    }

    @GetMapping("/bank/entries")
    @PreAuthorize("@ss.hasPerm('fin:bank:list')")
    public R<List<Map<String, Object>>> bankEntries(@RequestParam Long accountId,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(bankService.entries(accountId, begin, end));
    }

    @PostMapping("/bank/statement/import")
    @PreAuthorize("@ss.hasPerm('fin:bank:edit')")
    @OperLog(title = "导入银行对账单", type = OperType.IMPORT) // i18n-ignore
    public R<Integer> importStatements(@Valid @RequestBody BankStatementImport req) {
        return R.ok(bankService.importRows(req.accountId(), req.rows()));
    }

    @PostMapping("/bank/auto-match")
    @PreAuthorize("@ss.hasPerm('fin:bank:edit')")
    public R<Integer> autoMatch(@RequestParam Long accountId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(bankService.autoMatch(accountId, begin, end));
    }

    @PostMapping("/bank/statement/{id}/match/{entryId}")
    @PreAuthorize("@ss.hasPerm('fin:bank:edit')")
    public R<Void> match(@PathVariable Long id, @PathVariable Long entryId) {
        bankService.match(id, entryId);
        return R.ok();
    }

    @PostMapping("/bank/statement/{id}/unmatch")
    @PreAuthorize("@ss.hasPerm('fin:bank:edit')")
    public R<Void> unmatch(@PathVariable Long id) {
        bankService.unmatch(id);
        return R.ok();
    }

    @DeleteMapping("/bank/statement/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:bank:edit')")
    @OperLog(title = "银行对账单", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteStatements(@PathVariable List<Long> ids) {
        bankService.delete(ids);
        return R.ok();
    }

    @GetMapping("/bank/reconcile")
    @PreAuthorize("@ss.hasPerm('fin:bank:list')")
    public R<Map<String, Object>> reconcile(@RequestParam Long accountId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
                                            @RequestParam(required = false) BigDecimal bankBalance) {
        return R.ok(bankService.reconcile(accountId, asOf, bankBalance));
    }
}
