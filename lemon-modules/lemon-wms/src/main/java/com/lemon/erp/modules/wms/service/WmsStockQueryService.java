package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.wms.dto.BatchQuery;
import com.lemon.erp.modules.wms.dto.InOutQuery;
import com.lemon.erp.modules.wms.dto.InOutRow;
import com.lemon.erp.modules.wms.mapper.WmsInOutMapper;
import com.lemon.erp.modules.wms.dto.BatchView;
import com.lemon.erp.modules.wms.dto.FlowQuery;
import com.lemon.erp.modules.wms.dto.SerialLogView;
import com.lemon.erp.modules.wms.dto.SerialQuery;
import com.lemon.erp.modules.wms.dto.SerialView;
import com.lemon.erp.modules.wms.dto.StockFlowView;
import com.lemon.erp.modules.wms.dto.StockHistoryQuery;
import com.lemon.erp.modules.wms.dto.StockHistoryRow;
import com.lemon.erp.modules.wms.dto.StockHistoryView;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.wms.dto.StockQuery;
import com.lemon.erp.modules.wms.dto.StockSummaryView;
import com.lemon.erp.modules.wms.dto.StockView;
import com.lemon.erp.modules.wms.entity.WmsSerial;
import com.lemon.erp.modules.wms.mapper.WmsBatchMapper;
import com.lemon.erp.modules.wms.mapper.WmsSerialLogMapper;
import com.lemon.erp.modules.wms.mapper.WmsSerialMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockFlowMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockMapper;
import com.lemon.erp.modules.wms.model.SerialStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.wms.model.WmsConstants.batchOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.locationOrNone;

/**
 * 库存查询、库存流水、批次与序列号追踪（只读）
 */
@Service
@RequiredArgsConstructor
public class WmsStockQueryService {

    /** 经营历史最多返回的流水条数 */
    private static final int HISTORY_LIMIT = 5000;

    /** 选择序列号时最多返回的条数 */
    private static final int SERIAL_OPTION_LIMIT = 500;

    private final WmsStockMapper stockMapper;
    private final WmsInOutMapper inOutMapper;
    private final WmsStockFlowMapper flowMapper;
    private final WmsBatchMapper batchMapper;
    private final WmsSerialMapper serialMapper;
    private final WmsSerialLogMapper serialLogMapper;
    private final StockHoldService holdService;
    private final StockCostService costService;
    private final WmsMaterialMapper materialMapper;
    private final PermissionService permissionService;

    public PageResult<StockView> stockPage(long current, long size, StockQuery query) {
        Page<StockView> page = Page.of(current, size);
        stockMapper.selectPageList(page, query);
        if (!costVisible()) {
            page.getRecords().forEach(v -> {
                v.setUnitCost(null);
                v.setAmount(null);
            });
        }
        return PageResult.of(page);
    }

    public PageResult<StockSummaryView> summaryPage(long current, long size, StockQuery query) {
        Page<StockSummaryView> page = Page.of(current, size);
        stockMapper.selectSummary(page, query);
        if (!costVisible()) {
            page.getRecords().forEach(v -> {
                v.setUnitCost(null);
                v.setAmount(null);
            });
        }
        return PageResult.of(page);
    }

    /**
     * 库存查询的合计（全部页）：数量、库存金额（需要「查看成本金额」权限，否则为空）
     *
     * @param summary true 按物料汇总，false 库存明细
     */
    public Map<String, BigDecimal> stockTotal(StockQuery query, boolean summary) {
        Map<String, Object> row = stockMapper.selectTotal(query, summary);
        Map<String, BigDecimal> total = new HashMap<>();
        total.put("qty", decimal(row == null ? null : row.get("qty")));
        total.put("amount", costVisible() ? decimal(row == null ? null : row.get("amount")) : null);
        return total;
    }

    /** 各物料的当前均价（其他入库录入成本价时默认带出） */
    public Map<Long, BigDecimal> unitCosts(Collection<Long> materialIds) {
        return costService.unitCosts(materialIds);
    }

    /**
     * 经营历史：物料在日期范围内的每条流水（业务单价、成本价、入库 / 出库数量、结余数量和金额、往来单位），
     * 第一行为期初余额。日期默认本月 1 日到今天；不选仓库时为全公司（不含调拨和供应商寄售仓）
     */
    public StockHistoryView history(StockHistoryQuery query) {
        if (query.materialId() == null) {
            throw new BizException("wms.error.selectMaterialFirst");
        }
        WmsMaterial m = materialMapper.selectById(query.materialId());
        if (m == null) {
            throw new BizException("wms.error.materialNotFound");
        }
        LocalDate end = query.endDate() == null ? LocalDate.now() : query.endDate();
        LocalDate begin = query.beginDate() == null ? end.withDayOfMonth(1) : query.beginDate();
        if (end.isBefore(begin)) {
            throw new BizException("wms.error.endDateCannotEarlierStartDate");
        }
        StockHistoryQuery q = new StockHistoryQuery(query.materialId(), query.warehouseId(), begin, end);
        List<StockHistoryRow> rows = flowMapper.selectHistory(q, HISTORY_LIMIT + 1);
        if (rows.size() > HISTORY_LIMIT) {
            throw new BizException("wms.error.historyTooManyRows", HISTORY_LIMIT);
        }
        Map<String, Object> opening = flowMapper.selectHistoryOpening(q);
        BigDecimal qty = decimal(opening == null ? null : opening.get("qty"));
        BigDecimal amount = decimal(opening == null ? null : opening.get("amount"));
        BigDecimal openingQty = qty;
        BigDecimal openingAmount = amount;
        boolean costVisible = costVisible();
        for (StockHistoryRow r : rows) {
            qty = qty.add(r.getQty());
            amount = amount.add(r.getCostAmount() == null ? BigDecimal.ZERO : r.getCostAmount());
            r.setBalanceQty(qty);
            if (costVisible) {
                r.setBalanceAmount(amount);
            } else {
                r.setBizPrice(null);
                r.setCostPrice(null);
                r.setCostAmount(null);
            }
        }
        return new StockHistoryView(m.getId(), m.getCode(), m.getName(), m.getSpec(), m.getUnit(), begin, end,
                openingQty, costVisible ? openingAmount : null, rows, costVisible,
                opening != null && decimal(opening.get("estimated")).signum() > 0);
    }

