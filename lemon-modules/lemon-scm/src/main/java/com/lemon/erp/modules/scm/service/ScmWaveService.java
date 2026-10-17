package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.scm.dto.PickRequest;
import com.lemon.erp.modules.scm.dto.WaveRequest;
import com.lemon.erp.modules.scm.entity.ScmDelivery;
import com.lemon.erp.modules.scm.entity.ScmDeliveryPick;
import com.lemon.erp.modules.scm.entity.ScmWave;
import com.lemon.erp.modules.scm.mapper.ScmDeliveryMapper;
import com.lemon.erp.modules.scm.mapper.ScmDeliveryPickMapper;
import com.lemon.erp.modules.scm.mapper.ScmWaveMapper;
import com.lemon.erp.modules.scm.model.DeliveryStatus;
import com.lemon.erp.modules.scm.model.WaveStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 波次拣货：同一仓库的多张待拣货发货通知合并为一个波次。拣货单把各通知的拣货建议按 库位（区域 → 排序 → 编码）→ 物料 → 批次 汇总，
 * 每行列出各通知的分拣数量；完成拣货时按拣货建议确认各通知的拣货（库存不足或需要序列号的通知须先在发货通知中处理）
 */
@Service
@RequiredArgsConstructor
public class ScmWaveService {

    /** 波次号的编码规则标识 */
    public static final String DOC_NO = "BC";

    private final ScmWaveMapper waveMapper;
    private final ScmDeliveryMapper deliveryMapper;
    private final ScmDeliveryPickMapper pickMapper;
    private final ScmDeliveryService deliveryService;
    private final DocNoService docNoService;

    /** 拣货单的一行：一个库位、物料、批次的合计数量和各发货通知的分拣数量 */
    public record PickLine(Long locationId, String locationCode, String zone, Long materialId, String materialCode, String materialName,
                           String spec, String unit, String batchNo, Object expiryDate, BigDecimal qty, List<Map<String, Object>> sorts) {
    }

    public PageResult<ScmWave> page(long current, long size, WaveStatus status, Long warehouseId, String keyword) {
        Page<ScmWave> page = Page.of(current, size);
        waveMapper.selectPageList(page, status, warehouseId, keyword);
        return PageResult.of(page);
    }

    public ScmWave detail(Long id) {
        ScmWave w = id == null ? null : waveMapper.selectDetail(id);
        if (w == null) {
            throw new BizException("scm.error.waveDoesNotExist");
        }
        w.setDeliveries(deliveryMapper.selectByWave(id));
        return w;
    }

    /** 拣货单（按库位路径汇总）和库存不足的明细 */
    public Map<String, Object> pickList(Long id) {
        detail(id);
        Map<String, PickLine> lines = new LinkedHashMap<>();
        for (Map<String, Object> r : waveMapper.selectPickLines(id)) {
            String key = r.get("locationId") + "|" + r.get("materialId") + "|" + r.get("batchNo");
            BigDecimal qty = new BigDecimal(r.get("qty").toString());
            PickLine line = lines.get(key);
            List<Map<String, Object>> sorts = line == null ? new ArrayList<>() : line.sorts();
            sorts.add(Map.of("deliveryNo", r.get("deliveryNo"), "customerName", r.get("customerName"), "qty", qty));
            lines.put(key, new PickLine(r.get("locationId") == null ? null : ((Number) r.get("locationId")).longValue(),
                    (String) r.get("locationCode"), (String) r.get("zone"), ((Number) r.get("materialId")).longValue(),
                    (String) r.get("materialCode"), (String) r.get("materialName"), (String) r.get("spec"), (String) r.get("unit"),
                    (String) r.get("batchNo"), r.get("expiryDate"), line == null ? qty : line.qty().add(qty), sorts));
        }
        return Map.of("lines", new ArrayList<>(lines.values()), "shortages", waveMapper.selectShortages(id));
    }

