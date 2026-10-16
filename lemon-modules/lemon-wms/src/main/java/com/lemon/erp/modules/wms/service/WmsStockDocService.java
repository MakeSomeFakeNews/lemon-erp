package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.period.BizPeriodGuard;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsBatch;
import com.lemon.erp.modules.wms.entity.WmsLocation;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsStockDocItem;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.mapper.WmsBatchMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockDocItemMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockDocMapper;
import com.lemon.erp.modules.wms.model.DocStatus;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.lemon.erp.modules.wms.model.StockBizType.IN;
import static com.lemon.erp.modules.wms.model.StockBizType.OUT;
import static com.lemon.erp.modules.wms.model.StockBizType.TRANSFER;
import static com.lemon.erp.modules.wms.model.WmsConstants.IN_TYPES;
import static com.lemon.erp.modules.wms.model.WmsConstants.MANUAL_TYPES;
import static com.lemon.erp.modules.wms.model.WmsConstants.NO_BATCH;
import static com.lemon.erp.modules.wms.model.WmsConstants.NO_EXPIRED_OUT_TYPES;
import static com.lemon.erp.modules.wms.model.WmsConstants.NO_LOCATION;
import static com.lemon.erp.modules.wms.model.WmsConstants.OUT_TYPES;
import static com.lemon.erp.modules.wms.model.WmsConstants.STOCK_DOC_RULE;
import static com.lemon.erp.modules.wms.model.WmsConstants.batchOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.joinSerials;
import static com.lemon.erp.modules.wms.model.WmsConstants.locationOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.splitSerials;

/**
 * 出入库单：入库、出库、调拨。草稿可以修改、删除；确认后按明细更新库存，不能再修改。
 * 数据权限按制单人部门 + 制单人过滤。
 */
@Service
@RequiredArgsConstructor
public class WmsStockDocService {

    private final WmsStockDocMapper docMapper;
    private final WmsStockDocItemMapper itemMapper;
    private final WmsReferenceLoader refs;
    private final DocNoService docNoService;
    private final StockService stockService;
    private final BizPeriodGuard periodGuard;
    private final WmsBatchMapper batchMapper;
    private final com.lemon.erp.framework.workflow.FlowApi flowApi;

    public static final String PROCESS_KEY = "wms_stock_doc";
    private static final String BUSINESS_KEY_PREFIX = "wms_stock_doc:";

