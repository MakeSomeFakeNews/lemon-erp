package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.CostRunResult;
import com.lemon.erp.modules.fin.entity.FinCostRun;
import com.lemon.erp.modules.fin.entity.FinMaterialCost;
import com.lemon.erp.modules.fin.entity.FinVoucher;
import com.lemon.erp.modules.fin.entity.FinWoCost;
import com.lemon.erp.modules.fin.mapper.FinCostRunMapper;
import com.lemon.erp.modules.fin.mapper.FinConsumableUseMapper;
import com.lemon.erp.modules.fin.mapper.FinCostSourceMapper;
import com.lemon.erp.modules.fin.mapper.FinMaterialCostMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherEntryMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherMapper;
import com.lemon.erp.modules.fin.mapper.FinWoCostMapper;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.fin.model.VoucherStatus;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.fin.model.FinConstants.CONTRACT_COST;
import static com.lemon.erp.modules.fin.model.FinConstants.COST_OF_SALES;
import static com.lemon.erp.modules.fin.model.FinConstants.MANUFACTURING_OVERHEAD;
import static com.lemon.erp.modules.fin.model.FinConstants.OUTSOURCE_MATERIAL;
import static com.lemon.erp.modules.fin.model.FinConstants.PENDING_LOSS;
import static com.lemon.erp.modules.fin.model.FinConstants.PRODUCTION_COST;
import static com.lemon.erp.modules.fin.model.FinConstants.SELLING_EXPENSE;
import static com.lemon.erp.modules.fin.model.FinConstants.firstDay;
import static com.lemon.erp.modules.fin.model.FinConstants.inventoryAccount;
import static com.lemon.erp.modules.fin.model.FinConstants.lastDay;
import static com.lemon.erp.modules.fin.model.FinConstants.periodOf;
import static com.lemon.erp.modules.fin.model.FinConstants.previous;

/**
 * 成本核算（按期间）：月末一次加权平均法计算存货单价，工单归集料工费并结转完工产品成本。
 * <ol>
 *   <li>数量取仓库流水（按单据业务日期归属期间，不含调拨），销售按仓库出库类型统计；采购金额取采购入库 / 退货的不含税金额，
 *       没有经过采购订单的入库计入其他入库</li>
 *   <li>按「被领用物料 → 工单产品」的依赖顺序逐个物料计算：先算出下层物料单价，再算用到它的工单成本，最后算产品单价，
 *       所以半成品、成品的单价包含本期的材料成本</li>
 *   <li>工单成本 = 期初在产品 + 本期领料（领用数量 × 物料单价）+ 人工（工时 × 人工费率）+ 制造费用（工时 × 费用率）；
 *       完工产品成本按本期完工数量占剩余未完工数量的比例结转，工单全部完工时全部结转，其余为期末在产品</li>
 *   <li>加权平均单价 = (期初金额 + 采购金额 + 生产成本) / (期初数量 + 采购数量 + 生产数量)；
 *       领用、销售、其他出入库（盘盈盘亏等）按该单价计价</li>
 * </ol>
 * 生成成本结转凭证（未审核）：领料 借 生产成本 / 贷 原材料、库存商品；人工、制造费用 借 生产成本 / 贷 制造费用
 * （生产岗位工资计提时计入制造费用）；制造费用差异（期末制造费用余额）转主营业务成本；关闭工单的剩余在产品转主营业务成本；
 * 完工 借 库存商品 / 贷 生产成本；销售成本 借 主营业务成本 / 贷 存货；项目领料 借 合同履约成本；委外发料 借 委托加工物资；
 * 其他出入库 待处理财产损溢。
 * 必须逐月核算（首次核算或存货期初之后的第一期除外）；期间未结账、成本凭证未审核时可以重新核算
 */
@Service
@RequiredArgsConstructor
public class FinCostService {

    static final String RUN = "RUN";
    /** 存货期初：启用期间的上一期，保存期初数量、金额作为首期核算的期初 */
    static final String OPENING = "OPENING";
    private static final int AMOUNT_SCALE = 2;
    private static final int UNIT_SCALE = 6;

    private final FinCostRunMapper runMapper;
    private final FinMaterialCostMapper materialCostMapper;
    private final FinWoCostMapper woCostMapper;
    private final FinCostSourceMapper sourceMapper;
    private final FinVoucherMapper voucherMapper;
    private final FinVoucherEntryMapper entryMapper;
    private final FinVoucherService voucherService;
    private final FinAccountService accountService;
    private final FinPeriodService periodService;
    private final WmsMaterialService materialService;
    private final FinConsumableUseMapper consumableMapper;

