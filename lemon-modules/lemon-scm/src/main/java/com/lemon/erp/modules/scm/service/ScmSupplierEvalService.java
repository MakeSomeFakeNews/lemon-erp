package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.scm.dto.SupplierEvalRequest;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.entity.ScmSupplierEval;
import com.lemon.erp.modules.scm.mapper.ScmSupplierEvalMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;

/**
 * 供应商评估：质量 40% + 交期 30% + 价格 20% + 服务 10% 加权得出综合得分，
 * 按得分评定等级（≥90 A、≥75 B、≥60 C、其余 D），并更新供应商的等级
 */
@Service
@RequiredArgsConstructor
public class ScmSupplierEvalService {

    private final ScmSupplierEvalMapper evalMapper;
    private final ScmSupplierMapper supplierMapper;
    private final ScmSupplierService supplierService;

    public PageResult<ScmSupplierEval> page(long current, long size, Long supplierId, String period) {
        Page<ScmSupplierEval> page = Page.of(current, size);
        evalMapper.selectPageList(page, supplierId, period);
        return PageResult.of(page);
    }

    @Transactional
    public void create(SupplierEvalRequest req) {
        supplierService.get(req.supplierId());
        ScmSupplierEval eval = new ScmSupplierEval();
        eval.setSupplierId(req.supplierId());
        eval.setPeriod(req.period().trim());
        eval.setQualityScore(req.qualityScore());
        eval.setDeliveryScore(req.deliveryScore());
        eval.setPriceScore(req.priceScore());
        eval.setServiceScore(req.serviceScore());
        BigDecimal total = req.qualityScore().multiply(BigDecimal.valueOf(0.4))
                .add(req.deliveryScore().multiply(BigDecimal.valueOf(0.3)))
                .add(req.priceScore().multiply(BigDecimal.valueOf(0.2)))
                .add(req.serviceScore().multiply(BigDecimal.valueOf(0.1)))
                .setScale(1, RoundingMode.HALF_UP);
        eval.setTotalScore(total);
        eval.setGrade(grade(total));
        eval.setRemark(req.remark());
        eval.setCreateBy(SecurityUtils.getUserId());
        evalMapper.insert(eval);
        supplierMapper.update(Wrappers.<ScmSupplier>lambdaUpdate()
                .set(ScmSupplier::getLevel, eval.getGrade())
                .eq(ScmSupplier::getId, req.supplierId()));
    }

    /** 删除后供应商等级按剩余的最近一次评估（期间最新）重新确定，没有评估时清空 */
    @Transactional
    public void delete(List<Long> ids) {
        Set<Long> suppliers = evalMapper.selectByIds(ids).stream().map(ScmSupplierEval::getSupplierId).collect(Collectors.toSet());
        evalMapper.deleteByIds(ids);
        for (Long supplierId : suppliers) {
            ScmSupplierEval latest = evalMapper.selectPage(Page.of(1, 1, false), Wrappers.<ScmSupplierEval>lambdaQuery()
                    .eq(ScmSupplierEval::getSupplierId, supplierId)
                    .orderByDesc(ScmSupplierEval::getPeriod).orderByDesc(ScmSupplierEval::getId))
                    .getRecords().stream().findFirst().orElse(null);
            supplierMapper.update(Wrappers.<ScmSupplier>lambdaUpdate()
                    .set(ScmSupplier::getLevel, latest == null ? null : latest.getGrade())
                    .eq(ScmSupplier::getId, supplierId));
        }
    }

    static String grade(BigDecimal score) {
        double s = score.doubleValue();
        return s >= 90 ? "A" : s >= 75 ? "B" : s >= 60 ? "C" : "D";
    }
}
