package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.period.BizPeriodGuard;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.dto.TransferReceiveRequest;
import com.lemon.erp.modules.wms.dto.TransferResolveRequest;
import com.lemon.erp.modules.wms.dto.TransferTransitView;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsStockDocItem;
import com.lemon.erp.modules.wms.entity.WmsTransferDiff;
import com.lemon.erp.modules.wms.entity.WmsTransferReceipt;
import com.lemon.erp.modules.wms.entity.WmsTransferReceiptItem;
import com.lemon.erp.modules.wms.mapper.WmsStockDocItemMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockDocMapper;
import com.lemon.erp.modules.wms.mapper.WmsTransferDiffMapper;
import com.lemon.erp.modules.wms.mapper.WmsTransferReceiptItemMapper;
import com.lemon.erp.modules.wms.mapper.WmsTransferReceiptMapper;
import com.lemon.erp.modules.wms.model.DocStatus;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockChange;
import com.lemon.erp.modules.wms.model.StockOutType;
import com.lemon.erp.modules.wms.model.TransferDiffType;
import com.lemon.erp.modules.wms.model.TransferResolution;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.wms.model.StockBizType.TRANSFER;
import static com.lemon.erp.modules.wms.model.WmsConstants.STOCK_DOC_RULE;
import static com.lemon.erp.modules.wms.model.WmsConstants.joinSerials;
import static com.lemon.erp.modules.wms.model.WmsConstants.splitSerials;

/**
 * 在途调拨的分批验收与差异处理（issue #124）。在途调拨确认时调出仓库扣减、数量挂在途（按调拨单明细跟踪），之后：
 * <ul>
 *   <li><b>调拨入库（验收）</b>：调入仓库引用调拨单分批验收，每次一张验收单（物流单号、车牌、备注），验收入库的数量进入调入仓库，
 *       按调出时的批次、序列号入库；同时可登记差异（少到 / 破损 / 错发 / 多发），差异数量不入调入仓库</li>
 *   <li><b>差异处理</b>：退回调出仓（数量回到调出仓库）、确认损耗（退回调出仓库后生成报废出库单，按调出成本计入损失）、
 *       让步接收（入调入仓库）。多发在实发之外：退回为实物退回、不变动库存，让步接收生成一张调出仓 → 调入仓的调拨单草稿</li>
 *   <li><b>处理未到量</b>：确有丢失（确认损耗）或未发出 / 召回（退回调出仓）时，由有权限的人关闭剩余的未到量</li>
 *   <li><b>关单</b>：实发 = 已验收 + 退回 + 损耗，且没有待处理的差异时自动完成；否则为在途 / 部分到货 / 验收异常，不能手工完成</li>
 * </ul>
 * 成本：在途期间仍属本公司存货（实时成本的计价数量不变），验收、退回、损耗都按调出时的成本单价计价；调拨价只是内部结算价。
 * 验收、退回、让步接收的流水按各自的操作日期统计（{@code wms_stock_flow.biz_date}），同一张单据的各次操作串行执行（单据行锁）
 */
@Service
@RequiredArgsConstructor
public class WmsTransferTransitService {

    /** 调拨验收单号的编码规则标识 */
    public static final String RECEIPT_NO = "DR";

    private final WmsStockDocMapper docMapper;
    private final WmsStockDocItemMapper itemMapper;
    private final WmsTransferReceiptMapper receiptMapper;
    private final WmsTransferReceiptItemMapper receiptItemMapper;
    private final WmsTransferDiffMapper diffMapper;
    private final DocNoService docNoService;
    private final StockService stockService;
    private final StockCostService costService;
    private final BizPeriodGuard periodGuard;
    private final StockDocValidator validator;
    private final WmsReferenceLoader refs;
    private final WmsStockDocService docService;

    /** 一行明细的一次处理：数量和序列号 */
    private record Part(WmsStockDocItem item, BigDecimal qty, List<String> serials) {
    }

