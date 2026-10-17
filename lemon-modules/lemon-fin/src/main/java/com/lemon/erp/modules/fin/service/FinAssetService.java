package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.AssetRequest;
import com.lemon.erp.modules.fin.dto.AssetMoveRequest;
import com.lemon.erp.modules.fin.dto.DisposeRequest;
import com.lemon.erp.modules.fin.entity.FinAsset;
import com.lemon.erp.modules.fin.entity.FinAssetMove;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinDepreciation;
import com.lemon.erp.modules.fin.event.VoucherDeletedEvent;
import com.lemon.erp.modules.fin.mapper.FinAssetMapper;
import com.lemon.erp.modules.fin.mapper.FinAssetMoveMapper;
import com.lemon.erp.modules.fin.mapper.FinDepreciationMapper;
import com.lemon.erp.modules.fin.model.AssetMoveType;
import com.lemon.erp.modules.fin.model.AssetStatus;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.lemon.erp.modules.fin.model.FinConstants.firstDay;
import static com.lemon.erp.modules.fin.model.FinConstants.lastDay;
import static com.lemon.erp.modules.fin.model.FinConstants.periodOf;
import static com.lemon.erp.modules.fin.model.FinConstants.previous;

/**
 * 固定资产：平均年限法，月折旧额 = 原值 × (1 - 残值率) / 使用月数，开始使用的次月开始计提，最后一个月计提剩余部分。
 * 系统上线前已使用的资产，新增时录入期初已计提月数和累计折旧。
 * 按期间计提（每期一次），生成凭证：借 折旧费用科目（按部门）/ 贷 累计折旧；可以撤销最近一期（凭证未审核时）。
 * 处置：生成凭证 借 累计折旧、固定资产清理 / 贷 固定资产
 */
@Service
@RequiredArgsConstructor
public class FinAssetService {

    /** 新增、修改时可选的状态 */
    private static final Set<AssetStatus> STATUSES = Set.of(AssetStatus.IN_USE, AssetStatus.IDLE);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final FinAssetMapper assetMapper;
    private final FinDepreciationMapper depreciationMapper;
    private final FinAssetMoveMapper moveMapper;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinVoucherService voucherService;
    private final FinPeriodService periodService;
    private final FinBillService billService;
    private final com.lemon.erp.modules.fin.mapper.FinBillMapper billMapper;

    public PageResult<FinAsset> page(long current, long size, String keyword, String category, AssetStatus status, Long deptId) {
        Page<FinAsset> page = Page.of(current, size);
        assetMapper.selectPageList(page, keyword, category, status, deptId);
        page.getRecords().forEach(FinAssetService::fillComputed);
        return PageResult.of(page);
    }

    public FinAsset get(Long id) {
        FinAsset asset = id == null ? null : assetMapper.selectById(id);
        if (asset == null) {
            throw new BizException("资产不存在");
        }
        fillComputed(asset);
        return asset;
    }

    public void create(AssetRequest req) {
        accountService.requireLeaves(List.of(req.expenseAccountId()));
        FinAsset asset = new FinAsset();
        fill(asset, req);
        // 系统上线前已使用的资产：录入期初已计提月数和累计折旧，之后从下一个月继续计提
        int months = req.openingMonths() == null ? 0 : req.openingMonths();
        BigDecimal accumulated = req.openingAccumulated() == null ? BigDecimal.ZERO : req.openingAccumulated();
        if (months > req.usefulMonths() || accumulated.compareTo(req.originalValue()) > 0) {
            throw new BizException("期初已计提月数、累计折旧不能超过使用年限、原值");
        }
        asset.setDepreciatedMonths(months);
        asset.setAccumulated(accumulated);
        asset.setStatus(req.status() != null && STATUSES.contains(req.status()) ? req.status() : AssetStatus.IN_USE);
        try {
            assetMapper.insert(asset);
        } catch (DuplicateKeyException e) {
            throw new BizException("资产编码已存在");
        }
        if (req.supplierId() != null) {
            // 采购新增：生成应付单（借 固定资产、进项税额 / 贷 应付账款），在应付管理中生成凭证、付款
            if (months > 0) {
                throw new BizException("期初资产不能同时登记采购供应商");
            }
            BigDecimal tax = req.taxAmount() == null ? BigDecimal.ZERO : req.taxAmount();
            FinBill bill = billService.createFromSource("AP", req.supplierId(), "ASSET", asset.getId(), asset.getCode(), req.useDate(),
                    req.originalValue().add(tax), tax, "购入固定资产 " + asset.getName(), null);
            billMapper.update(Wrappers.<FinBill>lambdaUpdate()
                    .set(FinBill::getExpenseAccountId, accountRules.accountId(AccountRole.FIXED_ASSET))
                    .set(FinBill::getDeptId, asset.getDeptId()).eq(FinBill::getId, bill.getId()));
        }
    }

