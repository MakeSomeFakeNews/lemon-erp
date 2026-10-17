package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.fin.dto.CostRunResult;
import com.lemon.erp.modules.fin.entity.FinCostLayer;
import com.lemon.erp.modules.fin.entity.FinCostRun;
import com.lemon.erp.modules.fin.entity.FinMaterialCost;
import com.lemon.erp.modules.fin.entity.FinVoucher;
import com.lemon.erp.modules.fin.entity.FinWoCost;
import com.lemon.erp.modules.fin.mapper.FinCostLayerMapper;
import com.lemon.erp.modules.fin.mapper.FinCostRunMapper;
import com.lemon.erp.modules.fin.mapper.FinMaterialCostMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherMapper;
import com.lemon.erp.modules.fin.mapper.FinWoCostMapper;
import com.lemon.erp.modules.fin.model.CostRunKind;
import com.lemon.erp.modules.fin.model.VoucherStatus;
import com.lemon.erp.modules.fin.service.cost.CostCalculator;
import com.lemon.erp.modules.fin.service.cost.CostCalculator.CostResult;
import com.lemon.erp.modules.fin.service.cost.CostSource;
import com.lemon.erp.modules.fin.service.cost.CostSourceLoader;
import com.lemon.erp.modules.fin.service.cost.CostVoucherWriter;
import com.lemon.erp.modules.fin.service.cost.CostVoucherWriter.CostVoucher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.lemon.erp.modules.fin.model.FinConstants.lastDay;
import static com.lemon.erp.modules.fin.model.FinConstants.previous;

/**
 * 成本核算（按期间）：按物料的计价方法（月末一次加权平均、移动加权平均、先进先出）计算存货成本，工单归集料工费并结转完工产品成本，
 * 生成成本结转凭证（未审核）。这里只负责期间校验、编排和保存结果：
 * <ol>
 *   <li>取数：{@link CostSourceLoader}（仓库流水、采购销售、工单、组装拆卸等，转为强类型的 {@link CostSource}）</li>
 *   <li>计算：{@link CostCalculator}（纯计算，按依赖顺序计算物料单价和工单成本，逐笔计价见 Valuation）</li>
 *   <li>凭证：{@link CostVoucherWriter}</li>
 * </ol>
 * 必须逐月核算（首次核算或存货期初之后的第一期除外）；期间未结账、成本凭证未审核时可以重新核算
 */
@Service
@RequiredArgsConstructor
public class FinCostService {

    private final FinCostRunMapper runMapper;
    private final FinMaterialCostMapper materialCostMapper;
    private final FinWoCostMapper woCostMapper;
    private final FinCostLayerMapper layerMapper;
    private final FinVoucherMapper voucherMapper;
    private final FinVoucherService voucherService;
    private final FinPeriodService periodService;
    private final CostSourceLoader sourceLoader;
    private final CostVoucherWriter voucherWriter;

    public PageResult<FinCostRun> page(long current, long size) {
        Page<FinCostRun> page = Page.of(current, size);
        runMapper.selectPageList(page);
        return PageResult.of(page);
    }

    public PageResult<FinMaterialCost> materialCosts(long current, long size, int period, String keyword) {
        Page<FinMaterialCost> page = Page.of(current, size);
        materialCostMapper.selectPageList(page, period, keyword);
        return PageResult.of(page);
    }

    public PageResult<FinWoCost> woCosts(long current, long size, int period) {
        Page<FinWoCost> page = Page.of(current, size);
        woCostMapper.selectPageList(page, period);
        return PageResult.of(page);
    }

    /** 核算期间成本并生成成本结转凭证 */
    @Transactional
    public CostRunResult run(int period) {
        periodService.checkOpen(period);
        FinCostRun old = runMapper.selectOne(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getPeriod, period));
        if (old != null) {
            if (old.getKind() == CostRunKind.OPENING) {
                throw new BizException("fin.error.inventoryOpeningPeriodCannotCostedStart", period);
            }
            discard(old);
        }
        if (runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().gt(FinCostRun::getPeriod, period))) {
            throw new BizException("fin.error.laterPeriodsHaveCostedCannotRecalculate", period);
        }
        // 已有更早的核算（或存货期初）时，必须逐月核算，否则期初金额会按 0 处理
        if (runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().lt(FinCostRun::getPeriod, period))
                && !runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getPeriod, previous(period)))) {
            throw new BizException("fin.error.runCostAccountingPeriodFirst", previous(period));
        }

        CostSource src = sourceLoader.load(period);
        CostResult result = new CostCalculator(src).compute();

        if (!result.materials().isEmpty()) {
            materialCostMapper.insertBatch(new ArrayList<>(result.materials().values()));
        }
        if (!result.woCosts().isEmpty()) {
            woCostMapper.insertBatch(new ArrayList<>(result.woCosts().values()));
        }
        if (!result.closeLayers().isEmpty()) {
            layerMapper.insertBatch(new ArrayList<>(result.closeLayers()));
        }
        FinCostRun run = new FinCostRun();
        run.setPeriod(period);
        run.setKind(CostRunKind.RUN);
        CostVoucher voucher = voucherWriter.write(src, result, lastDay(period));
        run.setOverheadVariance(voucher.overheadVariance());
        run.setVoucherId(voucher.voucherId());
        runMapper.insert(run);
        return new CostRunResult(result.materials().size(), result.zeroCostWarnings());
    }

    /** 撤销核算：删除成本数据和未审核的成本凭证 */
    @Transactional
    public void undo(int period) {
        periodService.checkOpen(period);
        FinCostRun run = runMapper.selectOne(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getPeriod, period));
        if (run == null) {
            throw new BizException("fin.error.noCostAccountingHasRunPeriod", period);
        }
        if (run.getKind() == CostRunKind.OPENING) {
            throw new BizException("fin.error.inventoryOpeningModifyOpeningBalances", period);
        }
        if (runMapper.exists(Wrappers.<FinCostRun>lambdaQuery().gt(FinCostRun::getPeriod, period))) {
            throw new BizException("fin.error.undoCostAccountingLaterPeriodsFirst");
        }
        discard(run);
    }

    private void discard(FinCostRun run) {
        if (run.getVoucherId() != null) {
            FinVoucher v = voucherMapper.selectById(run.getVoucherId());
            if (v != null && v.getStatus() != VoucherStatus.DRAFT) {
                throw new BizException("fin.error.costTransferVoucherApprovedUnapproveBefore", v.getVoucherNo());
            }
            if (v != null) {
                voucherService.delete(List.of(v.getId()));
            }
        }
        materialCostMapper.delete(Wrappers.<FinMaterialCost>lambdaQuery().eq(FinMaterialCost::getPeriod, run.getPeriod()));
        woCostMapper.delete(Wrappers.<FinWoCost>lambdaQuery().eq(FinWoCost::getPeriod, run.getPeriod()));
        layerMapper.delete(Wrappers.<FinCostLayer>lambdaQuery().eq(FinCostLayer::getPeriod, run.getPeriod()));
        runMapper.deleteById(run.getId());
    }
}