    /** 验收单、差异、补发单 */
    public TransferTransitView records(Long id) {
        WmsStockDoc doc = accessible(id);
        Map<Long, List<WmsTransferReceiptItem>> lines = receiptItemMapper.selectByDocId(id).stream()
                .collect(Collectors.groupingBy(WmsTransferReceiptItem::getReceiptId, LinkedHashMap::new, Collectors.toList()));
        List<WmsTransferReceipt> receipts = receiptMapper.selectByDocId(id);
        receipts.forEach(r -> r.setItems(lines.getOrDefault(r.getId(), List.of())));
        List<WmsStockDoc> supplements = docMapper.selectByOrigin(doc.getId());
        Map<Long, List<String>> serials = new LinkedHashMap<>();
        if (doc.transit()) {
            availableSerials(id, items(id).values()).forEach((itemId, set) -> {
                if (!set.isEmpty()) {
                    serials.put(itemId, new ArrayList<>(set));
                }
            });
        }
        return new TransferTransitView(receipts, diffMapper.selectByDocId(id), supplements, serials);
    }

    /**
     * 调拨入库（验收）：本次验收入库的数量进入调入仓库（按调出成本计价），登记的差异挂起待处理
     *
     * @return 验收单 ID
     */
    @Transactional
    public Long receive(Long id, TransferReceiveRequest req) {
        WmsStockDoc doc = lockReceiving(id);
        LocalDate date = bizDate(doc, req.receiveDate(), false);
        validator.checkReceiveWarehouse(doc);
        Map<Long, WmsStockDocItem> items = items(id);
        Map<Long, WmsMaterial> materials = refs.materials(items.values().stream().map(WmsStockDocItem::getMaterialId).toList(), false);
        Map<Long, Set<String>> available = availableSerials(id, items.values());
        List<TransferReceiveRequest.Line> lines = req.lines() == null || req.lines().isEmpty()
                ? items.values().stream().filter(i -> i.getRemainingQty().signum() > 0)
                        .map(i -> new TransferReceiveRequest.Line(i.getId(), i.getRemainingQty(), null, null, null, null, null)).toList()
                : req.lines();

        List<Part> accepted = new ArrayList<>();
        List<WmsTransferDiff> diffs = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (TransferReceiveRequest.Line line : lines) {
            WmsStockDocItem item = items.get(line.itemId());
            if (item == null) {
                throw new BizException("wms.error.transferLineNotFound");
            }
            if (!seen.add(item.getId())) {
                throw new BizException("wms.error.transferLineDuplicated", lineName(item, materials));
            }
            try {
                checkLine(item, line, materials.get(item.getMaterialId()), available.get(item.getId()), accepted, diffs);
            } catch (BizException e) {
                throw new BizException("wms.error.transferLine", lineName(item, materials), e.getLocalizedMessage());
            }
        }
        BigDecimal totalQty = Decimals.sum(accepted, Part::qty);
        BigDecimal diffQty = Decimals.sum(diffs, WmsTransferDiff::getQty);
        if (totalQty.signum() == 0 && diffQty.signum() == 0) {
            throw new BizException("wms.error.enterReceiveQty");
        }

        WmsTransferReceipt receipt = new WmsTransferReceipt();
        receipt.setReceiptNo(docNoService.next(RECEIPT_NO));
        receipt.setDocId(id);
        receipt.setBizDate(date);
        receipt.setTotalQty(totalQty);
        receipt.setDiffQty(diffQty);
        receipt.setLogisticsNo(trim(req.logisticsNo()));
        receipt.setPlateNo(trim(req.plateNo()));
        receipt.setRemark(trim(req.remark()));
        receiptMapper.insert(receipt);
        if (!accepted.isEmpty()) {
            receiptItemMapper.insertBatch(accepted.stream().map(p -> {
                WmsTransferReceiptItem ri = new WmsTransferReceiptItem();
                ri.setReceiptId(receipt.getId());
                ri.setDocId(id);
                ri.setItemId(p.item().getId());
                ri.setMaterialId(p.item().getMaterialId());
                ri.setQty(p.qty());
                ri.setSerialNos(joinSerials(p.serials()));
                return ri;
            }).toList());
        }
        for (WmsTransferDiff d : diffs) {
            d.setReceiptId(receipt.getId());
            diffMapper.insert(d);
        }
        // 明细进度：验收入库、差异待处理（多发不占用未到量）
        Map<Long, BigDecimal> diffByItem = new HashMap<>();
        diffs.stream().filter(d -> !d.getDiffType().isOver()).forEach(d -> diffByItem.merge(d.getItemId(), d.getQty(), BigDecimal::add));
        Map<Long, BigDecimal> acceptedByItem = new HashMap<>();
        accepted.forEach(p -> acceptedByItem.merge(p.item().getId(), p.qty(), BigDecimal::add));
        Set<Long> touched = new HashSet<>(diffByItem.keySet());
        touched.addAll(acceptedByItem.keySet());
        for (Long itemId : touched) {
            itemMapper.addTransitQty(itemId, acceptedByItem.getOrDefault(itemId, BigDecimal.ZERO), diffByItem.getOrDefault(itemId, BigDecimal.ZERO),
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }
        toTarget(doc, accepted, date);
        docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate()
                .set(WmsStockDoc::getReceiveBy, SecurityUtils.getUserId())
                .set(WmsStockDoc::getReceiveDate, date)
                .set(WmsStockDoc::getReceiveTime, LocalDateTime.now())
                .eq(WmsStockDoc::getId, id));
        refreshStatus(doc);
        return receipt.getId();
    }

