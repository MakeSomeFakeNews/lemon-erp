package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.fin.dto.VoucherEntryRequest;
import com.lemon.erp.modules.fin.dto.VoucherQuery;
import com.lemon.erp.modules.fin.dto.VoucherRequest;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinCostRun;
import com.lemon.erp.modules.fin.entity.FinDepreciation;
import com.lemon.erp.modules.fin.entity.FinSettlement;
import com.lemon.erp.modules.fin.entity.FinVoucher;
import com.lemon.erp.modules.fin.entity.FinVoucherEntry;
import com.lemon.erp.modules.fin.event.VoucherDeletedEvent;
import com.lemon.erp.modules.fin.mapper.FinBillMapper;
import com.lemon.erp.modules.fin.mapper.FinCostRunMapper;
import com.lemon.erp.modules.fin.mapper.FinDepreciationMapper;
import com.lemon.erp.modules.fin.mapper.FinSettlementMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherEntryMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherMapper;
import com.lemon.erp.modules.fin.model.AuxItem;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.fin.model.VoucherStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.lemon.erp.modules.fin.model.FinConstants.periodOf;

/**
 * 记账凭证：未审核 → 已审核 → 已记账，只有已记账的凭证进入账簿和报表；所在期间结账后不能再做任何修改。
 * 凭证号按期间连续编号（记-0001），由会计期间的流水分配。
 * 业务生成的凭证（应收应付、收付款、折旧、成本结转）删除后，来源单据可以重新生成
 */
@Service
@RequiredArgsConstructor
public class FinVoucherService {

    /** 记录变更历史的字段 */
    private static final Map<String, String> CHANGE_FIELDS = ChangeLogger.fields("voucherDate", "凭证日期", "voucherNo", "凭证号", "summary", "摘要", "totalDebit", "借方合计", "attachmentCount", "附单据数");

    /** 凭证审批流程标识 */
    public static final String PROCESS_KEY = "fin_voucher";
    private static final String BUSINESS_KEY_PREFIX = "fin_voucher:";

    private static final String WORD = "记";

    private final FinVoucherMapper voucherMapper;
    private final FinVoucherEntryMapper entryMapper;
    private final FinPeriodService periodService;
    private final FinAccountService accountService;
    private final FinBillMapper billMapper;
    private final FinSettlementMapper settlementMapper;
    private final FinDepreciationMapper depreciationMapper;
    private final FinCostRunMapper costRunMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final FlowApi flowApi;
    private final ChangeLogger changeLogger;
    private final BizRules bizRules;