    @Transactional
    public Long create(WaveRequest req) {
        List<ScmDelivery> deliveries = requirePending(req.deliveryIds());
        Long warehouseId = deliveries.get(0).getWarehouseId();
        if (deliveries.stream().anyMatch(d -> !d.getWarehouseId().equals(warehouseId))) {
            throw new BizException("scm.error.waveOnlyContainDeliveryNoticesSame");
        }
        LoginUser user = SecurityUtils.getLoginUser();
        ScmWave w = new ScmWave();
        w.setWaveNo(docNoService.next(DOC_NO));
        w.setWarehouseId(warehouseId);
        w.setStatus(WaveStatus.PICKING);
        w.setRemark(req.remark());
        w.setDeptId(user.deptId());
        w.setCreateBy(user.userId());
        waveMapper.insert(w);
        join(w.getId(), deliveries);
        return w.getId();
    }

    @Transactional
    public void addDeliveries(Long id, WaveRequest req) {
        ScmWave w = requirePicking(id);
        List<ScmDelivery> deliveries = requirePending(req.deliveryIds());
        if (deliveries.stream().anyMatch(d -> !d.getWarehouseId().equals(w.getWarehouseId()))) {
            throw new BizException("scm.error.onlyDeliveryNoticesSameWarehouseAdded");
        }
        join(id, deliveries);
    }

    @Transactional
    public void removeDelivery(Long id, Long deliveryId) {
        requirePicking(id);
        if (deliveryMapper.update(Wrappers.<ScmDelivery>lambdaUpdate().set(ScmDelivery::getWaveId, null)
                .eq(ScmDelivery::getId, deliveryId).eq(ScmDelivery::getWaveId, id)) == 0) {
            throw new BizException("scm.error.deliveryNoticeNotWave");
        }
    }

    /** 取消波次：未拣货的发货通知移出波次 */
    @Transactional
    public void cancel(Long id) {
        requirePicking(id);
        deliveryMapper.update(Wrappers.<ScmDelivery>lambdaUpdate().set(ScmDelivery::getWaveId, null)
                .eq(ScmDelivery::getWaveId, id).eq(ScmDelivery::getStatus, DeliveryStatus.PENDING));
        changeStatus(id, WaveStatus.CANCELLED);
    }

    /**
     * 完成拣货的结果
     *
     * @param picked   确认了拣货的发货通知数
     * @param shortage 库存不足（建议数量不够）而跳过的发货通知
     * @param serial   有序列号物料而跳过的发货通知（须在发货通知中录入序列号拣货）
     */
    public record CompleteResult(int picked, List<String> shortage, List<String> serial) {
    }

    /**
     * 完成拣货：按拣货建议确认波次中各待拣货通知的拣货。库存不足、有序列号物料的通知跳过（仍为待拣货，可移出波次或在发货通知中单独拣货），
     * 波次完成
     */
    @Transactional
    public CompleteResult complete(Long id) {
        requirePicking(id);
        List<ScmDelivery> pending = deliveryMapper.selectByWave(id).stream().filter(d -> d.getStatus() == DeliveryStatus.PENDING).toList();
        if (pending.isEmpty()) {
            throw new BizException("scm.error.noDeliveryNoticesPendingPickingWave");
        }
        List<String> shortage = waveMapper.selectShortages(id).stream().map(s -> (String) s.get("deliveryNo")).distinct().toList();
        List<String> serial = waveMapper.selectPickLines(id).stream().filter(r -> r.get("serialEnabled") instanceof Number n && n.intValue() == 1)
                .map(r -> (String) r.get("deliveryNo")).filter(no -> !shortage.contains(no)).distinct().toList();
        List<ScmDelivery> ready = pending.stream().filter(d -> !shortage.contains(d.getDeliveryNo()) && !serial.contains(d.getDeliveryNo())).toList();
        if (ready.isEmpty()) {
            throw new BizException("scm.error.noDeliveryNoticesCompletePicking", reason(shortage, serial));
        }
        for (ScmDelivery d : ready) {
            List<PickRequest.Line> picks = pickMapper.selectList(Wrappers.<ScmDeliveryPick>lambdaQuery().eq(ScmDeliveryPick::getDeliveryId, d.getId())
                            .orderByAsc(ScmDeliveryPick::getId)).stream()
                    .map(p -> new PickRequest.Line(p.getDeliveryItemId(), p.getLocationId(), p.getBatchNo(), p.getQty(), null)).toList();
            deliveryService.confirmPick(d.getId(), new PickRequest(picks));
        }
        if (waveMapper.update(Wrappers.<ScmWave>lambdaUpdate().set(ScmWave::getStatus, WaveStatus.DONE)
                .set(ScmWave::getPickBy, SecurityUtils.getUserIdOrNull()).set(ScmWave::getPickTime, LocalDateTime.now())
                .set(ScmWave::getUpdateTime, LocalDateTime.now())
                .eq(ScmWave::getId, id).eq(ScmWave::getStatus, WaveStatus.PICKING)) == 0) {
            throw new BizException("scm.error.waveStatusHasChangedRefreshTry");
        }
        return new CompleteResult(ready.size(), shortage, serial);
    }