    /**
     * 处理差异：退回调出仓 / 确认损耗 / 让步接收。数量少于差异数量时拆分，剩余的继续待处理
     *
     * @return 处理生成的单据（确认损耗的报废出库单、多发让步接收的调拨单草稿），没有时为空
     */
    @Transactional
    public Long resolve(Long diffId, TransferResolveRequest req) {
        WmsTransferDiff found = diffMapper.selectById(diffId);
        if (found == null) {
            throw new BizException("wms.error.transferDiffNotFound");
        }
        WmsStockDoc doc = lockReceiving(found.getDocId());
        // 加锁后重新读取，防止并发重复处理
        WmsTransferDiff diff = diffMapper.selectById(diffId);
        if (!diff.pending()) {
            throw new BizException("wms.error.transferDiffResolved");
        }
        TransferResolution action = req.action();
        boolean over = diff.getDiffType().isOver();
        if (over && action == TransferResolution.LOSS) {
            throw new BizException("wms.error.overShipmentCannotLoss");
        }
        WmsStockDocItem item = itemMapper.selectById(diff.getItemId());
        WmsMaterial material = refs.materials(List.of(item.getMaterialId()), false).get(item.getMaterialId());
        BigDecimal qty = req.qty() == null ? diff.getQty() : req.qty();
        if (qty.compareTo(diff.getQty()) > 0) {
            throw new BizException("wms.error.resolveQtyExceedsDiff", plain(qty), plain(diff.getQty()));
        }
        List<String> diffSerials = splitSerials(diff.getSerialNos());
        List<String> serials = material.isSerialManaged() && !diffSerials.isEmpty()
                ? pickSerials(req.serialNos(), qty, new LinkedHashSet<>(diffSerials)) : List.of();
        LocalDate date = bizDate(doc, req.bizDate(), action == TransferResolution.LOSS && !over);

        // 部分处理：剩余的另存一条待处理
        if (qty.compareTo(diff.getQty()) < 0) {
            WmsTransferDiff rest = new WmsTransferDiff();
            rest.setDocId(diff.getDocId());
            rest.setItemId(diff.getItemId());
            rest.setReceiptId(diff.getReceiptId());
            rest.setMaterialId(diff.getMaterialId());
            rest.setDiffType(diff.getDiffType());
            rest.setQty(diff.getQty().subtract(qty));
            List<String> left = new ArrayList<>(diffSerials);
            left.removeAll(serials);
            rest.setSerialNos(joinSerials(serials.isEmpty() ? List.of() : left));
            rest.setRemark(diff.getRemark());
            diffMapper.insert(rest);
        }
        Part part = new Part(item, qty, serials);
        Long refDocId = null;
        if (over) {
            // 多发不在本单的实发中：退回为实物退回（不变动库存），让步接收生成调拨单草稿由调出方确认
            if (action == TransferResolution.ACCEPT) {
                refDocId = overTransfer(doc, part, date, req.remark());
            }
        } else {
            switch (action) {
                case RETURN -> {
                    toSource(doc, List.of(part), date);
                    itemMapper.addTransitQty(item.getId(), BigDecimal.ZERO, qty.negate(), qty, BigDecimal.ZERO);
                }
                case LOSS -> {
                    refDocId = loss(doc, List.of(part), date, req.remark());
                    itemMapper.addTransitQty(item.getId(), BigDecimal.ZERO, qty.negate(), BigDecimal.ZERO, qty);
                }
                case ACCEPT -> {
                    validator.checkReceiveWarehouse(doc);
                    toTarget(doc, List.of(part), date);
                    itemMapper.addTransitQty(item.getId(), qty, qty.negate(), BigDecimal.ZERO, BigDecimal.ZERO);
                }
                default -> throw new IllegalArgumentException(action.name());
            }
        }
        diffMapper.update(Wrappers.<WmsTransferDiff>lambdaUpdate()
                .set(WmsTransferDiff::getQty, qty)
                .set(WmsTransferDiff::getSerialNos, serials.isEmpty() ? diff.getSerialNos() : joinSerials(serials))
                .set(WmsTransferDiff::getResolution, action)
                .set(WmsTransferDiff::getResolveDate, date)
                .set(WmsTransferDiff::getResolveBy, SecurityUtils.getUserId())
                .set(WmsTransferDiff::getResolveTime, LocalDateTime.now())
                .set(WmsTransferDiff::getResolveRemark, trim(req.remark()))
                .set(WmsTransferDiff::getRefDocId, refDocId)
                .set(WmsTransferDiff::getUpdateTime, LocalDateTime.now())
                .eq(WmsTransferDiff::getId, diffId));
        refreshStatus(doc);
        return refDocId;
    }

