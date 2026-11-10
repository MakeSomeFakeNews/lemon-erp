package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.wms.dto.StockFlowView;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsMaterialCost;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.mapper.WmsMaterialCostMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockDocItemMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockFlowMapper;
import com.lemon.erp.modules.wms.mapper.WmsWarehouseMapper;
import com.lemon.erp.modules.wms.model.MovingAverage;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockChange;
import com.lemon.erp.modules.wms.model.StockDocType;
import com.lemon.erp.modules.wms.model.WarehouseType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 实时库存成本（移动加权平均，按物料、全公司不分仓库）：在 {@link StockService#apply} 的事务中，
 * 按物料 ID 顺序加行锁后随库存变动更新，并给每条流水计价。供应商寄售仓的库存属于供应商，不计价。
 * <ul>
 *   <li>入库按 {@link StockChange#unitCost()} 计价（采购入库的不含税单价、其他入库填写的成本价、销售退货的原出库成本等），没有时按当前均价</li>
 *   <li>出库按当前均价（采购退货按退货单价）；调拨（含在途）不改变数量和均价，在途调拨收货按调出时的成本计价；盘盈按填写的成本价（为空按当前均价），盘亏按当前均价</li>
 *   <li>物料还没有成本记录时初始化：数量 = 当前计价的库存合计，单价 = 参考成本价</li>
 * </ul>
 * 财务月末的成本核算（fin 模块）另行按物料的计价方法计算，用于记账，不使用这里的结果。
 */
@Service
@RequiredArgsConstructor
public class StockCostService {

    private final WmsMaterialCostMapper costMapper;
    private final WmsMaterialMapper materialMapper;
    private final WmsWarehouseMapper warehouseMapper;
    private final WmsStockFlowMapper flowMapper;
    private final WmsStockDocItemMapper docItemMapper;

    /** 数量、成本金额合计（与流水同号：入库为正、出库为负） */
    public record CostSum(BigDecimal qty, BigDecimal amount) {
        /** 单价 = 金额 / 数量，数量为 0 时为空 */
        public BigDecimal unitCost() {
            return qty.signum() == 0 ? null : amount.divide(qty, MovingAverage.PRICE_SCALE, RoundingMode.HALF_UP);
        }
    }

    /** 一条流水的计价，不计价（供应商寄售仓）时为 {@link #NONE} */
    public record Priced(BigDecimal unitCost, BigDecimal amount) {
        public static final Priced NONE = new Priced(null, null);
    }

    /**
     * 给库存变动计价并更新物料成本，必须在库存变动之前调用（初始化取的是变动前的库存）
     *
     * @return 与 changes 一一对应的计价
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Priced> price(StockBizType bizType, List<StockChange> changes, Map<Long, WmsMaterial> materials) {
        Set<Long> unvalued = unvaluedWarehouses(changes.stream().map(StockChange::warehouseId).collect(Collectors.toSet()));
        List<Priced> result = new ArrayList<>(changes.size());
        Set<Long> materialIds = changes.stream().filter(c -> !unvalued.contains(c.warehouseId()))
                .map(StockChange::materialId).collect(Collectors.toCollection(TreeSet::new));
        if (materialIds.isEmpty()) {
            changes.forEach(c -> result.add(Priced.NONE));
            return result;
        }
        Map<Long, WmsMaterialCost> rows = lock(materialIds, materials);
        Map<Long, MovingAverage.State> states = new HashMap<>();
        rows.forEach((id, r) -> states.put(id, new MovingAverage.State(r.getQty(), r.getAmount(), r.getUnitCost())));
        Set<Long> changed = new HashSet<>();
        for (StockChange c : changes) {
            if (unvalued.contains(c.warehouseId())) {
                result.add(Priced.NONE);
                continue;
            }
            WmsMaterial material = materials.get(c.materialId());
            BigDecimal refCost = material == null ? null : material.getRefCost();
            MovingAverage.State state = states.get(c.materialId());
            // 调拨（含在途调拨的调出、收货）只是存放位置变化，不改变全公司的数量和均价：按均价计价，
            // 在途调拨收货按调出时的成本单价（由调用方传入），调出、调入金额相抵
            if (bizType == StockBizType.TRANSFER) {
                BigDecimal avg = c.increase() && c.unitCost() != null ? c.unitCost() : MovingAverage.average(state, refCost);
                BigDecimal amount = MovingAverage.value(c.qty().abs(), avg);
                result.add(new Priced(avg, c.increase() ? amount : amount.negate()));
                continue;
            }
            // 出入库按指定单价（为空按均价）；盘盈按填写的成本价，盘亏按均价
            BigDecimal specified = bizType == StockBizType.IN || bizType == StockBizType.OUT
                    || bizType == StockBizType.CHECK && c.increase() ? c.unitCost() : null;
            MovingAverage.Result r = MovingAverage.apply(state, c.qty(), specified, refCost);
            states.put(c.materialId(), r.next());
            changed.add(c.materialId());
            result.add(new Priced(r.unitCost(), r.amount()));
        }
        for (Long id : changed) {
            MovingAverage.State s = states.get(id);
            costMapper.updateCost(rows.get(id).getId(), s.qty(), s.amount(), s.unitCost());
        }
        return result;
    }

    /**
     * 各物料的当前均价（只读，不加锁）：有成本记录时取记录的单价（数量为 0 且没有单价时取参考成本价），没有记录时取参考成本价
     */
    public Map<Long, BigDecimal> unitCosts(Collection<Long> materialIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (materialIds.isEmpty()) {
            return map;
        }
        Map<Long, WmsMaterialCost> rows = costMapper.selectList(Wrappers.<WmsMaterialCost>lambdaQuery()
                .in(WmsMaterialCost::getMaterialId, materialIds)).stream()
                .collect(Collectors.toMap(WmsMaterialCost::getMaterialId, Function.identity()));
        for (WmsMaterial m : materialMapper.selectByIds(materialIds)) {
            WmsMaterialCost r = rows.get(m.getId());
            MovingAverage.State s = r == null ? MovingAverage.State.EMPTY : new MovingAverage.State(r.getQty(), r.getAmount(), r.getUnitCost());
            map.put(m.getId(), MovingAverage.average(s, m.getRefCost()));
        }
        return map;
    }

    /**
     * 单据各物料的实际成本单价（成本金额合计 / 数量合计），用于退货、冲销按原单成本计价；没有计价的物料不在结果中
     *
     * @param kind   入库 / 出库 / 调拨 / 盘点
     * @param docIds 出入库单 ID（盘点为盘点单 ID）
     */
    public Map<Long, BigDecimal> docUnitCosts(StockBizType kind, Collection<Long> docIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (docIds.isEmpty()) {
            return map;
        }
        for (Map<String, Object> row : flowMapper.selectDocCosts(kind, docIds)) {
            BigDecimal qty = new BigDecimal(row.get("qty").toString());
            if (qty.signum() != 0 && row.get("amount") != null) {
                map.put(((Number) row.get("materialId")).longValue(),
                        new BigDecimal(row.get("amount").toString()).divide(qty, MovingAverage.PRICE_SCALE, RoundingMode.HALF_UP));
            }
        }
        return map;
    }

    /**
     * 单据各物料已计价流水的数量、成本金额合计（与流水同号），用于按工单实际领料成本计算完工入库成本
     *
     * @param kind   入库 / 出库
     * @param docIds 出入库单 ID
     */
    public Map<Long, CostSum> docCosts(StockBizType kind, Collection<Long> docIds) {
        return docIds.isEmpty() ? new HashMap<>() : toSums(flowMapper.selectDocCosts(kind, docIds));
    }

    /**
     * 按来源单号汇总某类出入库单各物料已计价流水的数量、成本金额合计（与流水同号），如委外订单的委外发料
     */
    public Map<Long, CostSum> sourceCosts(StockDocType type, String sourceNo) {
        return toSums(flowMapper.selectSourceCosts(type.kind(), type.name(), sourceNo));
    }

    private static Map<Long, CostSum> toSums(List<Map<String, Object>> rows) {
        Map<Long, CostSum> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            if (row.get("amount") != null) {
                map.put(((Number) row.get("materialId")).longValue(),
                        new CostSum(new BigDecimal(row.get("qty").toString()), new BigDecimal(row.get("amount").toString())));
            }
        }
        return map;
    }

    /**
     * 给已确认单据的物料追加成本（委外件收货追加核销的子件成本，委外件退货冲回），在同一事务中更新物料的实时成本、
     * 该单据该物料流水的成本单价和金额（按数量分摊，最后一条取尾差）以及单据明细的成本单价。
     * 库存金额不会被减成负数：超出部分不追加；物料已没有库存时不追加
     *
     * @param extra 物料 ID → 追加的成本金额，与库存金额同向（入库单为正数增加，出库单为负数表示多转出）
     * @return 物料 ID → 实际追加的金额（被上述规则限制后），没有追加的物料不在结果中
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, BigDecimal> addCost(StockBizType kind, Long docId, Map<Long, BigDecimal> extra) {
        Map<Long, BigDecimal> applied = new HashMap<>();
        Set<Long> ids = extra.entrySet().stream().filter(e -> e.getValue() != null && e.getValue().signum() != 0)
                .map(Map.Entry::getKey).collect(Collectors.toCollection(TreeSet::new));
        if (ids.isEmpty()) {
            return applied;
        }
        Map<Long, List<StockFlowView>> flows = flowMapper.selectPricedFlows(kind, docId, ids).stream()
                .collect(Collectors.groupingBy(StockFlowView::getMaterialId));
        ids.retainAll(flows.keySet());
        if (ids.isEmpty()) {
            return applied;
        }
        Map<Long, WmsMaterialCost> rows = costMapper.selectForUpdate(ids).stream()
                .collect(Collectors.toMap(WmsMaterialCost::getMaterialId, Function.identity()));
        for (Long id : ids) {
            WmsMaterialCost row = rows.get(id);
            if (row == null || row.getQty().signum() <= 0) {
                continue;
            }
            BigDecimal delta = extra.get(id).setScale(2, RoundingMode.HALF_UP).max(row.getAmount().negate());
            if (delta.signum() == 0) {
                continue;
            }
            applied.put(id, delta);
            BigDecimal amount = row.getAmount().add(delta);
            costMapper.updateCost(row.getId(), row.getQty(), amount,
                    amount.divide(row.getQty(), MovingAverage.PRICE_SCALE, RoundingMode.HALF_UP));
            List<StockFlowView> list = flows.get(id);
            BigDecimal totalQty = list.stream().map(f -> f.getQty().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal rest = delta;
            BigDecimal docQty = BigDecimal.ZERO;
            BigDecimal docAmount = BigDecimal.ZERO;
            for (int i = 0; i < list.size(); i++) {
                StockFlowView f = list.get(i);
                BigDecimal share = i == list.size() - 1 ? rest
                        : delta.multiply(f.getQty().abs()).divide(totalQty, 2, RoundingMode.HALF_UP);
                rest = rest.subtract(share);
                // 流水金额与数量同号：入库流水加上追加额，出库流水（负数）加上负的追加额
                BigDecimal flowAmount = f.getCostAmount().add(share);
                flowMapper.updateFlowCost(f.getId(), flowAmount.divide(f.getQty(), MovingAverage.PRICE_SCALE, RoundingMode.HALF_UP),
                        flowAmount);
                docQty = docQty.add(f.getQty().abs());
                docAmount = docAmount.add(flowAmount.abs());
            }
            docItemMapper.updateCostPrice(docId, id, docAmount.divide(docQty, MovingAverage.PRICE_SCALE, RoundingMode.HALF_UP));
        }
        return applied;
    }

    /** 在途调拨各物料调出流水的成本单价（调出时的均价），收货时按此计价；没有计价的物料不在结果中 */
    public Map<Long, BigDecimal> transferOutCosts(Long docId, Collection<Long> materialIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (materialIds.isEmpty()) {
            return map;
        }
        for (StockFlowView f : flowMapper.selectPricedFlows(StockBizType.TRANSFER, docId, materialIds)) {
            if (f.getQty().signum() < 0 && f.getCostPrice() != null) {
                map.putIfAbsent(f.getMaterialId(), f.getCostPrice());
            }
        }
        return map;
    }

    /** 物料当前的计价数量（全公司，不含供应商寄售仓），没有成本记录时为 0 */
    public BigDecimal valuedQty(Long materialId) {
        WmsMaterialCost row = costMapper.selectOne(Wrappers.<WmsMaterialCost>lambdaQuery()
                .select(WmsMaterialCost::getQty).eq(WmsMaterialCost::getMaterialId, materialId));
        return row == null || row.getQty() == null ? BigDecimal.ZERO : row.getQty();
    }

    /** 单据的成本金额合计（绝对值），组装时成套件的成本 = 子件出库成本合计 */
    public BigDecimal docAmount(StockBizType kind, Long docId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> row : flowMapper.selectDocCosts(kind, List.of(docId))) {
            if (row.get("amount") != null) {
                sum = sum.add(new BigDecimal(row.get("amount").toString()).abs());
            }
        }
        return sum;
    }

    /** 不计价的仓库：供应商寄售仓 */
    public Set<Long> unvaluedWarehouses(Collection<Long> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return Set.of();
        }
        return warehouseMapper.selectByIds(warehouseIds).stream().filter(w -> w.getType() == WarehouseType.CONSIGN_IN)
                .map(WmsWarehouse::getId).collect(Collectors.toSet());
    }

    /** 按物料 ID 顺序加锁读取成本记录，没有的先初始化（数量 = 当前计价的库存合计，单价 = 参考成本价） */
    private Map<Long, WmsMaterialCost> lock(Set<Long> materialIds, Map<Long, WmsMaterial> materials) {
        Map<Long, WmsMaterialCost> rows = costMapper.selectForUpdate(materialIds).stream()
                .collect(Collectors.toMap(WmsMaterialCost::getMaterialId, Function.identity()));
        List<Long> missing = materialIds.stream().filter(id -> !rows.containsKey(id)).toList();
        if (missing.isEmpty()) {
            return rows;
        }
        Map<Long, BigDecimal> stock = new HashMap<>();
        costMapper.selectValuedQty(missing).forEach(r -> stock.put(((Number) r.get("materialId")).longValue(),
                new BigDecimal(r.get("qty").toString())));
        for (Long id : missing) {
            WmsMaterial m = materials.get(id);
            BigDecimal ref = m == null || m.getRefCost() == null ? BigDecimal.ZERO : m.getRefCost();
            BigDecimal qty = stock.getOrDefault(id, BigDecimal.ZERO);
            WmsMaterialCost row = new WmsMaterialCost();
            row.setMaterialId(id);
            row.setQty(qty);
            row.setAmount(qty.signum() > 0 ? MovingAverage.value(qty, ref) : BigDecimal.ZERO);
            row.setUnitCost(ref.setScale(MovingAverage.PRICE_SCALE, RoundingMode.HALF_UP));
            try {
                costMapper.insert(row);
            } catch (DuplicateKeyException e) {
                // 并发确认的另一张单据刚好初始化了同一物料，下面重新加锁读取
            }
        }
        costMapper.selectForUpdate(missing).forEach(r -> rows.put(r.getMaterialId(), r));
        return rows;
    }
}
