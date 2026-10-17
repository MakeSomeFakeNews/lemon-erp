package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.fin.dto.StdCostBreakdown;
import com.lemon.erp.modules.fin.dto.StdCostRequests;
import com.lemon.erp.modules.fin.entity.FinStdCost;
import com.lemon.erp.modules.fin.entity.FinStdCostVersion;
import com.lemon.erp.modules.fin.mapper.FinStdCostMapper;
import com.lemon.erp.modules.fin.mapper.FinStdCostVersionMapper;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.StdCostStatus;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 标准成本：按版本维护，采购件手工维护标准单价，自制件按默认 BOM（用量含损耗）和工艺路线（标准工时 × 工作中心费率）逐级卷算。
 * 草稿版本可修改单价、重新卷算，发布后不能修改；修改单价后须重新卷算才能发布
 */
@Service
@RequiredArgsConstructor
public class FinStdCostService {

    /** 标准成本（单价、材料 / 人工 / 制造费用、工时）保留 6 位，与 fin_std_cost 的 DECIMAL(18,6) 一致，比金额、数量精度高以减少逐级累积误差 */
    private static final int SCALE = 6;
    private static final int BATCH = 500;

    private final FinStdCostVersionMapper versionMapper;
    private final FinStdCostMapper costMapper;
    private final WmsMaterialService materialService;

    public PageResult<FinStdCostVersion> page(long current, long size) {
        Page<FinStdCostVersion> page = Page.of(current, size);
        versionMapper.selectPageList(page);
        return PageResult.of(page);
    }

    public FinStdCostVersion detail(Long id) {
        FinStdCostVersion v = id == null ? null : versionMapper.selectDetail(id);
        if (v == null) {
            throw new BizException("fin.error.standardCostVersionNotFound");
        }
        return v;
    }

    public PageResult<FinStdCost> items(Long id, long current, long size, String keyword, Integer make) {
        detail(id);
        Page<FinStdCost> page = Page.of(current, size);
        costMapper.selectPageList(page, id, keyword, make);
        return PageResult.of(page);
    }

    /** 新建版本并卷算，返回版本 ID */
    @Transactional
    public Long create(StdCostRequests.Version req) {
        FinConstants.toYearMonth(req.effectivePeriod());
        checkName(req.name().trim(), null);
        FinStdCostVersion v = new FinStdCostVersion();
        v.setName(req.name().trim());
        v.setEffectivePeriod(req.effectivePeriod());
        v.setStatus(StdCostStatus.DRAFT);
        v.setRemark(req.remark());
        versionMapper.insert(v);
        if (req.copyFromId() != null) {
            detail(req.copyFromId());
            List<FinStdCost> rows = costMapper.selectList(Wrappers.<FinStdCost>lambdaQuery().eq(FinStdCost::getVersionId, req.copyFromId()));
            rows.forEach(r -> r.setVersionId(v.getId()));
            insert(rows);
        }
        rollup(v.getId());
        return v.getId();
    }

    @Transactional
    public void update(Long id, StdCostRequests.Version req) {
        FinStdCostVersion v = requireDraft(id);
        FinConstants.toYearMonth(req.effectivePeriod());
        checkName(req.name().trim(), id);
        v.setName(req.name().trim());
        v.setEffectivePeriod(req.effectivePeriod());
        v.setRemark(req.remark());
        versionMapper.updateById(v);
    }

    @Transactional
    public void delete(Long id) {
        requireDraft(id);
        costMapper.delete(Wrappers.<FinStdCost>lambdaQuery().eq(FinStdCost::getVersionId, id));
        versionMapper.deleteById(id);
    }

    /** 新增或修改采购件的标准单价；自制件的由卷算得出 */
    @Transactional
    public void savePrice(Long id, StdCostRequests.Price req) {
        requireDraft(id);
        WmsMaterial m = materialService.mapByIds(List.of(req.materialId())).get(req.materialId());
        if (m == null) {
            throw new BizException("fin.error.materialNotFound");
        }
        if (defaultBoms().containsKey(req.materialId())) {
            throw new BizException("fin.error.materialHasDefaultBomManufacturedStandard", m.getCode(), m.getName());
        }
        BigDecimal price = req.price().setScale(SCALE, Decimals.ROUNDING);
        FinStdCost row = costMapper.selectOne(Wrappers.<FinStdCost>lambdaQuery()
                .eq(FinStdCost::getVersionId, id).eq(FinStdCost::getMaterialId, req.materialId()));
        if (row == null) {
            insert(List.of(purchased(id, req.materialId(), price)));
        } else {
            costMapper.update(Wrappers.<FinStdCost>lambdaUpdate()
                    .set(FinStdCost::getMake, 0).set(FinStdCost::getBomId, null)
                    .set(FinStdCost::getMaterialCost, price).set(FinStdCost::getLaborCost, BigDecimal.ZERO)
                    .set(FinStdCost::getOverheadCost, BigDecimal.ZERO).set(FinStdCost::getUnitCost, price)
                    .set(FinStdCost::getStdHours, BigDecimal.ZERO).set(FinStdCost::getUpdateTime, LocalDateTime.now())
                    .eq(FinStdCost::getId, row.getId()));
        }
        markStale(id);
    }