    /**
     * 处理未到量：全部明细剩余的未到数量退回调出仓（未发出 / 召回），或确认损耗（在途丢失，生成报废出库单），
     * 各生成一条已处理的差异记录
     *
     * @return 确认损耗生成的报废出库单，退回时为空
     */
    @Transactional
    public Long closeRemaining(Long id, TransferResolveRequest req) {
        TransferResolution action = req.action();
        if (action == TransferResolution.ACCEPT) {
            throw new BizException("wms.error.remainingCannotAccept");
        }
        WmsStockDoc doc = lockReceiving(id);
        LocalDate date = bizDate(doc, req.bizDate(), action == TransferResolution.LOSS);
        Map<Long, WmsStockDocItem> items = items(id);
        Map<Long, Set<String>> available = availableSerials(id, items.values());
        List<Part> parts = new ArrayList<>();
        for (WmsStockDocItem item : items.values()) {
            BigDecimal rest = item.getRemainingQty();
            if (rest.signum() > 0) {
                List<String> serials = new ArrayList<>(available.getOrDefault(item.getId(), Set.of()));
                parts.add(new Part(item, rest, serials.size() > rest.intValue() ? serials.subList(0, rest.intValue()) : serials));
            }
        }
        if (parts.isEmpty()) {
            throw new BizException("wms.error.transferNoRemaining", doc.getDocNo());
        }
        Long refDocId = action == TransferResolution.LOSS ? loss(doc, parts, date, req.remark()) : null;
        if (action == TransferResolution.RETURN) {
            toSource(doc, parts, date);
        }
        LocalDateTime now = LocalDateTime.now();
        for (Part p : parts) {
            WmsTransferDiff d = new WmsTransferDiff();
            d.setDocId(id);
            d.setItemId(p.item().getId());
            d.setMaterialId(p.item().getMaterialId());
            d.setDiffType(action == TransferResolution.LOSS ? TransferDiffType.LOST : TransferDiffType.SHORT);
            d.setQty(p.qty());
            d.setSerialNos(joinSerials(p.serials()));
            d.setResolution(action);
            d.setResolveDate(date);
            d.setResolveBy(SecurityUtils.getUserId());
            d.setResolveTime(now);
            d.setResolveRemark(trim(req.remark()));
            d.setRefDocId(refDocId);
            diffMapper.insert(d);
            boolean lost = action == TransferResolution.LOSS;
            itemMapper.addTransitQty(p.item().getId(), BigDecimal.ZERO, BigDecimal.ZERO,
                    lost ? BigDecimal.ZERO : p.qty(), lost ? p.qty() : BigDecimal.ZERO);
        }
        refreshStatus(doc);
        return refDocId;
    }

    // ---------------------------------------------------------------- 校验

