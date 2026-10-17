package com.lemon.erp.modules.mes.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.mes.model.MrpResultStatus;
import com.lemon.erp.modules.mes.service.MrpCalculator.Demand;
import com.lemon.erp.modules.mes.model.WorkOrderSource;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.modules.mes.dto.MrpReleaseRequest;
import com.lemon.erp.modules.mes.dto.MrpResultRequest;
import com.lemon.erp.modules.mes.dto.MrpRunRequest;
import com.lemon.erp.modules.mes.dto.WorkOrderRequest;
import com.lemon.erp.modules.mes.entity.MesBom;
import com.lemon.erp.modules.mes.entity.MesBomItem;
import com.lemon.erp.modules.mes.entity.MesMaterialPlan;
import com.lemon.erp.modules.mes.entity.MesMrpResult;
import com.lemon.erp.modules.mes.entity.MesMrpRun;
import com.lemon.erp.modules.mes.entity.MesPlan;
import com.lemon.erp.modules.mes.mapper.MesForecastMapper;
import com.lemon.erp.modules.mes.mapper.MesMrpResultMapper;
import com.lemon.erp.modules.mes.mapper.MesMrpRunMapper;
import com.lemon.erp.modules.mes.mapper.MesWoMaterialMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.mes.model.PlanStatus;
import com.lemon.erp.modules.mes.model.SupplyType;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import com.lemon.erp.modules.scm.service.ScmQueryService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 物料需求计划（MRP）：按已确认的生产计划展开多级 BOM，逐层计算净需求，给出自制件的生产建议和采购件的采购建议。
 * <ol>
 *   <li>按默认 BOM 展开，计算低层码（物料在所有 BOM 中出现的最深层级），按低层码从上到下处理，保证汇总完上层需求再算下层</li>
 *   <li>净需求 = 毛需求 - (现有库存 + 在途 - 已分配 - 安全库存)，不小于 0。
 *       在途：采购件为未完成的采购订单，自制件为未完工的工单；已分配：未结束工单尚未领用的用料</li>
 *   <li>物料属性（{@link SupplyType}）：未设置时有默认 BOM 为自制、否则为采购；委外件向加工商下采购订单，同时展开子件；
 *       虚拟件不扣库存、不建议生产，需求直接传给子件</li>
 *   <li>建议数量：按物料的最小批量、批量倍数（{@link MesMaterialService#lotSize}）取整，采购件不低于默认供应商的最小起订量，
 *       序列号物料取整数；自制、委外件的建议数量按 BOM（含损耗）继续展开为下层毛需求</li>
 *   <li>建议日期 = 需求日期 - 提前期（物料设置的提前期优先，否则自制件取 BOM 生产提前期，采购 / 委外件取供货价格的提前期），
 *       下层的需求日期为上层的开工日期</li>
 * </ol>
 * 不做分时段（按周 / 天）的需求计划，同一物料的需求合并、取最早的需求日期。新运算后，之前未下达的建议作废（EXPIRED）。
 * 下达：自制件生成草稿工单；采购、委外件按供应商合并生成草稿采购订单
 */
@Service
@RequiredArgsConstructor
public class MesMrpService {

    private final MesMrpRunMapper runMapper;
    private final MesMrpResultMapper resultMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final MesWoMaterialMapper woMaterialMapper;
    private final MesPlanService planService;
    private final MesBomService bomService;
    private final MesWorkOrderService workOrderService;
    private final MesMaterialService materialPlanService;
    private final WmsMaterialService materialService;
    private final WmsStockQueryService stockQueryService;
    private final ScmQueryService scmQueryService;
    private final ScmOrderService orderService;
    private final DocNoService docNoService;
    private final MesForecastMapper forecastMapper;

    public PageResult<MesMrpRun> page(long current, long size) {
        Page<MesMrpRun> page = Page.of(current, size);
        runMapper.selectPageList(page);
        return PageResult.of(page);
    }

    public List<MesMrpResult> results(Long runId) {
        return resultMapper.selectByRun(runId);
    }

    /**
     * 运算：取需求和 BOM 结构、供需数据，由 {@link MrpCalculator} 计算净需求，保存结果。
     * 整个运算在一个事务中：MySQL（可重复读）下所有读取共享同一快照，避免运算期间入库等操作使在库和在途被重复或遗漏计算；
     * 读取阶段只有普通查询、不加锁，加锁的取号和写入集中在最后，缩小事务范围也不能减少锁的持有时间
     *
     * @return 运算 ID
     */
    @Transactional
    public Long run(MrpRunRequest req) {
        List<MesPlan> plans = (req.planIds() == null ? List.<Long>of() : req.planIds()).stream().distinct()
                .map(planService::getAccessible).toList();
        for (MesPlan plan : plans) {
            if (plan.getStatus() != PlanStatus.CONFIRMED) {
                throw new BizException("mes.error.planNotConfirmedCannotTakePart", plan.getPlanNo());
            }
        }
        List<String> sources = new ArrayList<>();
        List<Demand> demands = collectDemands(req, plans, sources);

        // 1. 展开 BOM 结构；2. 低层码
        BomStructure structure = loadBomStructure(demands);
        Map<Long, Integer> levels = MrpCalculator.lowLevelCodes(demands, structure.children());

        // 3. 供需数据
        Set<Long> all = levels.keySet();
        Map<Long, SupplyType> types = MrpCalculator.supplyTypes(all, structure.planAttrs(), structure.boms());
        Set<Long> makeIds = all.stream().filter(id -> types.get(id) == SupplyType.MAKE).collect(Collectors.toSet());
        Set<Long> buyIds = all.stream().filter(id -> types.get(id).purchased()).collect(Collectors.toSet());
        Map<Long, WmsMaterial> materials = materialService.mapByIds(all);
        Map<Long, BigDecimal> onHand = stockQueryService.onHand(all);
        Map<Long, BigDecimal> scheduled = new HashMap<>(scmQueryService.openPurchaseQty(buyIds));
        if (!makeIds.isEmpty()) {
            scheduled.putAll(toQtyMap(workOrderMapper.selectOpenOutput(WorkOrderStatus.OPEN, makeIds)));
        }
        Map<Long, BigDecimal> allocated = toQtyMap(woMaterialMapper.selectOpenAllocated(WorkOrderStatus.OPEN, all));
        Map<Long, ScmSupplierPrice> suppliers = scmQueryService.defaultSuppliers(buyIds);

        // 4. 毛需求、5. 按低层码逐层计算
        List<MesMrpResult> results = MrpCalculator.calculate(new MrpCalculator.Input(demands, structure.children(), structure.boms(),
                structure.planAttrs(), levels, types, materials, onHand, scheduled, allocated, suppliers), scmQueryService::effectivePrice);
        return saveRun(req, plans, sources, results);
    }

    /** 需求：生产计划 + 销售订单未计划部分 + 销售预测（扣减同月销售订单）；sources 中加入需求来源的名称 */
    private List<Demand> collectDemands(MrpRunRequest req, List<MesPlan> plans, List<String> sources) {
        List<Demand> demands = new ArrayList<>();
        plans.forEach(p -> demands.add(new Demand(p.getMaterialId(), p.getQty(), p.getDueDate())));
        if (!plans.isEmpty()) {
            sources.add("生产计划"); // i18n-ignore
        }
        if (Boolean.TRUE.equals(req.includeSales())) {
            sources.add("销售订单"); // i18n-ignore
            for (Map<String, Object> row : forecastMapper.selectUnplannedSales()) {
                demands.add(new Demand(((Number) row.get("materialId")).longValue(), new BigDecimal(row.get("qty").toString()),
                        toDate(row.get("dueDate"))));
            }
        }
        if (req.forecastMonths() != null && req.forecastMonths() > 0) {
            sources.add("销售预测 " + req.forecastMonths() + " 个月"); // i18n-ignore
            YearMonth first = YearMonth.now();
            YearMonth last = first.plusMonths(req.forecastMonths() - 1L);
            for (Map<String, Object> row : forecastMapper.selectNetForecast(first.getYear() * 100 + first.getMonthValue(),
                    last.getYear() * 100 + last.getMonthValue())) {
                int period = ((Number) row.get("period")).intValue();
                LocalDate due = YearMonth.of(period / 100, period % 100).atDay(1);
                demands.add(new Demand(((Number) row.get("materialId")).longValue(), new BigDecimal(row.get("qty").toString()),
                        due.isBefore(LocalDate.now()) ? LocalDate.now() : due));
            }
        }
        if (sources.isEmpty()) {
            throw new BizException("mes.error.selectProductionPlansIncludeSalesOrders");
        }
        if (demands.isEmpty()) {
            throw new BizException("mes.error.noDemandRunMrp");
        }
        return demands;
    }

    private record BomStructure(Map<Long, MesBom> boms, Map<Long, MesMaterialPlan> planAttrs, Map<Long, List<MesBomItem>> children) {
    }

    /** 逐层取 BOM 结构：需要 BOM 的物料（自制、委外、虚拟件）→ 默认 BOM 及子件；设为采购的物料即使有 BOM 也不展开 */
    private BomStructure loadBomStructure(List<Demand> demands) {
        Map<Long, MesBom> boms = new HashMap<>();
        Map<Long, MesMaterialPlan> planAttrs = new HashMap<>();
        Map<Long, List<MesBomItem>> children = new HashMap<>();
        Set<Long> frontier = demands.stream().map(Demand::materialId).collect(Collectors.toSet());
        Set<Long> seen = new HashSet<>(frontier);
        while (!frontier.isEmpty()) {
            planAttrs.putAll(materialPlanService.plans(frontier));
            Map<Long, MesBom> found = new HashMap<>(bomService.defaultBoms(frontier));
            for (Long id : frontier) {
                SupplyType type = MesMaterialService.supplyType(planAttrs.get(id), found.containsKey(id));
                if (!type.needsBom()) {
                    found.remove(id);
                } else if (!found.containsKey(id)) {
                    WmsMaterial m = materialService.get(id);
                    throw new BizException("mes.error.materialRequiresBomButHasNo", m.getCode(), m.getName());
                }
            }
            boms.putAll(found);
            Map<Long, List<MesBomItem>> items = bomService.items(found.values().stream().map(MesBom::getId).toList());
            Set<Long> next = new HashSet<>();
            found.values().forEach(bom -> {
                List<MesBomItem> list = items.getOrDefault(bom.getId(), List.of());
                children.put(bom.getMaterialId(), list);
                list.forEach(i -> {
                    if (seen.add(i.getMaterialId())) {
                        next.add(i.getMaterialId());
                    }
                });
            });
            frontier = next;
        }
        return new BomStructure(boms, planAttrs, children);
    }

    /** 新运算后，之前运算中未下达的建议作废（避免按过时的数据重复下单），保存本次运算和结果 */
    private Long saveRun(MrpRunRequest req, List<MesPlan> plans, List<String> sources, List<MesMrpResult> results) {
        resultMapper.update(Wrappers.<MesMrpResult>lambdaUpdate().set(MesMrpResult::getStatus, MrpResultStatus.EXPIRED)
                .eq(MesMrpResult::getStatus, MrpResultStatus.NEW));
        MesMrpRun run = new MesMrpRun();
        run.setRunNo(docNoService.next(MesDocNos.MRP_RUN));
        run.setPlanNos(plans.stream().map(MesPlan::getPlanNo).collect(Collectors.joining(",")));
        run.setSources(String.join("、", sources));
        run.setResultCount(results.size());
        run.setRemark(req.remark());
        run.setCreateBy(SecurityUtils.getUserId());
        runMapper.insert(run);
        if (!results.isEmpty()) {
            results.forEach(r -> r.setRunId(run.getId()));
            resultMapper.insertBatch(results);
        }
        return run.getId();
    }

    /** 下达前调整建议数量、采购件的供应商和单价；换了供应商且未填单价时取该供应商的供货价格 */
    public void updateResult(Long id, MrpResultRequest req) {
        MesMrpResult r = getNew(id);
        BigDecimal price = req.price();
        boolean buy = r.getSupplyType().purchased();
        if (buy && req.supplierId() != null && price == null) {
            ScmSupplierPrice p = scmQueryService.supplierPrice(req.supplierId(), r.getMaterialId());
            price = p == null ? null : p.getPrice();
        }
        resultMapper.update(Wrappers.<MesMrpResult>lambdaUpdate()
                .set(MesMrpResult::getSuggestQty, req.suggestQty())
                .set(buy, MesMrpResult::getSupplierId, req.supplierId())
                .set(buy, MesMrpResult::getPrice, price)
                .eq(MesMrpResult::getId, id));
    }

    /**
     * 下达建议：自制件逐行生成草稿工单；采购件按供应商合并为草稿采购订单（交货日期取最早需求日期）
     *
     * @return 生成的单据号
     */
    @Transactional
    public List<String> release(MrpReleaseRequest req) {
        List<MesMrpResult> rows = req.resultIds().stream().distinct().map(this::getNew).toList();
        if (rows.stream().anyMatch(r -> r.getSuggestQty().signum() <= 0)) {
            throw new BizException("mes.error.linesSuggestedQuantityDoNotNeed");
        }
        Map<Long, WmsMaterial> materials = materialService.mapByIds(rows.stream().map(MesMrpResult::getMaterialId).toList());
        MesMrpRun mrpRun = runMapper.selectById(rows.get(0).getRunId());
        String remark = "MRP " + mrpRun.getRunNo();
        // 只有一个计划参与运算时，该计划产品的工单关联到计划（完工后计划自动关闭）
        MesPlan singlePlan = mrpRun.getPlanNos() == null || mrpRun.getPlanNos().isBlank() || mrpRun.getPlanNos().contains(",") ? null
                : planService.getByNo(mrpRun.getPlanNos());
        List<String> docNos = new ArrayList<>();

        for (MesMrpResult r : rows.stream().filter(r -> r.getSupplyType() == SupplyType.MAKE).toList()) {
            Long planId = singlePlan != null && singlePlan.getMaterialId().equals(r.getMaterialId()) ? singlePlan.getId() : null;
            Long woId = workOrderService.createDraft(new WorkOrderRequest(r.getMaterialId(), r.getBomId(), r.getSuggestQty(),
                    r.getReleaseDate(), r.getRequiredDate(), req.warehouseId(), remark), WorkOrderSource.MRP, planId);
            String woNo = workOrderMapper.selectById(woId).getWoNo();
            markReleased(r.getId(), woNo);
            docNos.add(woNo);
        }

        Map<Long, List<MesMrpResult>> bySupplier = new LinkedHashMap<>();
        for (MesMrpResult r : rows.stream().filter(r -> r.getSupplyType().purchased()).toList()) {
            if (r.getSupplierId() == null) {
                WmsMaterial m = materials.get(r.getMaterialId());
                throw new BizException("mes.error.specifySupplierPurchasedOutsourcedItemFirst", m.getCode(), m.getName());
            }
            bySupplier.computeIfAbsent(r.getSupplierId(), k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<Long, List<MesMrpResult>> e : bySupplier.entrySet()) {
            List<MesMrpResult> list = e.getValue();
            LocalDate delivery = list.stream().map(MesMrpResult::getRequiredDate).filter(Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(null);
            List<OrderItemRequest> items = list.stream().map(r -> {
                ScmSupplierPrice p = scmQueryService.supplierPrice(e.getKey(), r.getMaterialId());
                BigDecimal price = r.getPrice() != null ? r.getPrice() : p != null ? p.getPrice() : BigDecimal.ZERO;
                return new OrderItemRequest(r.getMaterialId(), r.getSuggestQty(), price, p == null ? null : p.getTaxRate(),
                        r.getRequiredDate(), null);
            }).toList();
            Long orderId = orderService.createDraft(OrderKind.PURCHASE,
                    new OrderRequest(e.getKey(), LocalDate.now(), delivery, req.warehouseId(), remark, items), OrderSource.MRP);
            String orderNo = scmQueryService.orderNo(orderId);
            list.forEach(r -> markReleased(r.getId(), orderNo));
            docNos.add(orderNo);
        }
        return docNos;
    }

    // ---------------------------------------------------------------- 内部

    private MesMrpResult getNew(Long id) {
        MesMrpResult r = resultMapper.selectById(id);
        if (r == null) {
            throw new BizException("mes.error.mrpResultDoesNotExist");
        }
        if (r.getStatus() != MrpResultStatus.NEW) {
            throw new BizException("mes.error.suggestionHasReleasedNeedsNoAction");
        }
        return r;
    }

    private void markReleased(Long id, String releaseNo) {
        if (!StatusTransitions.tryChange(resultMapper, MesMrpResult::getId, id, MesMrpResult::getStatus, MrpResultStatus.NEW,
                MrpResultStatus.RELEASED, w -> w.set(MesMrpResult::getReleaseNo, releaseNo))) {
            throw new BizException("mes.error.suggestionHasAlreadyReleasedRefresh");
        }
    }

    private static LocalDate toDate(Object v) {
        if (v instanceof LocalDate d) {
            return d;
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        return v == null ? LocalDate.now() : LocalDate.parse(v.toString().substring(0, 10));
    }

    private static Map<Long, BigDecimal> toQtyMap(List<Map<String, Object>> rows) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            map.put(((Number) row.get("materialId")).longValue(), new BigDecimal(row.get("qty").toString()));
        }
        return map;
    }
}
