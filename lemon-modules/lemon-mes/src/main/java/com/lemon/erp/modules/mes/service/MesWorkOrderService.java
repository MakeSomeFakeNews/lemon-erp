package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.period.BizPeriodGuard;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.modules.mes.dto.ReportRequest;
import com.lemon.erp.modules.mes.dto.WorkOrderQuery;
import com.lemon.erp.modules.mes.dto.WorkOrderRequest;
import com.lemon.erp.modules.mes.entity.MesBom;
import com.lemon.erp.modules.mes.entity.MesBomItem;
import com.lemon.erp.modules.mes.entity.MesEquipment;
import com.lemon.erp.modules.mes.entity.MesMaterialPlan;
import com.lemon.erp.modules.mes.entity.MesPlan;
import com.lemon.erp.modules.mes.entity.MesReport;
import com.lemon.erp.modules.mes.entity.MesWoMaterial;
import com.lemon.erp.modules.mes.entity.MesWoOp;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.mapper.MesPlanMapper;
import com.lemon.erp.modules.mes.mapper.MesEquipmentMapper;
import com.lemon.erp.modules.mes.mapper.MesReportMapper;
import com.lemon.erp.modules.mes.mapper.MesWoMaterialMapper;
import com.lemon.erp.modules.mes.mapper.MesWoOpMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.mes.model.EquipmentStatus;
import com.lemon.erp.modules.mes.model.PlanStatus;
import com.lemon.erp.modules.mes.model.SupplyType;
import com.lemon.erp.modules.mes.model.WorkOrderClosedEvent;
import com.lemon.erp.modules.mes.model.WorkOrderSource;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.mes.model.WorkOrderType;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import com.lemon.erp.modules.mes.entity.MesInspection;
import com.lemon.erp.modules.mes.mapper.MesInspectionMapper;
import com.lemon.erp.modules.mes.model.QcType;
import com.lemon.erp.modules.mes.model.QcResult;
import com.lemon.erp.modules.mes.model.InspectionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.lemon.erp.modules.mes.model.MesConstants.WO_RULE;

/**
 * 生产工单：草稿 → 下达 → 生产中 → 已完工，未完工的可以关闭。
 * 保存时按 BOM 生成用料（需求数量 = 工单数量 × 用量 × (1 + 损耗率)，虚拟件展开为子件）和工序（工艺路线），之后修改 BOM 不影响已有工单；
 * 领料、退料、完工入库见 {@link MesWoDocService}，报工按工序累计合格、报废数量与工时
 */
