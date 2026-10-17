package com.lemon.erp.modules.mes.service;

import com.lemon.erp.modules.mes.model.SupplementStatus;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.mes.dto.SupplementQuery;
import com.lemon.erp.modules.mes.dto.SupplementRequest;
import com.lemon.erp.modules.mes.dto.WoDocItemRequest;
import com.lemon.erp.modules.mes.dto.WoDocRequest;
import com.lemon.erp.modules.mes.entity.MesWoDoc;
import com.lemon.erp.modules.mes.entity.MesWoMaterial;
import com.lemon.erp.modules.mes.entity.MesWoSupplement;
import com.lemon.erp.modules.mes.entity.MesWoSupplementItem;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.mapper.MesWoMaterialMapper;
import com.lemon.erp.modules.mes.mapper.MesWoSupplementItemMapper;
import com.lemon.erp.modules.mes.mapper.MesWoSupplementMapper;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.mes.model.WorkOrderClosedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.mes.model.MesConstants.SUPPLEMENT_RULE;

/**
 * 补料申请：工单领料达到上限（需求 × (1 + 允许超领比例) + 已补料）后，超出部分由车间提交补料申请。
 * 流程中心发布了「mes_supplement」流程时走审批（变量 woNo、reason、totalQty、suppNo），否则提交即通过；
 * 通过后（待发料）由仓库按申请数量发料，生成「补料」工单单据和生产领料出库单，计入工单的已领和已补料。
 * 审批中、待发料的申请可以由申请人撤销
 */
@Service
@RequiredArgsConstructor
public class MesSupplementService {

    public static final String PROCESS_KEY = "mes_supplement";
    private static final String BUSINESS_KEY_PREFIX = "mes_supplement:";

    private final MesWoSupplementMapper suppMapper;
    private final MesWoSupplementItemMapper itemMapper;
    private final MesWoMaterialMapper woMaterialMapper;
    private final MesWorkOrderService workOrderService;
    private final MesWoDocService docService;
    private final DocNoService docNoService;
    private final FlowApi flowApi;
    private final PermissionService permissionService;

    @DataPermission(table = "mes_wo_supplement", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<MesWoSupplement> page(long current, long size, SupplementQuery query) {
        Page<MesWoSupplement> page = Page.of(current, size);
        suppMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public MesWoSupplement detail(Long id) {
        // 有发料权限的仓库人员需要查看申请明细
        if (!permissionService.hasPerm("mes:supplement:issue")) {
            getAccessible(id);
        }
        MesWoSupplement s = suppMapper.selectDetail(id);
        s.setItems(itemMapper.selectBySupp(id));
        return s;
    }

    /** 提交补料申请，返回申请 ID */
    @Transactional
    public Long create(SupplementRequest req) {
        MesWorkOrder wo = workOrderService.getExecutable(req.woId());
        Map<Long, MesWoMaterial> lines = woMaterialMapper.selectByWo(wo.getId()).stream()
                .collect(Collectors.toMap(MesWoMaterial::getMaterialId, Function.identity()));
        Set<Long> seen = new HashSet<>();
        for (SupplementRequest.Item item : req.items()) {
            MesWoMaterial line = lines.get(item.materialId());
            if (line == null) {
                throw new BizException("mes.error.supplementaryIssueMaterialsMustWorkOrder");
            }
            if (!seen.add(item.materialId())) {
                throw new BizException("mes.error.duplicateMaterial", line.getMaterialCode(), line.getMaterialName());
            }
            if (Integer.valueOf(1).equals(line.getSerialEnabled()) && item.qty().stripTrailingZeros().scale() > 0) {
                throw new BizException("mes.error.materialSerialManagedQuantityMustInteger", line.getMaterialCode(), line.getMaterialName());
            }
        }
        LoginUser user = SecurityUtils.getLoginUser();
        BigDecimal total = req.items().stream().map(SupplementRequest.Item::qty).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean flow = flowApi.isAvailable(PROCESS_KEY);
        MesWoSupplement s = new MesWoSupplement();
        s.setSuppNo(docNoService.next(MesDocNos.SUPPLEMENT));
        s.setWoId(wo.getId());
        s.setReason(req.reason());
        s.setTotalQty(total);
        s.setStatus(flow ? SupplementStatus.APPROVING : SupplementStatus.APPROVED);
        s.setApproveTime(flow ? null : LocalDateTime.now());
        s.setRemark(req.remark());
        s.setDeptId(user.deptId());
        s.setCreateBy(user.userId());
        suppMapper.insert(s);
        itemMapper.insertBatch(req.items().stream().map(i -> {
            MesWoSupplementItem item = new MesWoSupplementItem();
            item.setSuppId(s.getId());
            item.setMaterialId(i.materialId());
            item.setQty(i.qty());
            return item;
        }).toList());
        if (flow) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("woNo", wo.getWoNo());
            vars.put("reason", req.reason());
            vars.put("totalQty", total.doubleValue());
            vars.put("suppNo", s.getSuppNo());
            String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + s.getId(), vars);
            suppMapper.update(Wrappers.<MesWoSupplement>lambdaUpdate()
                    .set(MesWoSupplement::getFlowInstanceId, instanceId).eq(MesWoSupplement::getId, s.getId()));
        }
        return s.getId();
    }

