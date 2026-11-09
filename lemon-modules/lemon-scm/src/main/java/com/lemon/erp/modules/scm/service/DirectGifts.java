package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.wms.service.WmsStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.wms.model.WmsConstants.joinSerials;
import static com.lemon.erp.modules.wms.model.WmsConstants.splitSerials;

/**
 * 直接开单（销售出库）的买赠赠品：隐式订单按促销规则生成赠品行（单价 0），出库明细对应到这些赠品行。
 * <ul>
 *   <li>请求带了赠品行（gift = true）：各赠品物料的数量合计须与促销规则计算的一致，按顺序对应到订单的赠品行（一行请求可拆到多行订单明细）</li>
 *   <li>请求没有赠品行：按表头仓库先到期先出（已过期、冻结的除外）自动选择库位批次补上，库存不足或需要序列号时报错</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class DirectGifts {

    private final WmsStockQueryService stockQueryService;

    /**
     * @param giftItems   隐式订单的赠品行（gift = 1，已带物料编码、名称、批次 / 序列号管理）
     * @param requests    请求中的赠品行
     * @param warehouseId 表头仓库（自动补赠品时从这里出）
     * @return 引用订单赠品行的出库明细
     */
    public List<TradeDocItemRequest> lines(List<ScmOrderItem> giftItems, List<TradeDocItemRequest> requests, Long warehouseId) {
        if (giftItems.isEmpty()) {
            if (!requests.isEmpty()) {
                throw new BizException("scm.error.giftNotApplicable");
            }
            return List.of();
        }
        return requests.isEmpty() ? autoPick(giftItems, warehouseId) : match(giftItems, requests);
    }

    /** 请求的赠品行对应到订单赠品行：按物料核对数量，再按顺序分配（必要时拆行，序列号随数量拆分） */
    static List<TradeDocItemRequest> match(List<ScmOrderItem> giftItems, List<TradeDocItemRequest> requests) {
        Map<Long, BigDecimal> expected = new LinkedHashMap<>();
        Map<Long, ScmOrderItem> sample = new LinkedHashMap<>();
        for (ScmOrderItem g : giftItems) {
            expected.merge(g.getMaterialId(), g.getQty(), BigDecimal::add);
            sample.putIfAbsent(g.getMaterialId(), g);
        }
        Map<Long, BigDecimal> given = new LinkedHashMap<>();
        for (TradeDocItemRequest r : requests) {
            if (r.materialId() == null || !expected.containsKey(r.materialId())) {
                throw new BizException("scm.error.giftNotInPromotion");
            }
            given.merge(r.materialId(), r.qty(), BigDecimal::add);
        }
        expected.forEach((materialId, qty) -> {
            BigDecimal g = given.getOrDefault(materialId, BigDecimal.ZERO);
            if (g.compareTo(qty) != 0) {
                ScmOrderItem m = sample.get(materialId);
                throw new BizException("scm.error.giftQtyMismatch", m.getMaterialCode(), m.getMaterialName(), plain(qty), plain(g));
            }
        });
        List<TradeDocItemRequest> lines = new ArrayList<>();
        for (Long materialId : expected.keySet()) {
            List<ScmOrderItem> targets = giftItems.stream().filter(g -> g.getMaterialId().equals(materialId)).toList();
            int t = 0;
            BigDecimal targetLeft = targets.get(0).getQty();
            for (TradeDocItemRequest r : requests) {
                if (!r.materialId().equals(materialId)) {
                    continue;
                }
                BigDecimal left = r.qty();
                List<String> serials = new ArrayList<>(splitSerials(r.serialNos()));
                while (left.signum() > 0) {
                    while (targetLeft.signum() <= 0) {
                        targetLeft = targets.get(++t).getQty();
                    }
                    BigDecimal take = left.min(targetLeft);
                    String part = null;
                    if (!serials.isEmpty()) {
                        int n = take.compareTo(left) == 0 ? serials.size() : Math.min(serials.size(), take.intValue());
                        part = joinSerials(new ArrayList<>(serials.subList(0, n)));
                        serials.subList(0, n).clear();
                    }
                    lines.add(line(targets.get(t).getId(), r.locationId(), r.batchNo(), take, part, r.remark(), r.warehouseId()));
                    left = left.subtract(take);
                    targetLeft = targetLeft.subtract(take);
                }
            }
        }
        return lines;
    }

    /** 没有指定赠品的库位批次：从表头仓库按先到期先出自动选择 */
    private List<TradeDocItemRequest> autoPick(List<ScmOrderItem> giftItems, Long warehouseId) {
        List<TradeDocItemRequest> lines = new ArrayList<>();
        // 同一物料的多行赠品共用候选库存，逐行扣减
        Map<Long, List<Map<String, Object>>> candidates = new LinkedHashMap<>();
        for (ScmOrderItem g : giftItems) {
            if (Integer.valueOf(1).equals(g.getSerialEnabled())) {
                throw new BizException("scm.error.giftNeedsSerial", g.getMaterialCode(), g.getMaterialName());
            }
            List<Map<String, Object>> list = candidates.computeIfAbsent(g.getMaterialId(),
                    id -> new ArrayList<>(stockQueryService.pickCandidates(warehouseId, id).stream().map(LinkedHashMap::new).toList()));
            BigDecimal left = g.getQty();
            for (Map<String, Object> c : list) {
                BigDecimal qty = decimal(c.get("qty"));
                if (left.signum() <= 0 || qty.signum() <= 0) {
                    continue;
                }
                BigDecimal take = left.min(qty);
                c.put("qty", qty.subtract(take));
                Object loc = c.get("locationId");
                lines.add(line(g.getId(), loc == null ? null : ((Number) loc).longValue(), (String) c.get("batchNo"), take, null, null, null));
                left = left.subtract(take);
            }
            if (left.signum() > 0) {
                throw new BizException("scm.error.giftStockShort", g.getMaterialCode(), g.getMaterialName(), plain(g.getQty()),
                        plain(g.getQty().subtract(left)));
            }
        }
        return lines;
    }

    private static TradeDocItemRequest line(Long orderItemId, Long locationId, String batchNo, BigDecimal qty, String serials, String remark,
                                            Long warehouseId) {
        return new TradeDocItemRequest(orderItemId, locationId, batchNo, null, null, qty, serials, remark, null, null, null, warehouseId);
    }

    private static BigDecimal decimal(Object v) {
        return v == null ? BigDecimal.ZERO : v instanceof BigDecimal b ? b : new BigDecimal(v.toString());
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