    @DataPermission(table = "wms_stock_doc", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<WmsStockDoc> page(StockBizType kind, long current, long size, DocQuery query) {
        Page<WmsStockDoc> page = Page.of(current, size);
        docMapper.selectPageList(page, kind.name(), query);
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
        checkManualType(kind, req.bizType());
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
        validate(kind, req.bizType(), req.warehouseId(), req.toWarehouseId(), items, false);
        LoginUser user = SecurityUtils.getLoginUser();
        WmsStockDoc doc = new WmsStockDoc();
        doc.setDocNo(docNoService.next(kind));
        doc.setKind(kind.name());
        doc.setBizType(kind == TRANSFER ? null : req.bizType());
        doc.setWarehouseId(req.warehouseId());
        doc.setToWarehouseId(kind == TRANSFER ? req.toWarehouseId() : null);
        doc.setBizDate(req.bizDate());
        doc.setRemark(req.remark());
        doc.setSourceNo(sourceNo);
        doc.setInTransit(kind == TRANSFER && Boolean.TRUE.equals(req.inTransit()) ? 1 : 0);
        doc.setReversed(0);
        doc.setTotalQty(totalQty(items));
        doc.setStatus(DocStatus.DRAFT.name());
        doc.setDeptId(user.deptId());
        doc.setCreateBy(user.userId());
        docMapper.insert(doc);
        saveItems(doc.getId(), items);
        return doc;
    }

    @Transactional
    public void update(StockBizType kind, Long id, StockDocRequest req) {
        WmsStockDoc draft = getDraft(kind, id);
        // 业务单据生成的草稿（目前都是保存即确认，不会出现）之外，手工单据只能是其他出入库
        if (draft.getSourceNo() == null) {
            checkManualType(kind, req.bizType());
        }
        List<WmsStockDocItem> items = toItems(kind, req.items());
        validate(kind, req.bizType(), req.warehouseId(), req.toWarehouseId(), items, false);
        docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate()
                .set(WmsStockDoc::getBizType, kind == TRANSFER ? null : req.bizType())
                .set(WmsStockDoc::getWarehouseId, req.warehouseId())
                .set(WmsStockDoc::getToWarehouseId, kind == TRANSFER ? req.toWarehouseId() : null)
                .set(WmsStockDoc::getBizDate, req.bizDate())
                .set(WmsStockDoc::getRemark, req.remark())
                .set(WmsStockDoc::getInTransit, kind == TRANSFER && Boolean.TRUE.equals(req.inTransit()) ? 1 : 0)
                .set(WmsStockDoc::getTotalQty, totalQty(items))
                .set(WmsStockDoc::getUpdateTime, LocalDateTime.now())
                .eq(WmsStockDoc::getId, id)
                .eq(WmsStockDoc::getStatus, DocStatus.DRAFT.name()));
        itemMapper.delete(Wrappers.<WmsStockDocItem>lambdaQuery().eq(WmsStockDocItem::getDocId, id));
        saveItems(id, items);
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
        if (doc.getSourceNo() == null && flowApi.isAvailable(PROCESS_KEY)) {
            List<WmsStockDocItem> items = itemMapper.selectList(Wrappers.<WmsStockDocItem>lambdaQuery()
                    .eq(WmsStockDocItem::getDocId, id).orderByAsc(WmsStockDocItem::getId));
            validate(kind, doc.getBizType(), doc.getWarehouseId(), doc.getToWarehouseId(), items, true);
            if (docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate().set(WmsStockDoc::getStatus, DocStatus.APPROVING.name())
                    .eq(WmsStockDoc::getId, id).eq(WmsStockDoc::getStatus, DocStatus.DRAFT.name())) == 0) {
                throw new BizException("单据已确认，请刷新");
            }
            String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + id, java.util.Map.of("docNo", doc.getDocNo(),
                    "kind", kind.name(), "bizType", doc.getBizType() == null ? "" : doc.getBizType(), "totalQty", doc.getTotalQty()));
            docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate().set(WmsStockDoc::getFlowInstanceId, instanceId).eq(WmsStockDoc::getId, id));
            return true;
        }
        doConfirm(kind, doc);
        return false;
    }

    @org.springframework.context.event.EventListener
    public void onFlowFinished(com.lemon.erp.framework.workflow.FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        WmsStockDoc doc = docMapper.selectById(Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length())));
        if (doc == null || !DocStatus.APPROVING.name().equals(doc.getStatus())) {
            return;
        }
        if (e.status() == com.lemon.erp.framework.workflow.FlowStatus.APPROVED) {
            doConfirm(StockBizType.valueOf(doc.getKind()), doc, DocStatus.APPROVING);
        } else {
            docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate().set(WmsStockDoc::getStatus, DocStatus.DRAFT.name())
                    .eq(WmsStockDoc::getId, doc.getId()).eq(WmsStockDoc::getStatus, DocStatus.APPROVING.name()));
        }
    }

    private void doConfirm(StockBizType kind, WmsStockDoc doc) {
        doConfirm(kind, doc, DocStatus.DRAFT);
    }

    private void doConfirm(StockBizType kind, WmsStockDoc doc, DocStatus from) {
        // 调拨不影响存货成本，只受结账限制
        if (kind == StockBizType.TRANSFER) {
            periodGuard.checkOpen(doc.getBizDate(), kind.getLabel());
        } else {
            periodGuard.checkStockOpen(doc.getBizDate(), kind.getLabel());
        }
        Long id = doc.getId();
        boolean transit = kind == TRANSFER && Integer.valueOf(1).equals(doc.getInTransit());
        int updated = docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate()
                .set(WmsStockDoc::getStatus, transit ? DocStatus.TRANSIT.name() : DocStatus.DONE.name())
                .set(WmsStockDoc::getConfirmBy, SecurityUtils.getUserId())
                .set(WmsStockDoc::getConfirmTime, LocalDateTime.now())
                .set(WmsStockDoc::getUpdateTime, LocalDateTime.now())
                .eq(WmsStockDoc::getId, id)
                .eq(WmsStockDoc::getStatus, from.name()));
        if (updated == 0) {
            throw new BizException("单据已确认，请刷新");
        }
        List<WmsStockDocItem> items = itemMapper.selectList(Wrappers.<WmsStockDocItem>lambdaQuery()
                .eq(WmsStockDocItem::getDocId, id).orderByAsc(WmsStockDocItem::getId));
        validate(kind, doc.getBizType(), doc.getWarehouseId(), doc.getToWarehouseId(), items, true);
        if (kind == OUT && NO_EXPIRED_OUT_TYPES.contains(doc.getBizType())) {
            checkNotExpired(items, doc.getBizDate());
        }
        List<StockChange> changes = toChanges(kind, doc, items);
        // 在途调拨：确认时只调出
        stockService.apply(kind, id, doc.getDocNo(), transit ? changes.stream().filter(c -> !c.increase()).toList() : changes);
    }

    /** 在途调拨收货：调入仓库入库，单据变为已完成 */
    @Transactional
    public void receive(Long id) {
        WmsStockDoc doc = getAccessible(TRANSFER, id);
        if (!DocStatus.TRANSIT.name().equals(doc.getStatus())) {
            throw new BizException("调拨单 " + doc.getDocNo() + " 不是在途状态");
        }
        periodGuard.checkOpen(LocalDate.now(), "调拨收货");
        int updated = docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate()
                .set(WmsStockDoc::getStatus, DocStatus.DONE.name())
                .set(WmsStockDoc::getReceiveBy, SecurityUtils.getUserId())
                .set(WmsStockDoc::getReceiveTime, LocalDateTime.now())
                .eq(WmsStockDoc::getId, id).eq(WmsStockDoc::getStatus, DocStatus.TRANSIT.name()));
        if (updated == 0) {
            throw new BizException("调拨单已收货，请刷新");
        }
        List<WmsStockDocItem> items = itemMapper.selectList(Wrappers.<WmsStockDocItem>lambdaQuery()
                .eq(WmsStockDocItem::getDocId, id).orderByAsc(WmsStockDocItem::getId));
        refs.warehouse(doc.getToWarehouseId(), "调入仓库", true);
        stockService.apply(TRANSFER, id, doc.getDocNo(), toChanges(TRANSFER, doc, items).stream().filter(StockChange::increase).toList());
    }

    /**
     * 冲销：为已完成的手工出入库单、调拨单生成红字单据，库存按原单反向变动（入库冲销要求库存仍在原库位）。
     * 业务单据生成的出入库须在业务单据中办理退货；在途调拨须先收货
     *
     * @return 红字单据 ID
     */
    @Transactional
    public Long reverse(StockBizType kind, Long id) {
        WmsStockDoc doc = getAccessible(kind, id);
        if (doc.getSourceNo() != null) {
            throw new BizException("由业务单据（" + doc.getSourceNo() + "）生成的出入库，请在业务单据中办理退货");
        }
        if (doc.getRedOf() != null) {
            throw new BizException("红字单据不能再冲销");
        }
        if (!DocStatus.DONE.name().equals(doc.getStatus())) {
            throw new BizException(DocStatus.TRANSIT.name().equals(doc.getStatus()) ? "在途调拨请先收货再冲销" : "只能冲销已完成的单据");
        }
        LocalDate bizDate = LocalDate.now();
        if (kind == TRANSFER) {
            periodGuard.checkOpen(bizDate, "冲销");
        } else {
            periodGuard.checkStockOpen(bizDate, "冲销");
        }
        if (docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate().set(WmsStockDoc::getReversed, 1)
                .eq(WmsStockDoc::getId, id).eq(WmsStockDoc::getReversed, 0)) == 0) {
            throw new BizException("单据已冲销");
        }
        List<WmsStockDocItem> items = itemMapper.selectList(Wrappers.<WmsStockDocItem>lambdaQuery()
                .eq(WmsStockDocItem::getDocId, id).orderByAsc(WmsStockDocItem::getId));
        LoginUser user = SecurityUtils.getLoginUser();
        WmsStockDoc red = new WmsStockDoc();
        red.setDocNo(docNoService.next(kind));
        red.setKind(kind.name());
        red.setBizType(doc.getBizType());
        red.setWarehouseId(doc.getWarehouseId());
        red.setToWarehouseId(doc.getToWarehouseId());
        red.setBizDate(bizDate);
        red.setRemark("冲销 " + doc.getDocNo());
        red.setInTransit(0);
        red.setRedOf(id);
        red.setReversed(0);
        red.setTotalQty(doc.getTotalQty().negate());
        red.setStatus(DocStatus.DONE.name());
        red.setDeptId(user.deptId());
        red.setCreateBy(user.userId());
        red.setConfirmBy(user.userId());
        red.setConfirmTime(LocalDateTime.now());
        docMapper.insert(red);
        List<WmsStockDocItem> redItems = items.stream().map(i -> {
            WmsStockDocItem r = new WmsStockDocItem();
            r.setMaterialId(i.getMaterialId());
            r.setLocationId(i.getLocationId());
            r.setToLocationId(i.getToLocationId());
            r.setBatchNo(i.getBatchNo());
            r.setProductionDate(i.getProductionDate());
            r.setExpiryDate(i.getExpiryDate());
            r.setQty(i.getQty().negate());
            r.setSerialNos(i.getSerialNos());
            r.setRemark(i.getRemark());
            return r;
        }).toList();
        saveItems(red.getId(), redItems);
        List<StockChange> changes = toChanges(kind, doc, items).stream()
                .map(c -> StockChange.of(c.warehouseId(), c.locationId(), c.materialId(), c.batchNo(), c.qty().negate(), c.serialNos()))
                .toList();
        stockService.apply(kind, red.getId(), red.getDocNo(), changes);
        return red.getId();
    }

    /** 销售、生产、项目等出库不能发出已过期的批次（可报废出库或调拨到不良品仓） */
    private void checkNotExpired(List<WmsStockDocItem> items, LocalDate bizDate) {
        for (WmsStockDocItem item : items) {
            if (item.getBatchNo() == null || item.getBatchNo().isEmpty()) {
                continue;
            }
            WmsBatch batch = batchMapper.selectOne(Wrappers.<WmsBatch>lambdaQuery().eq(WmsBatch::getMaterialId, item.getMaterialId())
                    .eq(WmsBatch::getBatchNo, item.getBatchNo()));
            if (batch != null && batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(bizDate)) {
                throw new BizException("批次 " + item.getBatchNo() + " 已于 " + batch.getExpiryDate() + " 过期，不能发出，请报废出库或调拨到不良品仓");
            }
        }
    }

    // ---------------------------------------------------------------- 校验

    /** 在数据权限范围内的单据 */
    private WmsStockDoc getAccessible(StockBizType kind, Long id) {
        Supplier<WmsStockDoc> loader = () -> docMapper.selectById(id);
        WmsStockDoc doc = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(STOCK_DOC_RULE, loader);
        if (doc == null || !kind.name().equals(doc.getKind())) {
            throw new BizException(kind.getLabel() + "不存在或没有权限");
        }
        return doc;
    }

    private WmsStockDoc getDraft(StockBizType kind, Long id) {
        WmsStockDoc doc = getAccessible(kind, id);
        if (!DocStatus.DRAFT.name().equals(doc.getStatus())) {
            throw new BizException(kind.getLabel() + " " + doc.getDocNo() + " 已确认，不能修改或删除");
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

    /**
     * 校验单据，并按物料的管理方式规范明细（未启用批次管理的批次号置空等）
     *
     * @param strict 确认时为 true：序列号个数必须与数量一致；草稿只要求不超过数量
     */
    /** 手工出入库只能是其他出入库：采购、销售、生产、项目等类型由对应的业务单据生成 */
    private static void checkManualType(StockBizType kind, String bizType) {
        if (kind != TRANSFER && bizType != null && !MANUAL_TYPES.contains(bizType)) {
            throw new BizException("手工" + (kind == IN ? "入库" : "出库") + "只能选择「其他" + (kind == IN ? "入库" : "出库")
                    + "」，采购、销售、生产、项目等出入库请在对应的业务单据中办理");
        }
    }

    private void validate(StockBizType kind, String bizType, Long warehouseId, Long toWarehouseId,
                          List<WmsStockDocItem> items, boolean strict) {
        if (kind != TRANSFER && !StringUtils.hasText(bizType)) {
            throw new BizException(kind == IN ? "请选择入库类型" : "请选择出库类型");
        }
        if (kind != TRANSFER && !(kind == IN ? IN_TYPES : OUT_TYPES).contains(bizType)) {
            throw new BizException((kind == IN ? "入库类型" : "出库类型") + "不正确：" + bizType);
        }
        if (items.isEmpty()) {
            throw new BizException("请添加明细");
        }
        // 出库、调出允许停用的仓库和物料（清空剩余库存），入库、调入要求启用
        WmsWarehouse warehouse = refs.warehouse(warehouseId, kind == TRANSFER ? "调出仓库" : "仓库", kind == IN);
        WmsWarehouse toWarehouse = kind == TRANSFER ? refs.warehouse(toWarehouseId, "调入仓库") : null;
        // 供应商寄售仓（VMI）只能做寄售入库，寄售入库只能入供应商寄售仓
        if (kind == IN && ("CONSIGN_IN".equals(bizType) != "CONSIGN_IN".equals(warehouse.getType()))) {
            throw new BizException("CONSIGN_IN".equals(bizType) ? "寄售入库请选择供应商寄售仓" : "供应商寄售仓只能做寄售入库");
        }
        Map<Long, WmsMaterial> materials = refs.materials(items.stream().map(WmsStockDocItem::getMaterialId).toList(), kind == IN);
        Map<Long, WmsLocation> locations = refs.locations(items.stream()
                .flatMap(i -> Stream.of(i.getLocationId(), i.getToLocationId())).toList());
        Map<Long, Set<String>> serialsByMaterial = new HashMap<>();

        for (int i = 0; i < items.size(); i++) {
            WmsStockDocItem item = items.get(i);
            WmsMaterial material = materials.get(item.getMaterialId());
            try {
                // 入库、调入要求库位启用；出库、调出允许从停用的库位取出
                WmsReferenceLoader.checkLocation(locations, item.getLocationId(), warehouse, kind == IN);
                if (kind == TRANSFER) {
                    WmsReferenceLoader.checkLocation(locations, item.getToLocationId(), toWarehouse, true);
                    if (warehouse.getId().equals(toWarehouse.getId()) && item.getLocationId().equals(item.getToLocationId())) {
                        throw new BizException("调出、调入的仓库和库位相同");
                    }
                }
                checkBatch(item, material);
                if (item.getProductionDate() != null && item.getExpiryDate() != null
                        && item.getExpiryDate().isBefore(item.getProductionDate())) {
                    throw new BizException("有效期不能早于生产日期");
                }
                List<String> serials = checkSerials(item.getSerialNos(), item.getQty(), material, strict,
                        serialsByMaterial.computeIfAbsent(material.getId(), k -> new HashSet<>()));
                item.setSerialNos(joinSerials(serials));
            } catch (BizException e) {
                throw new BizException("第 " + (i + 1) + " 行（" + StockService.describe(material) + "）：" + e.getMessage());
            }
        }
    }

    private static void checkBatch(WmsStockDocItem item, WmsMaterial material) {
        if (!material.isBatchManaged()) {
            item.setBatchNo(NO_BATCH);
            item.setProductionDate(null);
            item.setExpiryDate(null);
        } else if (item.getBatchNo().isEmpty()) {
            throw new BizException("物料启用了批次管理，请填写批次号");
        }
    }

    /**
     * 校验序列号：数量为整数、个数与数量一致、同一物料在单据内不重复。非序列号物料忽略序列号
     *
     * @param seen 该物料在本单据前面行中出现过的序列号
     * @return 规范后的序列号
     */
    static List<String> checkSerials(String text, BigDecimal qty, WmsMaterial material, boolean strict, Set<String> seen) {
        if (!material.isSerialManaged()) {
            return List.of();
        }
        if (qty.stripTrailingZeros().scale() > 0) {
            throw new BizException("序列号管理的物料数量必须为整数");
        }
        List<String> serials = splitSerials(text);
        int count = qty.intValue();
        if (strict ? serials.size() != count : serials.size() > count) {
            throw new BizException("序列号个数（" + serials.size() + "）与数量（" + count + "）不一致");
        }
        for (String sn : serials) {
            if (sn.length() > 64) {
                throw new BizException("序列号不能超过64个字符：" + sn);
            }
            if (!seen.add(sn)) {
                throw new BizException("序列号重复：" + sn);
            }
        }
        return serials;
    }

    private static List<StockChange> toChanges(StockBizType kind, WmsStockDoc doc, List<WmsStockDocItem> items) {
        List<StockChange> changes = new ArrayList<>();
        for (WmsStockDocItem item : items) {
            List<String> serials = splitSerials(item.getSerialNos());
            switch (kind) {
                case IN -> changes.add(new StockChange(doc.getWarehouseId(), item.getLocationId(), item.getMaterialId(),
                        item.getBatchNo(), item.getQty(), serials, item.getProductionDate(), item.getExpiryDate()));
                case OUT -> changes.add(StockChange.of(doc.getWarehouseId(), item.getLocationId(), item.getMaterialId(),
                        item.getBatchNo(), item.getQty().negate(), serials));
                case TRANSFER -> {
                    changes.add(StockChange.of(doc.getWarehouseId(), item.getLocationId(), item.getMaterialId(),
                            item.getBatchNo(), item.getQty().negate(), serials));
                    changes.add(StockChange.of(doc.getToWarehouseId(), item.getToLocationId(), item.getMaterialId(),
                            item.getBatchNo(), item.getQty(), serials));
                }
                default -> throw new IllegalArgumentException("不支持的单据类型：" + kind);
            }
        }
        return changes;
    }

    private void saveItems(Long docId, List<WmsStockDocItem> items) {
        items.forEach(i -> i.setDocId(docId));
        itemMapper.insertBatch(items);
    }

    private static BigDecimal totalQty(List<WmsStockDocItem> items) {
        return items.stream().map(WmsStockDocItem::getQty).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
