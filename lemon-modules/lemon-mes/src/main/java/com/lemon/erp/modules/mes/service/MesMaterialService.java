package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.mes.dto.MaterialPlanRequest;
import com.lemon.erp.modules.mes.dto.MaterialPlanView;
import com.lemon.erp.modules.mes.dto.SubstituteRequest;
import com.lemon.erp.modules.mes.entity.MesMaterialPlan;
import com.lemon.erp.modules.mes.entity.MesMaterialSubstitute;
import com.lemon.erp.modules.mes.mapper.MesMaterialPlanMapper;
import com.lemon.erp.modules.mes.mapper.MesMaterialSubstituteMapper;
import com.lemon.erp.modules.mes.model.SupplyType;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 物料的生产计划属性和替代料：物料主数据只有仓库物料一份，计划属性在「物料管理」页面的生产计划分区维护。
 * <ul>
 *   <li>物料属性：自制 / 采购 / 委外 / 虚拟件，未设置时有默认 BOM 为自制、否则为采购</li>
 *   <li>提前期、最小批量、批量倍数：MRP 和缺料下单计算建议数量与日期</li>
 *   <li>允许超领比例：工单领料的上限，超出部分须走补料申请</li>
 *   <li>替代料：齐套分析中缺料时可以切换</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MesMaterialService {

    private final MesMaterialPlanMapper planMapper;
    private final MesMaterialSubstituteMapper substituteMapper;
    private final WmsMaterialService materialService;

    /** 一组物料的计划属性摘要，最多 500 个 */
    public List<MaterialPlanView> views(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > 500) {
            throw new BizException("一次最多查询500个物料");
        }
        return planMapper.selectViews(ids);
    }

    public MaterialPlanView detail(Long id) {
        List<MaterialPlanView> views = planMapper.selectViews(List.of(id));
        if (views.isEmpty()) {
            throw new BizException("物料不存在");
        }
        MaterialPlanView view = views.get(0);
        view.setSubstitutes(substitutes(List.of(id)).getOrDefault(id, List.of()));
        view.setWhereUsed(planMapper.selectWhereUsed(id));
        return view;
    }

    /** 保存计划属性和替代料（替代料整体替换） */
    @Transactional
    public void savePlan(Long id, MaterialPlanRequest req) {
        materialService.get(id);
        List<SubstituteRequest> subs = req.substitutes() == null ? List.of() : req.substitutes();
        Set<Long> seen = new HashSet<>();
        for (SubstituteRequest s : subs) {
            if (s.substituteId().equals(id)) {
                throw new BizException("替代料不能是物料本身");
            }
            if (!seen.add(s.substituteId())) {
                throw new BizException("替代料重复");
            }
        }
        materialService.getEnabled(seen);
        SupplyType type = SupplyType.of(req.supplyType());
        if (type == SupplyType.PHANTOM && !subs.isEmpty()) {
            throw new BizException("虚拟件不入库、不领料，不需要替代料");
        }

        MesMaterialPlan plan = new MesMaterialPlan();
        plan.setMaterialId(id);
        plan.setSupplyType(type == null ? null : type.name());
        plan.setLeadDays(req.leadDays());
        plan.setMinLot(req.minLot());
        plan.setLotMultiple(req.lotMultiple());
        plan.setOverIssueRate(req.overIssueRate() == null ? BigDecimal.ZERO : req.overIssueRate());
        plan.setPlanner(req.planner() == null || req.planner().isBlank() ? null : req.planner().trim());
        plan.setFqcRequired(Integer.valueOf(1).equals(req.fqcRequired()) ? 1 : 0);
        MesMaterialPlan old = planMapper.selectOne(Wrappers.<MesMaterialPlan>lambdaQuery().eq(MesMaterialPlan::getMaterialId, id));
        if (old == null) {
            planMapper.insert(plan);
        } else {
            // 可为空的字段要能清空，不用 updateById
            planMapper.update(Wrappers.<MesMaterialPlan>lambdaUpdate()
                    .set(MesMaterialPlan::getSupplyType, plan.getSupplyType())
                    .set(MesMaterialPlan::getLeadDays, plan.getLeadDays())
                    .set(MesMaterialPlan::getMinLot, plan.getMinLot())
                    .set(MesMaterialPlan::getLotMultiple, plan.getLotMultiple())
                    .set(MesMaterialPlan::getOverIssueRate, plan.getOverIssueRate())
                    .set(MesMaterialPlan::getPlanner, plan.getPlanner())
                    .set(MesMaterialPlan::getFqcRequired, plan.getFqcRequired())
                    .set(MesMaterialPlan::getUpdateTime, LocalDateTime.now())
                    .eq(MesMaterialPlan::getId, old.getId()));
        }

        substituteMapper.delete(Wrappers.<MesMaterialSubstitute>lambdaQuery().eq(MesMaterialSubstitute::getMaterialId, id));
        if (!subs.isEmpty()) {
            substituteMapper.insertBatch(subs.stream().map(s -> {
                MesMaterialSubstitute x = new MesMaterialSubstitute();
                x.setMaterialId(id);
                x.setSubstituteId(s.substituteId());
                x.setRatio(s.ratio());
                x.setPriority(s.priority() == null ? 1 : s.priority());
                x.setRemark(s.remark());
                return x;
            }).toList());
        }
    }

    // ---------------------------------------------------------------- 供 MRP、工单、齐套分析使用
    /** 物料 ID → 计划属性（没有设置的不在结果中） */
    public Map<Long, MesMaterialPlan> plans(Collection<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        return planMapper.selectByMaterialIds(materialIds).stream()
                .collect(Collectors.toMap(MesMaterialPlan::getMaterialId, Function.identity()));
    }

    /** 主料 ID → 替代料（按优先级） */
    public Map<Long, List<MesMaterialSubstitute>> substitutes(Collection<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        return substituteMapper.selectByMaterialIds(materialIds).stream()
                .collect(Collectors.groupingBy(MesMaterialSubstitute::getMaterialId));
    }

    /** 生效的物料属性：设置的，或有默认 BOM 为自制、否则为采购 */
    public static SupplyType supplyType(MesMaterialPlan plan, boolean hasBom) {
        SupplyType type = plan == null ? null : SupplyType.of(plan.getSupplyType());
        return type != null ? type : hasBom ? SupplyType.MAKE : SupplyType.BUY;
    }

    /**
     * 按批量规则取整的建议数量：不低于最小批量和最小起订量，再向上取整到批量倍数；序列号物料取整数
     *
     * @param minQty 采购件默认供应商的最小起订量，可为空
     */
    public static BigDecimal lotSize(BigDecimal net, MesMaterialPlan plan, boolean serial, BigDecimal minQty) {
        if (net.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal qty = net;
        if (plan != null && plan.getMinLot() != null) {
            qty = qty.max(plan.getMinLot());
        }
        if (minQty != null) {
            qty = qty.max(minQty);
        }
        if (plan != null && plan.getLotMultiple() != null && plan.getLotMultiple().signum() > 0) {
            BigDecimal m = plan.getLotMultiple();
            qty = qty.divide(m, 0, RoundingMode.CEILING).multiply(m);
        }
        if (serial) {
            qty = qty.setScale(0, RoundingMode.CEILING);
        }
        return qty;
    }

    /** 允许超领比例（%），没有设置为 0 */
    public static BigDecimal overIssueRate(MesMaterialPlan plan) {
        return plan == null || plan.getOverIssueRate() == null ? BigDecimal.ZERO : plan.getOverIssueRate();
    }

}