    /** 删除采购件（BOM 中用到的会在下次卷算时按最近成本重新加入） */
    @Transactional
    public void removePrice(Long id, Long materialId) {
        requireDraft(id);
        FinStdCost row = costMapper.selectOne(Wrappers.<FinStdCost>lambdaQuery()
                .eq(FinStdCost::getVersionId, id).eq(FinStdCost::getMaterialId, materialId));
        if (row == null) {
            throw new BizException("fin.error.materialNotVersion");
        }
        if (row.getMake() == 1) {
            throw new BizException("fin.error.manufacturedItemsComeCostRollupCannot");
        }
        costMapper.deleteById(row.getId());
        markStale(id);
    }

    /**
     * 卷算：所有有默认 BOM 的物料（及其各级子件）按 BOM 逐级计算；子件没有标准单价时按最近一次成本核算的单价（没有时取参考成本价）加入。
     *
     * @return 单价为 0 的采购件数
     */
    @Transactional
    public int rollup(Long id) {
        requireDraft(id);
        Map<Long, FinStdCost> existing = costMapper.selectList(Wrappers.<FinStdCost>lambdaQuery().eq(FinStdCost::getVersionId, id))
                .stream().collect(Collectors.toMap(FinStdCost::getMaterialId, Function.identity()));
        Rollup r = new Rollup(id, existing, defaultBoms());
        r.load();
        r.boms.keySet().forEach(m -> r.compute(m, new LinkedHashMap<>()));
        // 原来是自制件、现在没有默认 BOM 的，按原单位成本转为采购件
        existing.values().stream().filter(e -> e.getMake() == 1 && !r.result.containsKey(e.getMaterialId()))
                .forEach(e -> r.result.put(e.getMaterialId(), purchased(id, e.getMaterialId(), e.getUnitCost())));
        existing.values().stream().filter(e -> e.getMake() == 0 && !r.result.containsKey(e.getMaterialId()))
                .forEach(e -> r.result.put(e.getMaterialId(), e));
        DetailLines.replace(costMapper, FinStdCost::getVersionId, id, new ArrayList<>(r.result.values()), FinStdCost::setVersionId, this::insert);
        versionMapper.update(Wrappers.<FinStdCostVersion>lambdaUpdate()
                .set(FinStdCostVersion::getRollupTime, LocalDateTime.now()).set(FinStdCostVersion::getUpdateTime, LocalDateTime.now())
                .eq(FinStdCostVersion::getId, id));
        return (int) r.result.values().stream().filter(c -> c.getMake() == 0 && c.getMaterialCost().signum() == 0).count();
    }

    /** 自制件的卷算明细：BOM 子件（用量 × 子件标准成本）和工序（工时 × 费率） */
    public StdCostBreakdown breakdown(Long id, Long materialId) {
        detail(id);
        FinStdCost row = costMapper.selectOne(Wrappers.<FinStdCost>lambdaQuery()
                .eq(FinStdCost::getVersionId, id).eq(FinStdCost::getMaterialId, materialId));
        if (row == null || row.getMake() != 1 || row.getBomId() == null) {
            throw new BizException("fin.error.onlyManufacturedItemsHaveRollupDetails");
        }
        List<StdCostBreakdown.BomItem> items = costMapper.selectBomItems(List.of(row.getBomId()));
        Set<Long> ids = items.stream().map(StdCostBreakdown.BomItem::getMaterialId).collect(Collectors.toSet());
        Map<Long, FinStdCost> prices = ids.isEmpty() ? Map.of() : costMapper.selectList(Wrappers.<FinStdCost>lambdaQuery()
                        .eq(FinStdCost::getVersionId, id).in(FinStdCost::getMaterialId, ids))
                .stream().collect(Collectors.toMap(FinStdCost::getMaterialId, Function.identity()));
        Map<Long, WmsMaterial> info = materialService.mapByIds(ids);
        List<StdCostBreakdown.Part> parts = new ArrayList<>();
        for (StdCostBreakdown.BomItem i : items) {
            Long mid = i.getMaterialId();
            BigDecimal qty = usage(i);
            FinStdCost p = prices.get(mid);
            BigDecimal unit = p == null ? BigDecimal.ZERO : p.getUnitCost();
            WmsMaterial m = info.get(mid);
            parts.add(new StdCostBreakdown.Part(mid, m == null ? null : m.getCode(), m == null ? null : m.getName(),
                    m == null ? null : m.getUnit(), p == null ? 0 : p.getMake(), i.getQty(), i.getLossRate(), qty, unit,
                    qty.multiply(unit).setScale(SCALE, Decimals.ROUNDING)));
        }
        List<StdCostBreakdown.BomOp> ops = costMapper.selectBomOps(List.of(row.getBomId()));
        for (StdCostBreakdown.BomOp op : ops) {
            BigDecimal hours = dec(op.getStdHours());
            op.setLabor(hours.multiply(dec(op.getLaborRate())).setScale(SCALE, Decimals.ROUNDING));
            op.setOverhead(hours.multiply(dec(op.getOverheadRate())).setScale(SCALE, Decimals.ROUNDING));
        }
        return new StdCostBreakdown(row, parts, ops);
    }