    /** 申请人撤销：审批中或待发料 */
    @Transactional
    public void cancel(Long id) {
        MesWoSupplement s = getAccessible(id);
        if (!Objects.equals(s.getCreateBy(), SecurityUtils.getUserId())) {
            throw new BizException("mes.error.onlyCancelOwnRequests");
        }
        if (!s.getStatus().isActive()) {
            throw new BizException("mes.error.onlyRequestsUnderApprovalAwaitingIssue");
        }
        if (s.getStatus() == SupplementStatus.APPROVING && s.getFlowInstanceId() != null
                && flowApi.getStatus(s.getFlowInstanceId()) == FlowStatus.RUNNING) {
            flowApi.cancel(s.getFlowInstanceId(), "撤销补料申请"); // i18n-ignore
        }
        transit(id, SupplementStatus.ACTIVE, SupplementStatus.CANCELED);
    }

    /**
     * 发料：物料为申请中的物料，各物料合计不超过未发数量；生成补料单据和出库单。可以分次发料，全部发完后申请变为已发料
     *
     * @return 补料单号
     */
    @Transactional
    public String issue(Long id, WoDocRequest req) {
        // 发料由仓库人员操作，不按申请人部门做数据权限（接口已要求 mes:supplement:issue 权限）
        MesWoSupplement s = id == null ? null : suppMapper.selectById(id);
        if (s == null) {
            throw new BizException("mes.error.supplementaryIssueRequestDoesNotExist");
        }
        if (s.getStatus() != SupplementStatus.APPROVED) {
            throw new BizException("mes.error.onlyRequestsAwaitingIssueIssued");
        }
        Map<Long, MesWoSupplementItem> items = itemMapper.selectBySupp(id).stream()
                .collect(Collectors.toMap(MesWoSupplementItem::getMaterialId, Function.identity()));
        Map<Long, BigDecimal> qtyByMaterial = new LinkedHashMap<>();
        req.items().forEach(i -> qtyByMaterial.merge(i.materialId(), i.qty(), BigDecimal::add));
        for (Map.Entry<Long, BigDecimal> e : qtyByMaterial.entrySet()) {
            MesWoSupplementItem item = items.get(e.getKey());
            if (item == null) {
                throw new BizException("mes.error.issuedMaterialNotSupplementaryIssueRequest");
            }
            BigDecimal left = item.getQty().subtract(item.getIssuedQty());
            if (e.getValue().compareTo(left) > 0) {
                throw new BizException("mes.error.issuedQuantityMaterialExceedsUnissuedQuantity", item.getMaterialCode(), item.getMaterialName(), left.stripTrailingZeros().toPlainString());
            }
        }
        String remark = "补料申请 " + s.getSuppNo() + (req.remark() == null ? "" : " " + req.remark()); // i18n-ignore
        MesWoDoc doc = docService.issueSupplement(s.getWoId(), new WoDocRequest(req.warehouseId(), req.bizDate(),
                remark.length() > 255 ? remark.substring(0, 255) : remark,
                req.items().stream().map(i -> new WoDocItemRequest(i.materialId(), i.locationId(), i.batchNo(), null, null,
                        i.qty(), i.serialNos())).toList()));
        suppMapper.update(Wrappers.<MesWoSupplement>lambdaUpdate()
                .set(MesWoSupplement::getDocId, doc.getId())
                .set(MesWoSupplement::getDocNo, doc.getDocNo())
                .eq(MesWoSupplement::getId, id));
        qtyByMaterial.forEach((materialId, qty) -> {
            // 条件更新防止并发重复发料
            if (itemMapper.addIssued(items.get(materialId).getId(), qty) == 0) {
                throw new BizException("mes.error.supplementaryIssueRequestHasIssuedSomeone");
            }
        });
        boolean allIssued = itemMapper.selectBySupp(id).stream().allMatch(i -> i.getIssuedQty().compareTo(i.getQty()) >= 0);
        if (allIssued) {
            transit(id, Set.of(SupplementStatus.APPROVED), SupplementStatus.DONE);
        }
        return doc.getDocNo();
    }