    /** 已计提折旧的资产不能修改原值、残值率、年限和开始使用日期 */
    public void update(Long id, AssetRequest req) {
        FinAsset old = get(id);
        if (old.getStatus() == AssetStatus.DISPOSED) {
            throw new BizException("资产已处置，不能修改");
        }
        accountService.requireLeaves(List.of(req.expenseAccountId()));
        boolean valueChanged = old.getOriginalValue().compareTo(req.originalValue()) != 0
                || old.getResidualRate().compareTo(req.residualRate() == null ? BigDecimal.valueOf(5) : req.residualRate()) != 0
                || !old.getUsefulMonths().equals(req.usefulMonths()) || !old.getUseDate().equals(req.useDate());
        if (old.getDepreciatedMonths() > 0 && valueChanged) {
            throw new BizException("资产已计提折旧，不能修改原值、残值率、使用年限和开始使用日期");
        }
        FinAsset asset = new FinAsset();
        fill(asset, req);
        asset.setId(id);
        asset.setStatus(req.status() != null && STATUSES.contains(req.status()) ? req.status() : old.getStatus());
        try {
            assetMapper.updateById(asset);
        } catch (DuplicateKeyException e) {
            throw new BizException("资产编码已存在");
        }
        // 直接修改了使用部门、使用人、存放地点的，记入变动履历
        if (!Objects.equals(old.getDeptId(), asset.getDeptId()) || !Objects.equals(old.getCustodianId(), asset.getCustodianId())
                || !Objects.equals(old.getLocation(), asset.getLocation())) {
            saveMove(old, AssetMoveType.TRANSFER, LocalDate.now(), asset.getDeptId(), asset.getCustodianId(), asset.getLocation(), "修改资产资料");
        }
    }

    public void delete(List<Long> ids) {
        for (FinAsset a : assetMapper.selectByIds(ids)) {
            if (depreciationMapper.exists(Wrappers.<FinDepreciation>lambdaQuery().eq(FinDepreciation::getAssetId, a.getId()))) {
                throw new BizException("资产「" + a.getName() + "」已计提折旧，不能删除，可以处置");
            }
        }
        assetMapper.deleteByIds(ids);
    }