    @Transactional
    public void release(Long id) {
        FinStdCostVersion v = requireDraft(id);
        if (v.getRollupTime() == null) {
            throw new BizException("fin.error.unitCostsChangedWithoutNewRollup");
        }
        boolean clash = versionMapper.exists(Wrappers.<FinStdCostVersion>lambdaQuery()
                .eq(FinStdCostVersion::getStatus, StdCostStatus.RELEASED).eq(FinStdCostVersion::getEffectivePeriod, v.getEffectivePeriod()));
        if (clash) {
            throw new BizException("fin.error.periodAlreadyHasReleasedStandardCost", v.getEffectivePeriod());
        }
        changeStatus(id, StdCostStatus.DRAFT, StdCostStatus.RELEASED, LocalDateTime.now());
    }

    /** 取消发布：差异分析即时计算，取消后改用更早的版本 */
    @Transactional
    public void unrelease(Long id) {
        if (detail(id).getStatus() != StdCostStatus.RELEASED) {
            throw new BizException("fin.error.versionNotReleased");
        }
        changeStatus(id, StdCostStatus.RELEASED, StdCostStatus.DRAFT, null);
    }

    /** 期间使用的标准成本版本，没有时为 null */
    public FinStdCostVersion effective(int period) {
        return versionMapper.selectEffective(period);
    }

    private void changeStatus(Long id, StdCostStatus from, StdCostStatus to, LocalDateTime releaseTime) {
        StatusTransitions.change(versionMapper, FinStdCostVersion::getId, id, FinStdCostVersion::getStatus, from, to,
                w -> w.set(FinStdCostVersion::getReleaseTime, releaseTime).set(FinStdCostVersion::getUpdateTime, LocalDateTime.now()), "fin.error.versionStatusHasChangedRefreshTry");
    }

    private FinStdCostVersion requireDraft(Long id) {
        FinStdCostVersion v = detail(id);
        if (v.getStatus() != StdCostStatus.DRAFT) {
            throw new BizException("fin.error.versionReleasedCannotModified");
        }
        return v;
    }

    private void checkName(String name, Long excludeId) {
        if (versionMapper.exists(Wrappers.<FinStdCostVersion>lambdaQuery().eq(FinStdCostVersion::getName, name)
                .ne(excludeId != null, FinStdCostVersion::getId, excludeId))) {
            throw new BizException("fin.error.versionNameAlreadyExists", name);
        }
    }

    private void markStale(Long id) {
        versionMapper.update(Wrappers.<FinStdCostVersion>lambdaUpdate()
                .set(FinStdCostVersion::getRollupTime, null).set(FinStdCostVersion::getUpdateTime, LocalDateTime.now())
                .eq(FinStdCostVersion::getId, id));
    }

    private void insert(List<FinStdCost> rows) {
        for (int i = 0; i < rows.size(); i += BATCH) {
            costMapper.insertBatch(rows.subList(i, Math.min(i + BATCH, rows.size())));
        }
    }

    /** 产品 → 默认 BOM */
    private Map<Long, Long> defaultBoms() {
        Map<Long, Long> boms = new HashMap<>();
        costMapper.selectDefaultBoms().forEach(b -> boms.put(((Number) b.get("materialId")).longValue(), ((Number) b.get("bomId")).longValue()));
        return boms;
    }

    private static FinStdCost purchased(Long versionId, Long materialId, BigDecimal price) {
        FinStdCost c = new FinStdCost();
        c.setVersionId(versionId);
        c.setMaterialId(materialId);
        c.setMake(0);
        c.setMaterialCost(price);
        c.setLaborCost(BigDecimal.ZERO);
        c.setOverheadCost(BigDecimal.ZERO);
        c.setUnitCost(price);
        c.setStdHours(BigDecimal.ZERO);
        return c;
    }