    /**
     * 校验一行验收：验收 + 差异（多发除外）不超过未到量，序列号从未到的序列号中选择（不填按顺序取）
     */
    private void checkLine(WmsStockDocItem item, TransferReceiveRequest.Line line, WmsMaterial material, Set<String> available,
                           List<Part> accepted, List<WmsTransferDiff> diffs) {
        BigDecimal qty = line.qty() == null ? BigDecimal.ZERO : line.qty();
        BigDecimal diffQty = line.diffQty() == null ? BigDecimal.ZERO : line.diffQty();
        TransferDiffType type = line.diffType();
        if (diffQty.signum() > 0 && type == null) {
            throw new BizException("wms.error.selectDiffType");
        }
        if (type == TransferDiffType.LOST) {
            throw new BizException("wms.error.lostOnlyForRemaining");
        }
        boolean over = type != null && type.isOver();
        BigDecimal used = qty.add(over ? BigDecimal.ZERO : diffQty);
        BigDecimal remaining = item.getRemainingQty();
        if (used.compareTo(remaining) > 0) {
            throw new BizException("wms.error.receiveExceedsRemaining", plain(used), plain(remaining));
        }
        boolean serial = material.isSerialManaged();
        if (serial && (qty.stripTrailingZeros().scale() > 0 || diffQty.stripTrailingZeros().scale() > 0)) {
            throw new BizException("wms.error.quantitySerialManagedMaterialsMustInteger");
        }
        Set<String> pool = new LinkedHashSet<>(available == null ? Set.of() : available);
        if (qty.signum() > 0) {
            List<String> serials = serial ? pickSerials(line.serialNos(), qty, pool) : List.of();
            serials.forEach(pool::remove);
            accepted.add(new Part(item, qty, serials));
        }
        if (diffQty.signum() > 0) {
            List<String> serials;
            if (!serial) {
                serials = List.of();
            } else if (over) {
                // 多发的序列号不在调拨单上，可不填；填写的不能是本单的序列号
                serials = splitSerials(line.diffSerialNos());
                Set<String> own = new HashSet<>(splitSerials(item.getSerialNos()));
                serials.stream().filter(own::contains).findFirst().ifPresent(sn -> {
                    throw new BizException("wms.error.overSerialInDoc", sn);
                });
                if (serials.size() > diffQty.intValue() || new HashSet<>(serials).size() != serials.size()) {
                    throw new BizException("wms.error.numberSerialNumbersDoesNotMatch", serials.size(), diffQty.intValue());
                }
            } else {
                serials = pickSerials(line.diffSerialNos(), diffQty, pool);
            }
            WmsTransferDiff d = new WmsTransferDiff();
            d.setDocId(item.getDocId());
            d.setItemId(item.getId());
            d.setMaterialId(item.getMaterialId());
            d.setDiffType(type);
            d.setQty(diffQty);
            d.setSerialNos(joinSerials(serials));
            d.setRemark(trim(line.diffRemark()));
            diffs.add(d);
        }
    }

    /**
     * 从可选的序列号中选出 qty 个：填写了的须个数一致、都在可选范围内且不重复；没填的按顺序取
     */
    private static List<String> pickSerials(String text, BigDecimal qty, Set<String> pool) {
        int count = qty.intValue();
        List<String> given = splitSerials(text);
        if (given.isEmpty()) {
            if (pool.size() < count) {
                throw new BizException("wms.error.numberSerialNumbersDoesNotMatch", pool.size(), count);
            }
            return pool.stream().limit(count).toList();
        }
        if (given.size() != count) {
            throw new BizException("wms.error.numberSerialNumbersDoesNotMatch", given.size(), count);
        }
        Set<String> unique = new HashSet<>();
        for (String sn : given) {
            if (!pool.contains(sn) || !unique.add(sn)) {
                throw new BizException("wms.error.serialNotInTransit", sn);
            }
        }
        return given;
    }

