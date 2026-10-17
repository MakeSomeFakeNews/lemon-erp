package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.PeriodStatus;
import com.lemon.erp.modules.fin.model.CostRunKind;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.AccountOpeningRequest;
import com.lemon.erp.modules.fin.dto.AccountOpeningRow;
import com.lemon.erp.modules.fin.dto.StockOpeningRequest;
import com.lemon.erp.modules.fin.dto.StockOpeningView;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinAccountOpening;
import com.lemon.erp.modules.fin.entity.FinCostRun;
import com.lemon.erp.modules.fin.entity.FinMaterialCost;
import com.lemon.erp.modules.fin.entity.FinPeriod;
import com.lemon.erp.modules.fin.mapper.FinAccountOpeningMapper;
import com.lemon.erp.modules.fin.mapper.FinCostRunMapper;
import com.lemon.erp.modules.fin.mapper.FinCostSourceMapper;
import com.lemon.erp.modules.fin.mapper.FinMaterialCostMapper;
import com.lemon.erp.modules.fin.mapper.FinPeriodMapper;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.model.CostMethod;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.fin.model.FinConstants.lastDay;
import static com.lemon.erp.modules.fin.model.FinConstants.previous;
import static com.lemon.erp.modules.fin.model.FinConstants.toYearMonth;

