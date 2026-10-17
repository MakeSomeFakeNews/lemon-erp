package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.fin.dto.CostVariance;
import com.lemon.erp.modules.fin.entity.FinCostRun;
import com.lemon.erp.modules.fin.entity.FinStdCost;
import com.lemon.erp.modules.fin.entity.FinStdCostVersion;
import com.lemon.erp.modules.fin.mapper.FinCostRunMapper;
import com.lemon.erp.modules.fin.mapper.FinCostVarianceMapper;
import com.lemon.erp.modules.fin.mapper.FinStdCostMapper;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 成本差异分析：期间内完工的工单，实际成本与标准成本比较并拆分差异（公式见 {@link CostVariance}）。
 * 使用期间生效的标准成本版本；实际成本取成本核算的工单成本，须先核算
 */
@Service
@RequiredArgsConstructor
public class FinCostVarianceService {

    private final FinCostVarianceMapper varianceMapper;
    private final FinStdCostMapper stdCostMapper;
    private final FinStdCostService stdCostService;
    private final FinCostRunMapper costRunMapper;
    private final WmsMaterialService materialService;

    /** 期间的成本差异：使用的标准成本版本、是否已核算、各工单差异与合计 */
    public record Report(Long versionId, String versionName, boolean costRun, List<CostVariance> orders, CostVariance total) {
    }

    /** 工单子件的用量差异与价格差异 */
    public record Component(Long materialId, String materialCode, String materialName, String unit, BigDecimal unitQty,
                            BigDecimal stdQty, BigDecimal actualQty, BigDecimal actualAmount, BigDecimal stdPrice, boolean hasStd,
                            BigDecimal usageVariance, BigDecimal priceVariance) {
    }

