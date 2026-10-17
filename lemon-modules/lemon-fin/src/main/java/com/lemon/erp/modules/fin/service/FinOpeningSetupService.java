package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.CostRunKind;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.modules.fin.dto.BillRequest;
import com.lemon.erp.modules.fin.dto.OpeningBillRow;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinAccountOpening;
import com.lemon.erp.modules.fin.entity.FinCostRun;
import com.lemon.erp.modules.fin.mapper.FinAccountOpeningMapper;
import com.lemon.erp.modules.fin.mapper.FinCostRunMapper;
import com.lemon.erp.modules.fin.mapper.FinOpeningQueryMapper;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.mapper.ScmCustomerMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierMapper;
import com.lemon.erp.modules.wms.dto.OpeningStockRow;
import com.lemon.erp.modules.wms.service.WmsOpeningStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.fin.model.FinConstants.lastDay;
import static com.lemon.erp.modules.fin.model.FinConstants.previous;
import static com.lemon.erp.modules.fin.model.FinConstants.toYearMonth;

/**
 * 系统启用向导：汇总各项期初数据的完成情况（科目期初、期初库存、存货期初、期初往来），
 * 并提供期初库存、期初往来的 Excel 导入；期初往来与应收账款、应付账款的科目期初核对
 */
@Service
@RequiredArgsConstructor
public class FinOpeningSetupService {

    private final FinOpeningQueryMapper queryMapper;
    private final FinAccountOpeningMapper openingMapper;
    private final FinCostRunMapper runMapper;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinBillService billService;
    private final ScmCustomerMapper customerMapper;
    private final ScmSupplierMapper supplierMapper;
    private final WmsOpeningStockService openingStockService;
    private final ImportExecutor importExecutor;

    /**
     * 启用向导：各步骤的数据
     *
     * @return {accounts: {count, debit, credit}, stockDocs: [...], stock: {startPeriod, locked, count, amount},
     * bills: {AR: {count, amount, ledger}, AP: {...}}}
     */
    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<FinAccountOpening> openings = openingMapper.selectList(null);
        BigDecimal debit = openings.stream().map(FinAccountOpening::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = openings.stream().map(FinAccountOpening::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("accounts", Map.of("count", openings.size(), "debit", debit, "credit", credit));
        result.put("stockDocs", queryMapper.selectOpeningStockDocs());

        FinCostRun opening = runMapper.selectOne(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getKind, CostRunKind.OPENING));
        Map<String, Object> stock = new LinkedHashMap<>();
        stock.put("saved", opening != null);
        stock.put("openingPeriod", opening == null ? null : opening.getPeriod());
        stock.put("costed", runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getKind, CostRunKind.RUN)));
        result.put("stock", stock);

        List<FinAccount> accounts = accountService.listAll();
        Map<String, Object> bills = new LinkedHashMap<>();
        for (BillKind kind : BillKind.values()) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("count", 0);
            b.put("amount", BigDecimal.ZERO);
            boolean ar = kind == BillKind.AR;
            String code = accountRules.code(ar ? AccountRole.RECEIVABLE : AccountRole.PAYABLE, null);
            BigDecimal ledger = BigDecimal.ZERO;
            for (FinAccountOpening o : openings) {
                FinAccount a = accounts.stream().filter(x -> x.getId().equals(o.getAccountId())).findFirst().orElse(null);
                if (a != null && a.getCode().startsWith(code)) {
                    ledger = ledger.add(ar ? o.getDebit().subtract(o.getCredit()) : o.getCredit().subtract(o.getDebit()));
                }
            }
            b.put("accountCode", code);
            b.put("ledger", ledger);
            bills.put(kind.name(), b);
        }
        for (Map<String, Object> row : queryMapper.selectManualBillSums()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> b = (Map<String, Object>) bills.get((String) row.get("kind"));
            if (b != null) {
                b.put("count", ((Number) row.get("count")).intValue());
                b.put("amount", new BigDecimal(row.get("amount").toString()));
            }
        }
        result.put("bills", bills);
        return result;
    }

    /** 期初库存导入：生成启用期间前一天的期初入库单 */
    public ImportResult importStock(int startPeriod, ImportRequest<OpeningStockRow> req) {
        toYearMonth(startPeriod);
        if (runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getKind, CostRunKind.RUN)
                .ge(FinCostRun::getPeriod, previous(startPeriod)))) {
            throw new BizException("fin.error.costsHaveCalculatedStartPeriodLater");
        }
        return openingStockService.importRows(req, lastDay(previous(startPeriod)));
    }

    /** 期初往来导入：按往来单位编码或名称逐行生成手工应收 / 应付单 */
    public ImportResult importBills(BillKind kind, ImportRequest<OpeningBillRow> req) {
        FinBillService.checkKind(kind);
        boolean ar = kind == BillKind.AR;
        return importExecutor.run(req, row -> {
            Long partnerId = ar ? customerId(row.partner().trim()) : supplierId(row.partner().trim());
            billService.create(new BillRequest(kind, partnerId, row.billDate(), row.dueDate(), row.amount(),
                    row.remark() == null || row.remark().isBlank() ? "期初余额" : row.remark())); // i18n-ignore
        });
    }

    private Long customerId(String key) {
        List<ScmCustomer> list = customerMapper.selectList(Wrappers.<ScmCustomer>lambdaQuery()
                .eq(ScmCustomer::getCode, key).or().eq(ScmCustomer::getName, key));
        if (list.isEmpty()) {
            throw new BizException("fin.error.customerDoesNotExist", key);
        }
        return list.stream().filter(c -> key.equals(c.getCode())).findFirst().orElse(list.get(0)).getId();
    }

    private Long supplierId(String key) {
        List<ScmSupplier> list = supplierMapper.selectList(Wrappers.<ScmSupplier>lambdaQuery()
                .eq(ScmSupplier::getCode, key).or().eq(ScmSupplier::getName, key));
        if (list.isEmpty()) {
            throw new BizException("fin.error.supplierDoesNotExist", key);
        }
        return list.stream().filter(s -> key.equals(s.getCode())).findFirst().orElse(list.get(0)).getId();
    }
}
