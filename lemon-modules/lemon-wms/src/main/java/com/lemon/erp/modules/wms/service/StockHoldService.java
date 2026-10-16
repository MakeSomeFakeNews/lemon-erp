package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockHold;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockHoldMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockMapper;
import com.lemon.erp.modules.wms.model.HoldLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存冻结：需要来料检验的物料采购入库后冻结为待检；检验合格（或让步接收）放行，不合格保持冻结等待退货。
 * 冻结数量记在 wms_stock.frozen_qty，出库、调拨、领料只能使用未冻结的数量；采购退货优先退冻结的不合格品
 */
@Service
@RequiredArgsConstructor
public class StockHoldService {

    public static final String PENDING = "PENDING";
    public static final String REJECTED = "REJECTED";
    public static final String RELEASED = "RELEASED";
    public static final String RETURNED = "RETURNED";

    private final WmsStockHoldMapper holdMapper;
    private final WmsStockMapper stockMapper;
    private final WmsMaterialMapper materialMapper;

    public PageResult<WmsStockHold> page(long current, long size, String status, String keyword, String sourceNo) {
        Page<WmsStockHold> page = Page.of(current, size);
        holdMapper.selectPageList(page, status, keyword, sourceNo);
        return PageResult.of(page);
    }

    /**
     * 采购入库后冻结需要来料检验的物料（入库单确认的同一事务中调用）
     *
     * @param sourceNo 采购入库单号（来料检验单填写的单号）
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void holdForInspection(String sourceNo, Long orderId, Long partnerId, List<HoldLine> lines) {
        Map<Long, WmsMaterial> materials = materialMapper.selectByIds(lines.stream().map(HoldLine::materialId).distinct().toList())
                .stream().collect(Collectors.toMap(WmsMaterial::getId, Function.identity()));
        for (HoldLine l : lines) {
            WmsMaterial m = materials.get(l.materialId());
            if (m == null || !m.needsIqc() || l.qty().signum() <= 0) {
                continue;
            }
            if (stockMapper.freeze(l.warehouseId(), l.locationId(), l.materialId(), l.batchNo(), l.qty()) == 0) {
                throw new BizException("冻结待检库存失败：" + StockService.describe(m));
            }
            WmsStockHold h = new WmsStockHold();
            h.setStatus(PENDING);
            h.setWarehouseId(l.warehouseId());
            h.setLocationId(l.locationId());
            h.setMaterialId(l.materialId());
            h.setBatchNo(l.batchNo());
            h.setHoldQty(l.qty());
            h.setQty(l.qty());
            h.setSourceNo(sourceNo);
            h.setOrderId(orderId);
            h.setPartnerId(partnerId);
            holdMapper.insert(h);
        }
    }

    /**
     * 来料检验确认：合格 / 让步接收放行，不合格保持冻结（待退货）
     *
     * @param batchNo 检验单填写了批次时只处理该批次
     * @return 处理的冻结记录数
     */
    @Transactional
    public int inspect(String sourceNo, Long materialId, String batchNo, boolean pass, String inspNo) {
        List<WmsStockHold> holds = holdMapper.selectList(Wrappers.<WmsStockHold>lambdaQuery()
                .eq(WmsStockHold::getSourceNo, sourceNo)
                .eq(WmsStockHold::getMaterialId, materialId)
                .eq(StringUtils.hasText(batchNo), WmsStockHold::getBatchNo, batchNo)
                .eq(WmsStockHold::getStatus, PENDING));
        for (WmsStockHold h : holds) {
            if (pass) {
                release(h, inspNo, null);
            } else {
                holdMapper.update(Wrappers.<WmsStockHold>lambdaUpdate()
                        .set(WmsStockHold::getStatus, REJECTED).set(WmsStockHold::getInspNo, inspNo)
                        .eq(WmsStockHold::getId, h.getId()).eq(WmsStockHold::getStatus, PENDING));
            }
        }
        return holds.size();
    }