    /**
     * 处置：生成处置凭证（未审核）
     * <ul>
     *   <li>转入清理：借 累计折旧、固定资产清理 / 贷 固定资产</li>
     *   <li>处置收入：借 银行存款 / 贷 固定资产清理；清理费用：借 固定资产清理 / 贷 银行存款</li>
     *   <li>清理净损失转营业外支出，净收益转营业外收入，固定资产清理结平</li>
     * </ul>
     * 当月减少的资产当月照提折旧：处置月份在使用中且尚未计提当月折旧时，需要先计提。删除处置凭证后资产恢复为使用中
     *
     * @return 凭证 ID
     */
    @Transactional
    public Long dispose(Long id, DisposeRequest req) {
        FinAsset asset = get(id);
        if (asset.getStatus() == AssetStatus.DISPOSED) {
            throw new BizException("资产已处置");
        }
        LocalDate disposeDate = req.date() == null ? LocalDate.now() : req.date();
        int period = periodOf(disposeDate);
        periodService.checkOpen(period);
        boolean depreciableThisMonth = asset.getUseDate().isBefore(firstDay(period))
                && asset.getDepreciatedMonths() < asset.getUsefulMonths();
        if (depreciableThisMonth && !depreciationMapper.exists(Wrappers.<FinDepreciation>lambdaQuery()
                .eq(FinDepreciation::getPeriod, period).eq(FinDepreciation::getAssetId, id))) {
            throw new BizException("当月减少的固定资产当月照提折旧，请先计提 " + period + " 期间的折旧再处置");
        }
        BigDecimal income = req.income() == null ? BigDecimal.ZERO : req.income();
        BigDecimal expense = req.expense() == null ? BigDecimal.ZERO : req.expense();
        if ((income.signum() > 0 || expense.signum() > 0) && req.settleAccountId() == null) {
            throw new BizException("有处置收入或清理费用时请选择收付款账户");
        }
        String summary = "处置固定资产 " + asset.getCode() + " " + asset.getName();
        Long disposal = accountRules.accountId(AccountRole.ASSET_DISPOSAL);
        BigDecimal net = asset.getOriginalValue().subtract(asset.getAccumulated());
        List<VoucherLine> lines = new ArrayList<>(List.of(
                VoucherLine.debit(summary, accountRules.accountId(AccountRole.DEPRECIATION), null, asset.getAccumulated()),
                VoucherLine.debit(summary, disposal, null, net),
                VoucherLine.credit(summary, accountRules.accountId(AccountRole.FIXED_ASSET), null, asset.getOriginalValue())));
        if (income.signum() > 0) {
            Long bank = accountService.requireCashAccount(req.settleAccountId()).getId();
            lines.add(VoucherLine.debit(summary + "-处置收入", bank, null, income));
            lines.add(VoucherLine.credit(summary + "-处置收入", disposal, null, income));
        }
        if (expense.signum() > 0) {
            Long bank = accountService.requireCashAccount(req.settleAccountId()).getId();
            lines.add(VoucherLine.debit(summary + "-清理费用", disposal, null, expense));
            lines.add(VoucherLine.credit(summary + "-清理费用", bank, null, expense));
        }
        // 固定资产清理余额（借方为损失）转营业外收支
        BigDecimal loss = net.add(expense).subtract(income);
        if (loss.signum() > 0) {
            lines.add(VoucherLine.debit(summary + "-净损失", accountRules.accountId(AccountRole.NON_OPERATING_EXPENSE), null, loss));
            lines.add(VoucherLine.credit(summary + "-净损失", disposal, null, loss));
        } else if (loss.signum() < 0) {
            lines.add(VoucherLine.debit(summary + "-净收益", disposal, null, loss.negate()));
            lines.add(VoucherLine.credit(summary + "-净收益", accountRules.accountId(AccountRole.NON_OPERATING_INCOME), null, loss.negate()));
        }
        Long voucherId = voucherService.createGenerated(disposeDate, VoucherSource.ASSET, asset.getCode(), lines);
        assetMapper.update(Wrappers.<FinAsset>lambdaUpdate()
                .set(FinAsset::getStatus, AssetStatus.DISPOSED)
                .set(FinAsset::getPrevStatus, asset.getStatus())
                .set(FinAsset::getDisposeDate, disposeDate)
                .set(FinAsset::getDisposeVoucherId, voucherId)
                .eq(FinAsset::getId, id));
        return voucherId;
    }

    /**
     * 领用（闲置 → 使用中）、归还（使用中 → 闲置，清空使用人）、调拨（变更部门 / 使用人 / 存放地点），记入变动履历。
     * 折旧费用按资产当时的使用部门归集
     */
    @Transactional
    public void move(Long id, AssetMoveRequest req) {
        FinAsset asset = get(id);
        if (asset.getStatus() == AssetStatus.DISPOSED) {
            throw new BizException("资产已处置");
        }
        String location = req.location() == null || req.location().isBlank() ? null : req.location().trim();
        Long deptId = req.deptId() != null ? req.deptId() : asset.getDeptId();
        AssetStatus status = asset.getStatus();
        Long userId;
        switch (req.moveType()) {
            case ASSIGN -> {
                if (asset.getStatus() != AssetStatus.IDLE) {
                    throw new BizException("只有闲置的资产可以领用");
                }
                if (req.userId() == null && req.deptId() == null) {
                    throw new BizException("请选择领用部门或使用人");
                }
                userId = req.userId();
                status = AssetStatus.IN_USE;
            }
            case RETURN -> {
                if (asset.getStatus() != AssetStatus.IN_USE) {
                    throw new BizException("只有使用中的资产可以归还");
                }
                userId = null;
                status = AssetStatus.IDLE;
            }
            default -> {
                // 调拨：没有填写的部门、使用人、存放地点保持不变
                userId = req.userId() != null ? req.userId() : asset.getCustodianId();
                if (Objects.equals(deptId, asset.getDeptId()) && Objects.equals(userId, asset.getCustodianId())
                        && Objects.equals(location == null ? asset.getLocation() : location, asset.getLocation())) {
                    throw new BizException("调拨后的部门、使用人、存放地点与现在相同");
                }
            }
        }
        String newLocation = location != null ? location : asset.getLocation();
        assetMapper.update(Wrappers.<FinAsset>lambdaUpdate()
                .set(FinAsset::getDeptId, deptId)
                .set(FinAsset::getCustodianId, userId)
                .set(FinAsset::getLocation, newLocation)
                .set(FinAsset::getStatus, status)
                .eq(FinAsset::getId, id));
        saveMove(asset, req.moveType(), req.moveDate(), deptId, userId, newLocation, req.remark());
    }