    /** 期间没有生效的标准成本版本时 versionId 为空、没有工单 */
    public Report analyze(int period) {
        FinConstants.toYearMonth(period);
        boolean costRun = costRunMapper.exists(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getPeriod, period));
        FinStdCostVersion version = stdCostService.effective(period);
        if (version == null) {
            return new Report(null, null, costRun, List.of(), total(List.of()));
        }
        LocalDate begin = FinConstants.firstDay(period);
        List<CostVariance> rows = varianceMapper.selectOrders(period, begin, begin.plusMonths(1), version.getId());
        rows.forEach(FinCostVarianceService::compute);
        return new Report(version.getId(), version.getName(), costRun, rows, total(rows));
    }

    public List<Component> components(int period, Long woId) {
        FinStdCostVersion version = requireVersion(period);
        LocalDate next = FinConstants.firstDay(period).plusMonths(1);
        CostVariance wo = varianceMapper.selectOrders(period, FinConstants.firstDay(period), next, version.getId()).stream()
                .filter(r -> r.getWoId().equals(woId)).findFirst()
                .orElseThrow(() -> new BizException("工单不在该期间的差异分析中"));
        Map<Long, BigDecimal> unitQty = new LinkedHashMap<>();
        varianceMapper.selectWoMaterials(woId).forEach(r -> unitQty.put(((Number) r.get("materialId")).longValue(), dec(r.get("unitQty"))));
        Map<Long, Map<String, Object>> issues = varianceMapper.selectIssues(woId, next).stream()
                .collect(Collectors.toMap(r -> ((Number) r.get("materialId")).longValue(), Function.identity()));
        Set<Long> ids = new LinkedHashSet<>(unitQty.keySet());
        ids.addAll(issues.keySet());
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, FinStdCost> std = stdCostMapper.selectList(Wrappers.<FinStdCost>lambdaQuery()
                        .eq(FinStdCost::getVersionId, version.getId()).in(FinStdCost::getMaterialId, ids))
                .stream().collect(Collectors.toMap(FinStdCost::getMaterialId, Function.identity()));
        Map<Long, WmsMaterial> info = materialService.mapByIds(ids);
        List<Component> list = new ArrayList<>();
        for (Long id : ids) {
            BigDecimal uq = unitQty.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal sq = wo.getFinishedQty().multiply(uq);
            Map<String, Object> iss = issues.get(id);
            BigDecimal aq = iss == null ? BigDecimal.ZERO : dec(iss.get("qty"));
            BigDecimal amount = iss == null ? BigDecimal.ZERO : money(dec(iss.get("amount")));
            FinStdCost s = std.get(id);
            BigDecimal sp = s == null ? BigDecimal.ZERO : s.getUnitCost();
            WmsMaterial m = info.get(id);
            list.add(new Component(id, m == null ? null : m.getCode(), m == null ? null : m.getName(), m == null ? null : m.getUnit(),
                    uq, sq.setScale(4, RoundingMode.HALF_UP), aq, amount, sp, s != null,
                    money(aq.subtract(sq).multiply(sp)), money(amount.subtract(aq.multiply(sp)))));
        }
        return list;
    }

    private FinStdCostVersion requireVersion(int period) {
        FinConstants.toYearMonth(period);
        FinStdCostVersion version = stdCostService.effective(period);
        if (version == null) {
            throw new BizException("期间 {0} 没有生效的标准成本版本，请先在「标准成本」中发布", period);
        }
        return version;
    }

    static void compute(CostVariance r) {
        BigDecimal f = r.getFinishedQty();
        BigDecimal ah = r.getHours();
        r.setActualTotal(r.getActualMaterial().add(r.getActualLabor()).add(r.getActualOverhead()));
        r.setStdMaterial(money(f.multiply(r.getStdUnitMaterial())));
        r.setStdLabor(money(f.multiply(r.getStdUnitLabor())));
        r.setStdOverhead(money(f.multiply(r.getStdUnitOverhead())));
        r.setStdTotal(r.getStdMaterial().add(r.getStdLabor()).add(r.getStdOverhead()));
        r.setStdHours(f.multiply(r.getStdUnitHours()).setScale(4, RoundingMode.HALF_UP));
        BigDecimal issuedAtStd = money(r.getIssuedAtStd());
        r.setMaterialPrice(r.getActualMaterial().subtract(issuedAtStd));
        r.setMaterialUsage(issuedAtStd.subtract(r.getStdMaterial()));
        // 标准小时费率 = 单位标准人工（制造费用）/ 单位标准工时；没有标准工时的全部计入费率（耗费）差异
        BigDecimal laborEff = efficiency(ah, r.getStdHours(), r.getStdUnitLabor(), r.getStdUnitHours());
        r.setLaborEfficiency(laborEff);
        r.setLaborRate(r.getActualLabor().subtract(r.getStdLabor()).subtract(laborEff));
        BigDecimal overheadEff = efficiency(ah, r.getStdHours(), r.getStdUnitOverhead(), r.getStdUnitHours());
        r.setOverheadEfficiency(overheadEff);
        r.setOverheadSpending(r.getActualOverhead().subtract(r.getStdOverhead()).subtract(overheadEff));
        r.setTotalVariance(r.getActualTotal().subtract(r.getStdTotal()));
    }

    private static BigDecimal efficiency(BigDecimal actualHours, BigDecimal stdHours, BigDecimal unitCost, BigDecimal unitHours) {
        if (unitHours.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return money(actualHours.subtract(stdHours).multiply(unitCost).divide(unitHours, 10, RoundingMode.HALF_UP));
    }

    private static CostVariance total(List<CostVariance> rows) {
        CostVariance t = new CostVariance();
        t.setActualTotal(sum(rows, CostVariance::getActualTotal));
        t.setStdTotal(sum(rows, CostVariance::getStdTotal));
        t.setActualMaterial(sum(rows, CostVariance::getActualMaterial));
        t.setActualLabor(sum(rows, CostVariance::getActualLabor));
        t.setActualOverhead(sum(rows, CostVariance::getActualOverhead));
        t.setStdMaterial(sum(rows, CostVariance::getStdMaterial));
        t.setStdLabor(sum(rows, CostVariance::getStdLabor));
        t.setStdOverhead(sum(rows, CostVariance::getStdOverhead));
        t.setMaterialPrice(sum(rows, CostVariance::getMaterialPrice));
        t.setMaterialUsage(sum(rows, CostVariance::getMaterialUsage));
        t.setLaborRate(sum(rows, CostVariance::getLaborRate));
        t.setLaborEfficiency(sum(rows, CostVariance::getLaborEfficiency));
        t.setOverheadSpending(sum(rows, CostVariance::getOverheadSpending));
        t.setOverheadEfficiency(sum(rows, CostVariance::getOverheadEfficiency));
        t.setTotalVariance(sum(rows, CostVariance::getTotalVariance));
        return t;
    }

    private static BigDecimal sum(List<CostVariance> rows, Function<CostVariance, BigDecimal> f) {
        return rows.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