    public PageResult<FinVoucher> page(long current, long size, VoucherQuery query) {
        Page<FinVoucher> page = Page.of(current, size);
        voucherMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public FinVoucher detail(Long id) {
        FinVoucher voucher = voucherMapper.selectDetail(id);
        if (voucher == null) {
            throw new BizException("凭证不存在");
        }
        voucher.setEntries(entryMapper.selectByVoucherId(id));
        return voucher;
    }

    /** @return 新凭证 ID */
    @Transactional
    public Long create(VoucherRequest req) {
        List<VoucherLine> lines = toLines(req.entries());
        checkAux(lines);
        return insert(req.voucherDate(), req.attachmentCount(), lines, VoucherSource.MANUAL, null, VoucherStatus.DRAFT);
    }

    /** 修改未审核的凭证；改到其他期间时重新分配凭证号 */
    @Transactional
    public void update(Long id, VoucherRequest req) {
        Object before = voucherMapper.selectById(id);
        FinVoucher old = getWithStatus(id, VoucherStatus.DRAFT);
        if (!VoucherSource.MANUAL.name().equals(old.getSourceType())) {
            throw new BizException("业务生成的凭证不能修改，可以删除后由业务单据重新生成");
        }
        List<VoucherLine> lines = toLines(req.entries());
        checkAux(lines);
        List<FinVoucherEntry> entries = validate(lines);
        int period = periodOf(req.voucherDate());
        LambdaUpdateWrapper<FinVoucher> update = Wrappers.<FinVoucher>lambdaUpdate()
                .set(FinVoucher::getVoucherDate, req.voucherDate())
                .set(FinVoucher::getAttachmentCount, req.attachmentCount() == null ? 0 : req.attachmentCount())
                .set(FinVoucher::getSummary, lines.get(0).summary())
                .set(FinVoucher::getTotalDebit, total(entries, true))
                .set(FinVoucher::getTotalCredit, total(entries, false))
                .set(FinVoucher::getUpdateTime, LocalDateTime.now())
                .eq(FinVoucher::getId, id)
                .eq(FinVoucher::getStatus, VoucherStatus.DRAFT.name());
        periodService.checkOpen(old.getPeriod());
        if (period != old.getPeriod()) {
            int seq = periodService.nextVoucherSeq(period);
            update.set(FinVoucher::getPeriod, period).set(FinVoucher::getVoucherSeq, seq).set(FinVoucher::getVoucherNo, voucherNo(seq));
        }
        voucherMapper.update(update);
        entryMapper.delete(Wrappers.<FinVoucherEntry>lambdaQuery().eq(FinVoucherEntry::getVoucherId, id));
        saveEntries(id, entries);
        changeLogger.log("fin_voucher", id, before, voucherMapper.selectById(id), CHANGE_FIELDS);
    }

    /** 删除未审核的凭证，解除来源单据的关联 */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            FinVoucher v = getWithStatus(id, VoucherStatus.DRAFT);
            periodService.checkOpen(v.getPeriod());
            if (UNDELETABLE_SOURCES.contains(v.getSourceType())) {
                throw new BizException("凭证 " + v.getVoucherNo() + " 由" + sourceLabel(v.getSourceType()) + "（" + v.getSourceNo()
                        + "）生成，不能删除，请在业务单据中撤销或做红字冲销");
            }
        }
        // 删除红字冲销凭证后，原凭证可以重新冲销
        List<Long> originals = voucherMapper.selectByIds(ids).stream().map(FinVoucher::getReversalOf).filter(java.util.Objects::nonNull).toList();
        if (!originals.isEmpty()) {
            voucherMapper.update(Wrappers.<FinVoucher>lambdaUpdate().set(FinVoucher::getReversed, 0).in(FinVoucher::getId, originals));
        }
        entryMapper.delete(Wrappers.<FinVoucherEntry>lambdaQuery().in(FinVoucherEntry::getVoucherId, ids));
        voucherMapper.deleteByIds(ids);
        billMapper.update(Wrappers.<FinBill>lambdaUpdate().set(FinBill::getVoucherId, null).in(FinBill::getVoucherId, ids));
        settlementMapper.update(Wrappers.<FinSettlement>lambdaUpdate().set(FinSettlement::getVoucherId, null).in(FinSettlement::getVoucherId, ids));
        depreciationMapper.update(Wrappers.<FinDepreciation>lambdaUpdate().set(FinDepreciation::getVoucherId, null).in(FinDepreciation::getVoucherId, ids));
        costRunMapper.update(Wrappers.<FinCostRun>lambdaUpdate().set(FinCostRun::getVoucherId, null).in(FinCostRun::getVoucherId, ids));
        eventPublisher.publishEvent(new VoucherDeletedEvent(ids));
    }

    /** 业务单据撤销时删除其生成的凭证（只能是未审核的），不受来源限制 */
    @Transactional
    public void deleteOwned(Long id) {
        FinVoucher v = voucherMapper.selectById(id);
        if (v == null) {
            return;
        }
        if (!VoucherStatus.DRAFT.name().equals(v.getStatus())) {
            throw new BizException("凭证 " + v.getVoucherNo() + " 已审核，请先反审核或做红字冲销");
        }
        periodService.checkOpen(v.getPeriod());
        entryMapper.delete(Wrappers.<FinVoucherEntry>lambdaQuery().eq(FinVoucherEntry::getVoucherId, id));
        voucherMapper.deleteById(id);
    }

    /** 预收预付、坏账、报销、借款生成的凭证与业务单据的金额联动，不能直接删除 */
    private static final java.util.Set<String> UNDELETABLE_SOURCES = java.util.Set.of(VoucherSource.ADVANCE.name(),
            VoucherSource.BAD_DEBT.name(), VoucherSource.CLAIM.name(), VoucherSource.LOAN.name(),
            VoucherSource.NOTE.name(), VoucherSource.TRANSFER.name());

    private static String sourceLabel(String source) {
        return switch (source) {
            case "ADVANCE" -> "预收预付";
            case "BAD_DEBT" -> "坏账核销";
            case "CLAIM" -> "费用报销";
            case "LOAN" -> "员工借款";
            case "NOTE" -> "票据";
            case "TRANSFER" -> "账户调拨";
            default -> source;
        };
    }

    /**
     * 红字冲销：为已记账的凭证生成一张红字凭证（同科目、同方向、金额为负数，未审核），原凭证标记为已冲销
     *
     * @return 红字凭证 ID
     */
    @Transactional
    public Long reverse(Long id, LocalDate date) {
        FinVoucher v = getWithStatus(id, VoucherStatus.POSTED, "只能冲销已记账的凭证");
        if (VoucherSource.CLOSE.name().equals(v.getSourceType())) {
            throw new BizException("结转损益凭证随结账生成，请反结账");
        }
        if (v.getReversalOf() != null) {
            throw new BizException("红字凭证不能再冲销");
        }
        LocalDate d = date == null ? LocalDate.now() : date;
        periodService.checkOpen(periodOf(d));
        if (voucherMapper.update(Wrappers.<FinVoucher>lambdaUpdate().set(FinVoucher::getReversed, 1)
                .eq(FinVoucher::getId, id).eq(FinVoucher::getReversed, 0)) == 0) {
            throw new BizException("凭证 " + v.getVoucherNo() + " 已冲销");
        }
        List<VoucherLine> lines = entryMapper.selectList(Wrappers.<FinVoucherEntry>lambdaQuery().eq(FinVoucherEntry::getVoucherId, id)
                        .orderByAsc(FinVoucherEntry::getSeq)).stream()
                .map(e -> new VoucherLine("冲销 " + v.getPeriod() + " " + v.getVoucherNo() + " " + e.getSummary(), e.getAccountId(), e.getDeptId(),
                        e.getDebit().negate(), e.getCredit().negate(), e.getProjectId(), e.getCustomerId(), e.getSupplierId(), e.getMaterialId()))
                .toList();
        Long redId = insert(d, 0, lines, VoucherSource.REVERSAL, v.getVoucherNo(), VoucherStatus.DRAFT);
        voucherMapper.update(Wrappers.<FinVoucher>lambdaUpdate().set(FinVoucher::getReversalOf, id).eq(FinVoucher::getId, redId));
        return redId;
    }

    /** 审核：未审核 → 已审核。发布了凭证审批流程时不能直接审核，由制单人提交审批 */
    @Transactional
    public void audit(List<Long> ids) {
        if (flowApi.isAvailable(PROCESS_KEY)) {
            throw new BizException("已启用凭证审批流程，请提交审批，审批通过后自动审核");
        }
        transit(ids, VoucherStatus.DRAFT, VoucherStatus.AUDITED, "只能审核未审核的凭证");
    }

    /** 提交审批（发布了凭证审批流程时）：未审核 → 审批中，每张凭证一个流程 */
    @Transactional
    public void submit(List<Long> ids) {
        if (!flowApi.isAvailable(PROCESS_KEY)) {
            throw new BizException("没有发布凭证审批流程，请直接审核");
        }
        for (Long id : ids) {
            FinVoucher v = getWithStatus(id, VoucherStatus.DRAFT, "只能提交未审核的凭证");
            periodService.checkOpen(v.getPeriod());
            int updated = voucherMapper.update(Wrappers.<FinVoucher>lambdaUpdate()
                    .set(FinVoucher::getStatus, VoucherStatus.APPROVING.name())
                    .eq(FinVoucher::getId, id).eq(FinVoucher::getStatus, VoucherStatus.DRAFT.name()));
            if (updated == 0) {
                throw new BizException("凭证 " + v.getVoucherNo() + " 状态已变化，请刷新");
            }
            String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + id, Map.of(
                    "amount", v.getTotalDebit() == null ? 0 : v.getTotalDebit().doubleValue(),
                    "voucherNo", v.getVoucherNo(),
                    "sourceType", v.getSourceType() == null ? "" : v.getSourceType()));
            voucherMapper.update(Wrappers.<FinVoucher>lambdaUpdate().set(FinVoucher::getFlowInstanceId, instanceId).eq(FinVoucher::getId, id));
        }
    }

    /** 凭证审批结束：通过 → 已审核（审核人为最后审批人）；拒绝、撤销 → 未审核 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        Long id = Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length()));
        boolean approved = e.status() == FlowStatus.APPROVED;
        voucherMapper.update(Wrappers.<FinVoucher>lambdaUpdate()
                .set(FinVoucher::getStatus, approved ? VoucherStatus.AUDITED.name() : VoucherStatus.DRAFT.name())
                .set(approved, FinVoucher::getAuditBy, SecurityUtils.getUserIdOrNull())
                .set(approved, FinVoucher::getAuditTime, LocalDateTime.now())
                .eq(FinVoucher::getId, id)
                .eq(FinVoucher::getStatus, VoucherStatus.APPROVING.name()));
    }

    @Transactional
    public void unaudit(List<Long> ids) {
        transit(ids, VoucherStatus.AUDITED, VoucherStatus.DRAFT, "只能反审核已审核、未记账的凭证");
    }

    /** 记账：已审核 → 已记账 */
    @Transactional
    public void post(List<Long> ids) {
        transit(ids, VoucherStatus.AUDITED, VoucherStatus.POSTED, "只能记账已审核的凭证");
    }

    @Transactional
    public void unpost(List<Long> ids) {
        for (Long id : ids) {
            FinVoucher v = voucherMapper.selectById(id);
            if (v != null && VoucherSource.CLOSE.name().equals(v.getSourceType())) {
                throw new BizException("结转损益凭证由结账自动生成，请反结账");
            }
        }
        transit(ids, VoucherStatus.POSTED, VoucherStatus.AUDITED, "只能反记账已记账的凭证");
    }

    // ---------------------------------------------------------------- 供业务生成凭证使用

    /**
     * 业务生成凭证（未审核），合并同科目、同部门、同方向的分录
     *
     * @return 凭证 ID
     */
    @Transactional
    public Long createGenerated(LocalDate date, VoucherSource source, String sourceNo, List<VoucherLine> lines) {
        return insert(date, 0, merge(lines), source, sourceNo, VoucherStatus.DRAFT);
    }

    /** 生成并直接记账（结账时的结转损益凭证） */
    @Transactional
    public Long createPosted(LocalDate date, VoucherSource source, String sourceNo, List<VoucherLine> lines) {
        return insert(date, 0, merge(lines), source, sourceNo, VoucherStatus.POSTED);
    }

    String voucherNoOf(Long id) {
        FinVoucher v = id == null ? null : voucherMapper.selectById(id);
        return v == null ? null : v.getVoucherNo();
    }

    // ---------------------------------------------------------------- 内部

    private Long insert(LocalDate date, Integer attachments, List<VoucherLine> lines, VoucherSource source, String sourceNo,
                        VoucherStatus status) {
        List<FinVoucherEntry> entries = validate(lines);
        int period = periodOf(date);
        int seq = periodService.nextVoucherSeq(period);
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        FinVoucher v = new FinVoucher();
        v.setPeriod(period);
        v.setVoucherSeq(seq);
        v.setVoucherNo(voucherNo(seq));
        v.setVoucherDate(date);
        v.setSummary(lines.get(0).summary());
        v.setTotalDebit(total(entries, true));
        v.setTotalCredit(total(entries, false));
        v.setAttachmentCount(attachments == null ? 0 : attachments);
        v.setStatus(status.name());
        v.setSourceType(source.name());
        v.setSourceNo(sourceNo);
        v.setReversed(0);
        v.setCreateBy(userId);
        if (status == VoucherStatus.POSTED) {
            v.setAuditBy(userId);
            v.setAuditTime(now);
            v.setPostBy(userId);
            v.setPostTime(now);
        }
        voucherMapper.insert(v);
        saveEntries(v.getId(), entries);
        return v.getId();
    }

    /** 每条分录只填借方或贷方、科目为启用的末级科目、借贷平衡且合计大于 0 */
    private List<FinVoucherEntry> validate(List<VoucherLine> lines) {
        if (lines.size() < 2) {
            throw new BizException("凭证至少需要两条分录");
        }
        accountService.requireLeaves(lines.stream().map(VoucherLine::accountId).toList());
        List<FinVoucherEntry> entries = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            VoucherLine l = lines.get(i);
            BigDecimal debit = l.debit() == null ? BigDecimal.ZERO : l.debit();
            BigDecimal credit = l.credit() == null ? BigDecimal.ZERO : l.credit();
            if ((debit.signum() == 0) == (credit.signum() == 0)) {
                throw new BizException("第 " + (i + 1) + " 条分录：借方、贷方金额必须且只能填一个");
            }
            FinVoucherEntry e = new FinVoucherEntry();
            e.setSeq(i + 1);
            e.setSummary(l.summary());
            e.setAccountId(l.accountId());
            e.setDeptId(l.deptId());
            e.setProjectId(l.projectId());
            e.setCustomerId(l.customerId());
            e.setSupplierId(l.supplierId());
            e.setMaterialId(l.materialId());
            e.setDebit(debit.setScale(2, java.math.RoundingMode.HALF_UP));
            e.setCredit(credit.setScale(2, java.math.RoundingMode.HALF_UP));
            entries.add(e);
        }
        BigDecimal debit = total(entries, true);
        BigDecimal credit = total(entries, false);
        if (debit.compareTo(credit) != 0) {
            throw new BizException("借贷不平衡：借方合计 " + debit.toPlainString() + "，贷方合计 " + credit.toPlainString());
        }
        return entries;
    }

    /**
     * 手工凭证：科目上设置的辅助核算必填（业务生成的凭证按业务单据带出辅助核算，不校验）。
     * 科目没有设置的辅助核算也可以填写
     */
    private void checkAux(List<VoucherLine> lines) {
        Map<Long, FinAccount> accounts = accountService.requireLeaves(lines.stream().map(VoucherLine::accountId).toList());
        for (int i = 0; i < lines.size(); i++) {
            VoucherLine l = lines.get(i);
            FinAccount a = accounts.get(l.accountId());
            for (AuxItem item : AuxItem.parse(a.getAuxItems())) {
                if (!l.has(item)) {
                    throw new BizException("第 " + (i + 1) + " 条分录：科目「" + a.getCode() + " " + a.getName() + "」需要填写" + item.label());
                }
            }
        }
    }

    /** 合并同摘要、同科目、同辅助核算、同方向的分录，去掉金额为 0 的 */
    private static List<VoucherLine> merge(List<VoucherLine> lines) {
        List<VoucherLine> merged = new ArrayList<>();
        for (VoucherLine l : lines) {
            if (l.isZero()) {
                continue;
            }
            boolean debit = l.debit().signum() > 0;
            int idx = -1;
            for (int i = 0; i < merged.size(); i++) {
                VoucherLine m = merged.get(i);
                if (m.summary().equals(l.summary()) && m.accountId().equals(l.accountId()) && m.sameAux(l)
                        && (m.debit().signum() > 0) == debit) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                merged.add(l);
            } else {
                VoucherLine m = merged.get(idx);
                merged.set(idx, m.withAmounts(m.debit().add(l.debit()), m.credit().add(l.credit())));
            }
        }
        return merged;
    }

    private void transit(List<Long> ids, VoucherStatus from, VoucherStatus to, String error) {
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        for (Long id : ids) {
            FinVoucher v = getWithStatus(id, from, error);
            periodService.checkOpen(v.getPeriod());
            // 不相容职务分离：制单人不能审核自己的凭证（超级管理员除外，便于单人体验）
            if (to == VoucherStatus.AUDITED && from == VoucherStatus.DRAFT && Objects.equals(v.getCreateBy(), userId)
                    && !SecurityUtils.getLoginUser().isAdmin() && !bizRules.bool(FinBizRules.VOUCHER_SELF_AUDIT)) {
                throw new BizException("凭证 " + v.getVoucherNo() + " 是你制单的，不能自己审核");
            }
            LambdaUpdateWrapper<FinVoucher> update = Wrappers.<FinVoucher>lambdaUpdate()
                    .set(FinVoucher::getStatus, to.name())
                    .set(FinVoucher::getUpdateTime, now)
                    .eq(FinVoucher::getId, id)
                    .eq(FinVoucher::getStatus, from.name());
            switch (to) {
                case AUDITED -> {
                    if (from == VoucherStatus.DRAFT) {
                        update.set(FinVoucher::getAuditBy, userId).set(FinVoucher::getAuditTime, now);
                    } else {
                        update.set(FinVoucher::getPostBy, null).set(FinVoucher::getPostTime, null);
                    }
                }
                case POSTED -> update.set(FinVoucher::getPostBy, userId).set(FinVoucher::getPostTime, now);
                case DRAFT -> update.set(FinVoucher::getAuditBy, null).set(FinVoucher::getAuditTime, null);
            }
            if (voucherMapper.update(update) == 0) {
                throw new BizException("凭证 " + v.getVoucherNo() + " 状态已变化，请刷新");
            }
        }
    }

    private FinVoucher getWithStatus(Long id, VoucherStatus status) {
        return getWithStatus(id, status, "凭证不是" + (status == VoucherStatus.DRAFT ? "未审核" : status == VoucherStatus.AUDITED ? "已审核" : "已记账") + "状态");
    }

    private FinVoucher getWithStatus(Long id, VoucherStatus status, String error) {
        FinVoucher v = voucherMapper.selectById(id);
        if (v == null) {
            throw new BizException("凭证不存在");
        }
        if (!status.name().equals(v.getStatus())) {
            throw new BizException(v.getPeriod() + " " + v.getVoucherNo() + "：" + error);
        }
        return v;
    }

    private static List<VoucherLine> toLines(List<VoucherEntryRequest> entries) {
        return entries.stream().map(e -> new VoucherLine(e.summary().trim(), e.accountId(), e.deptId(),
                e.debit() == null ? BigDecimal.ZERO : e.debit(), e.credit() == null ? BigDecimal.ZERO : e.credit(),
                e.projectId(), e.customerId(), e.supplierId(), e.materialId())).toList();
    }

    private void saveEntries(Long voucherId, List<FinVoucherEntry> entries) {
        entries.forEach(e -> e.setVoucherId(voucherId));
        entryMapper.insertBatch(entries);
    }

    private static BigDecimal total(List<FinVoucherEntry> entries, boolean debit) {
        return entries.stream().map(debit ? FinVoucherEntry::getDebit : FinVoucherEntry::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String voucherNo(int seq) {
        return WORD + "-" + String.format("%04d", seq);
    }
}
