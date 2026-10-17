package com.lemon.erp.modules.mes.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.mes.entity.MesBom;
import com.lemon.erp.modules.mes.entity.MesBomItem;
import com.lemon.erp.modules.mes.entity.MesMaterialPlan;
import com.lemon.erp.modules.mes.entity.MesMrpResult;
import com.lemon.erp.modules.mes.model.MrpResultStatus;
import com.lemon.erp.modules.mes.model.SupplyType;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.wms.entity.WmsMaterial;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MRP 净需求计算（纯计算，不依赖 Spring 和数据库，数据由 {@link MesMrpService} 取好后传入）：
 * <ul>
 *   <li>低层码：物料在 BOM 树中出现的最深层级，超过 {@link #MAX_LEVEL} 层视为循环引用</li>
 *   <li>按低层码（同层按物料编码）逐个计算，上层的建议数量按 BOM（含损耗）展开为下层毛需求，需求日期取最早</li>
 *   <li>净需求 = 毛需求 - (在库 + 在途 - 已分配 - 安全库存)，不小于 0；建议数量按批量规则取整
 *       （{@link MesMaterialService#lotSize}）；建议日期 = 需求日期 - 提前期</li>
 *   <li>虚拟件不扣库存、不建议生产，毛需求原样传给子件</li>
 * </ul>
 */
public final class MrpCalculator {

    /** BOM 最大层级，超过视为循环引用 */
    public static final int MAX_LEVEL = 50;

    /** 独立需求：生产计划、销售订单、销售预测 */
    public record Demand(Long materialId, BigDecimal qty, LocalDate dueDate) {
    }

    /** 阶梯价：按建议数量取供货价格 */
    @FunctionalInterface
    public interface PriceResolver {
        BigDecimal price(ScmSupplierPrice price, BigDecimal qty);
    }

    /**
     * 计算所需的数据，均以物料 ID 为键
     *
     * @param children  需要 BOM 的物料 → 默认 BOM 的子件
     * @param boms      需要 BOM 的物料 → 默认 BOM（取 ID、生产提前期）
     * @param planAttrs 物料计划属性（批量规则、提前期），可缺
     * @param materials 物料（编码、安全库存、是否序列号管理）
     * @param suppliers 采购 / 委外件的默认供货价格（供应商、最小起订量、提前期），可缺
     */
    public record Input(List<Demand> demands, Map<Long, List<MesBomItem>> children, Map<Long, MesBom> boms,
                        Map<Long, MesMaterialPlan> planAttrs, Map<Long, Integer> levels, Map<Long, SupplyType> types,
                        Map<Long, WmsMaterial> materials, Map<Long, BigDecimal> onHand, Map<Long, BigDecimal> scheduled,
                        Map<Long, BigDecimal> allocated, Map<Long, ScmSupplierPrice> suppliers) {
    }

    private MrpCalculator() {
    }

    /** 低层码：从各需求物料出发，物料在 BOM 树中出现的最深层级 */
    public static Map<Long, Integer> lowLevelCodes(Collection<Demand> demands, Map<Long, List<MesBomItem>> children) {
        Map<Long, Integer> levels = new HashMap<>();
        demands.forEach(d -> assignLevel(d.materialId(), 0, levels, children));
        return levels;
    }

    /** 各物料的供应方式：设置的属性优先，否则有默认 BOM 为自制、没有为采购 */
    public static Map<Long, SupplyType> supplyTypes(Collection<Long> materialIds, Map<Long, MesMaterialPlan> planAttrs,
                                                    Map<Long, MesBom> boms) {
        Map<Long, SupplyType> types = new HashMap<>();
        materialIds.forEach(id -> types.put(id, MesMaterialService.supplyType(planAttrs.get(id), boms.containsKey(id))));
        return types;
    }

    /**
     * 逐层计算净需求和建议
     *
     * @return 有毛需求的物料的运算结果（按低层码、物料编码排序），未设置运算 ID
     */
    public static List<MesMrpResult> calculate(Input in, PriceResolver pricing) {
        // 毛需求：各来源合并，取最早日期
        Map<Long, BigDecimal> gross = new HashMap<>();
        Map<Long, LocalDate> required = new HashMap<>();
        in.demands().forEach(d -> addDemand(gross, required, d.materialId(), d.qty(), d.dueDate()));

        List<Long> ordered = in.levels().keySet().stream()
                .sorted(Comparator.comparing((Long id) -> in.levels().get(id)).thenComparing(id -> in.materials().get(id).getCode()))
                .toList();
        List<MesMrpResult> results = new ArrayList<>();
        for (Long id : ordered) {
            BigDecimal g = gross.get(id);
            if (g == null || g.signum() <= 0) {
                continue;
            }
            WmsMaterial material = in.materials().get(id);
            MesMaterialPlan attr = in.planAttrs().get(id);
            SupplyType type = in.types().get(id);
            MesBom bom = in.boms().get(id);
            ScmSupplierPrice price = type.purchased() ? in.suppliers().get(id) : null;
            MesMrpResult r = new MesMrpResult();
            r.setMaterialId(id);
            r.setLevelNo(in.levels().get(id));
            r.setSupplyType(type);
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
                r.setStatus(MrpResultStatus.NONE);
                results.add(r);
                explode(in.children().getOrDefault(id, List.of()), g, r.getRequiredDate(), gross, required);
                continue;
            }
            r.setOnHandQty(in.onHand().getOrDefault(id, BigDecimal.ZERO));
            r.setScheduledQty(in.scheduled().getOrDefault(id, BigDecimal.ZERO));
            r.setAllocatedQty(in.allocated().getOrDefault(id, BigDecimal.ZERO));
            r.setSafetyQty(Decimals.nz(material.getSafetyStock()));
            r.setNetQty(netQty(g, r.getOnHandQty(), r.getScheduledQty(), r.getAllocatedQty(), r.getSafetyQty()));
            r.setSuggestQty(MesMaterialService.lotSize(r.getNetQty(), attr, material.isSerialManaged(), price == null ? null : price.getMinQty()));
            r.setReleaseDate(r.getRequiredDate().minusDays(leadDays(type, attr, bom, price)));
            if (price != null) {
                r.setSupplierId(price.getSupplierId());
                // 阶梯价：按建议数量
                r.setPrice(pricing.price(price, r.getSuggestQty()));
            }
            r.setStatus(r.getSuggestQty().signum() > 0 ? MrpResultStatus.NEW : MrpResultStatus.NONE);
            results.add(r);
            // 自制件、委外件按建议数量展开为下层的毛需求（委外件的子件提供给加工商），需求日期为本层开工 / 下单日期
            if (bom != null && r.getSuggestQty().signum() > 0) {
                explode(in.children().getOrDefault(id, List.of()), r.getSuggestQty(), r.getReleaseDate(), gross, required);
            }
        }
        return results;
    }

    /** 净需求 = 毛需求 - (在库 + 在途 - 已分配 - 安全库存)，不小于 0 */
    public static BigDecimal netQty(BigDecimal gross, BigDecimal onHand, BigDecimal scheduled, BigDecimal allocated, BigDecimal safety) {
        BigDecimal available = onHand.add(scheduled).subtract(allocated).subtract(safety);
        return gross.subtract(available).max(BigDecimal.ZERO);
    }

    /** 提前期：物料设置的优先，否则采购 / 委外件取供货价格的（没有为 0），自制件取 BOM 的生产提前期 */
    public static int leadDays(SupplyType type, MesMaterialPlan attr, MesBom bom, ScmSupplierPrice price) {
        return attr != null && attr.getLeadDays() != null ? attr.getLeadDays()
                : type.purchased() ? (price == null ? 0 : price.getLeadDays()) : bom.getLeadDays();
    }

    private static void assignLevel(Long materialId, int level, Map<Long, Integer> levels, Map<Long, List<MesBomItem>> children) {
        if (level > MAX_LEVEL) {
            throw new BizException("mes.error.bomExceedsLevelsMayCircularReference", MAX_LEVEL);
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
            BigDecimal qty = parentQty.multiply(WoBomExpander.unitQty(item)).setScale(4, RoundingMode.HALF_UP);
            addDemand(gross, required, item.getMaterialId(), qty, date);
        }
    }

    private static void addDemand(Map<Long, BigDecimal> gross, Map<Long, LocalDate> required, Long materialId,
                                  BigDecimal qty, LocalDate date) {
        gross.merge(materialId, qty, BigDecimal::add);
        required.merge(materialId, date, (a, b) -> a.isBefore(b) ? a : b);
    }
}