/**
 * 期初余额：系统启用时录入
 * <ul>
 *   <li>科目期初：末级科目的借方 / 贷方余额，借贷必须平衡；计入余额表、明细账、资产负债表的期初（汇总查询中合并）。
 *       已有结账期间后不能修改</li>
 *   <li>存货期初：启用期间之前的结存数量和金额，保存为启用期间上一期的成本核算结果（类型 OPENING），
 *       启用期间核算成本时作为期初；已核算之后的期间后不能修改。存货期初金额应与原材料、库存商品的科目期初一致</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FinOpeningService {

    private final FinAccountOpeningMapper openingMapper;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinPeriodMapper periodMapper;
    private final FinCostRunMapper runMapper;
    private final FinMaterialCostMapper materialCostMapper;
    private final FinCostSourceMapper sourceMapper;
    private final WmsMaterialService materialService;

    // ---------------------------------------------------------------- 科目期初

    public List<AccountOpeningRow> accountOpenings() {
        Map<Long, FinAccountOpening> saved = openingMapper.selectList(null).stream()
                .collect(Collectors.toMap(FinAccountOpening::getAccountId, Function.identity()));
        List<FinAccount> all = accountService.listAll();
        Set<Long> parents = all.stream().map(FinAccount::getParentId).collect(Collectors.toSet());
        List<AccountOpeningRow> rows = new ArrayList<>();
        for (FinAccount a : all) {
            if (parents.contains(a.getId()) || (!Integer.valueOf(1).equals(a.getStatus()) && !saved.containsKey(a.getId()))) {
                continue;
            }
            FinAccountOpening o = saved.get(a.getId());
            AccountOpeningRow r = new AccountOpeningRow();
            r.setAccountId(a.getId());
            r.setCode(a.getCode());
            r.setName(a.getName());
            r.setCategory(a.getCategory());
            r.setDirection(a.getDirection());
            r.setDebit(o == null ? BigDecimal.ZERO : o.getDebit());
            r.setCredit(o == null ? BigDecimal.ZERO : o.getCredit());
            rows.add(r);
        }
        return rows;
    }

    @Transactional
    public void saveAccountOpenings(AccountOpeningRequest req) {
        if (periodMapper.exists(Wrappers.<FinPeriod>lambdaQuery().eq(FinPeriod::getStatus, PeriodStatus.CLOSED))) {
            throw new BizException("fin.error.somePeriodsClosedAccountOpeningBalances");
        }
        List<FinAccountOpening> list = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (AccountOpeningRequest.Item item : req.items()) {
            BigDecimal d = item.debit() == null ? BigDecimal.ZERO : item.debit();
            BigDecimal c = item.credit() == null ? BigDecimal.ZERO : item.credit();
            if (d.signum() == 0 && c.signum() == 0) {
                continue;
            }
            if (!seen.add(item.accountId())) {
                throw new BizException("fin.error.duplicateAccount");
            }
            FinAccountOpening o = new FinAccountOpening();
            o.setAccountId(item.accountId());
            o.setDebit(d);
            o.setCredit(c);
            list.add(o);
            debit = debit.add(d);
            credit = credit.add(c);
        }
        if (debit.compareTo(credit) != 0) {
            throw new BizException("fin.error.openingBalancesDoNotBalanceDebit", debit.toPlainString(), credit.toPlainString());
        }
        if (!list.isEmpty()) {
            accountService.requireLeaves(seen);
        }
        openingMapper.delete(Wrappers.<FinAccountOpening>lambdaQuery().isNotNull(FinAccountOpening::getId));
        if (!list.isEmpty()) {
            openingMapper.insertBatch(list);
        }
    }

    // ---------------------------------------------------------------- 存货期初

    /**
     * 已保存的存货期初；没有时按 startPeriod 推算期初数量（启用期间之前的库存流水合计），金额待填
     */
    public StockOpeningView stockOpening(Integer startPeriod) {
        FinCostRun opening = openingRun();
        StockOpeningView view = new StockOpeningView();
        Integer start = opening != null ? Integer.valueOf(next(opening.getPeriod())) : startPeriod;
        view.setStartPeriod(start);
        view.setLocked(opening != null && runMapper.exists(Wrappers.<FinCostRun>lambdaQuery()
                .eq(FinCostRun::getKind, CostRunKind.RUN)));
        if (start == null) {
            view.setRows(List.of());
            view.setChecks(List.of());
            return view;
        }
        int openPeriod = previous(start);
        Map<Long, BigDecimal> book = new TreeMap<>();
        for (Map<String, Object> row : sourceMapper.selectStockMovements(null, lastDay(openPeriod))) {
            Long id = ((Number) row.get("materialId")).longValue();
            book.merge(id, dec(row, "inQty").subtract(dec(row, "outQty")), BigDecimal::add);
        }
        Map<Long, FinMaterialCost> saved = opening == null ? Map.of()
                : materialCostMapper.selectList(Wrappers.<FinMaterialCost>lambdaQuery().eq(FinMaterialCost::getPeriod, opening.getPeriod()))
                .stream().collect(Collectors.toMap(FinMaterialCost::getMaterialId, Function.identity()));
        Set<Long> ids = new HashSet<>(book.keySet());
        ids.addAll(saved.keySet());
        Map<Long, WmsMaterial> materials = materialService.mapByIds(ids);
        List<StockOpeningView.Row> rows = new ArrayList<>();
        Map<String, BigDecimal> byAccount = new LinkedHashMap<>();
        byAccount.put(accountRules.code(AccountRole.INVENTORY, FinConstants.CATEGORY_RAW), BigDecimal.ZERO);
        byAccount.put(accountRules.code(AccountRole.INVENTORY, FinConstants.CATEGORY_FINISHED), BigDecimal.ZERO);
        for (Long id : ids.stream().sorted((a, b) -> code(materials, a).compareTo(code(materials, b))).toList()) {
            BigDecimal bookQty = book.getOrDefault(id, BigDecimal.ZERO);
            FinMaterialCost s = saved.get(id);
            if (s == null && bookQty.signum() == 0) {
                continue;
            }
            WmsMaterial m = materials.get(id);
            StockOpeningView.Row r = new StockOpeningView.Row();
            r.setMaterialId(id);
            if (m != null) {
                r.setMaterialCode(m.getCode());
                r.setMaterialName(m.getName());
                r.setSpec(m.getSpec());
                r.setUnit(m.getUnit());
                r.setCategory(m.getCategory());
            }
            r.setBookQty(bookQty);
            r.setQty(s != null ? s.getCloseQty() : bookQty);
            r.setAmount(s != null ? s.getCloseAmount() : BigDecimal.ZERO);
            byAccount.merge(accountRules.code(AccountRole.INVENTORY, r.getCategory()), r.getAmount(), BigDecimal::add);
            rows.add(r);
        }
        view.setRows(rows);
        view.setChecks(accountChecks(byAccount));
        return view;
    }

    @Transactional
    public void saveStockOpening(StockOpeningRequest req) {
        toYearMonth(req.startPeriod());
        int openPeriod = previous(req.startPeriod());
        if (runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getKind, CostRunKind.RUN))) {
            throw new BizException("fin.error.costsHaveCalculatedInventoryOpeningsNo");
        }
        FinCostRun old = openingRun();
        if (old != null) {
            materialCostMapper.delete(Wrappers.<FinMaterialCost>lambdaQuery().eq(FinMaterialCost::getPeriod, old.getPeriod()));
            runMapper.deleteById(old.getId());
        }
        Set<Long> seen = new HashSet<>();
        List<FinMaterialCost> rows = new ArrayList<>();
        for (StockOpeningRequest.Item item : req.items()) {
            if (!seen.add(item.materialId())) {
                throw new BizException("fin.error.duplicateMaterial");
            }
            if (item.qty().signum() == 0 && item.amount().signum() == 0) {
                continue;
            }
            if (item.qty().signum() == 0) {
                throw new BizException("fin.error.materialsZeroQuantityCannotHaveOpening");
            }
            FinMaterialCost c = new FinMaterialCost();
            c.setPeriod(openPeriod);
            c.setMaterialId(item.materialId());
            BigDecimal zero = BigDecimal.ZERO;
            c.setOpenQty(zero);
            c.setOpenAmount(zero);
            c.setPurchaseQty(zero);
            c.setPurchaseAmount(zero);
            c.setMakeQty(zero);
            c.setMakeAmount(zero);
            c.setOtherInQty(item.qty());
            c.setOtherInAmount(item.amount());
            c.setIssueQty(zero);
            c.setIssueAmount(zero);
            c.setSalesQty(zero);
            c.setSalesAmount(zero);
            c.setOtherOutQty(zero);
            c.setOtherOutAmount(zero);
            c.setCostMethod(CostMethod.WEIGHTED);
            c.setCloseQty(item.qty());
            c.setCloseAmount(item.amount());
            c.setUnitCost(item.amount().divide(item.qty(), 6, RoundingMode.HALF_UP));
            rows.add(c);
        }
        materialService.mapByIds(seen);
        FinCostRun run = new FinCostRun();
        run.setPeriod(openPeriod);
        run.setKind(CostRunKind.OPENING);
        run.setOverheadVariance(BigDecimal.ZERO);
        run.setCreateBy(SecurityUtils.getUserId());
        runMapper.insert(run);
        if (!rows.isEmpty()) {
            materialCostMapper.insertBatch(rows);
        }
    }

    // ---------------------------------------------------------------- 内部

    private FinCostRun openingRun() {
        return runMapper.selectOne(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getKind, CostRunKind.OPENING));
    }

    /** 存货期初金额与原材料、库存商品科目期初余额（含下级）对比 */
    private List<StockOpeningView.AccountCheck> accountChecks(Map<String, BigDecimal> byAccount) {
        List<FinAccount> all = accountService.listAll();
        Map<Long, FinAccountOpening> openings = openingMapper.selectList(null).stream()
                .collect(Collectors.toMap(FinAccountOpening::getAccountId, Function.identity()));
        List<StockOpeningView.AccountCheck> checks = new ArrayList<>();
        byAccount.forEach((code, amount) -> {
            FinAccount account = all.stream().filter(a -> a.getCode().equals(code)).findFirst().orElse(null);
            BigDecimal ledger = all.stream().filter(a -> a.getCode().startsWith(code))
                    .map(a -> openings.get(a.getId())).filter(o -> o != null)
                    .map(o -> o.getDebit().subtract(o.getCredit())).reduce(BigDecimal.ZERO, BigDecimal::add);
            StockOpeningView.AccountCheck c = new StockOpeningView.AccountCheck();
            c.setAccountCode(code);
            c.setAccountName(account == null ? code : account.getName());
            c.setStockAmount(amount);
            c.setLedgerAmount(ledger);
            checks.add(c);
        });
        return checks;
    }

    private static int next(int period) {
        YearMonth ym = toYearMonth(period).plusMonths(1);
        return ym.getYear() * 100 + ym.getMonthValue();
    }

    private static String code(Map<Long, WmsMaterial> materials, Long id) {
        WmsMaterial m = materials.get(id);
        return m == null ? String.valueOf(id) : m.getCode();
    }

    private static BigDecimal dec(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