    /**
     * 各明细还在途、可验收或登记差异的序列号：调出的序列号去掉已验收的和登记差异的（多发的除外），保持调出时的顺序
     */
    private Map<Long, Set<String>> availableSerials(Long docId, java.util.Collection<WmsStockDocItem> items) {
        Map<Long, Set<String>> used = new HashMap<>();
        receiptItemMapper.selectList(Wrappers.<WmsTransferReceiptItem>lambdaQuery()
                        .select(WmsTransferReceiptItem::getItemId, WmsTransferReceiptItem::getSerialNos)
                        .eq(WmsTransferReceiptItem::getDocId, docId))
                .forEach(r -> used.computeIfAbsent(r.getItemId(), k -> new HashSet<>()).addAll(splitSerials(r.getSerialNos())));
        diffMapper.selectList(Wrappers.<WmsTransferDiff>lambdaQuery()
                        .select(WmsTransferDiff::getItemId, WmsTransferDiff::getDiffType, WmsTransferDiff::getSerialNos)
                        .eq(WmsTransferDiff::getDocId, docId))
                .stream().filter(d -> !d.getDiffType().isOver())
                .forEach(d -> used.computeIfAbsent(d.getItemId(), k -> new HashSet<>()).addAll(splitSerials(d.getSerialNos())));
        Map<Long, Set<String>> map = new HashMap<>();
        for (WmsStockDocItem item : items) {
            Set<String> set = new LinkedHashSet<>(splitSerials(item.getSerialNos()));
            set.removeAll(used.getOrDefault(item.getId(), Set.of()));
            map.put(item.getId(), set);
        }
        return map;
    }

    /**
     * 操作日期：为空取当天，不能早于调拨日期；退回、验收按调拨（只影响总账）做结账校验，损耗影响存货成本按出库校验
     */
    private LocalDate bizDate(WmsStockDoc doc, LocalDate given, boolean affectsCost) {
        LocalDate date = given == null ? LocalDate.now() : given;
        if (date.isBefore(doc.getBizDate())) {
            throw new BizException("wms.error.receiveDateBeforeTransferDate", date, doc.getBizDate());
        }
        if (affectsCost) {
            periodGuard.checkStockOpen(date, "wms.label.transferLoss");
        } else {
            periodGuard.checkOpen(date, "wms.label.transferReceipt");
        }
        return date;
    }

    // ---------------------------------------------------------------- 库存变动

    /** 入调入仓库：调入库位、调出的批次，按调出成本计价，业务单价为调拨价 */
    private void toTarget(WmsStockDoc doc, List<Part> parts, LocalDate date) {
        move(doc, parts, date, doc.getToWarehouseId(), true);
    }

    /** 退回调出仓库：调出库位、调出的批次，按调出成本计价 */
    private void toSource(WmsStockDoc doc, List<Part> parts, LocalDate date) {
        move(doc, parts, date, doc.getWarehouseId(), false);
    }

