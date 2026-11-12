package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.period.BizPeriodGuard;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsStockDocItem;
import com.lemon.erp.modules.wms.mapper.WmsStockDocItemMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockDocMapper;
import com.lemon.erp.modules.wms.model.DocStatus;
import com.lemon.erp.modules.wms.model.LineWarehouses;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockChange;
import com.lemon.erp.modules.wms.model.StockInType;
import com.lemon.erp.modules.wms.model.StockOutType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.wms.model.StockBizType.IN;
import static com.lemon.erp.modules.wms.model.StockBizType.TRANSFER;
import static com.lemon.erp.modules.wms.model.WmsConstants.NO_LOCATION;
import static com.lemon.erp.modules.wms.model.WmsConstants.STOCK_DOC_RULE;
import static com.lemon.erp.modules.wms.model.WmsConstants.batchOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.joinSerials;
import static com.lemon.erp.modules.wms.model.WmsConstants.locationOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.splitSerials;

/**
 * 出入库单：入库、出库、调拨。草稿可以修改、删除；确认后按明细更新库存，不能再修改。
 * 数据权限按制单人部门 + 制单人过滤。
 * <p>
 * 本类负责编排、审批接入和事务边界：校验由 {@link StockDocValidator}（纯规则在 {@link StockDocRules}），冲销由 {@link StockDocReverser} 完成
 */
@Service
@RequiredArgsConstructor
public class WmsStockDocService {

    private final WmsStockDocMapper docMapper;
    private final WmsStockDocItemMapper itemMapper;
    private final DocNoService docNoService;
    private final StockService stockService;
    private final StockCostService costService;
    private final BizPeriodGuard periodGuard;
    private final ApprovalSupport approvalSupport;
    private final StockDocValidator validator;
    private final StockDocReverser reverser;
    private final PermissionService permissionService;
    private final TransferTermsService transferTerms;

    public static final String PROCESS_KEY = "wms_stock_doc";
    /** 查看成本金额（均价、库存金额、流水和单据的单价金额） */
    public static final String COST_PERM = "wms:stock:cost";
    private static final ApprovalFlow FLOW = new ApprovalFlow(PROCESS_KEY, "wms_stock_doc:");

