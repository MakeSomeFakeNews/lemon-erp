package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.period.BizPeriodGuard;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.wms.dto.CheckItemRequest;
import com.lemon.erp.modules.wms.dto.CheckRequest;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.dto.StockQuery;
import com.lemon.erp.modules.wms.dto.StockView;
import com.lemon.erp.modules.wms.entity.WmsCheck;
import com.lemon.erp.modules.wms.entity.WmsCheckItem;
import com.lemon.erp.modules.wms.entity.WmsLocation;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsSerial;
import com.lemon.erp.modules.wms.entity.WmsStock;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.mapper.WmsCheckItemMapper;
import com.lemon.erp.modules.wms.mapper.WmsCheckMapper;
import com.lemon.erp.modules.wms.mapper.WmsSerialMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockMapper;
import com.lemon.erp.modules.wms.model.DocStatus;
import com.lemon.erp.modules.wms.model.SerialStatus;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockChange;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.wms.model.WmsConstants.CHECK_RULE;
import static com.lemon.erp.modules.wms.model.WmsConstants.NO_BATCH;
import static com.lemon.erp.modules.wms.model.WmsConstants.batchOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.joinSerials;
import static com.lemon.erp.modules.wms.model.WmsConstants.locationOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.splitSerials;

/**
 * 盘点单：按仓库当前库存生成明细，录入实盘数量后确认，库存调整为实盘数量。
 * <ul>
 *   <li>账面数量以确认时的库存为准（草稿中显示的是最近一次保存时的库存），确认后库存 = 实盘数量</li>
 *   <li>序列号物料按实盘序列号盘点：生成明细时带出在库序列号，确认时多出的序列号盘盈入库、缺少的盘亏出库</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class WmsCheckService {

    private final WmsCheckMapper checkMapper;
    private final WmsCheckItemMapper itemMapper;
    private final WmsStockMapper stockMapper;
    private final WmsSerialMapper serialMapper;
    private final WmsReferenceLoader refs;
    private final DocNoService docNoService;
    private final StockService stockService;
    private final BizPeriodGuard periodGuard;
    private final FlowApi flowApi;

    public static final String PROCESS_KEY = "wms_check";
    private static final String BUSINESS_KEY_PREFIX = "wms_check:";

    @DataPermission(table = "wms_check", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<WmsCheck> page(long current, long size, DocQuery query) {
        Page<WmsCheck> page = Page.of(current, size);
        checkMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public WmsCheck detail(Long id) {
        getAccessible(id);
        WmsCheck check = checkMapper.selectDetail(id);
        check.setItems(itemMapper.selectByCheckId(id));
        return check;
    }

    /**
     * 按当前库存生成盘点明细（不保存），序列号物料带出在库序列号
     *
     * @param locationId 只盘点该库位，可为空
     * @param keyword    物料编码或名称，可为空
     */
    public List<WmsCheckItem> snapshot(Long warehouseId, Long locationId, String keyword, String category) {
        refs.warehouse(warehouseId, "盘点仓库");
        List<StockView> stocks = stockMapper.selectStockList(
                new StockQuery(warehouseId, locationId, null, keyword, null, category, false, null));
        Set<Long> serialMaterialIds = stocks.stream().filter(s -> Integer.valueOf(1).equals(s.getSerialEnabled()))
                .map(StockView::getMaterialId).collect(Collectors.toSet());
        Map<String, Set<String>> serials = inStockSerials(warehouseId, serialMaterialIds);
        return stocks.stream().map(s -> {
            WmsCheckItem item = new WmsCheckItem();
            item.setMaterialId(s.getMaterialId());
            item.setMaterialCode(s.getMaterialCode());
            item.setMaterialName(s.getMaterialName());
            item.setSpec(s.getSpec());
            item.setUnit(s.getUnit());
            item.setBatchEnabled(s.getBatchEnabled());
            item.setSerialEnabled(s.getSerialEnabled());
            item.setLocationId(s.getLocationId());
            item.setLocationCode(s.getLocationCode());
            item.setBatchNo(s.getBatchNo());
            item.setBookQty(s.getQty());
            Set<String> slotSerials = serials.get(slotKey(s.getLocationId(), s.getMaterialId(), s.getBatchNo()));
            item.setSerialNos(slotSerials == null ? null : joinSerials(List.copyOf(slotSerials)));
            return item;
        }).toList();
    }

    @Transactional
    public Long create(CheckRequest req) {
        List<WmsCheckItem> items = toItems(req.items());
        validate(req.warehouseId(), items, false);
        fillBookQty(req.warehouseId(), items);
        LoginUser user = SecurityUtils.getLoginUser();
        WmsCheck check = new WmsCheck();
        check.setDocNo(docNoService.next(StockBizType.CHECK));
        check.setWarehouseId(req.warehouseId());
        check.setBizDate(req.bizDate());
        check.setRemark(req.remark());
        check.setFreeze(Boolean.TRUE.equals(req.freeze()) ? 1 : 0);
        check.setStatus(DocStatus.DRAFT.name());
        check.setDeptId(user.deptId());
        check.setCreateBy(user.userId());
        checkMapper.insert(check);
        saveItems(check.getId(), items);
        return check.getId();
    }

    @Transactional
    public void update(Long id, CheckRequest req) {
        getDraft(id);
        List<WmsCheckItem> items = toItems(req.items());
        validate(req.warehouseId(), items, false);
        fillBookQty(req.warehouseId(), items);
        checkMapper.update(Wrappers.<WmsCheck>lambdaUpdate()
                .set(WmsCheck::getWarehouseId, req.warehouseId())
                .set(WmsCheck::getBizDate, req.bizDate())
                .set(WmsCheck::getRemark, req.remark())
                .set(WmsCheck::getFreeze, Boolean.TRUE.equals(req.freeze()) ? 1 : 0)
                .set(WmsCheck::getUpdateTime, LocalDateTime.now())
                .eq(WmsCheck::getId, id)
                .eq(WmsCheck::getStatus, DocStatus.DRAFT.name()));
        itemMapper.delete(Wrappers.<WmsCheckItem>lambdaQuery().eq(WmsCheckItem::getCheckId, id));
        saveItems(id, items);
    }

    @Transactional
    public void delete(List<Long> ids) {
        ids.forEach(this::getDraft);
        itemMapper.delete(Wrappers.<WmsCheckItem>lambdaQuery().in(WmsCheckItem::getCheckId, ids));
        checkMapper.deleteByIds(ids);
    }

    /**
     * 确认盘点：发布了「盘点差异审批」流程且有差异时先审批，审批通过后调整库存；否则直接调整
     *
     * @return true 已发起审批
     */
    @Transactional
    public boolean confirm(Long id) {
        WmsCheck check = getDraft(id);
        periodGuard.checkStockOpen(check.getBizDate(), "盘点单");
        if (flowApi.isAvailable(PROCESS_KEY)) {
            List<WmsCheckItem> items = itemMapper.selectList(Wrappers.<WmsCheckItem>lambdaQuery().eq(WmsCheckItem::getCheckId, id));
            validate(check.getWarehouseId(), items, true);
            fillBookQty(check.getWarehouseId(), items);
            BigDecimal gain = BigDecimal.ZERO;
            BigDecimal loss = BigDecimal.ZERO;
            int diffLines = 0;
            for (WmsCheckItem i : items) {
                BigDecimal diff = i.getActualQty().subtract(i.getBookQty());
                if (diff.signum() != 0) {
                    diffLines++;
                }
                gain = diff.signum() > 0 ? gain.add(diff) : gain;
                loss = diff.signum() < 0 ? loss.subtract(diff) : loss;
            }
            if (diffLines > 0) {
                if (checkMapper.update(Wrappers.<WmsCheck>lambdaUpdate().set(WmsCheck::getStatus, DocStatus.APPROVING.name())
                        .eq(WmsCheck::getId, id).eq(WmsCheck::getStatus, DocStatus.DRAFT.name())) == 0) {
                    throw new BizException("盘点单已确认，请刷新");
                }
                String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + id,
                        Map.of("docNo", check.getDocNo(), "gainQty", gain, "lossQty", loss, "diffLines", diffLines));
                checkMapper.update(Wrappers.<WmsCheck>lambdaUpdate().set(WmsCheck::getFlowInstanceId, instanceId).eq(WmsCheck::getId, id));
                return true;
            }
        }
        adjust(check, DocStatus.DRAFT);
        return false;
    }

    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        Long id = Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length()));
        WmsCheck check = checkMapper.selectById(id);
        if (check == null || !DocStatus.APPROVING.name().equals(check.getStatus())) {
            return;
        }
        if (e.status() == FlowStatus.APPROVED) {
            adjust(check, DocStatus.APPROVING);
        } else {
            checkMapper.update(Wrappers.<WmsCheck>lambdaUpdate().set(WmsCheck::getStatus, DocStatus.DRAFT.name())
                    .eq(WmsCheck::getId, id).eq(WmsCheck::getStatus, DocStatus.APPROVING.name()));
        }
    }

    /** 按确认时的账面数量计算差异，盘盈入库、盘亏出库，库存调整为实盘数量 */
    private void adjust(WmsCheck check, DocStatus from) {
        Long id = check.getId();
        int updated = checkMapper.update(Wrappers.<WmsCheck>lambdaUpdate()
                .set(WmsCheck::getStatus, DocStatus.DONE.name())
                .set(WmsCheck::getConfirmBy, SecurityUtils.getUserId())
                .set(WmsCheck::getConfirmTime, LocalDateTime.now())
                .set(WmsCheck::getUpdateTime, LocalDateTime.now())
                .eq(WmsCheck::getId, id)
                .eq(WmsCheck::getStatus, from.name()));
        if (updated == 0) {
            throw new BizException("盘点单已确认，请刷新");
        }
        Long warehouseId = check.getWarehouseId();
        List<WmsCheckItem> items = itemMapper.selectList(Wrappers.<WmsCheckItem>lambdaQuery()
                .eq(WmsCheckItem::getCheckId, id).orderByAsc(WmsCheckItem::getId));
        Map<Long, WmsMaterial> materials = validate(warehouseId, items, true);
        fillBookQty(warehouseId, items);

        Set<Long> serialMaterialIds = materials.values().stream().filter(WmsMaterial::isSerialManaged)
                .map(WmsMaterial::getId).collect(Collectors.toSet());
        Map<String, Set<String>> inStock = inStockSerials(warehouseId, serialMaterialIds);
        List<StockChange> changes = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            WmsCheckItem item = items.get(i);
            WmsMaterial material = materials.get(item.getMaterialId());
            BigDecimal diff = item.getActualQty().subtract(item.getBookQty());
            if (!material.isSerialManaged()) {
                changes.add(StockChange.of(warehouseId, item.getLocationId(), item.getMaterialId(), item.getBatchNo(), diff, List.of()));
                continue;
            }
            Set<String> counted = new LinkedHashSet<>(splitSerials(item.getSerialNos()));
            Set<String> book = inStock.getOrDefault(slotKey(item.getLocationId(), item.getMaterialId(), item.getBatchNo()), Set.of());
            List<String> gain = counted.stream().filter(sn -> !book.contains(sn)).toList();
            List<String> loss = book.stream().filter(sn -> !counted.contains(sn)).toList();
            if (diff.compareTo(BigDecimal.valueOf(gain.size() - loss.size())) != 0) {
                throw new BizException("第 " + (i + 1) + " 行（" + StockService.describe(material) + "）：账面数量（"
                        + item.getBookQty().stripTrailingZeros().toPlainString() + "）与在库序列号个数（" + book.size() + "）不一致");
            }
            if (!loss.isEmpty()) {
                changes.add(StockChange.of(warehouseId, item.getLocationId(), item.getMaterialId(), item.getBatchNo(),
                        BigDecimal.valueOf(-loss.size()), loss));
            }
            if (!gain.isEmpty()) {
                changes.add(StockChange.of(warehouseId, item.getLocationId(), item.getMaterialId(), item.getBatchNo(),
                        BigDecimal.valueOf(gain.size()), gain));
            }
        }
        // 保存确认时的账面数量
        itemMapper.delete(Wrappers.<WmsCheckItem>lambdaQuery().eq(WmsCheckItem::getCheckId, id));
        saveItems(id, items);
        stockService.apply(StockBizType.CHECK, id, check.getDocNo(), changes);
    }

    // ---------------------------------------------------------------- 校验

    private WmsCheck getAccessible(Long id) {
        Supplier<WmsCheck> loader = () -> checkMapper.selectById(id);
        WmsCheck check = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(CHECK_RULE, loader);
        if (check == null) {
            throw new BizException("盘点单不存在或没有权限");
        }
        return check;
    }

    private WmsCheck getDraft(Long id) {
        WmsCheck check = getAccessible(id);
        if (!DocStatus.DRAFT.name().equals(check.getStatus())) {
            throw new BizException("盘点单 " + check.getDocNo() + " 已确认，不能修改或删除");
        }
        return check;
    }

    private static List<WmsCheckItem> toItems(List<CheckItemRequest> requests) {
        return requests.stream().map(r -> {
            WmsCheckItem item = new WmsCheckItem();
            item.setMaterialId(r.materialId());
            item.setLocationId(locationOrNone(r.locationId()));
            item.setBatchNo(batchOrNone(r.batchNo()));
            item.setActualQty(r.actualQty());
            item.setSerialNos(joinSerials(splitSerials(r.serialNos())));
            item.setRemark(r.remark());
            return item;
        }).toList();
    }

    /**
     * 校验明细并规范批次、序列号；同一库存位置（库位 + 物料 + 批次）只能出现一次
     *
     * @param strict 确认时为 true：必须填写全部实盘数量
     */
    private Map<Long, WmsMaterial> validate(Long warehouseId, List<WmsCheckItem> items, boolean strict) {
        if (items.isEmpty()) {
            throw new BizException("请添加盘点明细");
        }
        WmsWarehouse warehouse = refs.warehouse(warehouseId, "盘点仓库");
        // 盘点允许停用的物料、库位：它们可能还有库存
        Map<Long, WmsMaterial> materials = refs.materials(items.stream().map(WmsCheckItem::getMaterialId).toList(), false);
        Map<Long, WmsLocation> locations = refs.locations(items.stream().map(WmsCheckItem::getLocationId).toList());
        Set<String> slots = new HashSet<>();
        Map<Long, Set<String>> serialsByMaterial = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            WmsCheckItem item = items.get(i);
            WmsMaterial material = materials.get(item.getMaterialId());
            try {
                WmsReferenceLoader.checkLocation(locations, item.getLocationId(), warehouse, false);
                if (!material.isBatchManaged()) {
                    item.setBatchNo(NO_BATCH);
                } else if (item.getBatchNo().isEmpty()) {
                    throw new BizException("物料启用了批次管理，请填写批次号");
                }
                if (!slots.add(slotKey(item.getLocationId(), item.getMaterialId(), item.getBatchNo()))) {
                    throw new BizException("同一库位、物料、批次的明细重复");
                }
                if (item.getActualQty() == null) {
                    if (strict) {
                        throw new BizException("请填写实盘数量");
                    }
                    item.setSerialNos(null);
                    continue;
                }
                List<String> serials = WmsStockDocService.checkSerials(item.getSerialNos(), item.getActualQty(), material, true,
                        serialsByMaterial.computeIfAbsent(material.getId(), k -> new HashSet<>()));
                item.setSerialNos(joinSerials(serials));
            } catch (BizException e) {
                throw new BizException("第 " + (i + 1) + " 行（" + StockService.describe(material) + "）：" + e.getMessage());
            }
        }
        return materials;
    }

    /** 账面数量取当前库存 */
    private void fillBookQty(Long warehouseId, List<WmsCheckItem> items) {
        Map<String, BigDecimal> stock = stockMapper.selectList(Wrappers.<WmsStock>lambdaQuery()
                        .eq(WmsStock::getWarehouseId, warehouseId)
                        .in(WmsStock::getMaterialId, items.stream().map(WmsCheckItem::getMaterialId).distinct().toList()))
                .stream().collect(Collectors.toMap(s -> slotKey(s.getLocationId(), s.getMaterialId(), s.getBatchNo()), WmsStock::getQty));
        items.forEach(i -> i.setBookQty(stock.getOrDefault(slotKey(i.getLocationId(), i.getMaterialId(), i.getBatchNo()), BigDecimal.ZERO)));
    }

    /** 仓库中这些物料的在库序列号，按库存位置分组 */
    private Map<String, Set<String>> inStockSerials(Long warehouseId, Collection<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        return serialMapper.selectList(Wrappers.<WmsSerial>lambdaQuery()
                        .select(WmsSerial::getMaterialId, WmsSerial::getSerialNo, WmsSerial::getLocationId, WmsSerial::getBatchNo)
                        .eq(WmsSerial::getWarehouseId, warehouseId)
                        .eq(WmsSerial::getStatus, SerialStatus.IN_STOCK.name())
                        .in(WmsSerial::getMaterialId, materialIds)
                        .orderByAsc(WmsSerial::getSerialNo))
                .stream().collect(Collectors.groupingBy(s -> slotKey(s.getLocationId(), s.getMaterialId(), s.getBatchNo()),
                        Collectors.mapping(WmsSerial::getSerialNo, Collectors.toCollection(LinkedHashSet::new))));
    }

    private void saveItems(Long checkId, List<WmsCheckItem> items) {
        items.forEach(i -> i.setCheckId(checkId));
        itemMapper.insertBatch(items);
    }

    private static String slotKey(Long locationId, Long materialId, String batchNo) {
        return locationId + "\n" + materialId + "\n" + batchNo;
    }
}