@Service
@RequiredArgsConstructor
public class MesWorkOrderService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MAX_PHANTOM_DEPTH = 20;

    private final MesWorkOrderMapper workOrderMapper;
    private final MesWoMaterialMapper woMaterialMapper;
    private final MesWoOpMapper woOpMapper;
    private final MesReportMapper reportMapper;
    private final MesEquipmentMapper equipmentMapper;
    private final MesMoldService moldService;
    private final MesInspectionMapper inspectionMapper;
    private final MesPlanMapper planMapper;
    private final MesBomService bomService;
    private final MesMaterialService materialPlanService;
    private final WmsMaterialService materialService;
    private final WmsWarehouseService warehouseService;
    private final DocNoService docNoService;
    private final BizPeriodGuard periodGuard;
    private final ApplicationEventPublisher eventPublisher;
    private final com.lemon.erp.framework.record.ChangeLogger changeLogger;

    private static final Map<String, String> CHANGE_FIELDS = com.lemon.erp.framework.record.ChangeLogger.fields("qty", "数量", // i18n-ignore
            "planStart", "计划开工", "planEnd", "计划完工"); // i18n-ignore

    @DataPermission(table = "mes_work_order", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<MesWorkOrder> page(long current, long size, WorkOrderQuery query) {
        Page<MesWorkOrder> page = Page.of(current, size);
        workOrderMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public MesWorkOrder detail(Long id) {
        getAccessible(id);
        MesWorkOrder wo = workOrderMapper.selectDetail(id);
        wo.setMaterials(woMaterialMapper.selectByWo(id));
        wo.setOps(woOpMapper.selectByWo(id));
        return wo;
    }

    @Transactional
    public Long create(WorkOrderRequest req) {
        return createDraft(req, WorkOrderSource.MANUAL, null);
    }

    /**
     * 新建草稿工单，MRP 下达、生产计划转工单时也调用
     *
     * @param source 工单来源
     */
    @Transactional
    public Long createDraft(WorkOrderRequest req, WorkOrderSource source, Long planId) {
        MesBom bom = resolveBom(req);
        LoginUser user = SecurityUtils.getLoginUser();
        MesWorkOrder wo = new MesWorkOrder();
        wo.setWoNo(docNoService.next(MesDocNos.WORK_ORDER));
        wo.setMaterialId(req.materialId());
        wo.setBomId(bom.getId());
        wo.setQty(req.qty());
        wo.setPlanStart(req.planStart());
        wo.setPlanEnd(req.planEnd());
        wo.setWarehouseId(req.warehouseId());
        wo.setStatus(WorkOrderStatus.DRAFT);
        wo.setFinishedQty(BigDecimal.ZERO);
        wo.setSource(source);
        wo.setPlanId(planId);
        wo.setRemark(req.remark());
        wo.setDeptId(user.deptId());
        wo.setCreateBy(user.userId());
        workOrderMapper.insert(wo);
        generateLines(wo.getId(), bom, req.qty());
        return wo.getId();
    }

    /** 修改草稿，按（新的）BOM 重新生成用料和工序 */
    @Transactional
    public void update(Long id, WorkOrderRequest req) {
        getDraft(id);
        MesBom bom = resolveBom(req);
        workOrderMapper.update(Wrappers.<MesWorkOrder>lambdaUpdate()
                .set(MesWorkOrder::getMaterialId, req.materialId())
                .set(MesWorkOrder::getBomId, bom.getId())
                .set(MesWorkOrder::getQty, req.qty())
                .set(MesWorkOrder::getPlanStart, req.planStart())
                .set(MesWorkOrder::getPlanEnd, req.planEnd())
                .set(MesWorkOrder::getWarehouseId, req.warehouseId())
                .set(MesWorkOrder::getRemark, req.remark())
                .set(MesWorkOrder::getUpdateTime, LocalDateTime.now())
                .eq(MesWorkOrder::getId, id)
                .eq(MesWorkOrder::getStatus, WorkOrderStatus.DRAFT));
        woMaterialMapper.delete(Wrappers.<MesWoMaterial>lambdaQuery().eq(MesWoMaterial::getWoId, id));
        woOpMapper.delete(Wrappers.<MesWoOp>lambdaQuery().eq(MesWoOp::getWoId, id));
        generateLines(id, bom, req.qty());
    }

    @Transactional
    public void delete(List<Long> ids) {
        ids.forEach(this::getDraft);
        woMaterialMapper.delete(Wrappers.<MesWoMaterial>lambdaQuery().in(MesWoMaterial::getWoId, ids));
        woOpMapper.delete(Wrappers.<MesWoOp>lambdaQuery().in(MesWoOp::getWoId, ids));
        workOrderMapper.deleteByIds(ids);
    }

    /** 下达：车间可以开始领料、报工 */
    public void release(Long id) {
        getDraft(id);
        transit(id, Set.of(WorkOrderStatus.DRAFT), WorkOrderStatus.RELEASED, true);
    }

    /** 关闭：未完工的工单不再生产，已领未用的物料请先退料 */
    public void close(Long id) {
        MesWorkOrder wo = getAccessible(id);
        if (!wo.getStatus().isExecutable()) {
            throw new BizException("mes.error.onlyReleasedProductionWorkOrdersClosed");
        }
        transit(id, WorkOrderStatus.EXECUTABLE, WorkOrderStatus.CLOSED, false);
        eventPublisher.publishEvent(new WorkOrderClosedEvent(id));
    }

    /** 报工：合格 + 报废累计不超过工单数量 */
    @Transactional
    public void report(Long id, ReportRequest req) {
        MesWorkOrder wo = getExecutable(id);
        periodGuard.checkStockOpen(req.reportDate(), "mes.label.productionReport");
        MesWoOp op = woOpMapper.selectById(req.opId());
        if (op == null || !op.getWoId().equals(id)) {
            throw new BizException("mes.error.operationDoesNotBelongWorkOrder");
        }
        BigDecimal scrap = req.scrapQty() == null ? BigDecimal.ZERO : req.scrapQty();
        if (req.goodQty().signum() == 0 && scrap.signum() == 0 && req.hours().signum() == 0) {
            throw new BizException("mes.error.enterReportedQuantityHours");
        }
        // 工序顺序：本道累计（合格 + 报废）不能超过上道工序的累计合格数
        MesWoOp prev = woOpMapper.selectByWo(id).stream().filter(o -> o.getSeq() < op.getSeq())
                .max(Comparator.comparing(MesWoOp::getSeq)).orElse(null);
        if (prev != null && op.getGoodQty().add(op.getScrapQty()).add(req.goodQty()).add(scrap).compareTo(prev.getGoodQty()) > 0) {
            throw new BizException("mes.error.cumulativeGoodScrapOperationCannotExceed", op.getSeq(), op.getName(), prev.getSeq(), prev.getName(), prev.getGoodQty().stripTrailingZeros().toPlainString());
        }
        // 首件检验：首件检验合格前，该工序累计只能报工 1 件
        if (Integer.valueOf(1).equals(op.getFirstArticle())
                && op.getGoodQty().add(op.getScrapQty()).add(req.goodQty()).add(scrap).compareTo(BigDecimal.ONE) > 0
                && !firstArticlePassed(op.getId())) {
            throw new BizException("mes.error.operationRequiresPassedFirstArticleInspection", op.getSeq(), op.getName());
        }
        if (req.equipmentId() != null) {
            MesEquipment equipment = equipmentMapper.selectById(req.equipmentId());
            if (equipment == null || equipment.getStatus() == EquipmentStatus.SCRAPPED) {
                throw new BizException("mes.error.equipmentDoesNotExistHasScrapped");
            }
        }
        if (woOpMapper.addReport(op.getId(), req.goodQty(), scrap, req.hours(), wo.getQty()) == 0) {
            throw new BizException("mes.error.cumulativeGoodScrapQuantitiesOperationExceed", op.getSeq(), op.getName());
        }
        MesReport report = new MesReport();
        report.setWoId(id);
        report.setOpId(op.getId());
        report.setReportDate(req.reportDate());
        report.setGoodQty(req.goodQty());
        report.setScrapQty(scrap);
        report.setHours(req.hours());
        report.setRemark(req.remark());
        report.setOperatorId(req.operatorId() != null ? req.operatorId() : SecurityUtils.getUserId());
        report.setPieceWage(op.getPieceRate() == null ? null : req.goodQty().multiply(op.getPieceRate()).setScale(2, RoundingMode.HALF_UP));
        report.setCreateBy(SecurityUtils.getUserId());
        report.setEquipmentId(req.equipmentId());
        report.setMoldId(req.moldId());
        reportMapper.insert(report);
        if (req.moldId() != null) {
            int shots = moldService.use(req.moldId(), id, req.equipmentId(), report.getId(), req.reportDate(), req.goodQty().add(scrap));
            reportMapper.update(Wrappers.<MesReport>lambdaUpdate().set(MesReport::getShots, shots).eq(MesReport::getId, report.getId()));
        }
        markStarted(id);
    }

    // ---------------------------------------------------------------- 改量、拆分、返工

    /**
     * 修改已下达工单的数量：不少于已完工数量和各工序已报工（合格 + 报废）数量；用料需求按新数量重算（已领超出的部分可退料）
     */
    @Transactional
    public void changeQty(Long id, BigDecimal qty) {
        MesWorkOrder wo = getExecutable(id);
        WmsMaterial product = materialService.get(wo.getMaterialId());
        if (product.isSerialManaged() && qty.stripTrailingZeros().scale() > 0) {
            throw new BizException("mes.error.productSerialNumberManagedQuantityMust");
        }
        BigDecimal min = startedQty(wo);
        if (qty.compareTo(min) < 0 || qty.signum() <= 0) {
            throw new BizException("mes.error.quantityCannotLessFinishedReportedQuantity", min.stripTrailingZeros().toPlainString());
        }
        Object before = workOrderMapper.selectById(id);
        workOrderMapper.update(Wrappers.<MesWorkOrder>lambdaUpdate().set(MesWorkOrder::getQty, qty)
                .set(MesWorkOrder::getUpdateTime, LocalDateTime.now()).eq(MesWorkOrder::getId, id));
        Map<Long, WmsMaterial> materials = materialService.mapByIds(woMaterialMapper.selectByWo(id).stream().map(MesWoMaterial::getMaterialId).toList());
        for (MesWoMaterial m : woMaterialMapper.selectByWo(id)) {
            BigDecimal required = qty.multiply(m.getUnitQty());
            boolean whole = materials.get(m.getMaterialId()).isSerialManaged();
            woMaterialMapper.update(Wrappers.<MesWoMaterial>lambdaUpdate()
                    .set(MesWoMaterial::getRequiredQty, whole ? required.setScale(0, RoundingMode.CEILING) : required.setScale(4, RoundingMode.HALF_UP))
                    .eq(MesWoMaterial::getId, m.getId()));
        }
        changeLogger.log("mes_work_order", id, before, workOrderMapper.selectById(id), CHANGE_FIELDS);
        if (wo.getFinishedQty().compareTo(qty) >= 0) {
            markCompleted(id);
        }
    }

    /**
     * 拆分：从工单（草稿或已下达）未开工的数量中拆出一张新的草稿工单（同产品、同 BOM），原工单数量相应减少
     *
     * @return 新工单 ID
     */
    @Transactional
    public Long split(Long id, BigDecimal qty, LocalDate planStart, LocalDate planEnd) {
        MesWorkOrder wo = getAccessible(id);
        boolean draft = wo.getStatus() == WorkOrderStatus.DRAFT;
        if (!draft && !wo.getStatus().isExecutable()) {
            throw new BizException("mes.error.onlyDraftReleasedProductionWorkOrders");
        }
        BigDecimal free = wo.getQty().subtract(startedQty(wo));
        if (qty.signum() <= 0 || qty.compareTo(free) > 0 || qty.compareTo(wo.getQty()) >= 0) {
            throw new BizException("mes.error.splitQuantityMustGreaterLessWork", free.stripTrailingZeros().toPlainString());
        }
        Long newId = createDraft(new WorkOrderRequest(wo.getMaterialId(), wo.getBomId(), qty,
                planStart != null ? planStart : wo.getPlanStart(), planEnd != null ? planEnd : wo.getPlanEnd(), wo.getWarehouseId(),
                "拆分自 " + wo.getWoNo()), WorkOrderSource.SPLIT, wo.getPlanId()); // i18n-ignore
        workOrderMapper.update(Wrappers.<MesWorkOrder>lambdaUpdate().set(MesWorkOrder::getSourceWoId, id).eq(MesWorkOrder::getId, newId));
        BigDecimal left = wo.getQty().subtract(qty);
        if (draft) {
            update(id, new WorkOrderRequest(wo.getMaterialId(), wo.getBomId(), left, wo.getPlanStart(), wo.getPlanEnd(), wo.getWarehouseId(),
                    wo.getRemark()));
        } else {
            changeQty(id, left);
        }
        return newId;
    }

    /**
     * 返工工单：用料为产品本身（领出待返工的产品），工序取产品的默认 BOM，完工后重新入库
     *
     * @return 草稿返工工单 ID
     */
    @Transactional
    public Long createRework(WorkOrderRequest req, Long sourceWoId) {
        MesBom bom = resolveBom(new WorkOrderRequest(req.materialId(), null, req.qty(), req.planStart(), req.planEnd(), req.warehouseId(),
                req.remark()));
        if (sourceWoId != null) {
            getAccessible(sourceWoId);
        }
        LoginUser user = SecurityUtils.getLoginUser();
        MesWorkOrder wo = new MesWorkOrder();
        wo.setWoNo(docNoService.next(MesDocNos.REWORK_ORDER));
        wo.setMaterialId(req.materialId());
        wo.setBomId(bom.getId());
        wo.setQty(req.qty());
        wo.setWoType(WorkOrderType.REWORK);
        wo.setPlanStart(req.planStart());
        wo.setPlanEnd(req.planEnd());
        wo.setWarehouseId(req.warehouseId());
        wo.setStatus(WorkOrderStatus.DRAFT);
        wo.setFinishedQty(BigDecimal.ZERO);
        wo.setSource(WorkOrderSource.REWORK);
        wo.setSourceWoId(sourceWoId);
        wo.setRemark(req.remark());
        wo.setDeptId(user.deptId());
        wo.setCreateBy(user.userId());
        workOrderMapper.insert(wo);
        MesWoMaterial self = new MesWoMaterial();
        self.setWoId(wo.getId());
        self.setMaterialId(req.materialId());
        self.setUnitQty(BigDecimal.ONE.setScale(6));
        self.setRequiredQty(req.qty());
        woMaterialMapper.insertBatch(List.of(self));
        insertOps(wo.getId(), bom);
        return wo.getId();
    }

    /** 工序的首件检验已确认且不是不合格 */
    private boolean firstArticlePassed(Long woOpId) {
        return inspectionMapper.exists(Wrappers.<MesInspection>lambdaQuery()
                .eq(MesInspection::getWoOpId, woOpId).eq(MesInspection::getType, QcType.FAI)
                .eq(MesInspection::getStatus, InspectionStatus.CONFIRMED).ne(MesInspection::getResult, QcResult.FAIL));
    }

    /** 已开工的数量：已完工与各工序已报工（合格 + 报废）的最大值 */
    private BigDecimal startedQty(MesWorkOrder wo) {
        BigDecimal max = wo.getFinishedQty();
        for (MesWoOp op : woOpMapper.selectByWo(wo.getId())) {
            max = max.max(op.getGoodQty().add(op.getScrapQty()));
        }
        return max;
    }

    public PageResult<MesReport> reports(long current, long size, Long woId, LocalDate beginDate, LocalDate endDate) {
        if (woId != null) {
            getAccessible(woId);
        }
        Page<MesReport> page = Page.of(current, size);
        reportMapper.selectPageList(page, woId, beginDate, endDate);
        return PageResult.of(page);
    }

    // ---------------------------------------------------------------- 供领料、完工入库使用

    MesWorkOrder getAccessible(Long id) {
        Supplier<MesWorkOrder> loader = () -> workOrderMapper.selectById(id);
        MesWorkOrder wo = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(WO_RULE, loader);
        if (wo == null) {
            throw new BizException("mes.error.workOrderDoesNotExistAccess");
        }
        return wo;
    }

    MesWorkOrder getExecutable(Long id) {
        MesWorkOrder wo = getAccessible(id);
        if (!wo.getStatus().isExecutable()) {
            throw new BizException("mes.error.workOrderNotReleasedProduction", wo.getWoNo());
        }
        return wo;
    }

    /** 首次领料、报工、完工入库后：已下达 → 生产中 */
    void markStarted(Long id) {
        workOrderMapper.update(Wrappers.<MesWorkOrder>lambdaUpdate()
                .set(MesWorkOrder::getStatus, WorkOrderStatus.STARTED)
                .eq(MesWorkOrder::getId, id)
                .eq(MesWorkOrder::getStatus, WorkOrderStatus.RELEASED));
    }

    /** 完工入库满数量：已完工；计划下全部工单的完工数量达到计划数量时，计划自动关闭（不再参与 MRP） */
    void markCompleted(Long id) {
        if (workOrderMapper.markCompleted(id) == 0) {
            return;
        }
        MesWorkOrder wo = workOrderMapper.selectById(id);
        if (wo.getPlanId() == null) {
            return;
        }
        MesPlan plan = planMapper.selectById(wo.getPlanId());
        if (plan != null && plan.getStatus() == PlanStatus.CONFIRMED
                && planMapper.selectFinishedQty(plan.getId()).compareTo(plan.getQty()) >= 0) {
            planMapper.update(Wrappers.<MesPlan>lambdaUpdate().set(MesPlan::getStatus, PlanStatus.CLOSED)
                    .set(MesPlan::getUpdateTime, LocalDateTime.now())
                    .eq(MesPlan::getId, plan.getId()).eq(MesPlan::getStatus, PlanStatus.CONFIRMED));
        }
    }

    // ---------------------------------------------------------------- 内部

    private MesWorkOrder getDraft(Long id) {
        MesWorkOrder wo = getAccessible(id);
        if (wo.getStatus() != WorkOrderStatus.DRAFT) {
            throw new BizException("mes.error.workOrderHasReleasedCannotModified", wo.getWoNo());
        }
        return wo;
    }

    private void transit(Long id, Set<WorkOrderStatus> from, WorkOrderStatus to, boolean release) {
        var update = Wrappers.<MesWorkOrder>lambdaUpdate()
                .set(MesWorkOrder::getStatus, to)
                .set(MesWorkOrder::getUpdateTime, LocalDateTime.now())
                .eq(MesWorkOrder::getId, id)
                .in(MesWorkOrder::getStatus, from);
        if (release) {
            update.set(MesWorkOrder::getReleaseTime, LocalDateTime.now());
        }
        if (to == WorkOrderStatus.CLOSED) {
            update.set(MesWorkOrder::getCloseTime, LocalDateTime.now());
        }
        if (workOrderMapper.update(update) == 0) {
            throw new BizException("mes.error.workOrderStatusHasChangedRefresh");
        }
    }

    /** 校验产品、BOM、仓库；不指定 BOM 时用默认 BOM */
    private MesBom resolveBom(WorkOrderRequest req) {
        WmsMaterial product = materialService.getEnabled(List.of(req.materialId())).get(req.materialId());
        if (product.isSerialManaged() && req.qty().stripTrailingZeros().scale() > 0) {
            throw new BizException("mes.error.productSerialNumberManagedQuantityMust");
        }
        if (req.planStart() != null && req.planEnd() != null && req.planEnd().isBefore(req.planStart())) {
            throw new BizException("mes.error.plannedFinishDateCannotEarlierStart");
        }
        if (req.warehouseId() != null) {
            WmsWarehouse warehouse = warehouseService.get(req.warehouseId());
            if (!Integer.valueOf(1).equals(warehouse.getStatus())) {
                throw new BizException("mes.error.warehouseDisabled", warehouse.getName());
            }
        }
        MesBom bom;
        if (req.bomId() != null) {
            bom = bomService.getBom(req.bomId());
            if (!bom.getMaterialId().equals(req.materialId())) {
                throw new BizException("mes.error.bomDoesNotBelongProduct");
            }
        } else {
            bom = bomService.defaultBoms(List.of(req.materialId())).get(req.materialId());
            if (bom == null) {
                throw new BizException("mes.error.productHasNoDefaultBom", product.getCode(), product.getName());
            }
        }
        if (!Integer.valueOf(1).equals(bom.getStatus())) {
            throw new BizException("mes.error.bomDisabled", bom.getVersion());
        }
        return bom;
    }

    /** 按 BOM 生成工单用料（虚拟件展开为它的子件，同一物料合并；序列号物料向上取整）和工序 */
    /**
     * 工程变更生效：使用原 BOM 的草稿工单改用新 BOM，按新 BOM 重新生成用料和工序（已下达的工单不变）
     *
     * @return 同步的工单数
     */
    @Transactional
    public int switchDraftBom(Long fromBomId, Long toBomId) {
        MesBom bom = bomService.getBom(toBomId);
        List<MesWorkOrder> drafts = workOrderMapper.selectList(Wrappers.<MesWorkOrder>lambdaQuery()
                .eq(MesWorkOrder::getBomId, fromBomId).eq(MesWorkOrder::getStatus, WorkOrderStatus.DRAFT));
        for (MesWorkOrder wo : drafts) {
            workOrderMapper.update(Wrappers.<MesWorkOrder>lambdaUpdate().set(MesWorkOrder::getBomId, toBomId)
                    .set(MesWorkOrder::getUpdateTime, LocalDateTime.now()).eq(MesWorkOrder::getId, wo.getId()));
            woMaterialMapper.delete(Wrappers.<MesWoMaterial>lambdaQuery().eq(MesWoMaterial::getWoId, wo.getId()));
            woOpMapper.delete(Wrappers.<MesWoOp>lambdaQuery().eq(MesWoOp::getWoId, wo.getId()));
            generateLines(wo.getId(), bom, wo.getQty());
        }
        return drafts.size();
    }

    private void generateLines(Long woId, MesBom bom, BigDecimal qty) {
        Map<Long, BigDecimal> unitQtys = new LinkedHashMap<>();
        expandPhantoms(bomService.items(bom.getId()), BigDecimal.ONE, unitQtys, 0);
        Map<Long, WmsMaterial> materials = materialService.mapByIds(unitQtys.keySet());
        woMaterialMapper.insertBatch(unitQtys.entrySet().stream().map(e -> {
            MesWoMaterial m = new MesWoMaterial();
            m.setWoId(woId);
            m.setMaterialId(e.getKey());
            m.setUnitQty(e.getValue().setScale(6, RoundingMode.HALF_UP));
            BigDecimal required = qty.multiply(m.getUnitQty());
            boolean whole = materials.get(e.getKey()).isSerialManaged();
            m.setRequiredQty(whole ? required.setScale(0, RoundingMode.CEILING) : required.setScale(4, RoundingMode.HALF_UP));
            return m;
        }).toList());
        insertOps(woId, bom);
    }

    private void insertOps(Long woId, MesBom bom) {
        List<MesWoOp> ops = bomService.ops(bom.getId()).stream().map(o -> {
            MesWoOp op = new MesWoOp();
            op.setWoId(woId);
            op.setSeq(o.getSeq());
            op.setName(o.getName());
            op.setWorkCenterId(o.getWorkCenterId());
            op.setStdHours(o.getStdHours());
            op.setPieceRate(o.getPieceRate());
            op.setFirstArticle(o.getFirstArticle());
            return op;
        }).toList();
        if (!ops.isEmpty()) {
            woOpMapper.insertBatch(ops);
        }
    }

    /** 子件为虚拟件（且有默认 BOM）时用它的子件代替，用量相乘 */
    private void expandPhantoms(List<MesBomItem> items, BigDecimal factor, Map<Long, BigDecimal> unitQtys, int depth) {
        if (depth > MAX_PHANTOM_DEPTH) {
            throw new BizException("mes.error.phantomItemsExceedLevelsMayCircular", MAX_PHANTOM_DEPTH);
        }
        List<Long> ids = items.stream().map(MesBomItem::getMaterialId).toList();
        Map<Long, MesMaterialPlan> plans = materialPlanService.plans(ids);
        List<Long> phantomIds = ids.stream()
                .filter(id -> plans.containsKey(id) && plans.get(id).getSupplyType() == SupplyType.PHANTOM).toList();
        Map<Long, MesBom> phantomBoms = bomService.defaultBoms(phantomIds);
        for (MesBomItem item : items) {
            BigDecimal unit = factor.multiply(unitQty(item));
            MesBom sub = phantomBoms.get(item.getMaterialId());
            if (sub != null) {
                expandPhantoms(bomService.items(sub.getId()), unit, unitQtys, depth + 1);
            } else {
                unitQtys.merge(item.getMaterialId(), unit, BigDecimal::add);
            }
        }
    }

    /** 含损耗的单位用量 = 用量 × (1 + 损耗率 / 100) */
    static BigDecimal unitQty(MesBomItem item) {
        return item.getQty().multiply(BigDecimal.ONE.add(item.getLossRate().divide(HUNDRED, 6, RoundingMode.HALF_UP)))
                .setScale(6, RoundingMode.HALF_UP);
    }
}
