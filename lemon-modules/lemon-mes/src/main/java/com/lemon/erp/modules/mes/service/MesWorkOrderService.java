package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.modules.mes.dto.WorkOrderQuery;
import com.lemon.erp.modules.mes.dto.WorkOrderRequest;
import com.lemon.erp.modules.mes.entity.MesBom;
import com.lemon.erp.modules.mes.entity.MesPlan;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.mapper.MesPlanMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.mes.model.PlanStatus;
import com.lemon.erp.modules.mes.model.WorkOrderClosedEvent;
import com.lemon.erp.modules.mes.model.WorkOrderSource;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.mes.model.WorkOrderType;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.lemon.erp.modules.mes.model.MesConstants.WO_RULE;

/**
 * 生产工单：草稿 → 下达 → 生产中 → 已完工，未完工的可以关闭。
 * 保存时按 BOM 生成用料（需求数量 = 工单数量 × 用量 × (1 + 损耗率)，虚拟件展开为子件）和工序（工艺路线），之后修改 BOM 不影响已有工单；
 * 领料、退料、完工入库见 {@link MesWoDocService}，报工见 {@link WoReportService}，改量、拆分见 {@link WoChangeService}，
 * 用料和工序行的生成见 {@link WoLineService}
 */
@Service
@RequiredArgsConstructor
public class MesWorkOrderService {

    private final MesWorkOrderMapper workOrderMapper;
    private final MesPlanMapper planMapper;
    private final WoLineService lineService;
    private final MesBomService bomService;
    private final WmsMaterialService materialService;
    private final WmsWarehouseService warehouseService;
    private final DocNoService docNoService;
    private final ApplicationEventPublisher eventPublisher;

    @DataPermission(table = "mes_work_order", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<MesWorkOrder> page(long current, long size, WorkOrderQuery query) {
        Page<MesWorkOrder> page = Page.of(current, size);
        workOrderMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public MesWorkOrder detail(Long id) {
        getAccessible(id);
        MesWorkOrder wo = workOrderMapper.selectDetail(id);
        wo.setMaterials(lineService.materials(id));
        wo.setOps(lineService.ops(id));
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
        lineService.generate(wo.getId(), bom, req.qty());
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
        lineService.delete(List.of(id));
        lineService.generate(id, bom, req.qty());
    }

    @Transactional
    public void delete(List<Long> ids) {
        ids.forEach(this::getDraft);
        lineService.delete(ids);
        workOrderMapper.deleteByIds(ids);
    }

    /** 下达：车间可以开始领料、报工 */
    public void release(Long id) {
        getDraft(id);
        transit(id, Set.of(WorkOrderStatus.DRAFT), WorkOrderStatus.RELEASED, true);
    }

    /** 关闭：未完工的工单不再生产，已领未用的物料请先退料 */
    @Transactional
    public void close(Long id) {
        MesWorkOrder wo = getAccessible(id);
        if (!wo.getStatus().isExecutable()) {
            throw new BizException("mes.error.onlyReleasedProductionWorkOrdersClosed");
        }
        transit(id, WorkOrderStatus.EXECUTABLE, WorkOrderStatus.CLOSED, false);
        eventPublisher.publishEvent(new WorkOrderClosedEvent(id));
    }

    // ---------------------------------------------------------------- 返工

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
        lineService.generateRework(wo.getId(), req.materialId(), req.qty(), bom);
        return wo.getId();
    }

    // ---------------------------------------------------------------- 供领料、完工入库使用

    MesWorkOrder getAccessible(Long id) {
        return DataScopes.require(WO_RULE, () -> workOrderMapper.selectById(id), "mes.error.workOrderDoesNotExistAccess");
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
            if (warehouse.getStatus() != EnableStatus.ENABLED) {
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
        if (bom.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("mes.error.bomDisabled", bom.getVersion());
        }
        return bom;
    }

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
            lineService.delete(List.of(wo.getId()));
            lineService.generate(wo.getId(), bom, wo.getQty());
        }
        return drafts.size();
    }
}
