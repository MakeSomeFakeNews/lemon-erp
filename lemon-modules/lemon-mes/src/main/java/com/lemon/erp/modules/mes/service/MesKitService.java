package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.modules.mes.dto.KitLineView;
import com.lemon.erp.modules.mes.dto.KitReleaseRequest;
import com.lemon.erp.modules.mes.dto.MaterialKitView;
import com.lemon.erp.modules.mes.dto.SubstituteSwitchRequest;
import com.lemon.erp.modules.mes.dto.WoKitView;
import com.lemon.erp.modules.mes.dto.WorkOrderRequest;
import com.lemon.erp.modules.mes.entity.MesBom;
import com.lemon.erp.modules.mes.entity.MesMaterialPlan;
import com.lemon.erp.modules.mes.entity.MesMaterialSubstitute;
import com.lemon.erp.modules.mes.entity.MesWoMaterial;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.mapper.MesWoMaterialMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.mes.model.SupplyType;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import com.lemon.erp.modules.scm.service.ScmQueryService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.mes.model.MesConstants.WO_RULE;

/**
 * 齐套分析与生产用料：
 * <ul>
 *   <li>未结束工单（草稿、已下达、生产中）的每个用料：待领 = 需求 + 已补料 - 已领</li>
 *   <li>全部仓库的库存按优先级分配给各工单的待领数量：已下达 / 生产中的在前，草稿在后，再按计划开工日期、工单号；分不到的为缺料</li>
 *   <li>工单齐套状态：全部已领为「已领齐」，没有缺料为「齐套」，有缺料但按已领 + 分到的库存还能继续生产为「部分缺料」，否则为「缺料」</li>
 *   <li>缺料下单：按物料汇总缺料，扣除在途（采购订单 / 工单）后按批量规则取整，采购、委外件生成草稿采购订单，自制件生成草稿工单</li>
 *   <li>替代料：把主料未领的部分按替代比例切换为替代料</li>
 * </ul>
 * 库存不区分仓库（与 MRP 一致）；草稿工单修改时会按 BOM 重新生成用料，切换的替代料需要重新操作
 */