    /** 审批流程结束：通过 → 待发料，拒绝 → 已拒绝，撤回 / 终止 → 已撤销；与审批操作同一事务 */
    @EventListener(condition = "#e.processKey == 'mes_supplement'")
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        Long id = Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length()));
        SupplementStatus status = switch (e.status()) {
            case APPROVED -> SupplementStatus.APPROVED;
            case REJECTED -> SupplementStatus.REJECTED;
            default -> SupplementStatus.CANCELED;
        };
        suppMapper.update(Wrappers.<MesWoSupplement>lambdaUpdate()
                .set(MesWoSupplement::getStatus, status)
                .set(MesWoSupplement::getApproveTime, LocalDateTime.now())
                .set(MesWoSupplement::getUpdateTime, LocalDateTime.now())
                .eq(MesWoSupplement::getId, id)
                .eq(MesWoSupplement::getStatus, SupplementStatus.APPROVING));
    }

    /** 工单关闭：审批中、待发料的补料申请自动撤销（撤销审批流程） */
    @EventListener
    public void onWorkOrderClosed(WorkOrderClosedEvent e) {
        for (MesWoSupplement s : suppMapper.selectList(Wrappers.<MesWoSupplement>lambdaQuery()
                .eq(MesWoSupplement::getWoId, e.woId()).in(MesWoSupplement::getStatus, SupplementStatus.ACTIVE))) {
            if (s.getFlowInstanceId() != null && s.getStatus() == SupplementStatus.APPROVING
                    && flowApi.getStatus(s.getFlowInstanceId()) == FlowStatus.RUNNING) {
                flowApi.cancel(s.getFlowInstanceId(), "工单已关闭"); // i18n-ignore
            }
            suppMapper.update(Wrappers.<MesWoSupplement>lambdaUpdate().set(MesWoSupplement::getStatus, SupplementStatus.CANCELED)
                    .set(MesWoSupplement::getUpdateTime, LocalDateTime.now())
                    .eq(MesWoSupplement::getId, s.getId()).in(MesWoSupplement::getStatus, SupplementStatus.ACTIVE));
        }
    }

    // ---------------------------------------------------------------- 内部

    private MesWoSupplement getAccessible(Long id) {
        Supplier<MesWoSupplement> loader = () -> id == null ? null : suppMapper.selectById(id);
        MesWoSupplement s = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(SUPPLEMENT_RULE, loader);
        if (s == null) {
            throw new BizException("mes.error.supplementaryIssueRequestDoesNotExist2");
        }
        return s;
    }

    private void transit(Long id, Set<SupplementStatus> from, SupplementStatus to) {
        int updated = suppMapper.update(Wrappers.<MesWoSupplement>lambdaUpdate()
                .set(MesWoSupplement::getStatus, to)
                .set(MesWoSupplement::getUpdateTime, LocalDateTime.now())
                .eq(MesWoSupplement::getId, id)
                .in(MesWoSupplement::getStatus, from));
        if (updated == 0) {
            throw new BizException("mes.error.requestStatusHasChangedRefresh");
        }
    }
}