    /**
     * 不合格品处置（让步接收、报废）：放行来源单据该物料（批次）的待检、不合格冻结库存
     *
     * @return 放行的冻结记录（报废时据此生成报废出库单）
     */
    @Transactional
    public List<WmsStockHold> releaseBySource(String sourceNo, Long materialId, String batchNo, String remark) {
        List<WmsStockHold> holds = holdMapper.selectList(Wrappers.<WmsStockHold>lambdaQuery()
                .eq(WmsStockHold::getSourceNo, sourceNo)
                .eq(WmsStockHold::getMaterialId, materialId)
                .eq(StringUtils.hasText(batchNo), WmsStockHold::getBatchNo, batchNo)
                .in(WmsStockHold::getStatus, PENDING, REJECTED));
        holds.forEach(h -> release(h, null, remark));
        return holds;
    }

    /** 手工放行（让步接收、误判复检），待检、不合格的都可以 */
    @Transactional
    public void release(Long id, String remark) {
        WmsStockHold h = holdMapper.selectById(id);
        if (h == null || !(PENDING.equals(h.getStatus()) || REJECTED.equals(h.getStatus()))) {
            throw new BizException("冻结记录不存在或已处理");
        }
        release(h, null, remark);
    }

    /**
     * 采购退货前解冻同一订单、同一库存位置的冻结库存（先不合格、后待检），再按普通出库扣减
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseForReturn(Long orderId, List<HoldLine> lines) {
        for (HoldLine l : lines) {
            BigDecimal remaining = l.qty();
            List<WmsStockHold> holds = holdMapper.selectList(Wrappers.<WmsStockHold>lambdaQuery()
                    .eq(WmsStockHold::getOrderId, orderId)
                    .eq(WmsStockHold::getWarehouseId, l.warehouseId())
                    .eq(WmsStockHold::getLocationId, l.locationId())
                    .eq(WmsStockHold::getMaterialId, l.materialId())
                    .eq(WmsStockHold::getBatchNo, l.batchNo())
                    .in(WmsStockHold::getStatus, PENDING, REJECTED)
                    .orderByDesc(WmsStockHold::getStatus) // REJECTED 在 PENDING 之前
                    .orderByAsc(WmsStockHold::getId));
            for (WmsStockHold h : holds) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal take = remaining.min(h.getQty());
                if (holdMapper.consume(h.getId(), take, RETURNED) == 0
                        || stockMapper.unfreeze(h.getWarehouseId(), h.getLocationId(), h.getMaterialId(), h.getBatchNo(), take) == 0) {
                    throw new BizException("冻结库存已变化，请刷新后重试");
                }
                remaining = remaining.subtract(take);
            }
        }
    }

    /** 待检数量（MRP 视为将要可用的库存） */
    public Map<Long, BigDecimal> pendingQty(Collection<Long> materialIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (materialIds.isEmpty()) {
            return map;
        }
        for (Map<String, Object> row : holdMapper.selectPendingQty(materialIds)) {
            map.put(((Number) row.get("materialId")).longValue(), new BigDecimal(row.get("qty").toString()));
        }
        return map;
    }

    private void release(WmsStockHold h, String inspNo, String remark) {
        if (holdMapper.update(Wrappers.<WmsStockHold>lambdaUpdate()
                .set(WmsStockHold::getStatus, RELEASED)
                .set(WmsStockHold::getQty, BigDecimal.ZERO)
                .set(inspNo != null, WmsStockHold::getInspNo, inspNo)
                .set(remark != null, WmsStockHold::getRemark, remark)
                .set(WmsStockHold::getReleaseBy, SecurityUtils.getUserIdOrNull())
                .set(WmsStockHold::getReleaseTime, LocalDateTime.now())
                .eq(WmsStockHold::getId, h.getId())
                .eq(WmsStockHold::getQty, h.getQty())
                .in(WmsStockHold::getStatus, PENDING, REJECTED)) == 0
                || stockMapper.unfreeze(h.getWarehouseId(), h.getLocationId(), h.getMaterialId(), h.getBatchNo(), h.getQty()) == 0) {
            throw new BizException("冻结库存已变化，请刷新后重试");
        }
    }
}
