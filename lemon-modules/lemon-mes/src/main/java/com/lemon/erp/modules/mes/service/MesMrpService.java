package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
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
import java.math.RoundingMode;
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

    /** BOM 最大层级，超过视为循环引用 */
    private static final int MAX_LEVEL = 50;

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

    /** @return 运算 ID */
    @Transactional
    public Long run(MrpRunRequest req) {
        List<MesPlan> plans = (req.planIds() == null ? List.<Long>of() : req.planIds()).stream().distinct()
                .map(planService::getAccessible).toList();
        for (MesPlan plan : plans) {
            if (!PlanStatus.CONFIRMED.name().equals(plan.getStatus())) {
                throw new BizException("计划 {0} 不是已确认状态，不能参与运算", plan.getPlanNo());
            }
        }
        // 需求：生产计划 + 销售订单未计划部分 + 销售预测（扣减同月销售订单）
        List<Demand> demands = new ArrayList<>();
        plans.forEach(p -> demands.add(new Demand(p.getMaterialId(), p.getQty(), p.getDueDate())));
        List<String> sources = new ArrayList<>();
        if (!plans.isEmpty()) {
            sources.add("生产计划");
        }
        if (Boolean.TRUE.equals(req.includeSales())) {
            sources.add("销售订单");
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
            throw new BizException("请选择生产计划，或计入销售订单、销售预测");
        }
        if (demands.isEmpty()) {
            throw new BizException("没有需要运算的需求");
        }

        // 1. 展开 BOM 结构：需要 BOM 的物料（自制、委外、虚拟件）→ 默认 BOM 及子件；设为采购的物料即使有 BOM 也不展开
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
                    throw new BizException("物料「{0} {1}」的物料属性需要 BOM，但没有启用的默认 BOM", m.getCode(), m.getName());
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

        // 2. 低层码
        Map<Long, Integer> levels = new HashMap<>();
        demands.forEach(d -> assignLevel(d.materialId(), 0, levels, children));

        // 3. 供需数据
        Set<Long> all = levels.keySet();
        Map<Long, SupplyType> types = new HashMap<>();
        all.forEach(id -> types.put(id, MesMaterialService.supplyType(planAttrs.get(id), boms.containsKey(id))));
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

        // 4. 毛需求：各来源合并，取最早日期
        Map<Long, BigDecimal> gross = new HashMap<>();
        Map<Long, LocalDate> required = new HashMap<>();
        demands.forEach(d -> addDemand(gross, required, d.materialId(), d.qty(), d.dueDate()));

        // 5. 按低层码逐层计算
        List<Long> ordered = all.stream()
                .sorted(Comparator.comparing((Long id) -> levels.get(id)).thenComparing(id -> materials.get(id).getCode()))
                .toList();
        List<MesMrpResult> results = new ArrayList<>();
        for (Long id : ordered) {
            BigDecimal g = gross.get(id);
            if (g == null || g.signum() <= 0) {
                continue;
            }
            WmsMaterial material = materials.get(id);
            MesMaterialPlan attr = planAttrs.get(id);
            SupplyType type = types.get(id);
            MesBom bom = boms.get(id);
            ScmSupplierPrice price = type.purchased() ? suppliers.get(id) : null;
            MesMrpResult r = new MesMrpResult();
            r.setMaterialId(id);
            r.setLevelNo(levels.get(id));
            r.setSupplyType(type.name());
            r.setBomId(bom == null ? null : bom.getId());
            r.setGrossQty(g);
            r.setRequiredDate(required.get(id));
            if (type == SupplyType.PHANTOM) {
                // 虚拟件不入库：不扣库存、不建议生产，需求原样传给子件
                r.setOnHandQty(BigDecimal.ZERO);
                r.setScheduledQty(BigDecimal.ZERO);
                r.setAllocatedQty(BigDecimal.ZERO);
                r.setSafetyQty(BigDecimal.ZERO);
                r.setNetQty(g);
                r.setSuggestQty(BigDecimal.ZERO);
                r.setReleaseDate(r.getRequiredDate());
                r.setStatus("NONE");
                results.add(r);
                explode(children.getOrDefault(id, List.of()), g, r.getRequiredDate(), gross, required);
                continue;
            }
            r.setOnHandQty(onHand.getOrDefault(id, BigDecimal.ZERO));
            r.setScheduledQty(scheduled.getOrDefault(id, BigDecimal.ZERO));
            r.setAllocatedQty(allocated.getOrDefault(id, BigDecimal.ZERO));
            r.setSafetyQty(material.getSafetyStock() == null ? BigDecimal.ZERO : material.getSafetyStock());
            BigDecimal available = r.getOnHandQty().add(r.getScheduledQty()).subtract(r.getAllocatedQty()).subtract(r.getSafetyQty());
            BigDecimal net = g.subtract(available).max(BigDecimal.ZERO);
            r.setNetQty(net);
            r.setSuggestQty(MesMaterialService.lotSize(net, attr, material.isSerialManaged(), price == null ? null : price.getMinQty()));
            int leadDays = attr != null && attr.getLeadDays() != null ? attr.getLeadDays()
                    : type.purchased() ? (price == null ? 0 : price.getLeadDays()) : bom.getLeadDays();
            r.setReleaseDate(r.getRequiredDate().minusDays(leadDays));
            if (price != null) {
                r.setSupplierId(price.getSupplierId());
                // 阶梯价：按建议数量
                r.setPrice(scmQueryService.effectivePrice(price, r.getSuggestQty()));
            }
            r.setStatus(r.getSuggestQty().signum() > 0 ? "NEW" : "NONE");
            results.add(r);
            // 自制件、委外件按建议数量展开为下层的毛需求（委外件的子件提供给加工商），需求日期为本层开工 / 下单日期
            if (bom != null && r.getSuggestQty().signum() > 0) {
                explode(children.getOrDefault(id, List.of()), r.getSuggestQty(), r.getReleaseDate(), gross, required);
            }
        }

        // 新运算后，之前运算中未下达的建议作废，避免按过时的数据重复下单
        resultMapper.update(Wrappers.<MesMrpResult>lambdaUpdate().set(MesMrpResult::getStatus, "EXPIRED")
                .eq(MesMrpResult::getStatus, "NEW"));
        MesMrpRun run = new MesMrpRun();
        run.setRunNo(docNoService.next("MR"));
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
        boolean buy = SupplyType.valueOf(r.getSupplyType()).purchased();
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
            throw new BizException("建议数量为 0 的行不需要下达");
        }
        Map<Long, WmsMaterial> materials = materialService.mapByIds(rows.stream().map(MesMrpResult::getMaterialId).toList());
        MesMrpRun mrpRun = runMapper.selectById(rows.get(0).getRunId());
        String remark = "MRP " + mrpRun.getRunNo();
        // 只有一个计划参与运算时，该计划产品的工单关联到计划（完工后计划自动关闭）
        MesPlan singlePlan = mrpRun.getPlanNos() == null || mrpRun.getPlanNos().isBlank() || mrpRun.getPlanNos().contains(",") ? null
                : planService.getByNo(mrpRun.getPlanNos());
        List<String> docNos = new ArrayList<>();

        for (MesMrpResult r : rows.stream().filter(r -> SupplyType.MAKE.name().equals(r.getSupplyType())).toList()) {
            Long planId = singlePlan != null && singlePlan.getMaterialId().equals(r.getMaterialId()) ? singlePlan.getId() : null;
            Long woId = workOrderService.createDraft(new WorkOrderRequest(r.getMaterialId(), r.getBomId(), r.getSuggestQty(),
                    r.getReleaseDate(), r.getRequiredDate(), req.warehouseId(), remark), "MRP", planId);
            String woNo = workOrderMapper.selectById(woId).getWoNo();
            markReleased(r.getId(), woNo);
            docNos.add(woNo);
        }

        Map<Long, List<MesMrpResult>> bySupplier = new LinkedHashMap<>();
        for (MesMrpResult r : rows.stream().filter(r -> SupplyType.valueOf(r.getSupplyType()).purchased()).toList()) {
            if (r.getSupplierId() == null) {
                WmsMaterial m = materials.get(r.getMaterialId());
                throw new BizException("请先为采购 / 委外件「{0} {1}」指定供应商", m.getCode(), m.getName());
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
                    new OrderRequest(e.getKey(), LocalDate.now(), delivery, req.warehouseId(), remark, items), "MRP");
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
            throw new BizException("运算结果不存在");
        }
        if (!"NEW".equals(r.getStatus())) {
            throw new BizException("该建议已下达或无需处理");
        }
        return r;
    }

    private void markReleased(Long id, String releaseNo) {
        int updated = resultMapper.update(Wrappers.<MesMrpResult>lambdaUpdate()
                .set(MesMrpResult::getStatus, "RELEASED")
                .set(MesMrpResult::getReleaseNo, releaseNo)
                .eq(MesMrpResult::getId, id)
                .eq(MesMrpResult::getStatus, "NEW"));
        if (updated == 0) {
            throw new BizException("建议已被下达，请刷新");
        }
    }

    /** 低层码：物料在 BOM 树中出现的最深层级 */
    private static void assignLevel(Long materialId, int level, Map<Long, Integer> levels, Map<Long, List<MesBomItem>> children) {
        if (level > MAX_LEVEL) {
            throw new BizException("BOM 层级超过 {0} 层，可能存在循环引用", MAX_LEVEL);
        }
        Integer current = levels.get(materialId);
        if (current != null && current >= level) {
            return;
        }
        levels.put(materialId, level);
        for (MesBomItem item : children.getOrDefault(materialId, List.of())) {
            assignLevel(item.getMaterialId(), level + 1, levels, children);
        }
    }

    private static void explode(List<MesBomItem> items, BigDecimal parentQty, LocalDate date,
                                Map<Long, BigDecimal> gross, Map<Long, LocalDate> required) {
        for (MesBomItem item : items) {
            BigDecimal qty = parentQty.multiply(MesWorkOrderService.unitQty(item)).setScale(4, RoundingMode.HALF_UP);
            addDemand(gross, required, item.getMaterialId(), qty, date);
        }
    }

    private static void addDemand(Map<Long, BigDecimal> gross, Map<Long, LocalDate> required, Long materialId,
                                  BigDecimal qty, LocalDate date) {
        gross.merge(materialId, qty, BigDecimal::add);
        required.merge(materialId, date, (a, b) -> a.isBefore(b) ? a : b);
    }

    private record Demand(Long materialId, BigDecimal qty, LocalDate dueDate) {
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
