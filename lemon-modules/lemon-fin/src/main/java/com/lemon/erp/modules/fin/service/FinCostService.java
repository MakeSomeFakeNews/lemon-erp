package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.CostRunResult;
import com.lemon.erp.modules.fin.entity.FinCostLayer;
import com.lemon.erp.modules.fin.entity.FinCostRun;
import com.lemon.erp.modules.fin.entity.FinMaterialCost;
import com.lemon.erp.modules.fin.entity.FinVoucher;
import com.lemon.erp.modules.fin.entity.FinWoCost;
import com.lemon.erp.modules.fin.mapper.FinCostLayerMapper;
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
import com.lemon.erp.modules.wms.model.AssemblyKind;
import com.lemon.erp.modules.wms.model.CostMethod;
import com.lemon.erp.modules.wms.model.StockBizType;
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

import static com.lemon.erp.modules.fin.model.FinConstants.firstDay;
import static com.lemon.erp.modules.fin.model.FinConstants.lastDay;
import static com.lemon.erp.modules.fin.model.FinConstants.periodOf;
import static com.lemon.erp.modules.fin.model.FinConstants.previous;

/**
 * 成本核算（按期间）：按物料的计价方法（月末一次加权平均、移动加权平均、先进先出）计算存货成本，工单归集料工费并结转完工产品成本。
 * <ol>
 *   <li>数量取仓库流水（按单据业务日期归属期间，不含调拨），销售按仓库出库类型统计；采购金额取采购入库 / 退货的不含税金额，
 *       没有经过采购订单的入库计入其他入库</li>
 *   <li>按「被领用物料 → 工单产品」的依赖顺序逐个物料计算：先算出下层物料单价，再算用到它的工单成本，最后算产品单价，
 *       所以半成品、成品的单价包含本期的材料成本</li>
 *   <li>工单成本 = 期初在产品 + 本期领料（领用数量 × 物料单价）+ 人工（工时 × 人工费率）+ 制造费用（工时 × 费用率）；
 *       完工产品成本按本期完工数量占剩余未完工数量的比例结转，工单全部完工时全部结转，其余为期末在产品</li>
 *   <li>加权平均单价 = (期初金额 + 采购金额 + 生产成本) / (期初数量 + 采购数量 + 生产数量)；
 *       领用、销售、其他出入库（盘盈盘亏等）按该单价计价</li>
 *   <li>移动加权平均、先进先出的物料按出入库的发生顺序逐笔计价（{@link Valuation}）：采购入库按不含税金额、完工入库按工单单位成本入账，
 *       其他入库按当时的成本；得到期末结存后，本期出库总成本按数量分摊到领用、销售、其他出库。先进先出的期末结存批次保存到 fin_cost_layer</li>
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
    /** 出入库类型（字典 wms_in_type / wms_out_type）：采购入库、采购退货 */
    private static final String PURCHASE = "PURCHASE";
    private static final String PURCHASE_RETURN = "PURCHASE_RETURN";
    /** 退料、销售退货、委外收回：冲减对应的出库，不是其他入库 */
    private static final Set<String> RETURN_TYPES = Set.of("PRODUCTION_RETURN", "SALES_RETURN", "OUTSOURCE_RETURN");
    /** 组装拆卸单生成的出入库 */
    private static final String ASSEMBLY = "ASSEMBLY";

    private final FinCostRunMapper runMapper;
    private final FinMaterialCostMapper materialCostMapper;
    private final FinWoCostMapper woCostMapper;
    private final FinCostLayerMapper layerMapper;
    private final FinCostSourceMapper sourceMapper;
    private final FinVoucherMapper voucherMapper;
    private final FinVoucherEntryMapper entryMapper;
    private final FinVoucherService voucherService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
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
                throw new BizException("fin.error.inventoryOpeningPeriodCannotCostedStart", period);
            }
            discard(old);
        }
        if (runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().gt(FinCostRun::getPeriod, period))) {
            throw new BizException("fin.error.laterPeriodsHaveCostedCannotRecalculate", period);
        }
        // 已有更早的核算（或存货期初）时，必须逐月核算，否则期初金额会按 0 处理
        if (runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().lt(FinCostRun::getPeriod, period))
                && !runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getPeriod, previous(period)))) {
            throw new BizException("fin.error.runCostAccountingPeriodFirst", previous(period));
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
        if (!calc.closeLayers.isEmpty()) {
            layerMapper.insertBatch(calc.closeLayers);
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
            throw new BizException("fin.error.noCostAccountingHasRunPeriod", period);
        }
        if (OPENING.equals(run.getKind())) {
            throw new BizException("fin.error.inventoryOpeningModifyOpeningBalances", period);
        }
        if (runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().gt(FinCostRun::getPeriod, period))) {
            throw new BizException("fin.error.undoCostAccountingLaterPeriodsFirst");
        }
        discard(run);
    }

    private void discard(FinCostRun run) {
        if (run.getVoucherId() != null) {
            FinVoucher v = voucherMapper.selectById(run.getVoucherId());
            if (v != null && !VoucherStatus.DRAFT.name().equals(v.getStatus())) {
                throw new BizException("fin.error.costTransferVoucherApprovedUnapproveBefore", v.getVoucherNo());
            }
            if (v != null) {
                voucherService.delete(List.of(v.getId()));
            }
        }
        materialCostMapper.delete(Wrappers.<FinMaterialCost>lambdaQuery().eq(FinMaterialCost::getPeriod, run.getPeriod()));
        woCostMapper.delete(Wrappers.<FinWoCost>lambdaQuery().eq(FinWoCost::getPeriod, run.getPeriod()));
        layerMapper.delete(Wrappers.<FinCostLayer>lambdaQuery().eq(FinCostLayer::getPeriod, run.getPeriod()));
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
        /** 本期已完成的组装拆卸单 */
        private final List<Assembly> assemblies = new ArrayList<>();
        /** 物料 → 组装拆卸的入库数量（组装的成套件、拆卸的子件）、出库数量 */
        private final Map<Long, BigDecimal> assemblyIn = new HashMap<>();
        private final Map<Long, BigDecimal> assemblyOut = new HashMap<>();
        /** 组装拆卸单号 → 入库物料 → 入库成本（按出库物料的成本计算） */
        private final Map<String, Map<Long, BigDecimal>> assemblyAmounts = new HashMap<>();
        /** 移动加权平均、先进先出的物料 → 本期出入库明细（按发生顺序） */
        private final Map<Long, List<Map<String, Object>>> events = new HashMap<>();
        /** 先进先出的物料 → 上期结存批次 */
        private final Map<Long, List<FinCostLayer>> prevLayers = new HashMap<>();
        /** 先进先出的本期结存批次 */
        private final List<FinCostLayer> closeLayers = new ArrayList<>();

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
                } else if (ASSEMBLY.equals(row.get("bizType"))) {
                    assemblyIn.merge(id(row, "materialId"), dec(row, "inQty"), BigDecimal::add);
                    assemblyOut.merge(id(row, "materialId"), dec(row, "outQty"), BigDecimal::add);
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
            Map<String, Assembly> byNo = new LinkedHashMap<>();
            for (Map<String, Object> row : sourceMapper.selectAssemblies(begin, end)) {
                Assembly a = byNo.computeIfAbsent((String) row.get("assemblyNo"), no -> new Assembly(no,
                        AssemblyKind.ASSEMBLE.name().equals(row.get("kind")), id(row, "productId"), dec(row, "qty"), new LinkedHashMap<>()));
                a.parts().merge(id(row, "partId"), dec(row, "partQty"), BigDecimal::add);
            }
            assemblies.addAll(byNo.values());
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
            assemblies.forEach(a -> {
                all.add(a.product());
                all.addAll(a.parts().keySet());
            });
            materialInfo = materialService.mapByIds(all);
            Set<Long> sequenced = materialInfo.values().stream()
                    .filter(m -> m.getCostMethod() != null && m.getCostMethod() != CostMethod.WEIGHTED)
                    .map(WmsMaterial::getId).collect(Collectors.toSet());
            if (!sequenced.isEmpty()) {
                for (Map<String, Object> row : sourceMapper.selectCostEvents(begin, end, sequenced)) {
                    events.computeIfAbsent(id(row, "materialId"), k -> new ArrayList<>()).add(row);
                }
                layerMapper.selectList(Wrappers.<FinCostLayer>lambdaQuery().eq(FinCostLayer::getPeriod, prevPeriod)
                                .in(FinCostLayer::getMaterialId, sequenced).orderByAsc(FinCostLayer::getSeq))
                        .forEach(l -> prevLayers.computeIfAbsent(l.getMaterialId(), k -> new ArrayList<>()).add(l));
            }
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
            // 组装：成套件依赖子件；拆卸：子件依赖成套件
            for (Assembly a : assemblies) {
                if (a.assemble()) {
                    deps.computeIfAbsent(a.product(), k -> new HashSet<>()).addAll(a.parts().keySet());
                } else {
                    a.parts().keySet().forEach(p -> deps.computeIfAbsent(p, k -> new HashSet<>()).add(a.product()));
                }
            }
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
            // 组装得到的成套件、拆卸得到的子件按生产入库计：成本取所耗物料的成本
            BigDecimal assembled = assemblyIn.getOrDefault(m, BigDecimal.ZERO);
            if (assembled.signum() != 0) {
                make = make.add(assembled);
                makeAmount = makeAmount.add(assemblyInAmount(m));
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
            c.setCloseQty(baseQty.add(c.getOtherInQty()).subtract(c.getIssueQty()).subtract(c.getSalesQty()).subtract(c.getOtherOutQty()));
            CostMethod method = method(m);
            c.setCostMethod(method);
            if (method == CostMethod.WEIGHTED) {
                valueWeighted(c, m, baseQty, baseAmount);
            } else {
                valueSequenced(c, m, method, baseAmount, consumed);
            }
            if (isEmpty(c)) {
                return;
            }
            materials.put(m, c);
        }

        /** 月末一次加权平均：出库、其他入库都按 (期初 + 采购 + 生产) 的平均单价计价 */
        private void valueWeighted(FinMaterialCost c, Long m, BigDecimal baseQty, BigDecimal baseAmount) {
            BigDecimal unit = baseQty.signum() > 0 && baseAmount.signum() >= 0
                    ? baseAmount.divide(baseQty, UNIT_SCALE, RoundingMode.HALF_UP) : lastUnitCost(m);
            c.setUnitCost(unit);
            c.setOtherInAmount(money(c.getOtherInQty().multiply(unit)));
            c.setIssueAmount(money(c.getIssueQty().multiply(unit)));
            c.setSalesAmount(money(c.getSalesQty().multiply(unit)));
            c.setOtherOutAmount(money(c.getOtherOutQty().multiply(unit)));
            BigDecimal closeAmount = baseAmount.add(c.getOtherInAmount()).subtract(c.getIssueAmount())
                    .subtract(c.getSalesAmount()).subtract(c.getOtherOutAmount());
            // 期末数量为 0 时，尾差计入销售成本（或其他出库），避免留下金额
            if (c.getCloseQty().signum() == 0 && closeAmount.signum() != 0) {
                addOutResidual(c, closeAmount);
                closeAmount = BigDecimal.ZERO;
            }
            c.setCloseAmount(closeAmount);
        }

        /**
         * 移动加权平均、先进先出：按业务发生顺序逐笔计价，得到期末结存金额和其他入库金额；
         * 本期出库总成本 = 期初 + 采购 + 生产 + 其他入库 − 期末结存，领用、销售、其他出库按数量分摊（出库单价为本期出库的平均单价）
         */
        private void valueSequenced(FinMaterialCost c, Long m, CostMethod method, BigDecimal baseAmount, BigDecimal consumed) {
            Valuation v = method == CostMethod.FIFO ? new FifoValuation(openingLayers(m, c), lastUnitCost(m))
                    : new MovingValuation(c.getOpenQty(), c.getOpenAmount(), lastUnitCost(m));
            List<Map<String, Object>> list = events.getOrDefault(m, List.of());
            // 委外件核销的子件成本按采购入库数量分摊到各次入库
            BigDecimal purchaseQty = list.stream().filter(r -> PURCHASE.equals(r.get("bizType")) && r.get("tradeNet") != null)
                    .map(r -> dec(r, "qty")).filter(q -> q.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal consumedLeft = consumed;
            BigDecimal otherIn = BigDecimal.ZERO;
            for (Map<String, Object> row : list) {
                BigDecimal qty = dec(row, "qty");
                if (qty.signum() == 0) {
                    continue;
                }
                String type = (String) row.get("bizType");
                LocalDate date = toLocalDate(row.get("bizDate"));
                if (PURCHASE.equals(type) && row.get("tradeNet") != null && qty.signum() > 0) {
                    BigDecimal share = purchaseQty.signum() > 0 ? money(consumed.multiply(qty).divide(purchaseQty, 10, RoundingMode.HALF_UP)) : BigDecimal.ZERO;
                    share = share.min(consumedLeft.max(BigDecimal.ZERO));
                    consumedLeft = consumedLeft.subtract(share);
                    v.in(date, qty, tradeAmount(row, qty).add(share));
                } else if (PURCHASE_RETURN.equals(type) && row.get("tradeNet") != null && qty.signum() < 0) {
                    v.returnOut(qty.negate(), tradeAmount(row, qty.negate()));
                } else if (ASSEMBLY.equals(type) && qty.signum() > 0 && row.get("sourceNo") != null) {
                    v.in(date, qty, assemblyEventAmount((String) row.get("sourceNo"), m, qty));
                } else if (row.get("woId") != null && qty.signum() > 0) {
                    FinWoCost w = woCosts.get(id(row, "woId"));
                    BigDecimal unit = w != null && w.getUnitCost() != null ? w.getUnitCost() : v.unit();
                    v.in(date, qty, money(qty.multiply(unit)));
                } else if (qty.signum() > 0) {
                    BigDecimal amount = v.inAtCost(date, qty);
                    if (isOtherIn(row)) {
                        otherIn = otherIn.add(amount);
                    }
                } else {
                    BigDecimal cost = v.out(qty.negate());
                    // 红字入库单冲减其他入库
                    if (isOtherIn(row)) {
                        otherIn = otherIn.subtract(cost);
                    }
                }
            }
            // 尾差（分摊委外子件成本的四舍五入）计入最后结存
            if (consumedLeft.signum() != 0 && purchaseQty.signum() > 0) {
                v.adjust(consumedLeft);
            }
            BigDecimal closeAmount = v.closeAmount();
            c.setOtherInAmount(money(otherIn));
            BigDecimal outQty = c.getIssueQty().add(c.getSalesQty()).add(c.getOtherOutQty());
            BigDecimal outValue = baseAmount.add(c.getOtherInAmount()).subtract(closeAmount);
            BigDecimal unit;
            if (outQty.signum() > 0) {
                unit = outValue.divide(outQty, UNIT_SCALE, RoundingMode.HALF_UP);
            } else {
                unit = c.getCloseQty().signum() > 0 ? closeAmount.divide(c.getCloseQty(), UNIT_SCALE, RoundingMode.HALF_UP) : v.unit();
            }
            c.setUnitCost(unit.signum() < 0 ? BigDecimal.ZERO : unit);
            BigDecimal outUnit = outQty.signum() > 0 ? unit : BigDecimal.ZERO;
            c.setIssueAmount(money(c.getIssueQty().multiply(outUnit)));
            c.setSalesAmount(money(c.getSalesQty().multiply(outUnit)));
            c.setOtherOutAmount(money(c.getOtherOutQty().multiply(outUnit)));
            addOutResidual(c, outValue.subtract(c.getIssueAmount()).subtract(c.getSalesAmount()).subtract(c.getOtherOutAmount()));
            c.setCloseAmount(closeAmount);
            if (method == CostMethod.FIFO) {
                int seq = 1;
                for (FifoValuation.Layer l : ((FifoValuation) v).closingLayers()) {
                    FinCostLayer layer = new FinCostLayer();
                    layer.setPeriod(period);
                    layer.setMaterialId(m);
                    layer.setSeq(seq++);
                    layer.setInDate(l.date);
                    layer.setQty(l.qty);
                    layer.setAmount(l.amount);
                    closeLayers.add(layer);
                }
            }
        }

        /** 物料本期组装拆卸入库的成本合计 */
        private BigDecimal assemblyInAmount(Long m) {
            BigDecimal total = BigDecimal.ZERO;
            for (Assembly a : assemblies) {
                total = total.add(assemblyAmounts(a).getOrDefault(m, BigDecimal.ZERO));
            }
            return total;
        }

        /** 逐笔计价时一笔组装拆卸入库的金额：该单中此物料的入库成本（按数量比例） */
        private BigDecimal assemblyEventAmount(String assemblyNo, Long m, BigDecimal qty) {
            Assembly a = assemblies.stream().filter(x -> x.no().equals(assemblyNo)).findFirst().orElse(null);
            if (a == null) {
                return BigDecimal.ZERO;
            }
            BigDecimal amount = assemblyAmounts(a).getOrDefault(m, BigDecimal.ZERO);
            BigDecimal docQty = a.assemble() ? a.qty() : a.parts().getOrDefault(m, BigDecimal.ZERO);
            return docQty.signum() == 0 || docQty.compareTo(qty) == 0 ? amount : money(amount.multiply(qty).divide(docQty, 10, RoundingMode.HALF_UP));
        }

        /**
         * 一张组装拆卸单的入库成本：组装的成套件 = Σ 子件数量 × 子件单价；拆卸时成套件成本（数量 × 单价）
         * 按子件数量 × 参考单价（上期单价或参考成本价，都没有时按数量）分摊到各子件，尾差计入最后一个
         */
        private Map<Long, BigDecimal> assemblyAmounts(Assembly a) {
            return assemblyAmounts.computeIfAbsent(a.no(), no -> {
                Map<Long, BigDecimal> result = new LinkedHashMap<>();
                if (a.assemble()) {
                    BigDecimal total = BigDecimal.ZERO;
                    for (Map.Entry<Long, BigDecimal> e : a.parts().entrySet()) {
                        total = total.add(money(e.getValue().multiply(unitOf(e.getKey()))));
                    }
                    result.put(a.product(), total);
                    return result;
                }
                BigDecimal total = money(a.qty().multiply(unitOf(a.product())));
                Map<Long, BigDecimal> weights = new LinkedHashMap<>();
                a.parts().forEach((p, q) -> weights.put(p, q.multiply(lastUnitCost(p))));
                BigDecimal weightSum = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                if (weightSum.signum() == 0) {
                    weights.clear();
                    weights.putAll(a.parts());
                    weightSum = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                BigDecimal allocated = BigDecimal.ZERO;
                List<Long> parts = new ArrayList<>(weights.keySet());
                for (int i = 0; i < parts.size(); i++) {
                    Long p = parts.get(i);
                    BigDecimal amount = i == parts.size() - 1 ? total.subtract(allocated)
                            : weightSum.signum() == 0 ? BigDecimal.ZERO : money(total.multiply(weights.get(p)).divide(weightSum, 10, RoundingMode.HALF_UP));
                    result.merge(p, amount, BigDecimal::add);
                    allocated = allocated.add(amount);
                }
                return result;
            });
        }

        /** 已核算物料的本期单价，尚未核算（有环）时取上期单价 */
        private BigDecimal unitOf(Long m) {
            FinMaterialCost c = materials.get(m);
            return c != null ? c.getUnitCost() : lastUnitCost(m);
        }

        /** 出库金额的尾差：依次计入销售成本、生产领用、其他出库 */
        private void addOutResidual(FinMaterialCost c, BigDecimal residual) {
            if (residual.signum() == 0) {
                return;
            }
            if (c.getSalesQty().signum() > 0) {
                c.setSalesAmount(c.getSalesAmount().add(residual));
            } else if (c.getIssueQty().signum() > 0) {
                c.setIssueAmount(c.getIssueAmount().add(residual));
            } else {
                c.setOtherOutAmount(c.getOtherOutAmount().add(residual));
            }
        }

        /** 其他入库：入库单（非采购、完工、退货）和盘盈；退料、销售退货冲减出库，不计入其他入库 */
        private boolean isOtherIn(Map<String, Object> row) {
            String type = (String) row.get("bizType");
            if (RETURN_TYPES.contains(type) || ASSEMBLY.equals(type)) {
                return false;
            }
            Object kind = row.get("docKind");
            return kind == null ? dec(row, "qty").signum() > 0 : StockBizType.IN.name().equals(kind);
        }

        /** 采购入库 / 退货单据中该物料的不含税金额（出入库数量与采购单据不同时按比例） */
        private BigDecimal tradeAmount(Map<String, Object> row, BigDecimal qty) {
            BigDecimal net = dec(row, "tradeNet");
            BigDecimal tradeQty = dec(row, "tradeQty");
            if (tradeQty.signum() == 0 || tradeQty.compareTo(qty) == 0) {
                return money(net);
            }
            return money(net.multiply(qty).divide(tradeQty, 10, RoundingMode.HALF_UP));
        }

        /** 先进先出的期初批次：上期结存批次与上期期末一致时沿用，否则（首次使用先进先出等）以上期期末为一个批次 */
        private List<FifoValuation.Layer> openingLayers(Long m, FinMaterialCost c) {
            List<FinCostLayer> layers = prevLayers.getOrDefault(m, List.of());
            BigDecimal qty = layers.stream().map(FinCostLayer::getQty).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal amount = layers.stream().map(FinCostLayer::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (!layers.isEmpty() && qty.compareTo(c.getOpenQty()) == 0 && amount.compareTo(c.getOpenAmount()) == 0) {
                return layers.stream().map(l -> new FifoValuation.Layer(l.getInDate(), l.getQty(), l.getAmount())).toList();
            }
            if (c.getOpenQty().signum() == 0 && c.getOpenAmount().signum() == 0) {
                return List.of();
            }
            return List.of(new FifoValuation.Layer(firstDay(period).minusDays(1), c.getOpenQty(), c.getOpenAmount()));
        }

        private CostMethod method(Long m) {
            WmsMaterial info = materialInfo.get(m);
            return info == null || info.getCostMethod() == null ? CostMethod.WEIGHTED : info.getCostMethod();
        }

        /** 成本结转凭证分录 */
        List<VoucherLine> voucherLines() {
            String summary = "结转 " + period + " 期间成本"; // i18n-ignore
            Long wip = accountRules.accountId(AccountRole.PRODUCTION_COST);
            // 物料分类 → 存货科目
            Map<String, Long> inventoryAccounts = new HashMap<>();
            Function<String, Long> inventory = category -> inventoryAccounts.computeIfAbsent(category,
                    k -> accountRules.accountId(AccountRole.INVENTORY, k));
            List<VoucherLine> lines = new ArrayList<>();
            // 领料：按领用物料的存货科目，金额取工单成本中的材料成本合计（与工单口径一致）
            Map<Long, BigDecimal> issueByAccount = new HashMap<>();
            materials.values().forEach(c -> issueByAccount.merge(inventory.apply(category(c.getMaterialId())), c.getIssueAmount(), BigDecimal::add));
            BigDecimal issueTotal = issueByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal woMaterial = woCosts.values().stream().map(FinWoCost::getMaterialCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-生产领料", wip, null, woMaterial)); // i18n-ignore
            issueByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-生产领料", code, null, amount))); // i18n-ignore
            // 工单材料成本与物料领用金额的尾差（四舍五入）计入生产成本
            BigDecimal diff = issueTotal.subtract(woMaterial);
            if (diff.signum() != 0) {
                lines.add(VoucherLine.debit(summary + "-生产领料", wip, null, diff)); // i18n-ignore
            }
            // 人工、制造费用按工时 × 费率分配到工单：生产岗位工资计提时已计入制造费用，这里从制造费用转出
            Long overheadAccount = accountRules.accountId(AccountRole.MANUFACTURING_OVERHEAD);
            BigDecimal labor = woCosts.values().stream().map(FinWoCost::getLaborCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-直接人工", wip, null, labor)); // i18n-ignore
            lines.add(VoucherLine.credit(summary + "-直接人工", overheadAccount, null, labor)); // i18n-ignore
            BigDecimal overhead = woCosts.values().stream().map(FinWoCost::getOverheadCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-制造费用", wip, null, overhead)); // i18n-ignore
            lines.add(VoucherLine.credit(summary + "-制造费用", overheadAccount, null, overhead)); // i18n-ignore
            // 制造费用差异：截至期末的制造费用余额（实际发生 - 已分配）全部转入主营业务成本
            BigDecimal balance = entryMapper.sumBalanceUpTo(overheadAccount, period);
            overheadVariance = money((balance == null ? BigDecimal.ZERO : balance).subtract(labor).subtract(overhead));
            lines.add(VoucherLine.debit(summary + "-制造费用差异", accountRules.accountId(AccountRole.COST_OF_SALES), null, overheadVariance)); // i18n-ignore
            lines.add(VoucherLine.credit(summary + "-制造费用差异", overheadAccount, null, overheadVariance)); // i18n-ignore
            // 关闭工单的剩余在产品转生产损失（计入主营业务成本）
            BigDecimal loss = woCosts.values().stream().map(FinWoCost::getLossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-关闭工单损失", accountRules.accountId(AccountRole.COST_OF_SALES), null, loss)); // i18n-ignore
            lines.add(VoucherLine.credit(summary + "-关闭工单损失", wip, null, loss)); // i18n-ignore
            // 完工入库：按产品的存货科目
            Map<Long, BigDecimal> finishedByAccount = new HashMap<>();
            for (FinWoCost w : woCosts.values()) {
                Long product = id(workOrders.get(w.getWoId()), "materialId");
                finishedByAccount.merge(inventory.apply(category(product)), w.getFinishedAmount(), BigDecimal::add);
            }
            finishedByAccount.forEach((code, amount) -> lines.add(VoucherLine.debit(summary + "-完工入库", code, null, amount))); // i18n-ignore
            BigDecimal finished = finishedByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.credit(summary + "-完工入库", wip, null, finished)); // i18n-ignore
            // 销售成本、其他出入库
            Map<Long, BigDecimal> salesByAccount = new HashMap<>();
            // 主营业务成本按物料分类取科目
            Map<String, BigDecimal> salesByCategory = new HashMap<>();
            Map<Long, BigDecimal> otherByAccount = new HashMap<>();
            Map<Long, BigDecimal> projectByAccount = new HashMap<>();
            Map<Long, BigDecimal> outsourceByAccount = new HashMap<>();
            Map<Long, BigDecimal> serviceByAccount = new HashMap<>();
            Map<Long, BigDecimal> assemblyOutByAccount = new HashMap<>();
            materials.values().forEach(c -> {
                Long code = inventory.apply(category(c.getMaterialId()));
                salesByAccount.merge(code, c.getSalesAmount(), BigDecimal::add);
                salesByCategory.merge(category(c.getMaterialId()), c.getSalesAmount(), BigDecimal::add);
                // 其他出库中的项目领料、委外发料按单价拆出，其余（盘盈盘亏等）计入待处理财产损溢
                BigDecimal project = money(projectOut.getOrDefault(c.getMaterialId(), BigDecimal.ZERO).multiply(c.getUnitCost()));
                BigDecimal outsource = money(outsourceOut.getOrDefault(c.getMaterialId(), BigDecimal.ZERO).multiply(c.getUnitCost()));
                BigDecimal service = money(serviceOut.getOrDefault(c.getMaterialId(), BigDecimal.ZERO).multiply(c.getUnitCost()));
                BigDecimal assemblyOutAmount = money(assemblyOut.getOrDefault(c.getMaterialId(), BigDecimal.ZERO).multiply(c.getUnitCost()));
                assemblyOutByAccount.merge(code, assemblyOutAmount, BigDecimal::add);
                projectByAccount.merge(code, project, BigDecimal::add);
                outsourceByAccount.merge(code, outsource, BigDecimal::add);
                serviceByAccount.merge(code, service, BigDecimal::add);
                otherByAccount.merge(code, c.getOtherInAmount().subtract(c.getOtherOutAmount()).add(project).add(outsource).add(service)
                        .add(assemblyOutAmount), BigDecimal::add);
            });
            BigDecimal project = projectByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-项目领料", accountRules.accountId(AccountRole.CONTRACT_COST), null, project)); // i18n-ignore
            projectByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-项目领料", code, null, amount))); // i18n-ignore
            // 委外收货核销用料：借 委外件存货科目 / 贷 委托加工物资
            Map<Long, BigDecimal> consumedByAccount = new HashMap<>();
            outsourceConsumedAmount.forEach((product, amount) -> consumedByAccount.merge(inventory.apply(category(product)), amount, BigDecimal::add));
            consumedByAccount.forEach((code, amount) -> lines.add(VoucherLine.debit(summary + "-委外核销用料", code, null, amount))); // i18n-ignore
            BigDecimal consumedTotal = consumedByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.credit(summary + "-委外核销用料", accountRules.accountId(AccountRole.OUTSOURCE_MATERIAL), null, consumedTotal)); // i18n-ignore
            BigDecimal outsource = outsourceByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-委外发料", accountRules.accountId(AccountRole.OUTSOURCE_MATERIAL), null, outsource)); // i18n-ignore
            outsourceByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-委外发料", code, null, amount))); // i18n-ignore
            // 低值易耗品部门领用：按实际单价一次摊销，借 领用单的费用科目（领用部门），贷 周转材料；不再计入待处理财产损溢
            Map<Long, BigDecimal> consumeByAccount = new HashMap<>();
            for (Map<String, Object> row : consumeRows) {
                FinMaterialCost c = materials.get(id(row, "materialId"));
                if (c == null) {
                    continue;
                }
                BigDecimal amount = money(dec(row, "qty").multiply(c.getUnitCost()));
                Object dept = row.get("deptId");
                lines.add(VoucherLine.debit(summary + "-低值易耗品领用", id(row, "accountId"), dept == null ? null : ((Number) dept).longValue(), amount)); // i18n-ignore
                consumeByAccount.merge(inventory.apply(category(c.getMaterialId())), amount, BigDecimal::add);
            }
            consumeByAccount.forEach((code, amount) -> {
                lines.add(VoucherLine.credit(summary + "-低值易耗品领用", code, null, amount)); // i18n-ignore
                otherByAccount.merge(code, amount, BigDecimal::add);
            });
            // 组装拆卸：借 入库物料（组装的成套件、拆卸的子件）的存货科目 / 贷 出库物料的存货科目，尾差计入待处理财产损溢
            Map<Long, BigDecimal> assemblyInByAccount = new HashMap<>();
            for (Assembly a : assemblies) {
                assemblyAmounts(a).forEach((m, amount) -> assemblyInByAccount.merge(inventory.apply(category(m)), amount, BigDecimal::add));
            }
            assemblyInByAccount.forEach((code, amount) -> lines.add(VoucherLine.debit(summary + "-组装拆卸", code, null, amount))); // i18n-ignore
            assemblyOutByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-组装拆卸", code, null, amount))); // i18n-ignore
            BigDecimal assemblyDiff = assemblyOutByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .subtract(assemblyInByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            lines.add(VoucherLine.debit(summary + "-组装拆卸尾差", accountRules.accountId(AccountRole.PENDING_LOSS), null, assemblyDiff)); // i18n-ignore
            BigDecimal service = serviceByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(VoucherLine.debit(summary + "-售后备件", accountRules.accountId(AccountRole.SELLING_EXPENSE), null, service)); // i18n-ignore
            serviceByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-售后备件", code, null, amount))); // i18n-ignore
            salesByCategory.forEach((category, amount) -> lines.add(
                    VoucherLine.debit(summary + "-销售成本", accountRules.accountId(AccountRole.COST_OF_SALES, category), null, amount))); // i18n-ignore
            salesByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-销售成本", code, null, amount))); // i18n-ignore
            BigDecimal other = otherByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            otherByAccount.forEach((code, amount) -> lines.add(VoucherLine.debit(summary + "-其他出入库", code, null, amount))); // i18n-ignore
            lines.add(VoucherLine.credit(summary + "-其他出入库", accountRules.accountId(AccountRole.PENDING_LOSS), null, other)); // i18n-ignore
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

    /** 一张组装拆卸单：组装（assemble）时子件出库、成套件入库，拆卸相反 */
    private record Assembly(String no, boolean assemble, Long product, BigDecimal qty, Map<Long, BigDecimal> parts) {
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
