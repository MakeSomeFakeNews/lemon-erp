package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.mes.dto.QmsRequests;
import com.lemon.erp.modules.mes.dto.WorkOrderRequest;
import com.lemon.erp.modules.mes.entity.MesInspection;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.entity.QmsNcr;
import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.mes.mapper.QmsNcrMapper;
import com.lemon.erp.modules.mes.model.Disposition;
import com.lemon.erp.modules.mes.model.NcrSource;
import com.lemon.erp.modules.mes.model.NcrStatus;
import com.lemon.erp.modules.mes.model.QcType;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsStockHold;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.StockHoldService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 不合格品处理（MRB）：检验不合格时自动生成，也可手工登记（生产发现、客诉）。
 * 评审选择处置方式和责任方后提交，启用「不合格品处置」审批时审批通过后执行，否则立即执行：
 * <ul>
 *     <li>让步接收：来料放行冻结库存</li>
 *     <li>报废：来料放行冻结库存并生成报废出库单（草稿，仓库确认）</li>
 *     <li>返工：按工单产品生成返工工单（草稿）</li>
 *     <li>退货：来料保持冻结，通知采购办理采购退货</li>
 *     <li>挑选：只记录</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class QmsNcrService {

    /** 不合格品处理单号的编码规则标识 */
    public static final String DOC_NO = "NC";
    /** 不合格品处置审批的流程标识 */
    public static final String PROCESS_KEY = "qms_ncr";
    private static final String BUSINESS_KEY_PREFIX = PROCESS_KEY + ":";

    private final QmsNcrMapper ncrMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final WmsMaterialService materialService;
    private final StockHoldService holdService;
    private final WmsStockDocService stockDocService;
    private final DocNoService docNoService;
    private final FlowApi flowApi;
    private final MessageSender messageSender;
    private final MesWorkOrderService workOrderService;
    private final DictLookup dictLookup;

    public PageResult<QmsNcr> page(long current, long size, NcrStatus status, String keyword) {
        Page<QmsNcr> page = Page.of(current, size);
        ncrMapper.selectPageList(page, status, keyword);
        return PageResult.of(page);
    }

    public QmsNcr detail(Long id) {
        QmsNcr ncr = id == null ? null : ncrMapper.selectDetail(id);
        if (ncr == null) {
            throw new BizException("mes.error.nonconformanceReportDoesNotExist");
        }
        return ncr;
    }

    /** 检验不合格确认时生成：来料、成品按送检批量，过程、首件按不良数（没有时按送检数量） */
    @Transactional
    public Long createFromInspection(MesInspection i) {
        boolean lot = i.getType() == QcType.IQC || i.getType() == QcType.FQC;
        BigDecimal qty = lot || i.getDefectQty() == null || i.getDefectQty().signum() == 0 ? i.getInspectQty() : i.getDefectQty();
        QmsNcr ncr = newNcr(NcrSource.INSPECTION, i.getMaterialId(), i.getBatchNo(), qty);
        ncr.setInspectionId(i.getId());
        ncr.setSupplierId(i.getSupplierId());
        ncr.setWoId(i.getWoId());
        ncr.setSourceNo(i.getSourceNo());
        ncr.setDefectType(i.getDefectType());
        ncr.setDescription(StringUtils.hasText(i.getDefectDesc()) ? i.getDefectDesc() : "检验单 " + i.getInspNo() + " 判定不合格"); // i18n-ignore
        ncrMapper.insert(ncr);
        messageSender.sendToPermission("qms:ncr:edit", MessageSender.Message.of("ALERT", "待评审的不合格品：" + ncr.getNcrNo(), // i18n-ignore
                "检验单 " + i.getInspNo() + " 不合格，请评审处置", "/qms/ncr")); // i18n-ignore
        return ncr.getId();
    }

    /** 手工登记 */
    @Transactional
    public Long create(QmsRequests.Ncr req) {
        // 检验来源的处理单只由检验不合格自动生成（且不能删除，误判只能取消），不能手工登记
        if (req.source() == NcrSource.INSPECTION) {
            throw new BizException("mes.error.ncrInspectionSourceNotManual");
        }
        materialService.get(req.materialId());
        QmsNcr ncr = newNcr(req.source(), req.materialId(), req.batchNo(), req.qty());
        ncr.setSupplierId(req.supplierId());
        if (StringUtils.hasText(req.woNo())) {
            MesWorkOrder wo = workOrderMapper.selectOne(Wrappers.<MesWorkOrder>lambdaQuery().eq(MesWorkOrder::getWoNo, req.woNo().trim()));
            if (wo == null) {
                throw new BizException("mes.error.workOrderDoesNotExist", req.woNo());
            }
            ncr.setWoId(wo.getId());
            ncr.setSourceNo(wo.getWoNo());
        }
        ncr.setDefectType(req.defectType());
        ncr.setDescription(req.description());
        ncrMapper.insert(ncr);
        return ncr.getId();
    }

    /**
     * 提交评审处置：启用审批时进入审批，审批通过后执行处置；否则立即执行
     *
     * @return 是否进入审批
     */
    @Transactional
    public boolean review(Long id, QmsRequests.NcrReview req) {
        QmsNcr ncr = requireStatus(id, NcrStatus.OPEN, "mes.label.review");
        checkDisposition(ncr, req);
        dictLookup.check("qms_responsibility", req.responsibility(), "mes.label.responsibility");
        ncrMapper.update(Wrappers.<QmsNcr>lambdaUpdate()
                .set(QmsNcr::getDisposition, req.disposition())
                .set(QmsNcr::getResponsibility, req.responsibility())
                .set(QmsNcr::getReviewOpinion, req.reviewOpinion())
                .eq(QmsNcr::getId, id));
        ncr.setDisposition(req.disposition());
        ncr.setResponsibility(req.responsibility());
        if (flowApi.isAvailable(PROCESS_KEY)) {
            transit(id, NcrStatus.OPEN, NcrStatus.APPROVING);
            String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + id, Map.of(
                    "qty", ncr.getQty().doubleValue(),
                    "disposition", req.disposition().name(),
                    "responsibility", req.responsibility(),
                    "ncrNo", ncr.getNcrNo()));
            ncrMapper.update(Wrappers.<QmsNcr>lambdaUpdate().set(QmsNcr::getFlowInstanceId, instanceId).eq(QmsNcr::getId, id));
            return true;
        }
        execute(ncr, req.reworkWarehouseId(), NcrStatus.OPEN);
        return false;
    }

    /** 审批结束：通过 → 执行处置；拒绝、撤回 → 退回待评审 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        QmsNcr ncr = ncrMapper.selectById(Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length())));
        if (ncr == null || ncr.getStatus() != NcrStatus.APPROVING) {
            return;
        }
        if (e.status() == FlowStatus.APPROVED) {
            execute(ncr, null, NcrStatus.APPROVING);
        } else {
            transit(ncr.getId(), NcrStatus.APPROVING, NcrStatus.OPEN);
        }
    }

    /** 取消：只有待评审的可以取消（误登记） */
    @Transactional
    public void cancel(Long id) {
        requireStatus(id, NcrStatus.OPEN, "mes.label.cancel");
        transit(id, NcrStatus.OPEN, NcrStatus.CANCELLED);
    }

    /** 删除：只有手工登记、待评审或已取消的 */
    @Transactional
    public void delete(Long id) {
        QmsNcr ncr = detail(id);
        if (ncr.getSource() == NcrSource.INSPECTION) {
            throw new BizException("mes.error.ncrsGeneratedFailedInspectionsCannotDeleted");
        }
        if (ncr.getStatus() != NcrStatus.OPEN && ncr.getStatus() != NcrStatus.CANCELLED) {
            throw new BizException("mes.error.ncrsSubmittedReviewCannotDeleted");
        }
        ncrMapper.deleteById(id);
    }

    // ---------------------------------------------------------------- 内部

    private QmsNcr newNcr(NcrSource source, Long materialId, String batchNo, BigDecimal qty) {
        QmsNcr ncr = new QmsNcr();
        ncr.setNcrNo(docNoService.next(DOC_NO));
        ncr.setSource(source);
        ncr.setMaterialId(materialId);
        ncr.setBatchNo(StringUtils.hasText(batchNo) ? batchNo.trim() : null);
        ncr.setQty(qty);
        ncr.setStatus(NcrStatus.OPEN);
        ncr.setCreateBy(SecurityUtils.getUserIdOrNull());
        return ncr;
    }

    private boolean incoming(QmsNcr ncr) {
        return ncr.getSupplierId() != null && ncr.getSourceNo() != null && ncr.getWoId() == null;
    }

    private void checkDisposition(QmsNcr ncr, QmsRequests.NcrReview req) {
        if (req.disposition() == Disposition.REWORK) {
            if (ncr.getWoId() == null) {
                throw new BizException("mes.error.reworkOnlyAppliesNonconformingProductsWork");
            }
            if (req.reworkWarehouseId() == null) {
                throw new BizException("mes.error.selectCompletionWarehouseReworkOrder");
            }
        }
        if (req.disposition() == Disposition.RETURN && !incoming(ncr)) {
            throw new BizException("mes.error.returnOnlyAppliesNonconformingIncomingMaterials");
        }
    }

    /** 执行处置并标记已处置 */
    private void execute(QmsNcr ncr, Long reworkWarehouseId, NcrStatus from) {
        String note = switch (ncr.getDisposition()) {
            case CONCESSION -> incoming(ncr) ? releaseIncoming(ncr, "让步接收") : "让步接收"; // i18n-ignore
            case SCRAP -> incoming(ncr) ? scrapIncoming(ncr) : "已判定报废"; // i18n-ignore
            case REWORK -> rework(ncr, reworkWarehouseId);
            case RETURN -> {
                WmsMaterial m = materialService.get(ncr.getMaterialId());
                messageSender.sendToPermission("scm:purchaseReturn:add", MessageSender.Message.of("ALERT",
                        "不合格品退货：" + m.getCode() + " " + m.getName(), // i18n-ignore
                        "采购入库单 " + ncr.getSourceNo() + " 的 " + m.getName() + " 评审为退货（" + ncr.getNcrNo() + "），请办理采购退货", // i18n-ignore
                        "/scm/purchase-return"));
                yield "冻结库存保持冻结，已通知采购办理退货"; // i18n-ignore
            }
            case SORT -> "挑选处理"; // i18n-ignore
        };
        if (ncrMapper.update(Wrappers.<QmsNcr>lambdaUpdate()
                .set(QmsNcr::getStatus, NcrStatus.DONE)
                .set(QmsNcr::getResultNote, note)
                .set(QmsNcr::getReworkWoId, ncr.getReworkWoId())
                .set(QmsNcr::getDoneTime, LocalDateTime.now())
                .eq(QmsNcr::getId, ncr.getId()).eq(QmsNcr::getStatus, from)) == 0) {
            throw new BizException("mes.error.ncrStatusHasChangedRefreshTry");
        }
    }

    private String releaseIncoming(QmsNcr ncr, String action) {
        List<WmsStockHold> holds = holdService.releaseBySource(ncr.getSourceNo(), ncr.getMaterialId(), ncr.getBatchNo(),
                action + " " + ncr.getNcrNo());
        return holds.isEmpty() ? action + "（没有冻结库存）" : action + "，已放行冻结库存 " + holds.size() + " 条"; // i18n-ignore
    }

    /** 来料报废：放行冻结库存后生成报废出库单草稿（序列号物料需在出库单中补填序列号） */
    private String scrapIncoming(QmsNcr ncr) {
        List<WmsStockHold> holds = holdService.releaseBySource(ncr.getSourceNo(), ncr.getMaterialId(), ncr.getBatchNo(),
                "报废 " + ncr.getNcrNo()); // i18n-ignore
        if (holds.isEmpty()) {
            return "报废（没有冻结库存）"; // i18n-ignore
        }
        WmsMaterial m = materialService.get(ncr.getMaterialId());
        if (m.isSerialManaged()) {
            return "已放行冻结库存，序列号物料请手工办理报废出库"; // i18n-ignore
        }
        Map<Long, List<WmsStockHold>> byWarehouse = holds.stream().collect(Collectors.groupingBy(WmsStockHold::getWarehouseId));
        StringBuilder docs = new StringBuilder();
        for (Map.Entry<Long, List<WmsStockHold>> e : byWarehouse.entrySet()) {
            List<StockDocItemRequest> items = e.getValue().stream()
                    .map(h -> new StockDocItemRequest(h.getMaterialId(), h.getLocationId(), null, h.getBatchNo(), null, null,
                            h.getHoldQty(), null, ncr.getNcrNo()))
                    .toList();
            Long docId = stockDocService.create(StockBizType.OUT, new StockDocRequest("SCRAP", e.getKey(), null, LocalDate.now(),
                    "不合格品报废 " + ncr.getNcrNo(), items)); // i18n-ignore
            WmsStockDoc doc = stockDocService.detail(StockBizType.OUT, docId);
            docs.append(docs.isEmpty() ? "" : "、").append(doc.getDocNo());
        }
        return "已放行冻结库存，报废出库单 " + docs + "（草稿，请仓库确认）"; // i18n-ignore
    }

    private String rework(QmsNcr ncr, Long warehouseId) {
        if (warehouseId == null) {
            throw new BizException("mes.error.approvedWithoutReworkReceivingWarehouseReview");
        }
        Long woId = workOrderService.createRework(new WorkOrderRequest(ncr.getMaterialId(), null, ncr.getQty(), LocalDate.now(),
                null, warehouseId, "不合格品返工 " + ncr.getNcrNo()), ncr.getWoId()); // i18n-ignore
        ncr.setReworkWoId(woId);
        return "已生成返工工单（草稿）"; // i18n-ignore
    }

    private void transit(Long id, NcrStatus from, NcrStatus to) {
        if (ncrMapper.transit(id, from, to) == 0) {
            throw new BizException("mes.error.ncrStatusHasChangedRefreshTry");
        }
    }

    private QmsNcr requireStatus(Long id, NcrStatus status, String action) {
        QmsNcr ncr = detail(id);
        if (ncr.getStatus() != status) {
            throw new BizException("mes.error.currentNcrStatusDoesNotAllow", Messages.get(action));
        }
        return ncr;
    }
}