    /** 有「查看成本金额」权限 */
    private boolean costVisible() {
        return permissionService.hasPerm(WmsStockDocService.COST_PERM);
    }

    private static BigDecimal decimal(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    /** 某个库存位置上的在库序列号，出库、调拨时选择 */
    public List<String> availableSerials(Long warehouseId, Long locationId, Long materialId, String batchNo) {
        return serialMapper.selectPage(Page.of(1, SERIAL_OPTION_LIMIT, false), Wrappers.<WmsSerial>lambdaQuery()
                        .select(WmsSerial::getSerialNo)
                        .eq(WmsSerial::getWarehouseId, warehouseId)
                        .eq(WmsSerial::getLocationId, locationOrNone(locationId))
                        .eq(WmsSerial::getMaterialId, materialId)
                        .eq(WmsSerial::getStatus, SerialStatus.IN_STOCK)
                        .eq(WmsSerial::getBatchNo, batchOrNone(batchNo))
                        .orderByAsc(WmsSerial::getSerialNo))
                .getRecords().stream().map(WmsSerial::getSerialNo).toList();
    }

    /**
     * 各物料在全部仓库的现有库存（MRP 计算）：可用 + 待检（检验后将可用），不含不合格冻结的；没有库存的物料不在结果中
     */
    public Map<Long, BigDecimal> onHand(Collection<Long> materialIds) {
        Map<Long, BigDecimal> map = available(materialIds);
        holdService.pendingQty(materialIds).forEach((id, qty) -> map.merge(id, qty, BigDecimal::add));
        return map;
    }

    /** 拣货候选库存（先到期先出，已过期批次除外）：[{locationId, locationCode, batchNo, expiryDate, qty}] */
    public List<Map<String, Object>> pickCandidates(Long warehouseId, Long materialId) {
        return stockMapper.selectPickCandidates(warehouseId, materialId, java.time.LocalDate.now());
    }

    /** 各物料在全部仓库的可用库存（库存 - 冻结），齐套分析使用 */
    public Map<Long, BigDecimal> available(Collection<Long> materialIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (materialIds.isEmpty()) {
            return map;
        }
        for (Map<String, Object> row : stockMapper.selectQtyByMaterial(materialIds)) {
            BigDecimal qty = new BigDecimal(row.get("qty").toString());
            BigDecimal frozen = row.get("frozenQty") == null ? BigDecimal.ZERO : new BigDecimal(row.get("frozenQty").toString());
            map.put(((Number) row.get("materialId")).longValue(), qty.subtract(frozen));
        }
        return map;
    }

    public PageResult<StockFlowView> flowPage(long current, long size, FlowQuery query) {
        LocalDateTime begin = query.beginDate() == null ? null : query.beginDate().atStartOfDay();
        LocalDateTime end = query.endDate() == null ? null : query.endDate().plusDays(1).atStartOfDay();
        Page<StockFlowView> page = Page.of(current, size);
        flowMapper.selectPageList(page, query, begin, end);
        if (!costVisible()) {
            page.getRecords().forEach(v -> {
                v.setBizPrice(null);
                v.setCostPrice(null);
                v.setCostAmount(null);
            });
        }
        return PageResult.of(page);
    }

    /** 收发存汇总：日期范围内各物料的期初、各类出入库和期末数量；开始日期默认本月 1 日，结束日期默认今天，跨度不超过一年 */
    public PageResult<InOutRow> inOutPage(long current, long size, InOutQuery query) {
        LocalDate end = query.endDate() == null ? LocalDate.now() : query.endDate();
        LocalDate begin = query.beginDate() == null ? end.withDayOfMonth(1) : query.beginDate();
        if (end.isBefore(begin)) {
            throw new BizException("wms.error.endDateCannotEarlierStartDate");
        }
        if (begin.plusYears(1).isBefore(end)) {
            throw new BizException("wms.error.dateRangeCannotExceedOneYear");
        }
        Page<InOutRow> page = Page.of(current, size);
        inOutMapper.selectPage(page, new InOutQuery(begin, end, query.warehouseId(), query.category(), query.keyword()));
        return PageResult.of(page);
    }

    public PageResult<BatchView> batchPage(long current, long size, BatchQuery query) {
        Page<BatchView> page = Page.of(current, size);
        batchMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public BatchView batch(Long id) {
        BatchView batch = batchMapper.selectViewById(id);
        if (batch == null) {
            throw new BizException("wms.error.batchDoesNotExist");
        }
        return batch;
    }

    public PageResult<SerialView> serialPage(long current, long size, SerialQuery query) {
        Page<SerialView> page = Page.of(current, size);
        serialMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public SerialView serial(Long id) {
        SerialView serial = serialMapper.selectViewById(id);
        if (serial == null) {
            throw new BizException("wms.error.serialNoDoesNotExist");
        }
        return serial;
    }

    /** 序列号的完整轨迹：每次出入库、调拨、盘点 */
    public List<SerialLogView> serialLogs(Long id) {
        SerialView serial = serial(id);
        return serialLogMapper.selectBySerial(serial.getMaterialId(), serial.getSerialNo());
    }
}
