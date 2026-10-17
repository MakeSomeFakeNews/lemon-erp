package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.period.BizPeriodGuard;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.mes.dto.ReportRequest;
import com.lemon.erp.modules.mes.entity.MesEquipment;
import com.lemon.erp.modules.mes.entity.MesInspection;
import com.lemon.erp.modules.mes.entity.MesReport;
import com.lemon.erp.modules.mes.entity.MesWoOp;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.mapper.MesEquipmentMapper;
import com.lemon.erp.modules.mes.mapper.MesInspectionMapper;
import com.lemon.erp.modules.mes.mapper.MesReportMapper;
import com.lemon.erp.modules.mes.mapper.MesWoOpMapper;
import com.lemon.erp.modules.mes.model.EquipmentStatus;
import com.lemon.erp.modules.mes.model.InspectionStatus;
import com.lemon.erp.modules.mes.model.QcResult;
import com.lemon.erp.modules.mes.model.QcType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;

/**
 * 生产报工：按工序累计合格、报废数量与工时，计件工资，模具使用次数
 */
@Service
@RequiredArgsConstructor
public class WoReportService {

    private final MesWorkOrderService workOrderService;
    private final MesWoOpMapper woOpMapper;
    private final MesReportMapper reportMapper;
    private final MesEquipmentMapper equipmentMapper;
    private final MesInspectionMapper inspectionMapper;
    private final MesMoldService moldService;
    private final BizPeriodGuard periodGuard;

    /** 报工：合格 + 报废累计不超过工单数量 */
    @Transactional
    public void report(Long id, ReportRequest req) {
        MesWorkOrder wo = workOrderService.getExecutable(id);
        periodGuard.checkStockOpen(req.reportDate(), "mes.label.productionReport");
        MesWoOp op = woOpMapper.selectById(req.opId());
        if (op == null || !op.getWoId().equals(id)) {
            throw new BizException("mes.error.operationDoesNotBelongWorkOrder");
        }
        BigDecimal scrap = Decimals.nz(req.scrapQty());
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
        workOrderService.markStarted(id);
    }

    public PageResult<MesReport> reports(long current, long size, Long woId, LocalDate beginDate, LocalDate endDate) {
        if (woId != null) {
            workOrderService.getAccessible(woId);
        }
        Page<MesReport> page = Page.of(current, size);
        reportMapper.selectPageList(page, woId, beginDate, endDate);
        return PageResult.of(page);
    }

    /** 工序的首件检验已确认且不是不合格 */
    private boolean firstArticlePassed(Long woOpId) {
        return inspectionMapper.exists(Wrappers.<MesInspection>lambdaQuery()
                .eq(MesInspection::getWoOpId, woOpId).eq(MesInspection::getType, QcType.FAI)
                .eq(MesInspection::getStatus, InspectionStatus.CONFIRMED).ne(MesInspection::getResult, QcResult.FAIL));
    }
}