    public List<FinAssetMove> moves(Long id) {
        get(id);
        return moveMapper.selectByAsset(id);
    }

    /** 记录变动：原值取资产当前的部门、使用人、存放地点 */
    void saveMove(FinAsset before, AssetMoveType type, LocalDate date, Long deptId, Long userId, String location, String remark) {
        FinAssetMove m = new FinAssetMove();
        m.setAssetId(before.getId());
        m.setMoveType(type);
        m.setMoveDate(date);
        m.setFromDeptId(before.getDeptId());
        m.setToDeptId(deptId);
        m.setFromUserId(before.getCustodianId());
        m.setToUserId(userId);
        m.setFromLocation(before.getLocation());
        m.setToLocation(location);
        m.setRemark(remark);
        m.setCreateBy(SecurityUtils.getUserIdOrNull());
        m.setCreateTime(LocalDateTime.now());
        moveMapper.insert(m);
    }

    /** 处置凭证被删除：资产恢复处置前的状态，可以重新处置 */
    @EventListener
    public void onVoucherDeleted(VoucherDeletedEvent e) {
        for (FinAsset a : assetMapper.selectList(Wrappers.<FinAsset>lambdaQuery().in(FinAsset::getDisposeVoucherId, e.voucherIds()))) {
            assetMapper.update(Wrappers.<FinAsset>lambdaUpdate()
                    .set(FinAsset::getStatus, a.getPrevStatus() == null ? AssetStatus.IN_USE : a.getPrevStatus())
                    .set(FinAsset::getDisposeDate, null)
                    .set(FinAsset::getDisposeVoucherId, null)
                    .eq(FinAsset::getId, a.getId()));
        }
    }

    // ---------------------------------------------------------------- 折旧

    public PageResult<FinDepreciation> records(long current, long size, Integer period, Long assetId) {
        Page<FinDepreciation> page = Page.of(current, size);
        depreciationMapper.selectPageList(page, period, assetId);
        return PageResult.of(page);
    }

    /**
     * 计提某期间的折旧并生成凭证（未审核）
     *
     * @return 计提的资产数
     */
    @Transactional
    public int depreciate(int period) {
        periodService.checkOpen(period);
        if (depreciationMapper.exists(Wrappers.<FinDepreciation>lambdaQuery().eq(FinDepreciation::getPeriod, period))) {
            throw new BizException(period + " 期间已计提折旧");
        }
        // 必须按月顺序计提：之后的期间已计提时不能补提，之前有计提时上一期必须已计提
        if (depreciationMapper.exists(Wrappers.<FinDepreciation>lambdaQuery().gt(FinDepreciation::getPeriod, period))) {
            throw new BizException("之后的期间已计提折旧，不能再计提 " + period);
        }
        if (depreciationMapper.exists(Wrappers.<FinDepreciation>lambdaQuery().lt(FinDepreciation::getPeriod, period))
                && !depreciationMapper.exists(Wrappers.<FinDepreciation>lambdaQuery().eq(FinDepreciation::getPeriod, previous(period)))) {
            throw new BizException("请先计提 " + previous(period) + " 期间的折旧");
        }
        LocalDate start = firstDay(period);
        List<FinAsset> assets = assetMapper.selectDepreciable(start);
        if (assets.isEmpty()) {
            throw new BizException(period + " 期间没有需要计提折旧的资产");
        }
        Long userId = SecurityUtils.getUserId();
        String summary = "计提 " + period + " 期间折旧";
        Long credit = accountRules.accountId(AccountRole.DEPRECIATION);
        List<FinDepreciation> records = new ArrayList<>();
        List<VoucherLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (FinAsset a : assets) {
            BigDecimal amount = depreciationOf(a);
            if (amount.signum() <= 0) {
                continue;
            }
            if (assetMapper.addDepreciation(a.getId(), amount, a.getDepreciatedMonths()) == 0) {
                throw new BizException("资产「" + a.getName() + "」正在被修改，请重试");
            }
            FinDepreciation r = new FinDepreciation();
            r.setPeriod(period);
            r.setAssetId(a.getId());
            r.setAmount(amount);
            r.setCreateBy(userId);
            records.add(r);
            lines.add(VoucherLine.debit(summary, a.getExpenseAccountId(), a.getDeptId(), amount));
            total = total.add(amount);
        }
        if (records.isEmpty()) {
            throw new BizException(period + " 期间没有需要计提折旧的资产");
        }
        lines.add(VoucherLine.credit(summary, credit, null, total));
        Long voucherId = voucherService.createGenerated(lastDay(period), VoucherSource.DEPRECIATION, String.valueOf(period), lines);
        records.forEach(r -> r.setVoucherId(voucherId));
        try {
            depreciationMapper.insertBatch(records);
        } catch (DuplicateKeyException e) {
            throw new BizException(period + " 期间已计提折旧");
        }
        return records.size();
    }