    /** 单位用量（含损耗）= 用量 × (1 + 损耗率 / 100) */
    private static BigDecimal usage(StdCostBreakdown.BomItem item) {
        return dec(item.getQty()).multiply(BigDecimal.ONE.add(dec(item.getLossRate()).movePointLeft(2)));
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    /** 一次卷算 */
    private final class Rollup {

        private final Long versionId;
        private final Map<Long, FinStdCost> existing;
        private final Map<Long, Long> boms;
        private final Map<Long, List<StdCostBreakdown.BomItem>> items = new HashMap<>();
        private final Map<Long, List<StdCostBreakdown.BomOp>> ops = new HashMap<>();
        private final Map<Long, BigDecimal> initialPrices = new HashMap<>();
        private final Map<Long, FinStdCost> result = new LinkedHashMap<>();

        Rollup(Long versionId, Map<Long, FinStdCost> existing, Map<Long, Long> boms) {
            this.versionId = versionId;
            this.existing = existing;
            this.boms = boms;
        }

        void load() {
            if (boms.isEmpty()) {
                return;
            }
            costMapper.selectBomItems(boms.values()).forEach(i -> items.computeIfAbsent(i.getBomId(), k -> new ArrayList<>()).add(i));
            costMapper.selectBomOps(boms.values()).forEach(o -> ops.computeIfAbsent(o.getBomId(), k -> new ArrayList<>()).add(o));
            // 没有标准单价的采购件：取最近一次核算的单价
            Set<Long> missing = new HashSet<>();
            items.values().forEach(list -> list.forEach(i -> {
                Long m = i.getMaterialId();
                if (!boms.containsKey(m) && !existing.containsKey(m)) {
                    missing.add(m);
                }
            }));
            if (!missing.isEmpty()) {
                costMapper.selectInitialPrices(missing).forEach(p -> initialPrices.put(p.getMaterialId(), dec(p.getPrice())));
            }
        }

        /** path：当前展开路径，用于发现 BOM 循环 */
        BigDecimal compute(Long m, Map<Long, Boolean> path) {
            FinStdCost done = result.get(m);
            if (done != null) {
                return done.getUnitCost();
            }
            Long bomId = boms.get(m);
            if (bomId == null) {
                FinStdCost e = existing.get(m);
                FinStdCost c = e == null ? purchased(versionId, m, initialPrices.getOrDefault(m, BigDecimal.ZERO).setScale(SCALE, Decimals.ROUNDING))
                        : e.getMake() == 0 ? e : purchased(versionId, m, e.getUnitCost());
                result.put(m, c);
                return c.getUnitCost();
            }
            if (path.containsKey(m)) {
                Map<Long, WmsMaterial> info = materialService.mapByIds(path.keySet());
                throw new BizException("fin.error.circularBomReference", path.keySet().stream().map(id -> info.get(id) == null ? String.valueOf(id)
                        : info.get(id).getCode()).collect(Collectors.joining(" → ")));
            }
            path.put(m, Boolean.TRUE);
            BigDecimal material = BigDecimal.ZERO;
            for (StdCostBreakdown.BomItem i : items.getOrDefault(bomId, List.of())) {
                Long child = i.getMaterialId();
                material = material.add(usage(i).multiply(compute(child, path)));
            }
            path.remove(m);
            BigDecimal hours = BigDecimal.ZERO;
            BigDecimal labor = BigDecimal.ZERO;
            BigDecimal overhead = BigDecimal.ZERO;
            for (StdCostBreakdown.BomOp op : ops.getOrDefault(bomId, List.of())) {
                BigDecimal h = dec(op.getStdHours());
                hours = hours.add(h);
                labor = labor.add(h.multiply(dec(op.getLaborRate())));
                overhead = overhead.add(h.multiply(dec(op.getOverheadRate())));
            }
            FinStdCost c = new FinStdCost();
            c.setVersionId(versionId);
            c.setMaterialId(m);
            c.setMake(1);
            c.setBomId(bomId);
            c.setMaterialCost(material.setScale(SCALE, Decimals.ROUNDING));
            c.setLaborCost(labor.setScale(SCALE, Decimals.ROUNDING));
            c.setOverheadCost(overhead.setScale(SCALE, Decimals.ROUNDING));
            c.setUnitCost(c.getMaterialCost().add(c.getLaborCost()).add(c.getOverheadCost()));
            c.setStdHours(hours.setScale(SCALE, Decimals.ROUNDING));
            result.put(m, c);
            return c.getUnitCost();
        }
    }
}
