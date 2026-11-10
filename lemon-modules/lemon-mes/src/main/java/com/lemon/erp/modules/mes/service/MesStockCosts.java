package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.framework.finance.StandardCostProvider;
import com.lemon.erp.framework.finance.StandardCostProvider.StandardCost;
import com.lemon.erp.modules.mes.entity.MesWoDoc;
import com.lemon.erp.modules.mes.entity.MesWoMaterial;
import com.lemon.erp.modules.mes.entity.MesWoOp;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.mapper.MesWoDocMapper;
import com.lemon.erp.modules.mes.mapper.MesWoMaterialMapper;
import com.lemon.erp.modules.mes.mapper.MesWoOpMapper;
import com.lemon.erp.modules.mes.model.FinishCostBasis;
import com.lemon.erp.modules.mes.model.WoDocKind;
import com.lemon.erp.modules.wms.model.MovingAverage;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockOutType;
import com.lemon.erp.modules.wms.service.StockCostService;
import com.lemon.erp.modules.wms.service.StockCostService.CostSum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生产、委外的实时库存成本（业务参考成本，财务记账仍以月末成本核算为准）：
 * <ul>
 *   <li>完工入库：产品在所在期间生效的标准成本版本中有标准成本时按标准成本（料工费构成取自标准成本）；否则按工单实际成本 =
 *       净领料成本分摊（{@link WoFinishCost}）+ 工费（{@link WoConversionCost}：报工工时 × 工作中心人工 / 制造费用率，
 *       没有报工工时的工序按标准工时）；既没有已计价的领料也没有工费时按当前均价。每张完工入库单记录口径和料工费（{@code mes_wo_doc}）</li>
 *   <li>生产退料：按该工单该物料的领料平均成本入库（不按当前均价），工单的净领料成本 = 领料成本 − 退料成本；
 *       已有完工入库时，退料后已转入产品的材料成本超过「按新的净领料重新计算的已完工部分」的差额冲减产品成本（{@link #afterReturn}）</li>
 *   <li>最后一次完工：累计完工达到 工单数量 − 报工报废 时取全部剩余的净领料成本和工费（报废品的料工费由合格品承担）</li>
 *   <li>关闭工单（以及报工报废、改量后不经完工入库而完工）：未转入产品的净领料成本和工费，有完工时追加到已完工产品，
 *       没有完工时为在制品损失（{@link #applyClose}）</li>
 *   <li>委外收回（加工商退回子件）：按该委外订单该子件的发料平均成本入库</li>
 *   <li>委外件收货：采购入库按加工费（不含税单价）计价后，再追加核销的子件成本（核销数量 × 该订单该子件的发料平均成本）；
 *       委外件退货反向冲回</li>
 * </ul>
 * 冲减 / 追加到已完工产品的成本只计入仍在库的部分：比例 = min(产品当前计价数量, 工单完工数量) / 工单完工数量，
 * 其余对应已出库（销售、领用）的产品，不再进入库存（实时成本没有销售成本账，财务月末核算另行结转）。
 * 已按标准成本完工入库的工单，实际成本与标准成本的差异不调整库存成本（见财务「成本差异」）。
 */
@Service
@RequiredArgsConstructor
public class MesStockCosts {

    private static final Set<WoDocKind> ISSUE_KINDS = EnumSet.of(WoDocKind.ISSUE, WoDocKind.SUPPLEMENT);
    private static final BigDecimal CENT = new BigDecimal("0.01");

    private final MesWoDocMapper docMapper;
    private final MesWoMaterialMapper woMaterialMapper;
    private final MesWoOpMapper woOpMapper;
    private final StockCostService costService;
    private final ObjectProvider<StandardCostProvider> standardCosts;

    /**
     * 完工入库的成本
     *
     * @param unitCost 成本单价，为空按当前均价（{@link FinishCostBasis#AVERAGE}，料工费在入库后按实际计价金额记录）
     */
    public record FinishCost(FinishCostBasis basis, BigDecimal unitCost, BigDecimal material, BigDecimal labor, BigDecimal overhead) {
    }

    /**
     * 关闭工单时尚未转入产品的成本
     *
     * @param standard    已按标准成本完工入库：不调整，其余金额为 0
     * @param finishedQty 已完工数量
     * @param material    未转入的净领料成本
     * @param conversion  未转入的工费
     * @param toStock     其中追加到已完工产品库存的部分（预计，按产品当前库存比例）
     */
    public record CloseCost(boolean standard, BigDecimal finishedQty, BigDecimal material, BigDecimal conversion, BigDecimal toStock) {
        public BigDecimal total() {
            return material.add(conversion);
        }

        /** 不进入库存的部分：没有完工时为在制品损失，有完工时对应已出库的产品 */
        public BigDecimal toExpense() {
            return total().subtract(toStock);
        }
    }

    /**
     * 完工入库的成本，在完工数量回写之前调用
     *
     * @param wo  工单（完工数量为本次之前的累计）
     * @param qty 本次完工数量
     */
    public FinishCost finishCost(MesWorkOrder wo, LocalDate bizDate, BigDecimal qty) {
        StandardCostProvider provider = standardCosts.getIfAvailable();
        if (provider != null) {
            StandardCost std = provider.costs(bizDate, List.of(wo.getMaterialId())).get(wo.getMaterialId());
            if (std != null) {
                return new FinishCost(FinishCostBasis.STANDARD, std.unitCost(), money(qty.multiply(std.material())),
                        money(qty.multiply(std.labor())), money(qty.multiply(std.overhead())));
            }
        }
        Absorbed absorbed = absorbed(wo.getId());
        BigDecimal target = targetQty(wo);
        BigDecimal material = WoFinishCost.amount(target, wo.getFinishedQty(), qty, issued(wo), absorbed.material());
        WoConversionCost.Amount conversion = WoConversionCost.amount(target, wo.getFinishedQty(), qty, ops(wo.getId()),
                absorbed.conversion());
        if (material == null && conversion.total().signum() == 0) {
            return new FinishCost(FinishCostBasis.AVERAGE, null, null, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal m = material == null ? BigDecimal.ZERO : material;
        BigDecimal unit = m.add(conversion.total()).divide(qty, MovingAverage.PRICE_SCALE, RoundingMode.HALF_UP);
        return new FinishCost(FinishCostBasis.ACTUAL, unit, m, conversion.labor(), conversion.overhead());
    }

    /** 按当前均价入库的完工入库单：材料成本记为实际计价金额 */
    public BigDecimal docAmount(Long wmsDocId) {
        return costService.docAmount(StockBizType.IN, wmsDocId);
    }

    /** 生产退料的成本单价：该工单各物料的领料（含补料）平均成本，没有领料成本的物料不在结果中 */
    public Map<Long, BigDecimal> returnUnitCosts(Long woId) {
        return unitCosts(costService.docCosts(StockBizType.OUT, ids(wmsDocs(woId), ISSUE_KINDS)));
    }

    /**
     * 完工后退料：在退料单（含仓库入库）保存之后调用。按新的净领料重新计算已完工数量应承担的材料成本，
     * 已转入的超过它时冲减已完工产品的成本（不低于 0），只冲减仍在库的部分。工单没有完工入库、按标准成本完工时不处理
     *
     * @return [冲减的材料成本（负数）, 其中计入库存的部分（负数）]，不处理时为空
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal[] afterReturn(MesWorkOrder wo) {
        BigDecimal finished = wo.getFinishedQty();
        if (finished.signum() <= 0) {
            return null;
        }
        Absorbed absorbed = absorbed(wo.getId());
        if (absorbed.standard()) {
            return null;
        }
        BigDecimal target = WoFinishCost.amount(targetQty(wo), BigDecimal.ZERO, finished, issued(wo), BigDecimal.ZERO);
        // 按当前均价完工入库的金额不来自领料，不参与冲减
        BigDecimal over = absorbed.actualMaterial().subtract(target == null ? BigDecimal.ZERO : target);
        BigDecimal reduce = over.min(absorbed.actualMaterial().max(BigDecimal.ZERO));
        if (reduce.compareTo(CENT) < 0) {
            return null;
        }
        BigDecimal applied = toStock(wo, reduce.negate());
        return new BigDecimal[] {reduce.negate(), applied};
    }

    /** 关闭工单前预览：尚未转入产品的成本 */
    public CloseCost closeCost(MesWorkOrder wo) {
        Absorbed absorbed = absorbed(wo.getId());
        BigDecimal finished = wo.getFinishedQty();
        if (absorbed.standard()) {
            return new CloseCost(true, finished, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal net = issued(wo).values().stream().map(WoFinishCost.Issued::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 已转入的超过净领料（如先按当前均价完工入库）时不冲减
        BigDecimal material = money(net.subtract(absorbed.material()).max(BigDecimal.ZERO));
        BigDecimal conversion = WoConversionCost.incurred(ops(wo.getId()), finished).total().subtract(absorbed.conversion().total())
                .max(BigDecimal.ZERO);
        BigDecimal total = material.add(conversion);
        BigDecimal toStock = finished.signum() <= 0 || lastFinishWmsDoc(wo.getId()) == null ? BigDecimal.ZERO
                : money(total.multiply(stockRatio(wo)));
        return new CloseCost(false, finished, material, conversion, toStock);
    }

    /**
     * 关闭工单：未转入产品的成本有完工时追加到已完工产品（只追加仍在库的部分），没有完工时不进入库存（在制品损失）
     *
     * @return [未转入产品的成本, 其中实际追加到产品库存的部分]
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal[] applyClose(MesWorkOrder wo) {
        CloseCost c = closeCost(wo);
        if (c.standard()) {
            return new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO};
        }
        BigDecimal applied = c.toStock().signum() == 0 ? BigDecimal.ZERO : apply(wo, c.toStock());
        return new BigDecimal[] {c.total(), applied};
    }

    /** 委外订单各子件的发料平均成本，没有发料成本的子件不在结果中 */
    public Map<Long, BigDecimal> outsourceIssueUnitCosts(String orderNo) {
        return unitCosts(costService.sourceCosts(StockOutType.OUTSOURCE, orderNo));
    }

    // ---------------------------------------------------------------- 内部

    /**
     * 已转入产品的成本
     *
     * @param material 材料成本（含按当前均价完工入库的金额、退料冲减）
     * @param average  其中按当前均价完工入库的金额（不来自工单的领料）
     * @param standard 有按标准成本完工入库的单据
     */
    private record Absorbed(BigDecimal material, BigDecimal average, WoConversionCost.Amount conversion, boolean standard) {
        /** 来自工单领料的材料成本（不含按当前均价完工入库的金额） */
        BigDecimal actualMaterial() {
            return material.subtract(average);
        }
    }

    private Absorbed absorbed(Long woId) {
        BigDecimal material = BigDecimal.ZERO;
        BigDecimal average = BigDecimal.ZERO;
        BigDecimal labor = BigDecimal.ZERO;
        BigDecimal overhead = BigDecimal.ZERO;
        boolean standard = false;
        for (Map<String, Object> row : docMapper.selectCostSums(woId)) {
            WoDocKind kind = WoDocKind.valueOf(row.get("kind").toString());
            FinishCostBasis basis = row.get("basis") == null ? null : FinishCostBasis.valueOf(row.get("basis").toString());
            BigDecimal m = decimal(row.get("material"));
            material = material.add(m);
            if (kind == WoDocKind.FINISH) {
                labor = labor.add(decimal(row.get("labor")));
                overhead = overhead.add(decimal(row.get("overhead")));
                standard |= basis == FinishCostBasis.STANDARD;
                if (basis == FinishCostBasis.AVERAGE) {
                    average = average.add(m);
                }
            }
        }
        return new Absorbed(material, average, new WoConversionCost.Amount(labor, overhead), standard);
    }

    /** 工单各物料的净领料（领料 + 补料 − 退料的数量和成本） */
    private Map<Long, WoFinishCost.Issued> issued(MesWorkOrder wo) {
        Map<WoDocKind, List<Long>> docs = wmsDocs(wo.getId());
        Map<Long, CostSum> out = costService.docCosts(StockBizType.OUT, ids(docs, ISSUE_KINDS));
        Map<Long, WoFinishCost.Issued> issued = new HashMap<>();
        if (out.isEmpty()) {
            return issued;
        }
        Map<Long, CostSum> returned = costService.docCosts(StockBizType.IN, ids(docs, EnumSet.of(WoDocKind.RETURN)));
        Map<Long, BigDecimal> required = woMaterialMapper.selectList(Wrappers.<MesWoMaterial>lambdaQuery()
                        .eq(MesWoMaterial::getWoId, wo.getId())).stream()
                .collect(Collectors.toMap(MesWoMaterial::getMaterialId, MesWoMaterial::getRequiredQty, BigDecimal::add));
        out.forEach((materialId, o) -> {
            CostSum r = returned.get(materialId);
            // 出库流水为负数、退料入库为正数
            BigDecimal netQty = o.qty().negate().subtract(r == null ? BigDecimal.ZERO : r.qty());
            BigDecimal netAmount = o.amount().negate().subtract(r == null ? BigDecimal.ZERO : r.amount());
            issued.put(materialId, new WoFinishCost.Issued(netQty, netAmount, required.getOrDefault(materialId, BigDecimal.ZERO)));
        });
        return issued;
    }

    private List<WoConversionCost.Op> ops(Long woId) {
        return woOpMapper.selectCostOps(woId).stream()
                .map(o -> new WoConversionCost.Op(o.getHours(), o.getStdHours(), o.getGoodQty(), o.getLaborRate(), o.getOverheadRate()))
                .toList();
    }

    /**
     * 应完工数量 = 工单数量 − 报工报废：累计完工达到它即为最后一次完工，取全部剩余成本（报废品耗用的料工费由合格品承担）
     */
    private static BigDecimal targetQty(MesWorkOrder wo) {
        BigDecimal scrap = wo.getScrapQty() == null ? BigDecimal.ZERO : wo.getScrapQty();
        return wo.getQty().subtract(scrap).max(BigDecimal.ZERO);
    }

    /** 仍在库的比例 = min(产品当前计价数量, 完工数量) / 完工数量 */
    private BigDecimal stockRatio(MesWorkOrder wo) {
        BigDecimal finished = wo.getFinishedQty();
        BigDecimal stock = costService.valuedQty(wo.getMaterialId()).max(BigDecimal.ZERO);
        return stock.min(finished).divide(finished, 10, RoundingMode.HALF_UP);
    }

    /** 把 amount × 在库比例 追加到最后一张完工入库单的产品成本，返回实际追加的金额 */
    private BigDecimal toStock(MesWorkOrder wo, BigDecimal amount) {
        return apply(wo, money(amount.multiply(stockRatio(wo))));
    }

    private BigDecimal apply(MesWorkOrder wo, BigDecimal amount) {
        Long wmsDocId = lastFinishWmsDoc(wo.getId());
        if (wmsDocId == null || amount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return costService.addCost(StockBizType.IN, wmsDocId, Map.of(wo.getMaterialId(), amount))
                .getOrDefault(wo.getMaterialId(), BigDecimal.ZERO);
    }

    private Long lastFinishWmsDoc(Long woId) {
        MesWoDoc doc = Queries.first(docMapper, Wrappers.<MesWoDoc>lambdaQuery().select(MesWoDoc::getWmsDocId)
                .eq(MesWoDoc::getWoId, woId).eq(MesWoDoc::getKind, WoDocKind.FINISH).isNotNull(MesWoDoc::getWmsDocId)
                .orderByDesc(MesWoDoc::getId));
        return doc == null ? null : doc.getWmsDocId();
    }

    private static Map<Long, BigDecimal> unitCosts(Map<Long, CostSum> sums) {
        Map<Long, BigDecimal> map = new HashMap<>();
        sums.forEach((id, s) -> {
            BigDecimal unit = s.unitCost();
            if (unit != null) {
                map.put(id, unit.abs());
            }
        });
        return map;
    }

    /** 工单各类单据生成的仓库单据 ID */
    private Map<WoDocKind, List<Long>> wmsDocs(Long woId) {
        return docMapper.selectList(Wrappers.<MesWoDoc>lambdaQuery()
                        .select(MesWoDoc::getKind, MesWoDoc::getWmsDocId)
                        .eq(MesWoDoc::getWoId, woId)).stream()
                .filter(d -> d.getWmsDocId() != null)
                .collect(Collectors.groupingBy(MesWoDoc::getKind, Collectors.mapping(MesWoDoc::getWmsDocId, Collectors.toList())));
    }

    private static List<Long> ids(Map<WoDocKind, List<Long>> docs, Set<WoDocKind> kinds) {
        return kinds.stream().flatMap(k -> docs.getOrDefault(k, List.of()).stream()).toList();
    }

    private static BigDecimal decimal(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
