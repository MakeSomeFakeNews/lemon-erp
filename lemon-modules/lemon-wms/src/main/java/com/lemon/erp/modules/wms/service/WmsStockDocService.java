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
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockChange;
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
    private final BizPeriodGuard periodGuard;
    private final ApprovalSupport approvalSupport;
    private final StockDocValidator validator;
    private final StockDocReverser reverser;

    public static final String PROCESS_KEY = "wms_stock_doc";
    private static final ApprovalFlow FLOW = new ApprovalFlow(PROCESS_KEY, "wms_stock_doc:");

    @DataPermission(table = "wms_stock_doc", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<WmsStockDoc> page(StockBizType kind, long current, long size, DocQuery query) {
        Page<WmsStockDoc> page = Page.of(current, size);
        docMapper.selectPageList(page, kind, query);
        return PageResult.of(page);
    }

    public WmsStockDoc detail(StockBizType kind, Long id) {
        getAccessible(kind, id);
        WmsStockDoc doc = docMapper.selectDetail(id);
        doc.setItems(itemMapper.selectByDocId(id));
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
        List<WmsStockDocItem> items = toItems(kind, req.items());
        validator.validate(kind, req.bizType(), req.warehouseId(), req.toWarehouseId(), items, false);
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
        doc.setTotalQty(totalQty(items));
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
        List<WmsStockDocItem> items = toItems(kind, req.items());
        validator.validate(kind, req.bizType(), req.warehouseId(), req.toWarehouseId(), items, false);
        docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate()
                .set(WmsStockDoc::getBizType, kind == TRANSFER ? null : req.bizType())
                .set(WmsStockDoc::getIoCategory, validator.ioCategory(kind, req))
                .set(WmsStockDoc::getWarehouseId, req.warehouseId())
                .set(WmsStockDoc::getToWarehouseId, kind == TRANSFER ? req.toWarehouseId() : null)
                .set(WmsStockDoc::getBizDate, req.bizDate())
                .set(WmsStockDoc::getRemark, req.remark())
                .set(WmsStockDoc::getInTransit, kind == TRANSFER && Boolean.TRUE.equals(req.inTransit()) ? 1 : 0)
                .set(WmsStockDoc::getTotalQty, totalQty(items))
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
        // 调拨不影响存货成本，只受结账限制
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
    }

    /** 在途调拨收货：调入仓库入库，单据变为已完成 */
    @Transactional
    public void receive(Long id) {
        WmsStockDoc doc = getAccessible(TRANSFER, id);
        if (doc.getStatus() != DocStatus.TRANSIT) {
            throw new BizException("wms.error.transferOrderNotTransit", doc.getDocNo());
        }
        periodGuard.checkOpen(LocalDate.now(), "wms.label.transferReceipt");
        StatusTransitions.change(docMapper, WmsStockDoc::getId, id, WmsStockDoc::getStatus,
                DocStatus.TRANSIT, DocStatus.DONE, w -> w
                        .set(WmsStockDoc::getReceiveBy, SecurityUtils.getUserId())
                        .set(WmsStockDoc::getReceiveTime, LocalDateTime.now()),
                "wms.error.transferOrderHasReceivedRefresh");
        List<WmsStockDocItem> items = itemMapper.selectList(Wrappers.<WmsStockDocItem>lambdaQuery()
                .eq(WmsStockDocItem::getDocId, id).orderByAsc(WmsStockDocItem::getId));
        validator.checkReceiveWarehouse(doc);
        stockService.apply(TRANSFER, id, doc.getDocNo(), StockDocRules.toChanges(TRANSFER, doc, items).stream().filter(StockChange::increase).toList());
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

    /** 请求明细转为实体，统一库位、批次、序列号的表示方式；与单据种类无关的字段清空 */
    private static List<WmsStockDocItem> toItems(StockBizType kind, List<StockDocItemRequest> requests) {
        return requests.stream().map(r -> {
            WmsStockDocItem item = new WmsStockDocItem();
            item.setMaterialId(r.materialId());
            item.setLocationId(locationOrNone(r.locationId()));
            item.setToLocationId(kind == TRANSFER ? locationOrNone(r.toLocationId()) : NO_LOCATION);
            item.setBatchNo(batchOrNone(r.batchNo()));
            if (kind == IN) {
                item.setProductionDate(r.productionDate());
                item.setExpiryDate(r.expiryDate());
            }
            item.setQty(r.qty());
            item.setSerialNos(joinSerials(splitSerials(r.serialNos())));
            item.setRemark(r.remark());
            return item;
        }).toList();
    }

    private void saveItems(Long docId, List<WmsStockDocItem> items) {
        items.forEach(i -> i.setDocId(docId));
        itemMapper.insertBatch(items);
    }

    private static BigDecimal totalQty(List<WmsStockDocItem> items) {
        return Decimals.sum(items, WmsStockDocItem::getQty);
    }
}
