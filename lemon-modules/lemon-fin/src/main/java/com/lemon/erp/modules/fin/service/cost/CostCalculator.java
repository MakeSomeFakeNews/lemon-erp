package com.lemon.erp.modules.fin.service.cost;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.entity.FinCostLayer;
import com.lemon.erp.modules.fin.entity.FinMaterialCost;
import com.lemon.erp.modules.fin.entity.FinWoCost;
import com.lemon.erp.modules.fin.service.cost.CostRows.Assembly;
import com.lemon.erp.modules.fin.service.cost.CostRows.CostEvent;
import com.lemon.erp.modules.fin.service.cost.CostRows.CostMaterial;
import com.lemon.erp.modules.fin.service.cost.CostRows.QtyAmount;
import com.lemon.erp.modules.fin.service.cost.CostRows.StockSums;
import com.lemon.erp.modules.fin.service.cost.CostRows.WoDocQty;
import com.lemon.erp.modules.fin.service.cost.CostRows.WoReport;
import com.lemon.erp.modules.fin.service.cost.CostRows.WorkOrderInfo;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.model.CostMethod;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockInType;
import com.lemon.erp.modules.wms.model.StockOutType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.lemon.erp.modules.fin.model.FinConstants.firstDay;
import static com.lemon.erp.modules.fin.model.FinConstants.periodOf;
import static com.lemon.erp.modules.fin.service.cost.Valuation.UNIT_SCALE;
import static com.lemon.erp.modules.fin.service.cost.Valuation.money;

/**
 * 成本核算的计算过程（纯计算，不访问数据库）：按「被领用物料 → 工单产品」的依赖顺序逐个物料计算，先算下层物料单价，
 * 再算用到它的工单成本，最后算产品单价。
 * <ul>
 *   <li>工单成本 = 期初在产品 + 本期领料（领用数量 × 物料单价）+ 人工 + 制造费用；完工产品成本按本期完工数量占剩余未完工数量的比例结转，
 *       工单全部完工时全部结转，其余为期末在产品；本期及之前关闭的工单剩余在产品转为损失</li>
 *   <li>月末一次加权平均单价 = (期初金额 + 采购金额 + 生产成本) / (期初数量 + 采购数量 + 生产数量)</li>
 *   <li>移动加权平均、先进先出按出入库的发生顺序逐笔计价（{@link Valuation}），本期出库总成本按数量分摊到领用、销售、其他出库</li>
 *   <li>组装的成套件按子件成本、拆卸的子件按成套件成本分摊入账；委外件入库成本 = 加工费 + 核销的子件成本</li>
 * </ul>
 * 一个实例只计算一次（{@link #compute()}）
 */
public final class CostCalculator {

    /** 中间计算（按比例分摊）的小数位 */
    private static final int RATIO_SCALE = 10;
    /** 不计入其他入库的入库类型：退料、销售退货、委外收回冲减对应的出库；组装拆卸按子件成本计价 */
    private static final Set<StockInType> NOT_OTHER_IN = EnumSet.of(StockInType.PRODUCTION_RETURN, StockInType.SALES_RETURN,
            StockInType.OUTSOURCE_RETURN, StockInType.ASSEMBLY);

    private final CostSource src;
    private final int period;
    /** 物料 → 本期成本（按计算顺序） */
    private final Map<Long, FinMaterialCost> materials = new LinkedHashMap<>();
    /** 工单 → 本期成本（按计算顺序） */
    private final Map<Long, FinWoCost> woCosts = new LinkedHashMap<>();
    /** 委外件 → 本期核销用料的金额 */
    private final Map<Long, BigDecimal> outsourceConsumedAmount = new HashMap<>();
    /** 组装拆卸单号 → 入库物料 → 入库成本（按出库物料的成本计算，第一次用到时计算） */
    private final Map<String, Map<Long, BigDecimal>> assemblyAmounts = new HashMap<>();
    /** 先进先出的本期结存批次 */
    private final List<FinCostLayer> closeLayers = new ArrayList<>();

    public CostCalculator(CostSource src) {
        this.src = src;
        this.period = src.getPeriod();
    }