    @DataPermission(table = "wms_stock_doc", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<WmsStockDoc> page(StockBizType kind, long current, long size, DocQuery query) {
        Page<WmsStockDoc> page = Page.of(current, size);
        docMapper.selectPageList(page, kind, query);
        List<WmsStockDoc> docs = page.getRecords();
        if (!docs.isEmpty()) {
            Map<Long, String> headers = new java.util.HashMap<>();
            docs.forEach(d -> headers.put(d.getId(), d.getWarehouseName()));
            Map<Long, List<String>> names = LineWarehouses.names(docMapper.selectItemWarehouses(headers.keySet()), headers);
            docs.forEach(d -> d.setWarehouseNames(names.get(d.getId())));
            if (!permissionService.hasPerm(COST_PERM)) {
                docs.forEach(WmsStockDocService::hideTransferAmounts);
            }
        }
        return PageResult.of(page);
    }

    public WmsStockDoc detail(StockBizType kind, Long id) {
        getAccessible(kind, id);
        WmsStockDoc doc = docMapper.selectDetail(id);
        List<WmsStockDocItem> items = itemMapper.selectByDocId(id);
        // 成本单价、业务单价：有「查看成本金额」权限时返回；没有时只把草稿入库单上填写的成本价返回给能编辑入库单的人
        // （其他入库填写成本价），确认后的实际成本、出库成本不返回
        if (!permissionService.hasPerm(COST_PERM)) {
            boolean draftEditable = kind == IN && doc.getStatus() == DocStatus.DRAFT && permissionService.hasAnyPerm("wms:in:add", "wms:in:edit");
            items.forEach(i -> {
                if (!draftEditable) {
                    i.setCostPrice(null);
                }
                i.setBizPrice(null);
                i.setTransferPrice(null);
                i.setTransferAmount(null);
            });
            hideTransferAmounts(doc);
        }
        doc.setItems(items);
        return doc;
    }

    @Transactional
    public Long create(StockBizType kind, StockDocRequest req) {
        StockDocRules.checkManualType(kind, req.bizType());
        return insert(kind, req, null).getId();
    }

    /**
     * 由业务单据（采购入库、销售出库、工单领料等）生成出入库单并立即确认，在调用方的事务中执行，任一步失败一并回滚
     *
     * @param sourceNo 来源单号
     * @return 已确认的单据（含 ID、单据号）
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public WmsStockDoc createConfirmed(StockBizType kind, StockDocRequest req, String sourceNo) {
        WmsStockDoc doc = insert(kind, req, sourceNo);
        doConfirm(kind, doc);
        return doc;
    }

    private WmsStockDoc insert(StockBizType kind, StockDocRequest req, String sourceNo) {
        boolean manual = !generated(sourceNo, req);
        List<WmsStockDocItem> items = toItems(kind, req, manual);
        validator.validate(kind, req.bizType(), req.warehouseId(), req.toWarehouseId(), items, false);
        TransferTermsService.Terms terms = kind == TRANSFER ? transferTerms.terms(req) : null;
        if (terms != null) {
            transferTerms.applyLinePrices(terms, req, items);
        }
        LoginUser user = SecurityUtils.getLoginUser();
        WmsStockDoc doc = new WmsStockDoc();
        doc.setDocNo(docNoService.next(kind));
        doc.setKind(kind);
        doc.setBizType(kind == TRANSFER ? null : req.bizType());
        doc.setIoCategory(validator.ioCategory(kind, req));
        doc.setWarehouseId(req.warehouseId());
        doc.setToWarehouseId(kind == TRANSFER ? req.toWarehouseId() : null);
        doc.setBizDate(req.bizDate());
        doc.setRemark(req.remark());
        doc.setSourceNo(sourceNo);
        doc.setInTransit(kind == TRANSFER && Boolean.TRUE.equals(req.inTransit()) ? 1 : 0);
        doc.setReversed(0);
        if (!manual) {
            doc.setPartnerId(req.partnerId());
            doc.setPartnerName(req.partnerName() == null || req.partnerName().length() <= 128 ? req.partnerName() : req.partnerName().substring(0, 128));
        }
        doc.setTotalQty(totalQty(items));
        doc.setFee(BigDecimal.ZERO);
        if (terms != null) {
            TransferTermsService.fill(doc, terms);
        }
        doc.setStatus(DocStatus.DRAFT);
        doc.setDeptId(user.deptId());
        docMapper.insert(doc);
        saveItems(doc.getId(), items);
        return doc;
    }

    @Transactional
    public void update(StockBizType kind, Long id, StockDocRequest req) {
        WmsStockDoc draft = getDraft(kind, id);
        // 业务单据生成的草稿（目前都是保存即确认，不会出现）之外，手工单据只能是允许手工新增的类型
        if (draft.getSourceNo() == null) {
            StockDocRules.checkManualType(kind, req.bizType());
        }
        List<WmsStockDocItem> items = toItems(kind, req, draft.getSourceNo() == null);
        validator.validate(kind, req.bizType(), req.warehouseId(), req.toWarehouseId(), items, false);
        TransferTermsService.Terms terms = kind == TRANSFER ? transferTerms.terms(req) : null;
        if (terms != null) {
            transferTerms.applyLinePrices(terms, req, items);
        }
        docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate()
                .set(WmsStockDoc::getBizType, kind == TRANSFER ? null : req.bizType())
                .set(WmsStockDoc::getIoCategory, validator.ioCategory(kind, req))
                .set(WmsStockDoc::getWarehouseId, req.warehouseId())
                .set(WmsStockDoc::getToWarehouseId, kind == TRANSFER ? req.toWarehouseId() : null)
                .set(WmsStockDoc::getBizDate, req.bizDate())
                .set(WmsStockDoc::getRemark, req.remark())
                .set(WmsStockDoc::getInTransit, kind == TRANSFER && Boolean.TRUE.equals(req.inTransit()) ? 1 : 0)
                .set(WmsStockDoc::getTotalQty, totalQty(items))
                .set(WmsStockDoc::getTransferPricing, terms == null ? null : terms.pricing())
                .set(WmsStockDoc::getPriceLevel, terms == null ? null : terms.priceLevel())
                .set(WmsStockDoc::getPriceFactor, terms == null ? null : terms.factor())
                .set(WmsStockDoc::getFee, terms == null ? BigDecimal.ZERO : terms.fee())
                .set(WmsStockDoc::getFeeSettle, terms == null ? null : terms.settle())
                .set(WmsStockDoc::getFeeAccountId, terms == null ? null : terms.accountId())
                .set(WmsStockDoc::getFeeCarrierId, terms == null ? null : terms.carrierId())
                .set(WmsStockDoc::getUpdateTime, LocalDateTime.now())
                .eq(WmsStockDoc::getId, id)
                .eq(WmsStockDoc::getStatus, DocStatus.DRAFT));
        DetailLines.replace(itemMapper, WmsStockDocItem::getDocId, id, items, WmsStockDocItem::setDocId, itemMapper::insertBatch);
    }

    /** 只能删除草稿 */
    @Transactional
    public void delete(StockBizType kind, List<Long> ids) {
        ids.forEach(id -> getDraft(kind, id));
        itemMapper.delete(Wrappers.<WmsStockDocItem>lambdaQuery().in(WmsStockDocItem::getDocId, ids));
        docMapper.deleteByIds(ids);
    }

    /**
     * 确认单据：校验后更新库存。先把状态从草稿改为已完成（条件更新），防止并发重复确认。
     * 发布了「出入库审批」流程时，手工单据先提交审批，审批通过后再更新库存
     *
     * @return true 已提交审批
     */
    @Transactional
    public boolean confirm(StockBizType kind, Long id) {
        WmsStockDoc doc = getDraft(kind, id);
        // 业务单据生成的出入库不走审批
        if (doc.getSourceNo() != null) {
            doConfirm(kind, doc);
            return false;
        }
        return approvalSupport.submit(FLOW, id, Map.of("docNo", doc.getDocNo(),
                        "kind", kind.name(), "bizType", doc.getBizType() == null ? "" : doc.getBizType(), "totalQty", doc.getTotalQty()),
                () -> doConfirm(kind, doc),
                () -> {
                    List<WmsStockDocItem> items = itemMapper.selectList(Wrappers.<WmsStockDocItem>lambdaQuery()
                            .eq(WmsStockDocItem::getDocId, id).orderByAsc(WmsStockDocItem::getId));
                    validator.validate(kind, doc.getBizType(), doc.getWarehouseId(), doc.getToWarehouseId(), items, true);
                    StatusTransitions.change(docMapper, WmsStockDoc::getId, id, WmsStockDoc::getStatus,
                            DocStatus.DRAFT, DocStatus.APPROVING, "wms.error.documentHasConfirmedRefresh");
                },
                instanceId -> docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate()
                        .set(WmsStockDoc::getFlowInstanceId, instanceId).eq(WmsStockDoc::getId, id)));
    }

    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        FLOW.onFinished(e, (id, approved) -> {
            WmsStockDoc doc = docMapper.selectById(id);
            if (doc == null || doc.getStatus() != DocStatus.APPROVING) {
                return;
            }
            if (approved) {
                doConfirm(doc.getKind(), doc, DocStatus.APPROVING);
            } else {
                StatusTransitions.tryChange(docMapper, WmsStockDoc::getId, id, WmsStockDoc::getStatus,
                        DocStatus.APPROVING, DocStatus.DRAFT);
            }
        });
    }

    private void doConfirm(StockBizType kind, WmsStockDoc doc) {
        doConfirm(kind, doc, DocStatus.DRAFT);
    }

    private void doConfirm(StockBizType kind, WmsStockDoc doc, DocStatus from) {
        // 调拨不影响存货成本，只受结账限制（调拨费用计入当期费用，同样受结账限制）
        if (kind == StockBizType.TRANSFER) {
            periodGuard.checkOpen(doc.getBizDate(), kind.getLabelKey());
        } else {
            periodGuard.checkStockOpen(doc.getBizDate(), kind.getLabelKey());
        }
        Long id = doc.getId();
        boolean transit = kind == TRANSFER && Integer.valueOf(1).equals(doc.getInTransit());
        StatusTransitions.change(docMapper, WmsStockDoc::getId, id, WmsStockDoc::getStatus,
                from, transit ? DocStatus.TRANSIT : DocStatus.DONE, w -> w
                        .set(WmsStockDoc::getConfirmBy, SecurityUtils.getUserId())
                        .set(WmsStockDoc::getConfirmTime, LocalDateTime.now())
                        .set(WmsStockDoc::getUpdateTime, LocalDateTime.now()),
                "wms.error.documentHasConfirmedRefresh");
        List<WmsStockDocItem> items = itemMapper.selectList(Wrappers.<WmsStockDocItem>lambdaQuery()
                .eq(WmsStockDocItem::getDocId, id).orderByAsc(WmsStockDocItem::getId));
        validator.validate(kind, doc.getBizType(), doc.getWarehouseId(), doc.getToWarehouseId(), items, true);
        if (doc.docType() instanceof StockOutType out && out.forbidsExpired()) {
            validator.checkNotExpired(items, doc.getBizDate());
        }
        List<StockChange> changes = StockDocRules.toChanges(kind, doc, items);
        // 在途调拨：确认时只调出
        stockService.apply(kind, id, doc.getDocNo(), transit ? changes.stream().filter(c -> !c.increase()).toList() : changes);
        if (kind == TRANSFER) {
            // 调出成本价、调拨价和金额（在途调拨收货时调入流水的业务单价取这里的调拨价）；调拨费用生成费用支出单 / 承运商应付
            transferTerms.settle(doc, items);
            transferTerms.publishFee(doc, null);
        }
    }

    /**
     * 记录调拨费用生成的单据。由财务模块处理 {@link com.lemon.erp.modules.wms.event.TransferFeeEvent} 时调用，与确认 / 冲销同一事务
     *
     * @param billId 月结的承运商应付单
     * @param ioId   现付的费用支出单
     */
    public void bindFee(Long docId, Long billId, Long ioId) {
        docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate()
                .set(WmsStockDoc::getFeeBillId, billId)
                .set(WmsStockDoc::getFeeIoId, ioId)
                .eq(WmsStockDoc::getId, docId));
    }

    /**
     * 在途调拨收货：调入仓库入库，单据变为已完成。调入按调出时的成本计价（调拨不改变均价，在途期间计价数量不变），
     * 调出、调入流水金额相抵；调入流水的业务单价为确认时生成的调拨价。调拨费用已在确认（调出）时生成，收货不再处理
     *
     * @param receiveDate 收货日期，为空取当天；不能早于调拨单的业务日期，所在会计期间须未结账
     */
    @Transactional
    public void receive(Long id, LocalDate receiveDate) {
        WmsStockDoc doc = getAccessible(TRANSFER, id);
        if (doc.getStatus() != DocStatus.TRANSIT) {
            throw new BizException("wms.error.transferOrderNotTransit", doc.getDocNo());
        }
        LocalDate date = receiveDate == null ? LocalDate.now() : receiveDate;
        if (date.isBefore(doc.getBizDate())) {
            throw new BizException("wms.error.receiveDateBeforeTransferDate", date, doc.getBizDate());
        }
        periodGuard.checkOpen(date, "wms.label.transferReceipt");
        StatusTransitions.change(docMapper, WmsStockDoc::getId, id, WmsStockDoc::getStatus,
                DocStatus.TRANSIT, DocStatus.DONE, w -> w
                        .set(WmsStockDoc::getReceiveBy, SecurityUtils.getUserId())
                        .set(WmsStockDoc::getReceiveDate, date)
                        .set(WmsStockDoc::getReceiveTime, LocalDateTime.now()),
                "wms.error.transferOrderHasReceivedRefresh");
        List<WmsStockDocItem> items = itemMapper.selectList(Wrappers.<WmsStockDocItem>lambdaQuery()
                .eq(WmsStockDocItem::getDocId, id).orderByAsc(WmsStockDocItem::getId));
        validator.checkReceiveWarehouse(doc);
        List<StockChange> increases = StockDocRules.toChanges(TRANSFER, doc, items).stream().filter(StockChange::increase).toList();
        Map<Long, BigDecimal> outCosts = costService.transferOutCosts(id,
                increases.stream().map(StockChange::materialId).collect(Collectors.toSet()));
        stockService.apply(TRANSFER, id, doc.getDocNo(), increases.stream()
                .map(c -> c.withCost(outCosts.get(c.materialId()), c.bizPrice(), c.partnerId(), c.partnerName())).toList());
    }

    /**
     * 冲销：为已完成的手工出入库单、调拨单生成红字单据，库存按原单反向变动（入库冲销要求库存仍在原库位）。
     * 业务单据生成的出入库须在业务单据中办理退货；在途调拨须先收货
     *
     * @return 红字单据 ID
     */
    @Transactional
    public Long reverse(StockBizType kind, Long id) {
        return reverser.reverse(kind, getAccessible(kind, id));
    }

    // ---------------------------------------------------------------- 内部

    /** 在数据权限范围内的单据 */
    private WmsStockDoc getAccessible(StockBizType kind, Long id) {
        return DataScopes.require(STOCK_DOC_RULE, () -> docMapper.selectById(id), doc -> doc.getKind() == kind,
                "wms.error.doesNotExistAccessDenied", Messages.get(kind.getLabelKey()));
    }

    private WmsStockDoc getDraft(StockBizType kind, Long id) {
        WmsStockDoc doc = getAccessible(kind, id);
        if (doc.getStatus() != DocStatus.DRAFT) {
            throw new BizException("wms.error.hasConfirmedCannotModifiedDeleted", Messages.get(kind.getLabelKey()), doc.getDocNo());
        }
        return doc;
    }

    /**
     * 业务单据生成（有来源单号），或期初入库导入（由程序生成，没有来源单号）：保留传入的成本单价、业务单价和往来单位
     */
    private static boolean generated(String sourceNo, StockDocRequest req) {
        return sourceNo != null || StockInType.OPENING.matches(req.bizType());
    }

    /**
     * 请求明细转为实体，统一库位、批次、序列号的表示方式；与单据种类无关的字段清空，明细仓库与表头相同时不保存。
     * 手工单据只有入库（寄售入库除外）可以填写成本单价，业务单价只由业务单据传入
     */
    private static List<WmsStockDocItem> toItems(StockBizType kind, StockDocRequest req, boolean manual) {
        Long warehouseId = req.warehouseId();
        boolean manualCost = kind == IN && !StockInType.CONSIGN_IN.matches(req.bizType());
        return req.items().stream().map(r -> {
            WmsStockDocItem item = new WmsStockDocItem();
            item.setMaterialId(r.materialId());
            item.setWarehouseId(kind == TRANSFER || r.warehouseId() == null || r.warehouseId().equals(warehouseId) ? null : r.warehouseId());
            item.setLocationId(locationOrNone(r.locationId()));
            item.setToLocationId(kind == TRANSFER ? locationOrNone(r.toLocationId()) : NO_LOCATION);
            item.setBatchNo(batchOrNone(r.batchNo()));
            if (kind == IN) {
                item.setProductionDate(r.productionDate());
                item.setExpiryDate(r.expiryDate());
            }
            item.setQty(r.qty());
            if (kind != TRANSFER && (!manual || manualCost)) {
                item.setCostPrice(r.costPrice());
            }
            item.setBizPrice(manual ? null : r.bizPrice());
            item.setSerialNos(joinSerials(splitSerials(r.serialNos())));
            item.setRemark(r.remark());
            return item;
        }).toList();
    }

    private void saveItems(Long docId, List<WmsStockDocItem> items) {
        items.forEach(i -> i.setDocId(docId));
        itemMapper.insertBatch(items);
    }

    /** 没有「查看成本金额」权限时不返回调拨单的调出成本金额、调拨金额（同价调拨的调拨金额就是成本） */
    private static void hideTransferAmounts(WmsStockDoc doc) {
        doc.setCostAmount(null);
        doc.setTransferAmount(null);
    }

    private static BigDecimal totalQty(List<WmsStockDocItem> items) {
        return Decimals.sum(items, WmsStockDocItem::getQty);
    }
}
