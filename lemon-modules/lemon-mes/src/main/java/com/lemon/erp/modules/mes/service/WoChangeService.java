package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.modules.mes.dto.WorkOrderRequest;
import com.lemon.erp.modules.mes.entity.MesWoOp;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.mes.model.WorkOrderSource;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工单变更：改量（用料需求按新数量重算）、拆分（未开工的数量拆出新的草稿工单）
 */
@Service
@RequiredArgsConstructor
public class WoChangeService {

    private static final Map<String, String> CHANGE_FIELDS = ChangeLogger.fields("qty", "数量", // i18n-ignore
            "planStart", "计划开工", "planEnd", "计划完工"); // i18n-ignore

    private final MesWorkOrderService workOrderService;
    private final MesWorkOrderMapper workOrderMapper;
    private final WoLineService lineService;
    private final WmsMaterialService materialService;
    private final ChangeLogger changeLogger;

    /**
     * 修改已下达工单的数量：不少于已完工数量和各工序已报工（合格 + 报废）数量；用料需求按新数量重算（已领超出的部分可退料）
     */
    @Transactional
    public void changeQty(Long id, BigDecimal qty) {
        MesWorkOrder wo = workOrderService.getExecutable(id);
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
        lineService.recalcRequired(id, qty);
        changeLogger.log("mes_work_order", id, before, workOrderMapper.selectById(id), CHANGE_FIELDS);
        if (wo.getFinishedQty().compareTo(qty) >= 0) {
            workOrderService.markCompleted(id);
        }
    }

    /**
     * 拆分：从工单（草稿或已下达）未开工的数量中拆出一张新的草稿工单（同产品、同 BOM），原工单数量相应减少
     *
     * @return 新工单 ID
     */
    @Transactional
    public Long split(Long id, BigDecimal qty, LocalDate planStart, LocalDate planEnd) {
        MesWorkOrder wo = workOrderService.getAccessible(id);
        boolean draft = wo.getStatus() == WorkOrderStatus.DRAFT;
        if (!draft && !wo.getStatus().isExecutable()) {
            throw new BizException("mes.error.onlyDraftReleasedProductionWorkOrders");
        }
        BigDecimal free = wo.getQty().subtract(startedQty(wo));
        if (qty.signum() <= 0 || qty.compareTo(free) > 0 || qty.compareTo(wo.getQty()) >= 0) {
            throw new BizException("mes.error.splitQuantityMustGreaterLessWork", free.stripTrailingZeros().toPlainString());
        }
        Long newId = workOrderService.createDraft(new WorkOrderRequest(wo.getMaterialId(), wo.getBomId(), qty,
                planStart != null ? planStart : wo.getPlanStart(), planEnd != null ? planEnd : wo.getPlanEnd(), wo.getWarehouseId(),
                "拆分自 " + wo.getWoNo()), WorkOrderSource.SPLIT, wo.getPlanId()); // i18n-ignore
        workOrderMapper.update(Wrappers.<MesWorkOrder>lambdaUpdate().set(MesWorkOrder::getSourceWoId, id).eq(MesWorkOrder::getId, newId));
        BigDecimal left = wo.getQty().subtract(qty);
        if (draft) {
            workOrderService.update(id, new WorkOrderRequest(wo.getMaterialId(), wo.getBomId(), left, wo.getPlanStart(), wo.getPlanEnd(),
                    wo.getWarehouseId(), wo.getRemark()));
        } else {
            changeQty(id, left);
        }
        return newId;
    }

    /** 已开工的数量：已完工与各工序已报工（合格 + 报废）的最大值 */
    private BigDecimal startedQty(MesWorkOrder wo) {
        BigDecimal max = wo.getFinishedQty();
        for (MesWoOp op : lineService.ops(wo.getId())) {
            max = max.max(op.getGoodQty().add(op.getScrapQty()));
        }
        return max;
    }
}