    private void move(WmsStockDoc doc, List<Part> parts, LocalDate date, Long warehouseId, boolean target) {
        if (parts.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> costs = costService.transferOutCosts(doc.getId(),
                parts.stream().map(p -> p.item().getMaterialId()).collect(Collectors.toSet()));
        stockService.apply(TRANSFER, doc.getId(), doc.getDocNo(), parts.stream()
                .map(p -> StockChange.of(warehouseId, target ? p.item().getToLocationId() : p.item().getLocationId(),
                                p.item().getMaterialId(), p.item().getBatchNo(), p.qty(), p.serials())
                        .withCost(costs.get(p.item().getMaterialId()), p.item().getTransferPrice(), null, null))
                .toList(), date);
    }

    /**
     * 确认损耗：在途数量退回调出仓库，再生成报废出库单（来源单号为调拨单号）按调出成本出库，计入损失（成本凭证按报废出库处理）
     *
     * @return 报废出库单 ID
     */
    private Long loss(WmsStockDoc doc, List<Part> parts, LocalDate date, String remark) {
        toSource(doc, parts, date);
        Map<Long, BigDecimal> costs = costService.transferOutCosts(doc.getId(),
                parts.stream().map(p -> p.item().getMaterialId()).collect(Collectors.toSet()));
        List<StockDocItemRequest> lines = parts.stream().map(p -> new StockDocItemRequest(p.item().getMaterialId(),
                p.item().getLocationId(), null, p.item().getBatchNo(), null, null, p.qty(), joinSerials(p.serials()), null, null,
                costs.get(p.item().getMaterialId()), null)).toList();
        String text = "调拨损耗 " + doc.getDocNo() + (remark == null || remark.isBlank() ? "" : " " + remark.trim()); // i18n-ignore 存入单据备注
        WmsStockDoc out = docService.createConfirmed(StockBizType.OUT, new StockDocRequest(StockOutType.SCRAP, doc.getWarehouseId(), null,
                date, text.length() > 255 ? text.substring(0, 255) : text, lines), doc.getDocNo());
        return out.getId();
    }

    /** 多发让步接收：生成调出仓 → 调入仓的调拨单草稿（直接调拨，关联原单），由调出方确认后调出仓扣减、调入仓入库 */
    private Long overTransfer(WmsStockDoc doc, Part part, LocalDate date, String remark) {
        WmsStockDocItem item = part.item();
        String text = "多发补调 " + doc.getDocNo() + (remark == null || remark.isBlank() ? "" : " " + remark.trim()); // i18n-ignore 存入单据备注
        StockDocRequest req = new StockDocRequest(null, doc.getWarehouseId(), doc.getToWarehouseId(), date,
                text.length() > 255 ? text.substring(0, 255) : text,
                List.of(new StockDocItemRequest(item.getMaterialId(), item.getLocationId(), item.getToLocationId(), item.getBatchNo(),
                        null, null, part.qty(), joinSerials(part.serials()), null)),
                false, null, null, null, null, null, null, null, null, null, null, null, doc.getId());
        return docService.create(TRANSFER, req);
    }

    // ---------------------------------------------------------------- 内部

    /**
     * 按当前进度更新状态：有待处理的差异为验收异常；没有未到量为已完成；已有验收 / 退回 / 损耗为部分到货；否则在途
     */
    private void refreshStatus(WmsStockDoc doc) {
        List<WmsStockDocItem> items = itemMapper.selectList(Wrappers.<WmsStockDocItem>lambdaQuery().eq(WmsStockDocItem::getDocId, doc.getId()));
        boolean pending = diffMapper.exists(Wrappers.<WmsTransferDiff>lambdaQuery()
                .eq(WmsTransferDiff::getDocId, doc.getId()).isNull(WmsTransferDiff::getResolution));
        BigDecimal remaining = Decimals.sum(items, WmsStockDocItem::getRemainingQty);
        boolean progressed = items.stream().anyMatch(i -> i.getReceivedQty().signum() != 0 || i.getReturnedQty().signum() != 0
                || i.getLostQty().signum() != 0);
        DocStatus status = pending ? DocStatus.EXCEPTION
                : remaining.signum() == 0 ? DocStatus.DONE
                : progressed ? DocStatus.PARTIAL : DocStatus.TRANSIT;
        docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate()
                .set(WmsStockDoc::getStatus, status)
                .set(WmsStockDoc::getUpdateTime, LocalDateTime.now())
                .eq(WmsStockDoc::getId, doc.getId()));
    }

    /** 在数据权限范围内的调拨单 */
    private WmsStockDoc accessible(Long id) {
        return DataScopes.require(STOCK_DOC_RULE, () -> docMapper.selectById(id), doc -> doc.getKind() == TRANSFER,
                "wms.error.doesNotExistAccessDenied", Messages.get(TRANSFER.getLabelKey()));
    }

    /** 数据权限内、未完结的在途调拨单，加行锁 */
    private WmsStockDoc lockReceiving(Long id) {
        accessible(id);
        WmsStockDoc doc = docMapper.selectForUpdate(id);
        if (!doc.transit() || !doc.getStatus().isReceiving()) {
            throw new BizException("wms.error.transferOrderNotTransit", doc.getDocNo());
        }
        return doc;
    }

    private Map<Long, WmsStockDocItem> items(Long docId) {
        return itemMapper.selectList(Wrappers.<WmsStockDocItem>lambdaQuery()
                        .eq(WmsStockDocItem::getDocId, docId).orderByAsc(WmsStockDocItem::getId)).stream()
                .collect(Collectors.toMap(WmsStockDocItem::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private static String lineName(WmsStockDocItem item, Map<Long, WmsMaterial> materials) {
        String name = StockService.describe(materials.get(item.getMaterialId()));
        return item.getBatchNo() == null || item.getBatchNo().isEmpty() ? name : name + " " + Messages.get("wms.text.batch", item.getBatchNo());
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