    /**
     * 计算结果
     *
     * @param materials               物料 → 本期成本（按计算顺序，不含没有任何数据的物料）
     * @param woCosts                 工单 → 本期成本（按计算顺序）
     * @param closeLayers             先进先出的本期结存批次
     * @param outsourceConsumedAmount 委外件 → 本期核销用料的金额
     * @param assemblyAmounts         组装拆卸单号 → 入库物料 → 入库成本
     * @param zeroCostWarnings        有出库但单价为 0 的物料（编码 名称）
     */
    public record CostResult(Map<Long, FinMaterialCost> materials, Map<Long, FinWoCost> woCosts, List<FinCostLayer> closeLayers,
                             Map<Long, BigDecimal> outsourceConsumedAmount, Map<String, Map<Long, BigDecimal>> assemblyAmounts,
                             List<String> zeroCostWarnings) {
    }

    public CostResult compute() {
        // 依赖：工单领用的物料 → 工单产品；按拓扑顺序计算（有环时剩余的按编码顺序）
        Map<Long, Set<Long>> producers = new HashMap<>();
        Map<Long, Set<Long>> deps = new HashMap<>();
        for (Map.Entry<Long, WorkOrderInfo> e : src.getWorkOrders().entrySet()) {
            Long product = e.getValue().materialId();
            producers.computeIfAbsent(product, k -> new HashSet<>()).add(e.getKey());
            src.getWoDocs().getOrDefault(e.getKey(), Map.of()).forEach((m, v) -> {
                if (!m.equals(product) && (v.issue().signum() != 0 || v.returned().signum() != 0)) {
                    deps.computeIfAbsent(product, k -> new HashSet<>()).add(m);
                }
            });
        }
        // 委外件依赖核销的子件（先算子件单价）
        src.getOutsourceConsume().forEach((product, parts) -> deps.computeIfAbsent(product, k -> new HashSet<>()).addAll(parts.keySet()));
        // 组装：成套件依赖子件；拆卸：子件依赖成套件
        for (Assembly a : src.getAssemblies()) {
            if (a.assemble()) {
                deps.computeIfAbsent(a.product(), k -> new HashSet<>()).addAll(a.parts().keySet());
            } else {
                a.parts().keySet().forEach(p -> deps.computeIfAbsent(p, k -> new HashSet<>()).add(a.product()));
            }
        }
        for (Long m : topoOrder(src.getMaterials().keySet(), deps)) {
            for (Long woId : producers.getOrDefault(m, Set.of())) {
                computeWo(woId);
            }
            computeMaterial(m, producers.getOrDefault(m, Set.of()));
        }
        // 计算过程中没有用到的组装拆卸单（不影响入库成本的计价）按最终单价计算，供凭证使用
        src.getAssemblies().forEach(this::assemblyAmounts);
        return new CostResult(Collections.unmodifiableMap(materials), Collections.unmodifiableMap(woCosts),
                Collections.unmodifiableList(closeLayers), Collections.unmodifiableMap(outsourceConsumedAmount),
                Collections.unmodifiableMap(assemblyAmounts), zeroCostWarnings());
    }

    private void computeWo(Long woId) {
        WorkOrderInfo wo = src.getWorkOrders().get(woId);
        Long product = wo.materialId();
        FinWoCost c = new FinWoCost();
        c.setPeriod(period);
        c.setWoId(woId);
        FinWoCost p = src.getPrevWo().get(woId);
        c.setWipOpen(p == null ? BigDecimal.ZERO : p.getWipClose());
        BigDecimal material = BigDecimal.ZERO;
        BigDecimal finished = BigDecimal.ZERO;
        for (Map.Entry<Long, WoDocQty> e : src.getWoDocs().getOrDefault(woId, Map.of()).entrySet()) {
            WoDocQty v = e.getValue();
            if (e.getKey().equals(product)) {
                finished = finished.add(v.finished());
            }
            BigDecimal net = v.issue().subtract(v.returned());
            if (net.signum() != 0 && !e.getKey().equals(product)) {
                material = material.add(money(net.multiply(unitOf(e.getKey()))));
            }
        }
        WoReport r = src.getWoReports().getOrDefault(woId, WoReport.ZERO);
        c.setHours(r.hours());
        c.setMaterialCost(material);
        c.setLaborCost(money(r.labor()));
        c.setOverheadCost(money(r.overhead()));
        c.setFinishedQty(finished);
        BigDecimal total = c.getWipOpen().add(material).add(c.getLaborCost()).add(c.getOverheadCost());
        BigDecimal remainingBefore = wo.qty().subtract(wo.finishedBefore());
        BigDecimal finishedAmount;
        if (finished.signum() <= 0) {
            finishedAmount = BigDecimal.ZERO;
        } else if (finished.compareTo(remainingBefore) >= 0 || remainingBefore.signum() <= 0) {
            finishedAmount = total;
        } else {
            finishedAmount = money(total.multiply(finished).divide(remainingBefore, RATIO_SCALE, RoundingMode.HALF_UP));
        }
        c.setFinishedAmount(finishedAmount);
        c.setUnitCost(finished.signum() > 0 ? finishedAmount.divide(finished, UNIT_SCALE, RoundingMode.HALF_UP) : null);
        // 本期及之前关闭的工单不再生产，剩余在产品成本作为生产损失转出
        boolean closed = wo.status() == WorkOrderStatus.CLOSED && wo.closeDate() != null && periodOf(wo.closeDate()) <= period;
        BigDecimal remaining = total.subtract(finishedAmount);
        c.setLossAmount(closed ? remaining : BigDecimal.ZERO);
        c.setWipClose(closed ? BigDecimal.ZERO : remaining);
        woCosts.put(woId, c);
    }

