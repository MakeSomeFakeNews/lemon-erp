package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.mes.dto.InspectionItemRequest;
import com.lemon.erp.modules.mes.dto.InspectionQuery;
import com.lemon.erp.modules.mes.dto.InspectionRequest;
import com.lemon.erp.modules.mes.dto.QmsViews;
import com.lemon.erp.modules.mes.dto.QualitySummary;
import com.lemon.erp.modules.mes.entity.MesInspection;
import com.lemon.erp.modules.mes.entity.MesWoMaterial;
import com.lemon.erp.modules.mes.entity.MesWoOp;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.entity.QmsInspectionItem;
import com.lemon.erp.modules.mes.entity.QmsInspectionPlan;
import com.lemon.erp.modules.mes.entity.QmsInspectionPlanItem;
import com.lemon.erp.modules.mes.mapper.MesInspectionMapper;
import com.lemon.erp.modules.mes.mapper.MesWoMaterialMapper;
import com.lemon.erp.modules.mes.mapper.MesWoOpMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.mes.mapper.QmsInspectionItemMapper;
import com.lemon.erp.modules.mes.mapper.QmsInspectionPlanMapper;
import com.lemon.erp.modules.mes.model.InspectionStatus;
import com.lemon.erp.modules.mes.model.ItemMethod;
import com.lemon.erp.modules.mes.model.QcResult;
import com.lemon.erp.modules.mes.model.QcType;
import com.lemon.erp.modules.mes.model.QualitySummaryBy;
import com.lemon.erp.modules.scm.service.ScmQueryService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.StockHoldService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 质量检验：来料检验（IQC）填采购入库单号自动带出供应商，过程 / 成品 / 首件检验（IPQC / FQC / FAI）填工单号关联工单，
 * 首件检验还要选择工单工序。
 * <ul>
 *     <li>有检验方案时按方案计算抽样数量与 Ac / Re，逐项记录结果：关键项目不合格或不合格数超过 Ac 判为不合格</li>
 *     <li>没有方案时手工填写抽检数量、不良数量和结论</li>
 *     <li>确认后：来料检验放行或保持冻结；不合格自动生成不合格品处理单</li>
 * </ul>
 * 统计按检验类型、按供应商汇总批次合格率与抽样不良率
 */
@Service
@RequiredArgsConstructor
public class MesInspectionService {

    /** 检验单号的编码规则标识 */
    public static final String DOC_NO = "ZJ";

    private final MesInspectionMapper inspectionMapper;
    private final QmsInspectionItemMapper itemMapper;
    private final QmsInspectionPlanMapper planMapper;
    private final QmsPlanService planService;
    private final QmsNcrService ncrService;
    private final MesWorkOrderMapper workOrderMapper;
    private final MesWoOpMapper woOpMapper;
    private final WmsMaterialService materialService;
    private final ScmQueryService scmQueryService;
    private final DocNoService docNoService;
    private final MesWoMaterialMapper woMaterialMapper;
    private final StockHoldService holdService;
    private final MessageSender messageSender;