    private static String reason(List<String> shortage, List<String> serial) {
        List<String> parts = new ArrayList<>();
        if (!shortage.isEmpty()) {
            parts.add(Messages.get("scm.text.insufficientStock", String.join(Messages.get("scm.label.comma"), shortage)));
        }
        if (!serial.isEmpty()) {
            parts.add(Messages.get("scm.text.serialNumberedMaterialsSerialNumbersMust", String.join(Messages.get("scm.label.comma"), serial)));
        }
        return String.join(Messages.get("scm.label.message"), parts);
    }

    /** 库存变化后重新生成波次中各通知的拣货建议 */
    @Transactional
    public void resuggest(Long id) {
        requirePicking(id);
        deliveryMapper.selectByWave(id).stream().filter(d -> d.getStatus() == DeliveryStatus.PENDING)
                .forEach(d -> deliveryService.resuggest(d.getId()));
    }

    private void join(Long waveId, List<ScmDelivery> deliveries) {
        for (ScmDelivery d : deliveries) {
            // 原波次（已完成或已取消）中跳过的通知可以加入新波次；按读取时的波次做并发校验
            if (deliveryMapper.update(Wrappers.<ScmDelivery>lambdaUpdate().set(ScmDelivery::getWaveId, waveId)
                    .eq(ScmDelivery::getId, d.getId()).eq(ScmDelivery::getStatus, DeliveryStatus.PENDING)
                    .isNull(d.getWaveId() == null, ScmDelivery::getWaveId)
                    .eq(d.getWaveId() != null, ScmDelivery::getWaveId, d.getWaveId())) == 0) {
                throw new BizException("scm.error.deliveryNoticeAlreadyAnotherWaveStatus", d.getDeliveryNo());
            }
            // 按当前库存重新生成拣货建议
            deliveryService.resuggest(d.getId());
        }
    }

    private List<ScmDelivery> requirePending(List<Long> ids) {
        List<ScmDelivery> deliveries = deliveryMapper.selectByIds(ids.stream().distinct().toList());
        if (deliveries.size() != ids.stream().distinct().count()) {
            throw new BizException("scm.error.deliveryNoticeDoesNotExist");
        }
        for (ScmDelivery d : deliveries) {
            if (d.getStatus() != DeliveryStatus.PENDING) {
                throw new BizException("scm.error.deliveryNoticeNotPendingPicking", d.getDeliveryNo());
            }
            if (d.getWaveId() != null && waveMapper.exists(Wrappers.<ScmWave>lambdaQuery().eq(ScmWave::getId, d.getWaveId())
                    .eq(ScmWave::getStatus, WaveStatus.PICKING))) {
                throw new BizException("scm.error.deliveryNoticeAlreadyAnotherWave", d.getDeliveryNo());
            }
        }
        return deliveries;
    }

    private ScmWave requirePicking(Long id) {
        ScmWave w = detail(id);
        if (w.getStatus() != WaveStatus.PICKING) {
            throw new BizException("scm.error.waveCompletedCanceled");
        }
        return w;
    }

    private void changeStatus(Long id, WaveStatus to) {
        if (waveMapper.update(Wrappers.<ScmWave>lambdaUpdate().set(ScmWave::getStatus, to).set(ScmWave::getUpdateTime, LocalDateTime.now())
                .eq(ScmWave::getId, id).eq(ScmWave::getStatus, WaveStatus.PICKING)) == 0) {
            throw new BizException("scm.error.waveStatusHasChangedRefreshTry");
        }
    }
}