@Service
@RequiredArgsConstructor
public class MesKitService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final MesWoMaterialMapper woMaterialMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final MesWorkOrderService workOrderService;
    private final MesMaterialService materialPlanService;
    private final MesBomService bomService;
    private final WmsMaterialService materialService;
    private final WmsStockQueryService stockQueryService;
    private final ScmQueryService scmQueryService;
    private final ScmOrderService orderService;

    /** 未结束工单的齐套情况（只返回有数据权限的工单） */
    public List<WoKitView> orders(String woNo, String kitStatus) {
        List<KitLineView> lines = allocate(null);
        Set<Long> visible = visibleWoIds(lines.stream().map(KitLineView::getWoId).collect(Collectors.toSet()));
        Map<Long, List<KitLineView>> byWo = lines.stream().filter(l -> visible.contains(l.getWoId()))
                .collect(Collectors.groupingBy(KitLineView::getWoId, LinkedHashMap::new, Collectors.toList()));
        if (byWo.isEmpty()) {
            return List.of();
        }
        Map<Long, MesWorkOrder> wos = workOrderMapper.selectByIds(byWo.keySet()).stream()
                .collect(Collectors.toMap(MesWorkOrder::getId, Function.identity()));
        Map<Long, WmsMaterial> products = materialService.mapByIds(wos.values().stream().map(MesWorkOrder::getMaterialId).toList());
        List<WoKitView> result = new ArrayList<>();
        byWo.forEach((woId, list) -> {
            WoKitView v = summarize(wos.get(woId), products.get(wos.get(woId).getMaterialId()), list);
            if ((!StringUtils.hasText(woNo) || v.getWoNo().contains(woNo.trim()))
                    && (!StringUtils.hasText(kitStatus) || kitStatus.equals(v.getKitStatus()))) {
                result.add(v);
            }
        });
        return result;
    }

    /** 某工单的用料齐套明细，缺料的行附带替代料 */
    public List<KitLineView> orderLines(Long woId) {
        MesWorkOrder wo = workOrderService.getAccessible(woId);
        if (!WorkOrderStatus.OPEN.contains(wo.getStatus())) {
            throw new BizException("工单 {0} 已结束", wo.getWoNo());
        }
        List<Long> materialIds = woMaterialMapper.selectByWo(woId).stream().map(MesWoMaterial::getMaterialId).toList();
        if (materialIds.isEmpty()) {
            return List.of();
        }
        List<KitLineView> lines = allocate(materialIds).stream().filter(l -> l.getWoId().equals(woId)).toList();
        List<Long> shortIds = lines.stream().filter(l -> l.getShortageQty().signum() > 0).map(KitLineView::getMaterialId).toList();
        Map<Long, List<MesMaterialSubstitute>> subs = materialPlanService.substitutes(shortIds);
        lines.forEach(l -> l.setSubstitutes(subs.getOrDefault(l.getMaterialId(), List.of())));
        return lines;
    }

    /** 生产用料汇总：按物料汇总全部未结束工单 */
    public List<MaterialKitView> materials(String keyword, boolean onlyShort) {
        List<KitLineView> lines = allocate(null);
        Map<Long, List<KitLineView>> byMaterial = lines.stream()
                .collect(Collectors.groupingBy(KitLineView::getMaterialId, LinkedHashMap::new, Collectors.toList()));
        List<MaterialKitView> rows = summarizeMaterials(byMaterial);
        String kw = keyword == null ? "" : keyword.trim();
        return rows.stream()
                .filter(r -> kw.isEmpty() || r.getMaterialCode().contains(kw) || r.getMaterialName().contains(kw))
                .filter(r -> !onlyShort || r.getShortageQty().signum() > 0)
                .sorted(Comparator.comparing((MaterialKitView r) -> r.getShortageQty().signum() > 0 ? 0 : 1)
                        .thenComparing(MaterialKitView::getMaterialCode))
                .toList();
    }

    /** 某物料在各工单的分配明细（只返回有数据权限的工单） */
    public List<KitLineView> materialLines(Long materialId) {
        List<KitLineView> lines = allocate(List.of(materialId));
        Set<Long> visible = visibleWoIds(lines.stream().map(KitLineView::getWoId).collect(Collectors.toSet()));
        return lines.stream().filter(l -> visible.contains(l.getWoId())).toList();
    }

    /**
     * 缺料下单：采购、委外件按默认供应商合并为草稿采购订单（交货日期取最早需求日期），自制件逐个生成草稿工单
     *
     * @return 生成的单据号
     */
    @Transactional
    public List<String> release(KitReleaseRequest req) {
        List<Long> ids = req.materialIds().stream().distinct().toList();
        Map<Long, List<KitLineView>> byMaterial = allocate(ids).stream()
                .collect(Collectors.groupingBy(KitLineView::getMaterialId, LinkedHashMap::new, Collectors.toList()));
        List<MaterialKitView> rows = summarizeMaterials(byMaterial).stream().filter(r -> r.getSuggestQty().signum() > 0).toList();
        if (rows.isEmpty()) {
            throw new BizException("所选物料没有缺料，或缺料已有在途的采购订单 / 工单");
        }
        List<String> docNos = new ArrayList<>();
        Map<Long, List<MaterialKitView>> bySupplier = new LinkedHashMap<>();
        for (MaterialKitView r : rows) {
            SupplyType type = SupplyType.valueOf(r.getSupplyType());
            if (type == SupplyType.MAKE) {
                LocalDate due = r.getRequiredDate();
                Long woId = workOrderService.createDraft(new WorkOrderRequest(r.getMaterialId(), null, r.getSuggestQty(),
                        null, due, req.warehouseId(), "生产缺料"), "KIT", null); // i18n-ignore
                docNos.add(workOrderMapper.selectById(woId).getWoNo());
            } else {
                if (r.getSupplierId() == null) {
                    throw new BizException("请先为「{0} {1}」设置默认供应商", r.getMaterialCode(), r.getMaterialName());
                }
                bySupplier.computeIfAbsent(r.getSupplierId(), k -> new ArrayList<>()).add(r);
            }
        }
        for (Map.Entry<Long, List<MaterialKitView>> e : bySupplier.entrySet()) {
            LocalDate delivery = e.getValue().stream().map(MaterialKitView::getRequiredDate).filter(Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(null);
            List<OrderItemRequest> items = e.getValue().stream().map(r -> {
                ScmSupplierPrice p = scmQueryService.supplierPrice(e.getKey(), r.getMaterialId());
                return new OrderItemRequest(r.getMaterialId(), r.getSuggestQty(),
                        p == null ? BigDecimal.ZERO : scmQueryService.effectivePrice(p, r.getSuggestQty()),
                        p == null ? null : p.getTaxRate(), r.getRequiredDate(), null);
            }).toList();
            Long orderId = orderService.createDraft(OrderKind.PURCHASE,
                    new OrderRequest(e.getKey(), LocalDate.now(), delivery, req.warehouseId(), "生产缺料", items), "KIT"); // i18n-ignore
            docNos.add(scmQueryService.orderNo(orderId));
        }
        return docNos;
    }

    /** 替代料切换：主料需求减少 qty，替代料需求增加 qty × 替代比例（没有该用料时新增） */
    @Transactional
    public void switchSubstitute(SubstituteSwitchRequest req) {
        MesWorkOrder wo = workOrderService.getAccessible(req.woId());
        if (!WorkOrderStatus.OPEN.contains(wo.getStatus())) {
            throw new BizException("工单 {0} 已结束", wo.getWoNo());
        }
        Map<Long, MesWoMaterial> lines = woMaterialMapper.selectByWo(wo.getId()).stream()
                .collect(Collectors.toMap(MesWoMaterial::getMaterialId, Function.identity()));
        MesWoMaterial main = lines.get(req.materialId());
        if (main == null) {
            throw new BizException("主料不是工单用料");
        }
        MesMaterialSubstitute sub = materialPlanService.substitutes(List.of(req.materialId())).getOrDefault(req.materialId(), List.of())
                .stream().filter(s -> s.getSubstituteId().equals(req.substituteId())).findFirst()
                .orElseThrow(() -> new BizException("「{0} {1}」没有设置该替代料", main.getMaterialCode(), main.getMaterialName()));
        WmsMaterial subMaterial = materialService.getEnabled(List.of(req.substituteId())).get(req.substituteId());
        BigDecimal toIssue = main.getRequiredQty().add(main.getSupplementQty()).subtract(main.getIssuedQty()).max(BigDecimal.ZERO);
        if (req.qty().compareTo(toIssue) > 0) {
            throw new BizException("替代数量不能超过主料待领数量 {0}", toIssue.stripTrailingZeros().toPlainString());
        }
        if (Integer.valueOf(1).equals(main.getSerialEnabled()) && req.qty().stripTrailingZeros().scale() > 0) {
            throw new BizException("主料启用了序列号管理，替代数量必须为整数");
        }
        BigDecimal subQty = req.qty().multiply(sub.getRatio());
        subQty = subMaterial.isSerialManaged() ? subQty.setScale(0, RoundingMode.CEILING) : subQty.setScale(4, RoundingMode.HALF_UP);

        updateRequired(main, main.getRequiredQty().subtract(req.qty()), wo.getQty());
        MesWoMaterial existing = lines.get(req.substituteId());
        if (existing != null) {
            updateRequired(existing, existing.getRequiredQty().add(subQty), wo.getQty());
        } else {
            MesWoMaterial m = new MesWoMaterial();
            m.setWoId(wo.getId());
            m.setMaterialId(req.substituteId());
            m.setRequiredQty(subQty);
            m.setUnitQty(subQty.divide(wo.getQty(), 6, RoundingMode.HALF_UP));
            woMaterialMapper.insertBatch(List.of(m));
        }
    }

    // ---------------------------------------------------------------- 内部

    /** 按优先级分配库存，返回所有未结束工单（或指定物料）的用料行 */
    private List<KitLineView> allocate(Collection<Long> materialIds) {
        List<KitLineView> lines = woMaterialMapper.selectOpenLines(WorkOrderStatus.OPEN, materialIds);
        Set<Long> ids = lines.stream().map(KitLineView::getMaterialId).collect(Collectors.toSet());
        Map<Long, BigDecimal> remaining = new HashMap<>(stockQueryService.available(ids));
        Map<Long, BigDecimal> onHand = Map.copyOf(remaining);
        Map<Long, BigDecimal> onOrder = onOrder(ids);
        for (KitLineView l : lines) {
            BigDecimal avail = remaining.getOrDefault(l.getMaterialId(), BigDecimal.ZERO);
            BigDecimal alloc = l.getToIssueQty().min(avail).max(BigDecimal.ZERO);
            remaining.put(l.getMaterialId(), avail.subtract(alloc));
            l.setOnHandQty(onHand.getOrDefault(l.getMaterialId(), BigDecimal.ZERO));
            l.setAllocatedQty(alloc);
            l.setShortageQty(l.getToIssueQty().subtract(alloc));
            l.setOnOrderQty(onOrder.getOrDefault(l.getMaterialId(), BigDecimal.ZERO));
        }
        return lines;
    }

    /** 在途：采购、委外件为未完成的采购订单，自制件为未完工的工单；同时填写生效的物料属性 */
    private Map<Long, BigDecimal> onOrder(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> map = new HashMap<>(scmQueryService.openPurchaseQty(ids));
        for (Map<String, Object> row : workOrderMapper.selectOpenOutput(WorkOrderStatus.OPEN, ids)) {
            map.merge(((Number) row.get("materialId")).longValue(), new BigDecimal(row.get("qty").toString()), BigDecimal::add);
        }
        return map;
    }

    private Map<Long, SupplyType> supplyTypes(Collection<Long> ids, Map<Long, MesMaterialPlan> plans) {
        Map<Long, MesBom> boms = bomService.defaultBoms(ids);
        Map<Long, SupplyType> map = new HashMap<>();
        ids.forEach(id -> map.put(id, MesMaterialService.supplyType(plans.get(id), boms.containsKey(id))));
        return map;
    }

    private List<MaterialKitView> summarizeMaterials(Map<Long, List<KitLineView>> byMaterial) {
        if (byMaterial.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = byMaterial.keySet();
        Map<Long, MesMaterialPlan> plans = materialPlanService.plans(ids);
        Map<Long, SupplyType> types = supplyTypes(ids, plans);
        Map<Long, ScmSupplierPrice> suppliers = scmQueryService.defaultSuppliers(ids);
        List<MaterialKitView> rows = new ArrayList<>();
        byMaterial.forEach((id, list) -> {
            KitLineView first = list.get(0);
            MaterialKitView r = new MaterialKitView();
            r.setMaterialId(id);
            r.setMaterialCode(first.getMaterialCode());
            r.setMaterialName(first.getMaterialName());
            r.setSpec(first.getSpec());
            r.setUnit(first.getUnit());
            SupplyType type = types.get(id);
            r.setSupplyType(type.name());
            r.setWoCount((int) list.stream().map(KitLineView::getWoId).distinct().count());
            r.setRequiredQty(sum(list, KitLineView::getRequiredQty));
            r.setSupplementQty(sum(list, KitLineView::getSupplementQty));
            r.setIssuedQty(sum(list, KitLineView::getIssuedQty));
            r.setToIssueQty(sum(list, KitLineView::getToIssueQty));
            r.setShortageQty(sum(list, KitLineView::getShortageQty));
            r.setOnHandQty(first.getOnHandQty());
            r.setOnOrderQty(first.getOnOrderQty());
            r.setRequiredDate(list.stream().filter(l -> l.getShortageQty().signum() > 0).map(KitLineView::getPlanStart)
                    .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null));
            ScmSupplierPrice price = type.purchased() ? suppliers.get(id) : null;
            if (price != null) {
                r.setSupplierId(price.getSupplierId());
                r.setSupplierName(price.getSupplierName());
            }
            BigDecimal net = r.getShortageQty().subtract(r.getOnOrderQty()).max(BigDecimal.ZERO);
            r.setSuggestQty(MesMaterialService.lotSize(net, plans.get(id), Integer.valueOf(1).equals(first.getSerialEnabled()),
                    price == null ? null : price.getMinQty()));
            rows.add(r);
        });
        return rows;
    }

    private static WoKitView summarize(MesWorkOrder wo, WmsMaterial product, List<KitLineView> lines) {
        WoKitView v = new WoKitView();
        v.setWoId(wo.getId());
        v.setWoNo(wo.getWoNo());
        v.setStatus(wo.getStatus());
        v.setProductId(wo.getMaterialId());
        v.setProductCode(product == null ? null : product.getCode());
        v.setProductName(product == null ? null : product.getName());
        v.setUnit(product == null ? null : product.getUnit());
        v.setQty(wo.getQty());
        v.setFinishedQty(wo.getFinishedQty());
        v.setPlanStart(wo.getPlanStart());
        v.setPlanEnd(wo.getPlanEnd());
        v.setLineCount(lines.size());
        int shortLines = (int) lines.stream().filter(l -> l.getShortageQty().signum() > 0).count();
        v.setShortLineCount(shortLines);
        v.setKitRate(lines.isEmpty() ? HUNDRED
                : BigDecimal.valueOf(lines.size() - shortLines).multiply(HUNDRED).divide(BigDecimal.valueOf(lines.size()), 1, RoundingMode.HALF_UP));
        // 各用料（扣除补料）已领 + 分到的库存能支持的产品数量，取最小
        BigDecimal buildable = wo.getQty();
        for (KitLineView l : lines) {
            if (l.getUnitQty().signum() > 0) {
                BigDecimal covered = l.getIssuedQty().subtract(l.getSupplementQty()).max(BigDecimal.ZERO).add(l.getAllocatedQty());
                buildable = buildable.min(covered.divide(l.getUnitQty(), 4, RoundingMode.DOWN));
            }
        }
        if (product != null && product.isSerialManaged()) {
            buildable = buildable.setScale(0, RoundingMode.DOWN);
        }
        v.setBuildableQty(buildable);
        boolean allIssued = lines.stream().allMatch(l -> l.getToIssueQty().signum() == 0);
        v.setKitStatus(allIssued ? "ISSUED" : shortLines == 0 ? "COMPLETE"
                : buildable.compareTo(wo.getFinishedQty()) > 0 ? "PARTIAL" : "SHORT");
        return v;
    }

    private static BigDecimal sum(List<KitLineView> list, Function<KitLineView, BigDecimal> f) {
        return list.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 有数据权限的工单 ID */
    private Set<Long> visibleWoIds(Set<Long> woIds) {
        if (woIds.isEmpty() || SecurityUtils.getDataScope().all()) {
            return woIds;
        }
        return DataPermissionContext.execute(WO_RULE, () -> workOrderMapper.selectList(Wrappers.<MesWorkOrder>lambdaQuery()
                        .select(MesWorkOrder::getId).in(MesWorkOrder::getId, woIds)))
                .stream().map(MesWorkOrder::getId).collect(Collectors.toSet());
    }

    private void updateRequired(MesWoMaterial line, BigDecimal required, BigDecimal woQty) {
        int updated = woMaterialMapper.update(Wrappers.<MesWoMaterial>lambdaUpdate()
                .set(MesWoMaterial::getRequiredQty, required)
                .set(MesWoMaterial::getUnitQty, required.divide(woQty, 6, RoundingMode.HALF_UP))
                .set(MesWoMaterial::getUpdateTime, LocalDateTime.now())
                .eq(MesWoMaterial::getId, line.getId())
                .eq(MesWoMaterial::getRequiredQty, line.getRequiredQty())
                .eq(MesWoMaterial::getIssuedQty, line.getIssuedQty()));
        if (updated == 0) {
            throw new BizException("工单用料已变化，请刷新");
        }
    }
}