    public PageResult<FinCostRun> page(long current, long size) {
        Page<FinCostRun> page = Page.of(current, size);
        runMapper.selectPageList(page);
        return PageResult.of(page);
    }

    public PageResult<FinMaterialCost> materialCosts(long current, long size, int period, String keyword) {
        Page<FinMaterialCost> page = Page.of(current, size);
        materialCostMapper.selectPageList(page, period, keyword);
        return PageResult.of(page);
    }

    public PageResult<FinWoCost> woCosts(long current, long size, int period) {
        Page<FinWoCost> page = Page.of(current, size);
        woCostMapper.selectPageList(page, period);
        return PageResult.of(page);
    }

    /** 核算期间成本并生成成本结转凭证 */
    @Transactional
    public CostRunResult run(int period) {
        periodService.checkOpen(period);
        FinCostRun old = runMapper.selectOne(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getPeriod, period));
        if (old != null) {
            if (OPENING.equals(old.getKind())) {
                throw new BizException(period + " 是存货期初所在期间，不能核算，请从下一期间开始核算");
            }
            discard(old);
        }
        if (runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().gt(FinCostRun::getPeriod, period))) {
            throw new BizException("之后的期间已核算成本，不能重新核算 " + period);
        }
        // 已有更早的核算（或存货期初）时，必须逐月核算，否则期初金额会按 0 处理
        if (runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().lt(FinCostRun::getPeriod, period))
                && !runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getPeriod, previous(period)))) {
            throw new BizException("请先核算 " + previous(period) + " 期间的成本");
        }

        LocalDate begin = firstDay(period);
        LocalDate end = lastDay(period);
        Calc calc = new Calc(period);
        calc.load(begin, end);
        calc.compute();

        if (!calc.materials.isEmpty()) {
            materialCostMapper.insertBatch(new ArrayList<>(calc.materials.values()));
        }
        if (!calc.woCosts.isEmpty()) {
            woCostMapper.insertBatch(new ArrayList<>(calc.woCosts.values()));
        }
        FinCostRun run = new FinCostRun();
        run.setPeriod(period);
        run.setCreateBy(SecurityUtils.getUserId());
        run.setKind(RUN);
        List<VoucherLine> lines = calc.voucherLines();
        run.setOverheadVariance(calc.overheadVariance);
        if (!lines.isEmpty()) {
            run.setVoucherId(voucherService.createGenerated(end, VoucherSource.COST, String.valueOf(period), lines));
        }
        runMapper.insert(run);
        return new CostRunResult(calc.materials.size(), calc.zeroCostWarnings());
    }

    /** 撤销核算：删除成本数据和未审核的成本凭证 */
    @Transactional
    public void undo(int period) {
        periodService.checkOpen(period);
        FinCostRun run = runMapper.selectOne(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getPeriod, period));
        if (run == null) {
            throw new BizException(period + " 期间没有核算成本");
        }
        if (OPENING.equals(run.getKind())) {
            throw new BizException(period + " 是存货期初，请在「期初余额」中修改");
        }
        if (runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().gt(FinCostRun::getPeriod, period))) {
            throw new BizException("请先撤销之后期间的成本核算");
        }
        discard(run);
    }

    private void discard(FinCostRun run) {
        if (run.getVoucherId() != null) {
            FinVoucher v = voucherMapper.selectById(run.getVoucherId());
            if (v != null && !VoucherStatus.DRAFT.name().equals(v.getStatus())) {
                throw new BizException("成本结转凭证 " + v.getVoucherNo() + " 已审核，请先反审核后再重新核算");
            }
            if (v != null) {
                voucherService.delete(List.of(v.getId()));
            }
        }
        materialCostMapper.delete(Wrappers.<FinMaterialCost>lambdaQuery().eq(FinMaterialCost::getPeriod, run.getPeriod()));
        woCostMapper.delete(Wrappers.<FinWoCost>lambdaQuery().eq(FinWoCost::getPeriod, run.getPeriod()));
        runMapper.deleteById(run.getId());
    }

    // ---------------------------------------------------------------- 计算

    /** 一次核算的计算过程 */
    private final class Calc {

        private final int period;
        /** 物料 → 本期成本（按计算顺序） */
        private final Map<Long, FinMaterialCost> materials = new LinkedHashMap<>();
        /** 工单 → 本期成本 */
        private final Map<Long, FinWoCost> woCosts = new LinkedHashMap<>();

        private Map<Long, FinMaterialCost> prev;
        private Map<Long, BigDecimal> openQtyFallback;
        /** 物料 → [入库合计, 出库合计, 销售出库, 销售退货] */
        private final Map<Long, BigDecimal[]> stock = new HashMap<>();
        /** 物料 → 种类 → [数量, 不含税金额] */
        private final Map<Long, Map<String, BigDecimal[]>> trade = new HashMap<>();
        /** 工单 → 物料 → [领料, 退料, 完工] */
        private final Map<Long, Map<Long, BigDecimal[]>> woDocs = new HashMap<>();
        private final Map<Long, BigDecimal[]> woReports = new HashMap<>();
        private final Map<Long, Map<String, Object>> workOrders = new HashMap<>();
        private final Map<Long, FinWoCost> prevWo = new HashMap<>();
        /** 物料 → 项目领料数量（扣除退回）、委外发料数量，计入合同履约成本、委托加工物资 */
        private final Map<Long, BigDecimal> projectOut = new HashMap<>();
        private final Map<Long, BigDecimal> outsourceOut = new HashMap<>();
        /** 服务工单领用的备件（售后），计入销售费用 */
        private final Map<Long, BigDecimal> serviceOut = new HashMap<>();
        /** 低值易耗品部门领用：[物料、部门、费用科目、数量] */
        private List<Map<String, Object>> consumeRows = List.of();
        /** 委外件 → 子件 → 收货核销的数量：子件成本从委托加工物资转入委外件 */
        private final Map<Long, Map<Long, BigDecimal>> outsourceConsume = new HashMap<>();
        /** 委外件 → 本期核销用料的金额 */
        private final Map<Long, BigDecimal> outsourceConsumedAmount = new HashMap<>();
        private Map<Long, WmsMaterial> materialInfo;
        /** 制造费用差异（实际发生 - 按费率分配），转入主营业务成本 */
        private BigDecimal overheadVariance = BigDecimal.ZERO;

        Calc(int period) {
            this.period = period;
        }

        private final Map<Long, BigDecimal> refCosts = new HashMap<>();

        void load(LocalDate begin, LocalDate end) {
            for (Map<String, Object> row : sourceMapper.selectRefCosts()) {
                refCosts.put(id(row, "materialId"), dec(row, "refCost"));
            }
            int prevPeriod = previous(period);
            prev = materialCostMapper.selectList(Wrappers.<FinMaterialCost>lambdaQuery().eq(FinMaterialCost::getPeriod, prevPeriod))
                    .stream().collect(Collectors.toMap(FinMaterialCost::getMaterialId, Function.identity()));
            woCostMapper.selectList(Wrappers.<FinWoCost>lambdaQuery().eq(FinWoCost::getPeriod, prevPeriod))
                    .forEach(w -> prevWo.put(w.getWoId(), w));
            // 上期没有核算：期初数量取本期之前的全部流水，金额按 0（首次核算）
            openQtyFallback = new HashMap<>();
            if (prev.isEmpty()) {
                for (Map<String, Object> row : sourceMapper.selectStockMovements(null, begin.minusDays(1))) {
                    openQtyFallback.merge(id(row, "materialId"), dec(row, "inQty").subtract(dec(row, "outQty")), BigDecimal::add);
                }
            }
            for (Map<String, Object> row : sourceMapper.selectStockMovements(begin, end)) {
                BigDecimal[] v = stock.computeIfAbsent(id(row, "materialId"), k -> zeros(4));
                v[0] = v[0].add(dec(row, "inQty"));
                v[1] = v[1].add(dec(row, "outQty"));
                // 销售按仓库出库类型统计（含不经过销售订单、直接在仓库做的销售出库）
                if ("SALES".equals(row.get("bizType"))) {
                    v[2] = v[2].add(dec(row, "outQty"));
                } else if ("SALES_RETURN".equals(row.get("bizType"))) {
                    v[3] = v[3].add(dec(row, "inQty"));
                } else if ("PROJECT".equals(row.get("bizType"))) {
                    projectOut.merge(id(row, "materialId"), dec(row, "outQty").subtract(dec(row, "inQty")), BigDecimal::add);
                } else if ("OUTSOURCE".equals(row.get("bizType")) || "OUTSOURCE_RETURN".equals(row.get("bizType"))) {
                    // 委外发料计入委托加工物资，加工商退回（委外收回）冲减
                    outsourceOut.merge(id(row, "materialId"), dec(row, "outQty").subtract(dec(row, "inQty")), BigDecimal::add);
                } else if ("SERVICE".equals(row.get("bizType"))) {
                    serviceOut.merge(id(row, "materialId"), dec(row, "outQty").subtract(dec(row, "inQty")), BigDecimal::add);
                }
            }
            consumeRows = consumableMapper.selectForCost(begin, end);
            for (Map<String, Object> row : sourceMapper.selectTradeSums(begin, end)) {
                trade.computeIfAbsent(id(row, "materialId"), k -> new HashMap<>())
                        .put((String) row.get("kind"), new BigDecimal[]{dec(row, "qty"), dec(row, "net")});
            }
            for (Map<String, Object> row : sourceMapper.selectOutsourceConsumption(begin, end)) {
                outsourceConsume.computeIfAbsent(id(row, "productId"), k -> new HashMap<>())
                        .merge(id(row, "materialId"), dec(row, "qty"), BigDecimal::add);
            }
            for (Map<String, Object> row : sourceMapper.selectWoDocSums(begin, end)) {
                // 报废入库不是完工，按其他入库计价（贷记待处理财产损溢）
                if ("SCRAP".equals(row.get("kind"))) {
                    continue;
                }
                BigDecimal[] v = woDocs.computeIfAbsent(id(row, "woId"), k -> new HashMap<>())
                        .computeIfAbsent(id(row, "materialId"), k -> zeros(3));
                int idx = switch ((String) row.get("kind")) {
                    case "ISSUE", "SUPPLEMENT" -> 0;
                    case "RETURN" -> 1;
                    default -> 2;
                };
                v[idx] = v[idx].add(dec(row, "qty"));
            }
            for (Map<String, Object> row : sourceMapper.selectReportCosts(begin, end)) {
                woReports.put(id(row, "woId"), new BigDecimal[]{dec(row, "hours"), dec(row, "labor"), dec(row, "overhead")});
            }
            Set<Long> woIds = new HashSet<>(woDocs.keySet());
            woIds.addAll(woReports.keySet());
            prevWo.values().stream().filter(w -> w.getWipClose().signum() != 0).forEach(w -> woIds.add(w.getWoId()));
            if (!woIds.isEmpty()) {
                for (Map<String, Object> row : sourceMapper.selectWorkOrders(new ArrayList<>(woIds), begin)) {
                    workOrders.put(id(row, "id"), row);
                }
            }
            Set<Long> all = new TreeSet<>(prev.keySet());
            all.addAll(openQtyFallback.keySet());
            all.addAll(stock.keySet());
            all.addAll(trade.keySet());
            workOrders.values().forEach(w -> all.add(id(w, "materialId")));
            woDocs.values().forEach(m -> all.addAll(m.keySet()));
            outsourceConsume.forEach((product, parts) -> {
                all.add(product);
                all.addAll(parts.keySet());
            });
            materialInfo = materialService.mapByIds(all);
        }

        void compute() {
            // 依赖：工单领用的物料 → 工单产品；按拓扑顺序计算（有环时剩余的按编码顺序）
            Map<Long, Set<Long>> producers = new HashMap<>();
            Map<Long, Set<Long>> deps = new HashMap<>();
            for (Map.Entry<Long, Map<String, Object>> e : workOrders.entrySet()) {
                Long product = id(e.getValue(), "materialId");
                producers.computeIfAbsent(product, k -> new HashSet<>()).add(e.getKey());
                woDocs.getOrDefault(e.getKey(), Map.of()).forEach((m, v) -> {
                    if (!m.equals(product) && (v[0].signum() != 0 || v[1].signum() != 0)) {
                        deps.computeIfAbsent(product, k -> new HashSet<>()).add(m);
                    }
                });
            }
            // 委外件依赖核销的子件（先算子件单价）
            outsourceConsume.forEach((product, parts) -> deps.computeIfAbsent(product, k -> new HashSet<>()).addAll(parts.keySet()));
            for (Long m : topoOrder(materialInfo.keySet(), deps)) {
                for (Long woId : producers.getOrDefault(m, Set.of())) {
                    computeWo(woId);
                }
                computeMaterial(m, producers.getOrDefault(m, Set.of()));
            }
        }

        private void computeWo(Long woId) {
            Map<String, Object> wo = workOrders.get(woId);
            Long product = id(wo, "materialId");
            FinWoCost c = new FinWoCost();
            c.setPeriod(period);
            c.setWoId(woId);
            FinWoCost p = prevWo.get(woId);
            c.setWipOpen(p == null ? BigDecimal.ZERO : p.getWipClose());
            BigDecimal material = BigDecimal.ZERO;
            BigDecimal finished = BigDecimal.ZERO;
            for (Map.Entry<Long, BigDecimal[]> e : woDocs.getOrDefault(woId, Map.of()).entrySet()) {
                BigDecimal[] v = e.getValue();
                if (e.getKey().equals(product)) {
                    finished = finished.add(v[2]);
                }
                BigDecimal net = v[0].subtract(v[1]);
                if (net.signum() != 0 && !e.getKey().equals(product)) {
                    FinMaterialCost mc = materials.get(e.getKey());
                    BigDecimal unit = mc != null ? mc.getUnitCost() : lastUnitCost(e.getKey());
                    material = material.add(money(net.multiply(unit)));
                }
            }
            BigDecimal[] r = woReports.getOrDefault(woId, zeros(3));
            c.setHours(r[0]);
            c.setMaterialCost(material);
            c.setLaborCost(money(r[1]));
            c.setOverheadCost(money(r[2]));
            c.setFinishedQty(finished);
            BigDecimal total = c.getWipOpen().add(material).add(c.getLaborCost()).add(c.getOverheadCost());
            BigDecimal remainingBefore = dec(wo, "qty").subtract(dec(wo, "finishedBefore"));
            BigDecimal finishedAmount;
            if (finished.signum() <= 0) {
                finishedAmount = BigDecimal.ZERO;
            } else if (finished.compareTo(remainingBefore) >= 0 || remainingBefore.signum() <= 0) {
                finishedAmount = total;
            } else {
                finishedAmount = money(total.multiply(finished).divide(remainingBefore, 10, RoundingMode.HALF_UP));
            }
            c.setFinishedAmount(finishedAmount);
            c.setUnitCost(finished.signum() > 0 ? finishedAmount.divide(finished, UNIT_SCALE, RoundingMode.HALF_UP) : null);
            // 本期及之前关闭的工单不再生产，剩余在产品成本作为生产损失转出
            Object closeTime = wo.get("closeTime");
            boolean closed = "CLOSED".equals(wo.get("status")) && closeTime != null
                    && periodOf(toLocalDate(closeTime)) <= period;
            BigDecimal remaining = total.subtract(finishedAmount);
            c.setLossAmount(closed ? remaining : BigDecimal.ZERO);
            c.setWipClose(closed ? BigDecimal.ZERO : remaining);
            woCosts.put(woId, c);
        }

        private void computeMaterial(Long m, Set<Long> producerWos) {
            FinMaterialCost c = new FinMaterialCost();
            c.setPeriod(period);
            c.setMaterialId(m);
            FinMaterialCost p = prev.get(m);
            c.setOpenQty(p != null ? p.getCloseQty() : openQtyFallback.getOrDefault(m, BigDecimal.ZERO));
            c.setOpenAmount(p != null ? p.getCloseAmount() : BigDecimal.ZERO);

            Map<String, BigDecimal[]> t = trade.getOrDefault(m, Map.of());
            BigDecimal[] pin = t.getOrDefault("PURCHASE_IN", zeros(2));
            BigDecimal[] pret = t.getOrDefault("PURCHASE_RETURN", zeros(2));

            BigDecimal issue = BigDecimal.ZERO;
            BigDecimal issueReturn = BigDecimal.ZERO;
            BigDecimal make = BigDecimal.ZERO;
            BigDecimal makeAmount = BigDecimal.ZERO;
            for (Map.Entry<Long, Map<Long, BigDecimal[]>> e : woDocs.entrySet()) {
                BigDecimal[] v = e.getValue().get(m);
                if (v == null) {
                    continue;
                }
                issue = issue.add(v[0]);
                issueReturn = issueReturn.add(v[1]);
                make = make.add(v[2]);
            }
            for (Long woId : producerWos) {
                makeAmount = makeAmount.add(woCosts.get(woId).getFinishedAmount());
            }
            BigDecimal[] s = stock.getOrDefault(m, zeros(4));
            BigDecimal salesOut = s[2];
            BigDecimal salesReturn = s[3];

            c.setPurchaseQty(pin[0].subtract(pret[0]));
            // 委外件：入库成本 = 加工费（采购入库金额）+ 核销的子件成本
            BigDecimal consumed = BigDecimal.ZERO;
            for (Map.Entry<Long, BigDecimal> e : outsourceConsume.getOrDefault(m, Map.of()).entrySet()) {
                FinMaterialCost part = materials.get(e.getKey());
                BigDecimal unitCost = part != null ? part.getUnitCost() : lastUnitCost(e.getKey());
                consumed = consumed.add(e.getValue().multiply(unitCost));
            }
            consumed = money(consumed);
            if (consumed.signum() != 0) {
                outsourceConsumedAmount.put(m, consumed);
            }
            c.setPurchaseAmount(money(pin[1].subtract(pret[1])).add(consumed));
            c.setMakeQty(make);
            c.setMakeAmount(makeAmount);
            // 其他出入库 = 库存流水合计 - 已归类的业务
            c.setOtherInQty(s[0].subtract(pin[0]).subtract(make).subtract(issueReturn).subtract(salesReturn));
            c.setOtherOutQty(s[1].subtract(pret[0]).subtract(issue).subtract(salesOut));
            c.setIssueQty(issue.subtract(issueReturn));
            c.setSalesQty(salesOut.subtract(salesReturn));

            BigDecimal baseQty = c.getOpenQty().add(c.getPurchaseQty()).add(make);
            BigDecimal baseAmount = c.getOpenAmount().add(c.getPurchaseAmount()).add(makeAmount);
            BigDecimal unit = baseQty.signum() > 0 && baseAmount.signum() >= 0
                    ? baseAmount.divide(baseQty, UNIT_SCALE, RoundingMode.HALF_UP) : lastUnitCost(m);
            c.setUnitCost(unit);
            c.setOtherInAmount(money(c.getOtherInQty().multiply(unit)));
            c.setIssueAmount(money(c.getIssueQty().multiply(unit)));
            c.setSalesAmount(money(c.getSalesQty().multiply(unit)));
            c.setOtherOutAmount(money(c.getOtherOutQty().multiply(unit)));
            c.setCloseQty(baseQty.add(c.getOtherInQty()).subtract(c.getIssueQty()).subtract(c.getSalesQty()).subtract(c.getOtherOutQty()));
            BigDecimal closeAmount = baseAmount.add(c.getOtherInAmount()).subtract(c.getIssueAmount())
                    .subtract(c.getSalesAmount()).subtract(c.getOtherOutAmount());
            // 期末数量为 0 时，尾差计入销售成本（或其他出库），避免留下金额
            if (c.getCloseQty().signum() == 0 && closeAmount.signum() != 0) {
                if (c.getSalesQty().signum() > 0) {
                    c.setSalesAmount(c.getSalesAmount().add(closeAmount));
                } else if (c.getIssueQty().signum() > 0) {
                    c.setIssueAmount(c.getIssueAmount().add(closeAmount));
                } else {
                    c.setOtherOutAmount(c.getOtherOutAmount().add(closeAmount));
                }
                closeAmount = BigDecimal.ZERO;
            }
            c.setCloseAmount(closeAmount);
            if (isEmpty(c)) {
                return;
            }
            materials.put(m, c);
        }

        /** 成本结转凭证分录 */
        List<VoucherLine> voucherLines() {
            String summary = "结转 " + period + " 期间成本";
            Long wip = accountService.leafByCode(PRODUCTION_COST).getId();
            Map<String, Long> accounts = new HashMap<>();
            Function<String, Long> account = code -> accounts.computeIfAbsent(code, k -> accountService.leafByCode(k).getId());
            List<VoucherLine> lines = new ArrayList<>();
            // 领料：按领用物料的存货科目，金额取工单成本中的材料成本合计（与工单口径一致）
            Map<String, BigDecimal> issueByAccount = new HashMap<>();
            materials.values().forEach(c -> issueByAccount.merge(inventoryAccount(category(c.getMaterialId())), c.getIssueAmount(), BigDecimal::add));
            BigDecimal issueTotal = issueByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal woMaterial = woCosts.values().stream().map(FinWoCost::getMaterialCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-生产领料", wip, null, woMaterial));
            issueByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-生产领料", account.apply(code), null, amount)));
            // 工单材料成本与物料领用金额的尾差（四舍五入）计入生产成本
            BigDecimal diff = issueTotal.subtract(woMaterial);
            if (diff.signum() != 0) {
                lines.add(VoucherLine.debit(summary + "-生产领料", wip, null, diff));
            }
            // 人工、制造费用按工时 × 费率分配到工单：生产岗位工资计提时已计入制造费用，这里从制造费用转出
            Long overheadAccount = account.apply(MANUFACTURING_OVERHEAD);
            BigDecimal labor = woCosts.values().stream().map(FinWoCost::getLaborCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-直接人工", wip, null, labor));
            lines.add(VoucherLine.credit(summary + "-直接人工", overheadAccount, null, labor));
            BigDecimal overhead = woCosts.values().stream().map(FinWoCost::getOverheadCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-制造费用", wip, null, overhead));
            lines.add(VoucherLine.credit(summary + "-制造费用", overheadAccount, null, overhead));
            // 制造费用差异：截至期末的制造费用余额（实际发生 - 已分配）全部转入主营业务成本
            BigDecimal balance = entryMapper.sumBalanceUpTo(overheadAccount, period);
            overheadVariance = money((balance == null ? BigDecimal.ZERO : balance).subtract(labor).subtract(overhead));
            lines.add(VoucherLine.debit(summary + "-制造费用差异", account.apply(COST_OF_SALES), null, overheadVariance));
            lines.add(VoucherLine.credit(summary + "-制造费用差异", overheadAccount, null, overheadVariance));
            // 关闭工单的剩余在产品转生产损失（计入主营业务成本）
            BigDecimal loss = woCosts.values().stream().map(FinWoCost::getLossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-关闭工单损失", account.apply(COST_OF_SALES), null, loss));
            lines.add(VoucherLine.credit(summary + "-关闭工单损失", wip, null, loss));
            // 完工入库：按产品的存货科目
            Map<String, BigDecimal> finishedByAccount = new HashMap<>();
            for (FinWoCost w : woCosts.values()) {
                Long product = id(workOrders.get(w.getWoId()), "materialId");
                finishedByAccount.merge(inventoryAccount(category(product)), w.getFinishedAmount(), BigDecimal::add);
            }
            finishedByAccount.forEach((code, amount) -> lines.add(VoucherLine.debit(summary + "-完工入库", account.apply(code), null, amount)));
            BigDecimal finished = finishedByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.credit(summary + "-完工入库", wip, null, finished));
            // 销售成本、其他出入库
            Map<String, BigDecimal> salesByAccount = new HashMap<>();
            Map<String, BigDecimal> otherByAccount = new HashMap<>();
            Map<String, BigDecimal> projectByAccount = new HashMap<>();
            Map<String, BigDecimal> outsourceByAccount = new HashMap<>();
            Map<String, BigDecimal> serviceByAccount = new HashMap<>();
            materials.values().forEach(c -> {
                String code = inventoryAccount(category(c.getMaterialId()));
                salesByAccount.merge(code, c.getSalesAmount(), BigDecimal::add);
                // 其他出库中的项目领料、委外发料按单价拆出，其余（盘盈盘亏等）计入待处理财产损溢
                BigDecimal project = money(projectOut.getOrDefault(c.getMaterialId(), BigDecimal.ZERO).multiply(c.getUnitCost()));
                BigDecimal outsource = money(outsourceOut.getOrDefault(c.getMaterialId(), BigDecimal.ZERO).multiply(c.getUnitCost()));
                BigDecimal service = money(serviceOut.getOrDefault(c.getMaterialId(), BigDecimal.ZERO).multiply(c.getUnitCost()));
                projectByAccount.merge(code, project, BigDecimal::add);
                outsourceByAccount.merge(code, outsource, BigDecimal::add);
                serviceByAccount.merge(code, service, BigDecimal::add);
                otherByAccount.merge(code, c.getOtherInAmount().subtract(c.getOtherOutAmount()).add(project).add(outsource).add(service),
                        BigDecimal::add);
            });
            BigDecimal project = projectByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-项目领料", account.apply(CONTRACT_COST), null, project));
            projectByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-项目领料", account.apply(code), null, amount)));
            // 委外收货核销用料：借 委外件存货科目 / 贷 委托加工物资
            Map<String, BigDecimal> consumedByAccount = new HashMap<>();
            outsourceConsumedAmount.forEach((product, amount) -> consumedByAccount.merge(inventoryAccount(category(product)), amount, BigDecimal::add));
            consumedByAccount.forEach((code, amount) -> lines.add(VoucherLine.debit(summary + "-委外核销用料", account.apply(code), null, amount)));
            BigDecimal consumedTotal = consumedByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.credit(summary + "-委外核销用料", account.apply(OUTSOURCE_MATERIAL), null, consumedTotal));
            BigDecimal outsource = outsourceByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-委外发料", account.apply(OUTSOURCE_MATERIAL), null, outsource));
            outsourceByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-委外发料", account.apply(code), null, amount)));
            // 低值易耗品部门领用：按实际单价一次摊销，借 领用单的费用科目（领用部门），贷 周转材料；不再计入待处理财产损溢
            Map<String, BigDecimal> consumeByAccount = new HashMap<>();
            for (Map<String, Object> row : consumeRows) {
                FinMaterialCost c = materials.get(id(row, "materialId"));
                if (c == null) {
                    continue;
                }
                BigDecimal amount = money(dec(row, "qty").multiply(c.getUnitCost()));
                Object dept = row.get("deptId");
                lines.add(VoucherLine.debit(summary + "-低值易耗品领用", id(row, "accountId"), dept == null ? null : ((Number) dept).longValue(), amount));
                consumeByAccount.merge(inventoryAccount(category(c.getMaterialId())), amount, BigDecimal::add);
            }
            consumeByAccount.forEach((code, amount) -> {
                lines.add(VoucherLine.credit(summary + "-低值易耗品领用", account.apply(code), null, amount));
                otherByAccount.merge(code, amount, BigDecimal::add);
            });
            BigDecimal service = serviceByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-售后备件", account.apply(SELLING_EXPENSE), null, service));
            serviceByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-售后备件", account.apply(code), null, amount)));
            BigDecimal sales = salesByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-销售成本", account.apply(COST_OF_SALES), null, sales));
            salesByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-销售成本", account.apply(code), null, amount)));
            BigDecimal other = otherByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            otherByAccount.forEach((code, amount) -> lines.add(VoucherLine.debit(summary + "-其他出入库", account.apply(code), null, amount)));
            lines.add(VoucherLine.credit(summary + "-其他出入库", account.apply(PENDING_LOSS), null, other));
            return lines.stream().anyMatch(l -> !l.isZero()) ? lines : List.of();
        }

        private String category(Long materialId) {
            WmsMaterial m = materialInfo.get(materialId);
            return m == null ? null : m.getCategory();
        }

        /** 有出库但单价为 0 的物料（没有期初、采购、生产金额），提示补录存货期初 */
        List<String> zeroCostWarnings() {
            return materials.values().stream()
                    .filter(c -> c.getUnitCost().signum() == 0
                            && (c.getIssueQty().signum() > 0 || c.getSalesQty().signum() > 0 || c.getOtherOutQty().signum() > 0))
                    .map(c -> {
                        WmsMaterial m = materialInfo.get(c.getMaterialId());
                        return m == null ? String.valueOf(c.getMaterialId()) : m.getCode() + " " + m.getName();
                    })
                    .toList();
        }

        /** 没有本期单价时取最近一次核算的单价 */
        /** 上期单价；没有上期单价时取物料的参考成本价 */
        private BigDecimal lastUnitCost(Long m) {
            FinMaterialCost p = prev.get(m);
            if (p != null && p.getUnitCost() != null && p.getUnitCost().signum() > 0) {
                return p.getUnitCost();
            }
            return refCosts.getOrDefault(m, p != null ? p.getUnitCost() : BigDecimal.ZERO);
        }
    }

    /** 拓扑排序：依赖（下层物料）在前；剩余（有环）按 ID 顺序追加 */
    private static List<Long> topoOrder(Collection<Long> all, Map<Long, Set<Long>> deps) {
        Map<Long, Integer> indegree = new HashMap<>();
        Map<Long, List<Long>> users = new HashMap<>();
        for (Long m : all) {
            indegree.putIfAbsent(m, 0);
        }
        deps.forEach((product, children) -> children.forEach(child -> {
            if (indegree.containsKey(child) && indegree.containsKey(product)) {
                indegree.merge(product, 1, Integer::sum);
                users.computeIfAbsent(child, k -> new ArrayList<>()).add(product);
            }
        }));
        Deque<Long> queue = new ArrayDeque<>(new TreeSet<>(indegree.entrySet().stream()
                .filter(e -> e.getValue() == 0).map(Map.Entry::getKey).toList()));
        List<Long> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long m = queue.poll();
            order.add(m);
            for (Long u : users.getOrDefault(m, List.of())) {
                if (indegree.merge(u, -1, Integer::sum) == 0) {
                    queue.add(u);
                }
            }
        }
        new TreeSet<>(all).stream().filter(m -> !order.contains(m)).forEach(order::add);
        return order;
    }

    private static boolean isEmpty(FinMaterialCost c) {
        return c.getOpenQty().signum() == 0 && c.getOpenAmount().signum() == 0 && c.getPurchaseQty().signum() == 0
                && c.getMakeQty().signum() == 0 && c.getOtherInQty().signum() == 0 && c.getIssueQty().signum() == 0
                && c.getSalesQty().signum() == 0 && c.getOtherOutQty().signum() == 0 && c.getCloseQty().signum() == 0;
    }

    private static LocalDate toLocalDate(Object v) {
        if (v instanceof java.time.LocalDateTime t) {
            return t.toLocalDate();
        }
        if (v instanceof java.sql.Timestamp t) {
            return t.toLocalDateTime().toLocalDate();
        }
        return LocalDate.parse(v.toString().substring(0, 10));
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal[] zeros(int n) {
        BigDecimal[] a = new BigDecimal[n];
        java.util.Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }

    private static Long id(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
    }

    private static BigDecimal dec(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