    /** 撤销最近一期折旧：凭证未审核时删除凭证并恢复资产的累计折旧 */
    @Transactional
    public void undoDepreciation(int period) {
        FinDepreciation latest = depreciationMapper.selectOne(Wrappers.<FinDepreciation>lambdaQuery()
                .orderByDesc(FinDepreciation::getPeriod).last("LIMIT 1"));
        if (latest == null || latest.getPeriod() != period) {
            throw new BizException("只能撤销最近一期（" + (latest == null ? "无" : latest.getPeriod()) + "）的折旧");
        }
        List<FinDepreciation> records = depreciationMapper.selectList(Wrappers.<FinDepreciation>lambdaQuery()
                .eq(FinDepreciation::getPeriod, period));
        records.stream().map(FinDepreciation::getVoucherId).filter(java.util.Objects::nonNull).distinct()
                .forEach(voucherId -> voucherService.delete(List.of(voucherId)));
        for (FinDepreciation r : records) {
            assetMapper.addDepreciation(r.getAssetId(), r.getAmount().negate(), null);
        }
        depreciationMapper.delete(Wrappers.<FinDepreciation>lambdaQuery().eq(FinDepreciation::getPeriod, period));
    }

    // ---------------------------------------------------------------- 内部

    /** 本月折旧额：最后一个月为剩余可折旧金额 */
    static BigDecimal depreciationOf(FinAsset a) {
        BigDecimal depreciable = a.getOriginalValue().multiply(BigDecimal.ONE.subtract(a.getResidualRate().divide(HUNDRED, 6, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = depreciable.subtract(a.getAccumulated());
        if (a.getDepreciatedMonths() + 1 >= a.getUsefulMonths()) {
            return remaining.max(BigDecimal.ZERO);
        }
        return monthly(a).min(remaining).max(BigDecimal.ZERO);
    }

    static BigDecimal monthly(FinAsset a) {
        return a.getOriginalValue().multiply(BigDecimal.ONE.subtract(a.getResidualRate().divide(HUNDRED, 6, RoundingMode.HALF_UP)))
                .divide(BigDecimal.valueOf(a.getUsefulMonths()), 2, RoundingMode.HALF_UP);
    }

    private static void fillComputed(FinAsset a) {
        a.setMonthlyDepreciation(monthly(a));
        a.setNetValue(a.getOriginalValue().subtract(a.getAccumulated()));
    }

    private static void fill(FinAsset asset, AssetRequest req) {
        asset.setCode(req.code().trim());
        asset.setName(req.name().trim());
        asset.setCategory(req.category());
        asset.setModel(req.model());
        asset.setDeptId(req.deptId());
        asset.setCustodianId(req.custodianId());
        asset.setLocation(req.location() == null || req.location().isBlank() ? null : req.location().trim());
        asset.setUseDate(req.useDate());
        asset.setOriginalValue(req.originalValue());
        asset.setResidualRate(req.residualRate() == null ? BigDecimal.valueOf(5) : req.residualRate());
        asset.setUsefulMonths(req.usefulMonths());
        asset.setExpenseAccountId(req.expenseAccountId());
        asset.setRemark(req.remark());
    }

    /** 当前期间（今天所在月份） */
    static int currentPeriod() {
        return periodOf(LocalDate.now());
    }
}
