package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.AccountOpeningRequest;
import com.lemon.erp.modules.fin.dto.AccountOpeningRow;
import com.lemon.erp.modules.fin.dto.StockOpeningRequest;
import com.lemon.erp.modules.fin.dto.StockOpeningView;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.modules.fin.dto.OpeningBillRow;
import com.lemon.erp.modules.fin.service.FinOpeningService;
import com.lemon.erp.modules.fin.service.FinOpeningSetupService;
import com.lemon.erp.modules.wms.dto.OpeningStockRow;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 期初余额：启用向导、科目期初、期初库存、存货期初、期初往来
 */
@RestController
@RequestMapping("/fin/opening")
@RequiredArgsConstructor
public class FinOpeningController {

    private final FinOpeningService openingService;
    private final FinOpeningSetupService setupService;

    /** 启用向导：各项期初数据的完成情况 */
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('fin:opening:list')")
    public R<Map<String, Object>> summary() {
        return R.ok(setupService.summary());
    }

    /** 期初库存导入：按仓库生成启用期间前一天的期初入库单 */
    @PostMapping("/stock-in/import")
    @PreAuthorize("@ss.hasPerm('fin:opening:edit')")
    @OperLog(title = "期初库存导入", type = OperType.IMPORT) // i18n-ignore
    public R<ImportResult> importStock(@RequestParam int startPeriod, @Valid @RequestBody ImportRequest<OpeningStockRow> req) {
        return R.ok(setupService.importStock(startPeriod, req));
    }

    /** 期初往来导入：kind 为 AR / AP */
    @PostMapping("/bills/{kind}/import")
    @PreAuthorize("@ss.hasPerm('fin:opening:edit')")
    @OperLog(title = "期初往来导入", type = OperType.IMPORT) // i18n-ignore
    public R<ImportResult> importBills(@PathVariable String kind, @Valid @RequestBody ImportRequest<OpeningBillRow> req) {
        return R.ok(setupService.importBills(kind, req));
    }

    @GetMapping("/accounts")
    @PreAuthorize("@ss.hasPerm('fin:opening:list')")
    public R<List<AccountOpeningRow>> accounts() {
        return R.ok(openingService.accountOpenings());
    }

    @PutMapping("/accounts")
    @PreAuthorize("@ss.hasPerm('fin:opening:edit')")
    @OperLog(title = "科目期初余额", type = OperType.UPDATE) // i18n-ignore
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
    @OperLog(title = "存货期初", type = OperType.UPDATE) // i18n-ignore
    public R<Void> saveStock(@Valid @RequestBody StockOpeningRequest req) {
        openingService.saveStockOpening(req);
        return R.ok();
    }
}
