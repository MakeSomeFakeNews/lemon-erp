package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.AccountOpeningRequest;
import com.lemon.erp.modules.fin.dto.AccountOpeningRow;
import com.lemon.erp.modules.fin.dto.StockOpeningRequest;
import com.lemon.erp.modules.fin.dto.StockOpeningView;
import com.lemon.erp.modules.fin.service.FinOpeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 期初余额：科目期初、存货期初
 */
@RestController
@RequestMapping("/fin/opening")
@RequiredArgsConstructor
public class FinOpeningController {

    private final FinOpeningService openingService;

    @GetMapping("/accounts")
    @PreAuthorize("@ss.hasPerm('fin:opening:list')")
    public R<List<AccountOpeningRow>> accounts() {
        return R.ok(openingService.accountOpenings());
    }

    @PutMapping("/accounts")
    @PreAuthorize("@ss.hasPerm('fin:opening:edit')")
    @OperLog(title = "科目期初余额", type = OperType.UPDATE)
    public R<Void> saveAccounts(@Valid @RequestBody AccountOpeningRequest req) {
        openingService.saveAccountOpenings(req);
        return R.ok();
    }

    /** @param startPeriod 尚未保存存货期初时，按该启用期间推算期初数量 */
    @GetMapping("/stock")
    @PreAuthorize("@ss.hasPerm('fin:opening:list')")
    public R<StockOpeningView> stock(@RequestParam(required = false) Integer startPeriod) {
        return R.ok(openingService.stockOpening(startPeriod));
    }

    @PutMapping("/stock")
    @PreAuthorize("@ss.hasPerm('fin:opening:edit')")
    @OperLog(title = "存货期初", type = OperType.UPDATE)
    public R<Void> saveStock(@Valid @RequestBody StockOpeningRequest req) {
        openingService.saveStockOpening(req);
        return R.ok();
    }
}