    public PageResult<MesInspection> page(long current, long size, InspectionQuery query) {
        Page<MesInspection> page = Page.of(current, size);
        inspectionMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public MesInspection get(Long id) {
        MesInspection inspection = id == null ? null : inspectionMapper.selectById(id);
        if (inspection == null) {
            throw new BizException("mes.error.inspectionDoesNotExist");
        }
        return inspection;
    }

    /** 详情：含逐项检验结果 */
    public MesInspection detail(Long id) {
        MesInspection inspection = get(id);
        inspection.setItems(itemMapper.selectList(Wrappers.<QmsInspectionItem>lambdaQuery()
                .eq(QmsInspectionItem::getInspectionId, id).orderByAsc(QmsInspectionItem::getSeq)));
        return inspection;
    }

    @Transactional
    public Long create(InspectionRequest req) {
        Prepared p = prepare(req);
        MesInspection inspection = p.inspection();
        inspection.setInspNo(docNoService.next(DOC_NO));
        inspection.setStatus(InspectionStatus.DRAFT);
        inspection.setCreateBy(SecurityUtils.getUserId());
        inspectionMapper.insert(inspection);
        saveItems(inspection.getId(), p.items());
        return inspection.getId();
    }

    @Transactional
    public void update(Long id, InspectionRequest req) {
        getDraft(id);
        Prepared p = prepare(req);
        MesInspection inspection = p.inspection();
        inspectionMapper.update(Wrappers.<MesInspection>lambdaUpdate()
                .set(MesInspection::getType, inspection.getType())
                .set(MesInspection::getSourceNo, inspection.getSourceNo())
                .set(MesInspection::getSupplierId, inspection.getSupplierId())
                .set(MesInspection::getWoId, inspection.getWoId())
                .set(MesInspection::getWoOpId, inspection.getWoOpId())
                .set(MesInspection::getPlanId, inspection.getPlanId())
                .set(MesInspection::getSampleCode, inspection.getSampleCode())
                .set(MesInspection::getAcceptQty, inspection.getAcceptQty())
                .set(MesInspection::getRejectQty, inspection.getRejectQty())
                .set(MesInspection::getMaterialId, inspection.getMaterialId())
                .set(MesInspection::getBatchNo, inspection.getBatchNo())
                .set(MesInspection::getInspectQty, inspection.getInspectQty())
                .set(MesInspection::getSampleQty, inspection.getSampleQty())
                .set(MesInspection::getDefectQty, inspection.getDefectQty())
                .set(MesInspection::getResult, inspection.getResult())
                .set(MesInspection::getDefectType, inspection.getDefectType())
                .set(MesInspection::getDefectDesc, inspection.getDefectDesc())
                .set(MesInspection::getInspectDate, inspection.getInspectDate())
                .set(MesInspection::getRemark, inspection.getRemark())
                .eq(MesInspection::getId, id));
        itemMapper.delete(Wrappers.<QmsInspectionItem>lambdaQuery().eq(QmsInspectionItem::getInspectionId, id));
        saveItems(id, p.items());
    }

    @Transactional
    public void delete(List<Long> ids) {
        ids.forEach(this::getDraft);
        inspectionMapper.deleteByIds(ids);
        itemMapper.delete(Wrappers.<QmsInspectionItem>lambdaQuery().in(QmsInspectionItem::getInspectionId, ids));
    }

    /**
     * 确认：检验结论生效，之后不能修改、删除。来料检验合格、让步接收放行待检库存，不合格保持冻结并通知采购；
     * 不合格的检验自动生成不合格品处理单
     */
    @Transactional
    public void confirm(Long id) {
        MesInspection i = getDraft(id);
        int updated = inspectionMapper.update(Wrappers.<MesInspection>lambdaUpdate()
                .set(MesInspection::getStatus, InspectionStatus.CONFIRMED)
                .set(MesInspection::getConfirmBy, SecurityUtils.getUserId()).set(MesInspection::getConfirmTime, LocalDateTime.now())
                .eq(MesInspection::getId, id).eq(MesInspection::getStatus, InspectionStatus.DRAFT));
        if (updated == 0) {
            return;
        }
        boolean pass = i.getResult() != QcResult.FAIL;
        if (i.getType() == QcType.IQC && i.getSourceNo() != null) {
            int holds = holdService.inspect(i.getSourceNo(), i.getMaterialId(), i.getBatchNo(), pass, i.getInspNo());
            if (!pass && holds > 0) {
                WmsMaterial m = materialService.get(i.getMaterialId());
                messageSender.sendToPermission("scm:purchaseReturn:add", MessageSender.Message.of(MessageCategory.ALERT,
                        "来料检验不合格：" + m.getCode() + " " + m.getName(), // i18n-ignore
                        "采购入库单 " + i.getSourceNo() + " 的 " + m.getName() + " 检验不合格（" + i.getInspNo() + "），库存已冻结，请评审处置", // i18n-ignore
                        "/qms/ncr"));
            }
        }
        if (!pass) {
            ncrService.createFromInspection(i);
        }
    }

    public List<QualitySummary> summary(QualitySummaryBy by, LocalDate beginDate, LocalDate endDate) {
        return by == QualitySummaryBy.SUPPLIER ? inspectionMapper.selectSummaryBySupplier(beginDate, endDate)
                : inspectionMapper.selectSummaryByType(beginDate, endDate);
    }

    // ---------------------------------------------------------------- 内部

    private record Prepared(MesInspection inspection, List<QmsInspectionItem> items) {
    }

    private MesInspection getDraft(Long id) {
        MesInspection i = get(id);
        if (i.getStatus() == InspectionStatus.CONFIRMED) {
            throw new BizException("mes.error.inspectionConfirmedCannotModifiedDeleted", i.getInspNo());
        }
        return i;
    }

    private Prepared prepare(InspectionRequest req) {
        materialService.get(req.materialId());
        MesInspection i = new MesInspection();
        i.setType(req.type());
        String sourceNo = StringUtils.hasText(req.sourceNo()) ? req.sourceNo().trim() : null;
        i.setSourceNo(sourceNo);
        if (req.type() == QcType.FAI && sourceNo == null) {
            throw new BizException("mes.error.enterWorkOrderNoFirstArticle");
        }
        // 先校验数量与判定，再校验来源单据
        List<QmsInspectionItem> items = req.planId() != null ? judgeByPlan(i, req) : judgeManually(i, req);
        if (sourceNo != null) {
            if (req.type() == QcType.IQC) {
                fillIqcSource(i, sourceNo, req);
            } else {
                fillWoSource(i, sourceNo, req);
            }
        }
        i.setMaterialId(req.materialId());
        i.setBatchNo(StringUtils.hasText(req.batchNo()) ? req.batchNo().trim() : null);
        i.setInspectQty(req.inspectQty());
        i.setDefectType(StringUtils.hasText(req.defectType()) ? req.defectType() : null);
        i.setDefectDesc(req.defectDesc());
        i.setInspectDate(req.inspectDate());
        i.setRemark(req.remark());
        return new Prepared(i, items);
    }

    private void fillIqcSource(MesInspection i, String sourceNo, InspectionRequest req) {
        Long supplierId = scmQueryService.purchaseInSupplier(sourceNo);
        if (supplierId == null) {
            throw new BizException("mes.error.purchaseReceiptDoesNotExist", sourceNo);
        }
        BigDecimal received = scmQueryService.purchaseInQty(sourceNo, req.materialId());
        if (received.signum() == 0) {
            throw new BizException("mes.error.purchaseReceiptDoesNotContainMaterial", sourceNo);
        }
        if (req.inspectQty().compareTo(received) > 0) {
            throw new BizException("mes.error.lotQuantityCannotExceedReceivedQuantity", received.stripTrailingZeros().toPlainString());
        }
        i.setSupplierId(supplierId);
    }

    private void fillWoSource(MesInspection i, String sourceNo, InspectionRequest req) {
        MesWorkOrder wo = workOrderMapper.selectOne(Wrappers.<MesWorkOrder>lambdaQuery().eq(MesWorkOrder::getWoNo, sourceNo));
        if (wo == null) {
            throw new BizException("mes.error.workOrderDoesNotExist", sourceNo);
        }
        // 成品、首件检验：工单产品；过程检验：工单产品或工单用料
        boolean product = wo.getMaterialId().equals(req.materialId());
        if ((req.type() == QcType.FQC || req.type() == QcType.FAI) && !product) {
            throw new BizException("mes.error.materialFinalFirstArticleInspectionsMust");
        }
        if (req.type() == QcType.IPQC && !product && !woMaterialMapper.exists(Wrappers.<MesWoMaterial>lambdaQuery()
                .eq(MesWoMaterial::getWoId, wo.getId()).eq(MesWoMaterial::getMaterialId, req.materialId()))) {
            throw new BizException("mes.error.materialProcessInspectionMustWorkOrder");
        }
        i.setWoId(wo.getId());
        if (req.type() == QcType.FAI) {
            MesWoOp op = req.woOpId() == null ? null : woOpMapper.selectById(req.woOpId());
            if (op == null || !op.getWoId().equals(wo.getId())) {
                throw new BizException("mes.error.selectOperationWorkOrder", sourceNo);
            }
            i.setWoOpId(op.getId());
        }
    }

    /** 无检验方案：手工填写的抽检数量、不良数量、结论 */
    private List<QmsInspectionItem> judgeManually(MesInspection i, InspectionRequest req) {
        if (req.sampleQty() == null) {
            throw new BizException("mes.error.enterSampleQuantity");
        }
        if (req.result() == null) {
            throw new BizException("mes.error.selectConclusion");
        }
        BigDecimal defect = req.defectQty() == null ? BigDecimal.ZERO : req.defectQty();
        if (req.sampleQty().compareTo(req.inspectQty()) > 0) {
            throw new BizException("mes.error.sampleQuantityCannotExceedLotQuantity");
        }
        if (defect.compareTo(req.sampleQty()) > 0) {
            throw new BizException("mes.error.defectQuantityCannotExceedSampleQuantity");
        }
        i.setSampleQty(req.sampleQty());
        i.setDefectQty(defect);
        i.setResult(req.result());
        return List.of();
    }

    /**
     * 按检验方案：抽样数量、Ac / Re 由方案和送检数量计算；不合格数 = 各项目不合格样本数合计，
     * 关键项目不合格或不合格数 ≥ Re 判为不合格
     */
    private List<QmsInspectionItem> judgeByPlan(MesInspection i, InspectionRequest req) {
        QmsInspectionPlan plan = planMapper.selectById(req.planId());
        if (plan == null) {
            throw new BizException("mes.error.inspectionPlanDoesNotExist");
        }
        if (plan.getInspType() != req.type()) {
            throw new BizException("mes.error.inspectionPlanNotInspectionType", plan.getName());
        }
        QmsViews.PlanMatch sampling = planService.sampling(plan, req.inspectQty());
        Map<Long, QmsInspectionPlanItem> planItems = planService.items(plan.getId()).stream()
                .collect(Collectors.toMap(QmsInspectionPlanItem::getId, Function.identity()));
        List<InspectionItemRequest> reqs = req.items() == null ? List.of() : req.items();
        if (reqs.size() != planItems.size() || reqs.stream().anyMatch(r -> !planItems.containsKey(r.planItemId()))) {
            throw new BizException("mes.error.enterResultsEachItemInspectionPlan");
        }
        int sample = sampling.sampleQty().intValue();
        List<QmsInspectionItem> items = new ArrayList<>();
        int defects = 0;
        boolean criticalFail = false;
        for (InspectionItemRequest r : reqs) {
            QmsInspectionPlanItem p = planItems.get(r.planItemId());
            QmsInspectionItem item = fromPlan(p);
            if (p.getMethod() == ItemMethod.QUANTITATIVE) {
                List<BigDecimal> values = r.measured() == null ? List.of() : r.measured().stream().filter(Objects::nonNull).toList();
                if (values.isEmpty()) {
                    throw new BizException("mes.error.enterMeasuredValues", p.getName());
                }
                item.setMeasured(values.stream().map(BigDecimal::toPlainString).collect(Collectors.joining(",")));
                item.setDefectCount((int) values.stream().filter(v -> outOfSpec(v, p)).count());
            } else {
                int count = r.defectCount() == null ? 0 : r.defectCount();
                if (count > sample) {
                    throw new BizException("mes.error.defectCountCannotExceedSampleQuantity", p.getName(), sample);
                }
                item.setDefectCount(count);
            }
            item.setResult(item.getDefectCount() > 0 ? QcResult.FAIL : QcResult.PASS);
            defects += item.getDefectCount();
            criticalFail |= item.getDefectCount() > 0 && Integer.valueOf(1).equals(p.getCritical());
            items.add(item);
        }
        i.setPlanId(plan.getId());
        i.setSampleCode(sampling.sampleCode());
        i.setAcceptQty(sampling.acceptQty());
        i.setRejectQty(sampling.rejectQty());
        i.setSampleQty(sampling.sampleQty());
        // 不合格数按样本计，不超过抽样数量
        i.setDefectQty(BigDecimal.valueOf(Math.min(defects, sample)));
        i.setResult(criticalFail || defects >= sampling.rejectQty() ? QcResult.FAIL : QcResult.PASS);
        return items;
    }

    private static QmsInspectionItem fromPlan(QmsInspectionPlanItem p) {
        QmsInspectionItem item = new QmsInspectionItem();
        item.setPlanItemId(p.getId());
        item.setSeq(p.getSeq());
        item.setName(p.getName());
        item.setMethod(p.getMethod());
        item.setStandard(p.getStandard());
        item.setLowerLimit(p.getLowerLimit());
        item.setUpperLimit(p.getUpperLimit());
        item.setUnit(p.getUnit());
        item.setCritical(p.getCritical());
        return item;
    }

    private static boolean outOfSpec(BigDecimal v, QmsInspectionPlanItem p) {
        return (p.getLowerLimit() != null && v.compareTo(p.getLowerLimit()) < 0)
                || (p.getUpperLimit() != null && v.compareTo(p.getUpperLimit()) > 0);
    }

    private void saveItems(Long inspectionId, List<QmsInspectionItem> items) {
        if (items.isEmpty()) {
            return;
        }
        items.forEach(it -> it.setInspectionId(inspectionId));
        itemMapper.insertBatch(items);
    }
}
