package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.modules.mes.dto.QmsRequests;
import com.lemon.erp.modules.mes.dto.QmsViews;
import com.lemon.erp.modules.mes.entity.QmsInspectionPlan;
import com.lemon.erp.modules.mes.entity.QmsInspectionPlanItem;
import com.lemon.erp.modules.mes.mapper.QmsInspectionPlanItemMapper;
import com.lemon.erp.modules.mes.mapper.QmsInspectionPlanMapper;
import com.lemon.erp.modules.mes.model.ItemMethod;
import com.lemon.erp.modules.mes.model.QcType;
import com.lemon.erp.modules.mes.model.SamplingMode;
import com.lemon.erp.modules.mes.quality.AqlSampling;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 检验方案：按物料（优先）或物料分类定义检验项目与抽样方式（全检 / 固定数量 / AQL）
 */
@Service
@RequiredArgsConstructor
public class QmsPlanService {

    private final QmsInspectionPlanMapper planMapper;
    private final QmsInspectionPlanItemMapper itemMapper;
    private final WmsMaterialService materialService;

    public PageResult<QmsInspectionPlan> page(long current, long size, QcType type, String keyword) {
        Page<QmsInspectionPlan> page = Page.of(current, size);
        planMapper.selectPageList(page, type, keyword);
        return PageResult.of(page);
    }

    public QmsInspectionPlan detail(Long id) {
        QmsInspectionPlan plan = id == null ? null : planMapper.selectDetail(id);
        if (plan == null) {
            throw new BizException("mes.error.inspectionPlanDoesNotExist");
        }
        plan.setItems(items(id));
        return plan;
    }

    public List<QmsInspectionPlanItem> items(Long planId) {
        return itemMapper.selectList(Wrappers.<QmsInspectionPlanItem>lambdaQuery()
                .eq(QmsInspectionPlanItem::getPlanId, planId).orderByAsc(QmsInspectionPlanItem::getSeq));
    }

    @Transactional
    public Long create(QmsRequests.Plan req) {
        QmsInspectionPlan plan = toEntity(req);
        planMapper.insert(plan);
        saveItems(plan.getId(), req.items());
        return plan.getId();
    }

    @Transactional
    public void update(Long id, QmsRequests.Plan req) {
        detail(id);
        QmsInspectionPlan plan = toEntity(req);
        plan.setId(id);
        planMapper.updateById(plan);
        saveItems(id, req.items());
    }

    /** 删除方案：已使用该方案的检验单保留逐项结果，不受影响 */
    @Transactional
    public void delete(List<Long> ids) {
        planMapper.deleteByIds(ids);
        itemMapper.delete(Wrappers.<QmsInspectionPlanItem>lambdaQuery().in(QmsInspectionPlanItem::getPlanId, ids));
    }

    /**
     * 匹配检验方案并计算抽样：物料方案优先，其次物料分类方案；没有方案返回 null
     *
     * @param lotQty 送检数量（批量）
     */
    public QmsViews.PlanMatch match(QcType type, Long materialId, BigDecimal lotQty) {
        QmsInspectionPlan plan = planMapper.selectMatch(type, materialId);
        if (plan == null) {
            return null;
        }
        plan.setItems(items(plan.getId()));
        return sampling(plan, lotQty);
    }

    /** 按方案和批量计算抽样数量、Ac / Re */
    public QmsViews.PlanMatch sampling(QmsInspectionPlan plan, BigDecimal lotQty) {
        if (lotQty == null || lotQty.signum() <= 0) {
            throw new BizException("mes.valid.enterLotQuantity");
        }
        return switch (plan.getSamplingMode()) {
            case FULL -> new QmsViews.PlanMatch(plan, lotQty, null, 0, 1);
            case FIXED -> {
                int ac = plan.getAcceptQty() == null ? 0 : plan.getAcceptQty();
                yield new QmsViews.PlanMatch(plan, plan.getFixedQty().min(lotQty), null, ac, ac + 1);
            }
            case AQL -> {
                long lot = lotQty.setScale(0, RoundingMode.CEILING).longValue();
                if (lot < 2) {
                    yield new QmsViews.PlanMatch(plan, lotQty, null, 0, 1);
                }
                AqlSampling.Plan p = AqlSampling.plan(lot, plan.getInspectionLevel(), plan.getAql());
                yield new QmsViews.PlanMatch(plan, BigDecimal.valueOf(p.sampleSize()), p.code(), p.accept(), p.reject());
            }
        };
    }

    private QmsInspectionPlan toEntity(QmsRequests.Plan req) {
        boolean byMaterial = req.materialId() != null;
        if (byMaterial == StringUtils.hasText(req.materialCategory())) {
            throw new BizException("mes.error.selectApplicableMaterialMaterialCategoryOne");
        }
        if (byMaterial) {
            materialService.get(req.materialId());
        }
        QmsInspectionPlan plan = new QmsInspectionPlan();
        plan.setName(req.name().trim());
        plan.setInspType(req.inspType());
        plan.setMaterialId(req.materialId());
        plan.setMaterialCategory(byMaterial ? null : req.materialCategory());
        plan.setSamplingMode(req.samplingMode());
        if (req.samplingMode() == SamplingMode.FIXED) {
            if (req.fixedQty() == null) {
                throw new BizException("mes.error.enterFixedSampleQuantity");
            }
            plan.setFixedQty(req.fixedQty());
            plan.setAcceptQty(req.acceptQty() == null ? 0 : req.acceptQty());
        } else if (req.samplingMode() == SamplingMode.AQL) {
            if (req.aql() == null || req.inspectionLevel() == null) {
                throw new BizException("mes.error.enterAqlInspectionLevel");
            }
            // 校验 AQL 在表中
            AqlSampling.plan(100, req.inspectionLevel(), req.aql());
            plan.setAql(req.aql());
            plan.setInspectionLevel(req.inspectionLevel());
        }
        plan.setStatus(req.status() == null ? EnableStatus.ENABLED : req.status());
        plan.setRemark(req.remark());
        return plan;
    }

    private void saveItems(Long planId, List<QmsRequests.PlanItem> reqs) {
        List<QmsInspectionPlanItem> items = new ArrayList<>();
        int seq = 1;
        for (QmsRequests.PlanItem r : reqs) {
            boolean quantitative = r.method() == ItemMethod.QUANTITATIVE;
            if (quantitative && r.lowerLimit() == null && r.upperLimit() == null) {
                throw new BizException("mes.error.enterUpperLowerLimitQuantitativeItem", r.name());
            }
            if (quantitative && r.lowerLimit() != null && r.upperLimit() != null && r.lowerLimit().compareTo(r.upperLimit()) > 0) {
                throw new BizException("mes.error.lowerLimitItemCannotGreaterUpper", r.name());
            }
            QmsInspectionPlanItem item = new QmsInspectionPlanItem();
            item.setSeq(seq++);
            item.setName(r.name().trim());
            item.setMethod(r.method());
            item.setStandard(r.standard());
            item.setTarget(quantitative ? r.target() : null);
            item.setLowerLimit(quantitative ? r.lowerLimit() : null);
            item.setUpperLimit(quantitative ? r.upperLimit() : null);
            item.setUnit(r.unit());
            item.setCritical(Boolean.TRUE.equals(r.critical()) ? 1 : 0);
            items.add(item);
        }
        DetailLines.replace(itemMapper, QmsInspectionPlanItem::getPlanId, planId, items, QmsInspectionPlanItem::setPlanId, itemMapper::insertBatch);
    }
}
