package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.modules.mes.dto.EcnDiff;
import com.lemon.erp.modules.mes.dto.PlmRequests;
import com.lemon.erp.modules.mes.entity.MesBom;
import com.lemon.erp.modules.mes.entity.MesBomItem;
import com.lemon.erp.modules.mes.entity.MesBomOp;
import com.lemon.erp.modules.mes.entity.MesEcn;
import com.lemon.erp.modules.mes.mapper.MesEcnMapper;
import com.lemon.erp.modules.mes.model.EcnStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工程变更（ECN）：复制基准 BOM 为新版本（停用）→ 在物料清单中修改新版本 → 提交（发布了 mes_ecn 时审批）→
 * 到生效日期生效：新版本启用并接替默认，原版本停用，可选同步草稿工单的用料和工序。
 * 生效日期在今天及以前的批准后立即生效，之后的由「工程变更生效」定时任务处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MesEcnService {

    /** 工程变更单号的编码规则标识 */
    public static final String DOC_NO = "ECN";
    public static final String PROCESS_KEY = "mes_ecn";
    private static final ApprovalFlow FLOW = new ApprovalFlow(PROCESS_KEY, PROCESS_KEY + ":");

    private final MesEcnMapper ecnMapper;
    private final MesBomService bomService;
    private final MesWorkOrderService workOrderService;
    private final DocNoService docNoService;
    private final ApprovalSupport approvalSupport;
    private final DictLookup dictLookup;
    private final TransactionTemplate transactionTemplate;

    public PageResult<MesEcn> page(long current, long size, EcnStatus status, String keyword) {
        Page<MesEcn> page = Page.of(current, size);
        ecnMapper.selectPageList(page, status, keyword);
        return PageResult.of(page);
    }

    public MesEcn detail(Long id) {
        MesEcn ecn = id == null ? null : ecnMapper.selectDetail(id);
        if (ecn == null) {
            throw new BizException("mes.error.ecnDoesNotExist");
        }
        return ecn;
    }

    /** 变更前后 BOM 的差异 */
    public EcnDiff diff(Long id) {
        MesEcn ecn = detail(id);
        MesBom from = bomService.detail(ecn.getFromBomId());
        MesBom to = bomService.detail(ecn.getToBomId());
        return new EcnDiff(diffItems(from.getItems(), to.getItems()), diffOps(from.getOps(), to.getOps()));
    }

    /** 新建：复制基准 BOM 为新版本 */
    @Transactional
    public Long create(PlmRequests.Ecn req) {
        if (req.fromBomId() == null) {
            throw new BizException("mes.error.selectBomChange");
        }
        if (!StringUtils.hasText(req.newVersion())) {
            throw new BizException("mes.error.enterNewVersion");
        }
        MesBom from = bomService.getBom(req.fromBomId());
        if (from.getEcnId() != null && ecnMapper.exists(Wrappers.<MesEcn>lambdaQuery().eq(MesEcn::getId, from.getEcnId())
                .ne(MesEcn::getStatus, EcnStatus.EFFECTIVE))) {
            throw new BizException("mes.error.versionStillEcnCannotUsedBase");
        }
        if (ecnMapper.exists(Wrappers.<MesEcn>lambdaQuery().eq(MesEcn::getFromBomId, from.getId())
                .in(MesEcn::getStatus, EcnStatus.DRAFT, EcnStatus.APPROVING, EcnStatus.APPROVED))) {
            throw new BizException("mes.error.bomAlreadyHasUnfinishedEcn");
        }
        LoginUser user = SecurityUtils.getLoginUser();
        MesEcn ecn = new MesEcn();
        ecn.setEcnNo(docNoService.next(DOC_NO));
        ecn.setMaterialId(from.getMaterialId());
        ecn.setFromBomId(from.getId());
        apply(ecn, req);
        ecn.setStatus(EcnStatus.DRAFT);
        ecn.setDeptId(user.deptId());
        ecn.setCreateBy(user.userId());
        ecnMapper.insert(ecn);
        Long toBomId = bomService.copyForEcn(from.getId(), req.newVersion(), ecn.getId());
        ecnMapper.update(Wrappers.<MesEcn>lambdaUpdate().set(MesEcn::getToBomId, toBomId).eq(MesEcn::getId, ecn.getId()));
        return ecn.getId();
    }

    @Transactional
    public void update(Long id, PlmRequests.Ecn req) {
        MesEcn ecn = requireStatus(id, EcnStatus.DRAFT, "mes.label.edit");
        apply(ecn, req);
        ecnMapper.updateById(ecn);
    }

    /** 删除草稿：变更后的 BOM 一并删除 */
    @Transactional
    public void delete(Long id) {
        MesEcn ecn = requireStatus(id, EcnStatus.DRAFT, "mes.label.delete");
        ecnMapper.deleteById(id);
        bomService.deleteForEcn(ecn.getToBomId());
    }

    /**
     * 提交：发布了审批流程时进入审批，否则直接批准（生效日期已到时立即生效）
     *
     * @return 是否进入审批
     */
    @Transactional
    public boolean submit(Long id) {
        MesEcn ecn = requireStatus(id, EcnStatus.DRAFT, "mes.label.submit");
        EcnDiff diff = diff(id);
        if (diff.items().isEmpty() && diff.ops().isEmpty()) {
            throw new BizException("mes.error.newBomIdenticalOriginalModifyVersion", ecn.getToVersion());
        }
        return approvalSupport.submit(FLOW, id, Map.of(
                        "reason", ecn.getReason(),
                        "materialCode", ecn.getMaterialCode(),
                        "ecnNo", ecn.getEcnNo()),
                () -> approve(id, EcnStatus.DRAFT),
                () -> transit(id, EcnStatus.DRAFT, EcnStatus.APPROVING),
                instanceId -> ecnMapper.update(Wrappers.<MesEcn>lambdaUpdate().set(MesEcn::getFlowInstanceId, instanceId).eq(MesEcn::getId, id)));
    }

    /** 审批结束：通过 → 批准（到期立即生效）；拒绝、撤回 → 草稿 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        FLOW.onFinished(e, (id, approved) -> {
            MesEcn ecn = ecnMapper.selectById(id);
            if (ecn == null || ecn.getStatus() != EcnStatus.APPROVING) {
                return;
            }
            if (approved) {
                approve(id, EcnStatus.APPROVING);
            } else {
                transit(id, EcnStatus.APPROVING, EcnStatus.DRAFT);
            }
        });
    }

    /** 取消：草稿、待生效的可以取消，变更后的 BOM（没有被工单使用时）删除 */
    @Transactional
    public void cancel(Long id) {
        MesEcn ecn = detail(id);
        if (ecn.getStatus() != EcnStatus.DRAFT && ecn.getStatus() != EcnStatus.APPROVED) {
            throw new BizException("mes.error.currentEcnStatusDoesNotAllow");
        }
        transit(id, ecn.getStatus(), EcnStatus.CANCELLED);
        bomService.deleteForEcn(ecn.getToBomId());
    }

    /** 立即生效（待生效的，不等生效日期） */
    @Transactional
    public void effectNow(Long id) {
        MesEcn ecn = requireStatus(id, EcnStatus.APPROVED, "mes.label.takeEffect");
        effect(ecn);
    }

    /**
     * 定时任务：到了生效日期的待生效变更。每张变更单独一个事务，一张失败（如状态已变化、BOM 不可用）不影响其他
     *
     * @return 生效的数量和失败的变更（单号：原因）
     */
    public DueResult effectDue(LocalDate today) {
        int effected = 0;
        List<String> failures = new ArrayList<>();
        for (MesEcn ecn : ecnMapper.selectDue(today)) {
            try {
                transactionTemplate.executeWithoutResult(s -> effect(ecn));
                effected++;
            } catch (RuntimeException e) {
                log.warn("工程变更 {} 到期生效失败", ecn.getEcnNo(), e);
                failures.add(ecn.getEcnNo() + "：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        }
        return new DueResult(effected, failures);
    }

    public record DueResult(int effected, List<String> failures) {
    }

    // ---------------------------------------------------------------- 内部

    private void approve(Long id, EcnStatus from) {
        if (!StatusTransitions.tryChange(ecnMapper, MesEcn::getId, id, MesEcn::getStatus, from, EcnStatus.APPROVED,
                w -> w.set(MesEcn::getApproveTime, LocalDateTime.now()))) {
            throw new BizException("mes.error.ecnStatusHasChangedRefreshTry");
        }
        MesEcn ecn = ecnMapper.selectById(id);
        if (!ecn.getEffectiveDate().isAfter(LocalDate.now())) {
            effect(ecn);
        }
    }

    private void effect(MesEcn ecn) {
        bomService.activateForEcn(ecn.getFromBomId(), ecn.getToBomId());
        int synced = Integer.valueOf(1).equals(ecn.getSyncDraftWo()) ? workOrderService.switchDraftBom(ecn.getFromBomId(), ecn.getToBomId()) : 0;
        String note = "新版本已启用" + (synced > 0 ? "，同步草稿工单 " + synced + " 张" : ""); // i18n-ignore
        if (!StatusTransitions.tryChange(ecnMapper, MesEcn::getId, ecn.getId(), MesEcn::getStatus, EcnStatus.APPROVED, EcnStatus.EFFECTIVE,
                w -> w.set(MesEcn::getEffectTime, LocalDateTime.now()).set(MesEcn::getResultNote, note))) {
            throw new BizException("mes.error.ecnStatusHasChangedRefreshTry");
        }
    }

    private void apply(MesEcn ecn, PlmRequests.Ecn req) {
        dictLookup.check("mes_ecn_reason", req.reason(), "mes.label.changeReason");
        ecn.setTitle(req.title().trim());
        ecn.setReason(req.reason());
        ecn.setDescription(req.description());
        ecn.setEffectiveDate(req.effectiveDate());
        ecn.setSyncDraftWo(Boolean.FALSE.equals(req.syncDraftWo()) ? 0 : 1);
    }

    /** 状态变更（校验原状态），同时更新修改时间 */
    private void transit(Long id, EcnStatus from, EcnStatus to) {
        if (!StatusTransitions.tryChange(ecnMapper, MesEcn::getId, id, MesEcn::getStatus, from, to,
                w -> w.set(MesEcn::getUpdateTime, LocalDateTime.now()))) {
            throw new BizException("mes.error.ecnStatusHasChangedRefreshTry");
        }
    }

    private MesEcn requireStatus(Long id, EcnStatus status, String action) {
        MesEcn ecn = detail(id);
        if (ecn.getStatus() != status) {
            throw new BizException("mes.error.currentEcnStatusDoesNotAllow2", Messages.get(action));
        }
        return ecn;
    }

    static List<EcnDiff.ItemDiff> diffItems(List<MesBomItem> before, List<MesBomItem> after) {
        Map<Long, MesBomItem> old = before.stream().collect(Collectors.toMap(MesBomItem::getMaterialId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, MesBomItem> now = after.stream().collect(Collectors.toMap(MesBomItem::getMaterialId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<EcnDiff.ItemDiff> diffs = new ArrayList<>();
        for (MesBomItem o : old.values()) {
            MesBomItem n = now.get(o.getMaterialId());
            if (n == null) {
                diffs.add(itemDiff(EcnDiff.Action.REMOVED, o, null));
            } else if (!same(o.getQty(), n.getQty()) || !same(o.getLossRate(), n.getLossRate())
                    || !Objects.equals(blank(o.getRefDes()), blank(n.getRefDes()))) {
                diffs.add(itemDiff(EcnDiff.Action.CHANGED, o, n));
            }
        }
        now.values().stream().filter(n -> !old.containsKey(n.getMaterialId())).forEach(n -> diffs.add(itemDiff(EcnDiff.Action.ADDED, null, n)));
        return diffs;
    }

    static List<EcnDiff.OpDiff> diffOps(List<MesBomOp> before, List<MesBomOp> after) {
        Map<Integer, MesBomOp> old = before.stream().collect(Collectors.toMap(MesBomOp::getSeq, Function.identity(), (a, b) -> a));
        Map<Integer, MesBomOp> now = after.stream().collect(Collectors.toMap(MesBomOp::getSeq, Function.identity(), (a, b) -> a));
        List<EcnDiff.OpDiff> diffs = new ArrayList<>();
        TreeSet<Integer> seqs = new TreeSet<>(old.keySet());
        seqs.addAll(now.keySet());
        for (Integer seq : seqs) {
            MesBomOp o = old.get(seq);
            MesBomOp n = now.get(seq);
            if (o == null) {
                diffs.add(opDiff(EcnDiff.Action.ADDED, seq, null, n));
            } else if (n == null) {
                diffs.add(opDiff(EcnDiff.Action.REMOVED, seq, o, null));
            } else if (!Objects.equals(o.getName(), n.getName()) || !Objects.equals(o.getWorkCenterId(), n.getWorkCenterId())
                    || !same(o.getStdHours(), n.getStdHours()) || !same(o.getPieceRate(), n.getPieceRate())
                    || !Objects.equals(o.getFirstArticle(), n.getFirstArticle())) {
                diffs.add(opDiff(EcnDiff.Action.CHANGED, seq, o, n));
            }
        }
        return diffs;
    }

    private static EcnDiff.ItemDiff itemDiff(EcnDiff.Action action, MesBomItem o, MesBomItem n) {
        MesBomItem any = n != null ? n : o;
        return new EcnDiff.ItemDiff(action, any.getMaterialId(), any.getMaterialCode(), any.getMaterialName(), any.getUnit(),
                o == null ? null : o.getQty(), n == null ? null : n.getQty(), o == null ? null : o.getLossRate(),
                n == null ? null : n.getLossRate(), o == null ? null : o.getRefDes(), n == null ? null : n.getRefDes());
    }

    private static EcnDiff.OpDiff opDiff(EcnDiff.Action action, Integer seq, MesBomOp o, MesBomOp n) {
        return new EcnDiff.OpDiff(action, seq, o == null ? null : o.getName(), n == null ? null : n.getName(),
                o == null ? null : o.getWorkCenterName(), n == null ? null : n.getWorkCenterName(),
                o == null ? null : o.getStdHours(), n == null ? null : n.getStdHours(),
                o == null ? null : o.getPieceRate(), n == null ? null : n.getPieceRate(),
                o == null ? null : o.getFirstArticle(), n == null ? null : n.getFirstArticle());
    }

    private static boolean same(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    private static String blank(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