    private void computeMaterial(Long m, Set<Long> producerWos) {
        FinMaterialCost c = new FinMaterialCost();
        c.setPeriod(period);
        c.setMaterialId(m);
        FinMaterialCost p = src.getPrev().get(m);
        c.setOpenQty(p != null ? p.getCloseQty() : src.getOpenQtyFallback().getOrDefault(m, BigDecimal.ZERO));
        c.setOpenAmount(p != null ? p.getCloseAmount() : BigDecimal.ZERO);

        Map<TradeKind, QtyAmount> t = src.getTrade().getOrDefault(m, Map.of());
        QtyAmount pin = t.getOrDefault(TradeKind.PURCHASE_IN, QtyAmount.ZERO);
        QtyAmount pret = t.getOrDefault(TradeKind.PURCHASE_RETURN, QtyAmount.ZERO);

        BigDecimal issue = BigDecimal.ZERO;
        BigDecimal issueReturn = BigDecimal.ZERO;
        BigDecimal make = BigDecimal.ZERO;
        BigDecimal makeAmount = BigDecimal.ZERO;
        for (Map<Long, WoDocQty> byMaterial : src.getWoDocs().values()) {
            WoDocQty v = byMaterial.get(m);
            if (v == null) {
                continue;
            }
            issue = issue.add(v.issue());
            issueReturn = issueReturn.add(v.returned());
            make = make.add(v.finished());
        }
        for (Long woId : producerWos) {
            makeAmount = makeAmount.add(woCosts.get(woId).getFinishedAmount());
        }
        // 组装得到的成套件、拆卸得到的子件按生产入库计：成本取所耗物料的成本
        BigDecimal assembled = src.getAssemblyIn().getOrDefault(m, BigDecimal.ZERO);
        if (assembled.signum() != 0) {
            make = make.add(assembled);
            makeAmount = makeAmount.add(assemblyInAmount(m));
        }
        StockSums s = src.getStock().getOrDefault(m, StockSums.ZERO);
        BigDecimal salesOut = s.salesOut();
        BigDecimal salesReturn = s.salesReturn();

        c.setPurchaseQty(pin.qty().subtract(pret.qty()));
        // 委外件：入库成本 = 加工费（采购入库金额）+ 核销的子件成本
        BigDecimal consumed = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> e : src.getOutsourceConsume().getOrDefault(m, Map.of()).entrySet()) {
            consumed = consumed.add(e.getValue().multiply(unitOf(e.getKey())));
        }
        consumed = money(consumed);
        if (consumed.signum() != 0) {
            outsourceConsumedAmount.put(m, consumed);
        }
        c.setPurchaseAmount(money(pin.amount().subtract(pret.amount())).add(consumed));
        c.setMakeQty(make);
        c.setMakeAmount(makeAmount);
        // 其他出入库 = 库存流水合计 - 已归类的业务
        c.setOtherInQty(s.inQty().subtract(pin.qty()).subtract(make).subtract(issueReturn).subtract(salesReturn));
        c.setOtherOutQty(s.outQty().subtract(pret.qty()).subtract(issue).subtract(salesOut));
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
        List<CostEvent> list = src.getEvents().getOrDefault(m, List.of());
        // 委外件核销的子件成本按采购入库数量分摊到各次入库
        BigDecimal purchaseQty = list.stream().filter(r -> r.is(StockInType.PURCHASE) && r.hasTrade())
                .map(CostEvent::qty).filter(q -> q.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal consumedLeft = consumed;
        BigDecimal otherIn = BigDecimal.ZERO;
        for (CostEvent row : list) {
            BigDecimal qty = row.qty();
            if (qty.signum() == 0) {
                continue;
            }
            LocalDate date = row.bizDate();
            if (row.is(StockInType.PURCHASE) && row.hasTrade() && qty.signum() > 0) {
                BigDecimal share = purchaseQty.signum() > 0
                        ? money(consumed.multiply(qty).divide(purchaseQty, RATIO_SCALE, RoundingMode.HALF_UP)) : BigDecimal.ZERO;
                share = share.min(consumedLeft.max(BigDecimal.ZERO));
                consumedLeft = consumedLeft.subtract(share);
                v.in(date, qty, tradeAmount(row, qty).add(share));
            } else if (row.is(StockOutType.PURCHASE_RETURN) && row.hasTrade() && qty.signum() < 0) {
                v.returnOut(qty.negate(), tradeAmount(row, qty.negate()));
            } else if (row.is(StockInType.ASSEMBLY) && qty.signum() > 0 && row.sourceNo() != null) {
                v.in(date, qty, assemblyEventAmount(row.sourceNo(), m, qty));
            } else if (row.woId() != null && qty.signum() > 0) {
                FinWoCost w = woCosts.get(row.woId());
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
        if (v instanceof FifoValuation fifo) {
            int seq = 1;
            for (FifoValuation.Layer l : fifo.closingLayers()) {
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
        for (Assembly a : src.getAssemblies()) {
            total = total.add(assemblyAmounts(a).getOrDefault(m, BigDecimal.ZERO));
        }
        return total;
    }

    /** 逐笔计价时一笔组装拆卸入库的金额：该单中此物料的入库成本（按数量比例） */
    private BigDecimal assemblyEventAmount(String assemblyNo, Long m, BigDecimal qty) {
        Assembly a = src.getAssemblies().stream().filter(x -> x.no().equals(assemblyNo)).findFirst().orElse(null);
        if (a == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = assemblyAmounts(a).getOrDefault(m, BigDecimal.ZERO);
        BigDecimal docQty = a.assemble() ? a.qty() : a.parts().getOrDefault(m, BigDecimal.ZERO);
        return docQty.signum() == 0 || docQty.compareTo(qty) == 0 ? amount
                : money(amount.multiply(qty).divide(docQty, RATIO_SCALE, RoundingMode.HALF_UP));
    }

    /**
     * 一张组装拆卸单的入库成本：组装的成套件 = Σ 子件数量 × 子件单价；拆卸时成套件成本（数量 × 单价）
     * 按子件数量 × 参考单价（上期单价或参考成本价，都没有时按数量）分摊到各子件，尾差计入最后一个。
     * 第一次用到时按当时已算出的单价计算并缓存
     */
    private Map<Long, BigDecimal> assemblyAmounts(Assembly a) {
        return assemblyAmounts.computeIfAbsent(a.no(), no -> allocateAssembly(a));
    }

    private Map<Long, BigDecimal> allocateAssembly(Assembly a) {
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
        if (Decimals.sum(weights.values()).signum() == 0) {
            weights.clear();
            weights.putAll(a.parts());
        }
        allocate(total, weights).forEach((p, amount) -> result.merge(p, amount, BigDecimal::add));
        return result;
    }

    /**
     * 按权重分摊金额（保留 2 位），尾差计入最后一个；权重合计为 0 时除最后一个外都为 0
     *
     * @return 与 weights 顺序一致
     */
    static Map<Long, BigDecimal> allocate(BigDecimal total, Map<Long, BigDecimal> weights) {
        BigDecimal weightSum = Decimals.sum(weights.values());
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal allocated = BigDecimal.ZERO;
        List<Long> keys = new ArrayList<>(weights.keySet());
        for (int i = 0; i < keys.size(); i++) {
            Long k = keys.get(i);
            BigDecimal amount = i == keys.size() - 1 ? total.subtract(allocated)
                    : weightSum.signum() == 0 ? BigDecimal.ZERO
                    : money(total.multiply(weights.get(k)).divide(weightSum, RATIO_SCALE, RoundingMode.HALF_UP));
            result.put(k, amount);
            allocated = allocated.add(amount);
        }
        return result;
    }

    /** 已核算物料的本期单价，尚未核算（有环）时取上期单价 */
    private BigDecimal unitOf(Long m) {
        FinMaterialCost c = materials.get(m);
        return c != null ? c.getUnitCost() : lastUnitCost(m);
    }

    /** 出库金额的尾差：依次计入销售成本、生产领用、其他出库 */
    private static void addOutResidual(FinMaterialCost c, BigDecimal residual) {
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
    private static boolean isOtherIn(CostEvent row) {
        if (row.isAny(NOT_OTHER_IN)) {
            return false;
        }
        StockBizType kind = row.docKind();
        return kind == null ? row.qty().signum() > 0 : kind == StockBizType.IN;
    }

    /** 采购入库 / 退货单据中该物料的不含税金额（出入库数量与采购单据不同时按比例） */
    private static BigDecimal tradeAmount(CostEvent row, BigDecimal qty) {
        BigDecimal net = row.tradeNet();
        BigDecimal tradeQty = row.tradeQty();
        if (tradeQty.signum() == 0 || tradeQty.compareTo(qty) == 0) {
            return money(net);
        }
        return money(net.multiply(qty).divide(tradeQty, RATIO_SCALE, RoundingMode.HALF_UP));
    }

    /** 先进先出的期初批次：上期结存批次与上期期末一致时沿用，否则（首次使用先进先出等）以上期期末为一个批次 */
    private List<FifoValuation.Layer> openingLayers(Long m, FinMaterialCost c) {
        List<FinCostLayer> layers = src.getPrevLayers().getOrDefault(m, List.of());
        BigDecimal qty = Decimals.sum(layers, FinCostLayer::getQty);
        BigDecimal amount = Decimals.sum(layers, FinCostLayer::getAmount);
        if (!layers.isEmpty() && qty.compareTo(c.getOpenQty()) == 0 && amount.compareTo(c.getOpenAmount()) == 0) {
            return layers.stream().map(l -> new FifoValuation.Layer(l.getInDate(), l.getQty(), l.getAmount())).toList();
        }
        if (c.getOpenQty().signum() == 0 && c.getOpenAmount().signum() == 0) {
            return List.of();
        }
        return List.of(new FifoValuation.Layer(firstDay(period).minusDays(1), c.getOpenQty(), c.getOpenAmount()));
    }

    private CostMethod method(Long m) {
        CostMaterial info = src.getMaterials().get(m);
        return info == null || info.costMethod() == null ? CostMethod.WEIGHTED : info.costMethod();
    }

    /** 有出库但单价为 0 的物料（没有期初、采购、生产金额），提示补录存货期初 */
    private List<String> zeroCostWarnings() {
        return materials.values().stream()
                .filter(c -> c.getUnitCost().signum() == 0
                        && (c.getIssueQty().signum() > 0 || c.getSalesQty().signum() > 0 || c.getOtherOutQty().signum() > 0))
                .map(c -> {
                    CostMaterial m = src.getMaterials().get(c.getMaterialId());
                    return m == null ? String.valueOf(c.getMaterialId()) : m.code() + " " + m.name();
                })
                .toList();
    }

    /** 上期单价；没有上期单价时取物料的参考成本价 */
    private BigDecimal lastUnitCost(Long m) {
        FinMaterialCost p = src.getPrev().get(m);
        if (p != null && p.getUnitCost() != null && p.getUnitCost().signum() > 0) {
            return p.getUnitCost();
        }
        return src.getRefCosts().getOrDefault(m, p != null ? p.getUnitCost() : BigDecimal.ZERO);
    }

    /** 拓扑排序：依赖（下层物料）在前；剩余（有环）按 ID 顺序追加 */
    static List<Long> topoOrder(Collection<Long> all, Map<Long, Set<Long>> deps) {
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
}
