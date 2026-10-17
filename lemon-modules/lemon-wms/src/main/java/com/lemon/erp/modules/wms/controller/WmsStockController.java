package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.wms.dto.BatchQuery;
import com.lemon.erp.modules.wms.dto.InOutQuery;
import com.lemon.erp.modules.wms.dto.InOutRow;
import com.lemon.erp.modules.wms.dto.BatchView;
import com.lemon.erp.modules.wms.dto.FlowQuery;
import com.lemon.erp.modules.wms.dto.SerialLogView;
import com.lemon.erp.modules.wms.dto.SerialQuery;
import com.lemon.erp.modules.wms.dto.SerialView;
import com.lemon.erp.modules.wms.dto.StockFlowView;
import com.lemon.erp.modules.wms.dto.StockQuery;
import com.lemon.erp.modules.wms.dto.StockSummaryView;
import com.lemon.erp.modules.wms.dto.StockView;
import com.lemon.erp.modules.wms.service.WmsStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存查询、库存流水、批次与序列号追踪
 */
@RestController
@RequestMapping("/wms")
@RequiredArgsConstructor
public class WmsStockController {

    /**
     * 出库、调拨、盘点选择库存，批次追踪查看库存分布，都需要查询库存明细和在库序列号；
     * 供应链的销售出库、采购退货，生产的工单领料、补料发料，项目领料也从库存中选择
     */
    private static final String STOCK_READ_PERMS = "@ss.hasAnyPerm('wms:stock:list', 'wms:trace:list', 'wms:out:add', "
            + "'wms:out:edit', 'wms:transfer:add', 'wms:transfer:edit', 'wms:check:add', 'wms:check:edit', "
            + "'scm:salesOut:add', 'scm:purchaseReturn:add', 'mes:wo:issue', 'mes:supplement:issue', 'pm:cost:issue')";

    private final WmsStockQueryService queryService;

    @GetMapping("/stock")
    @PreAuthorize(STOCK_READ_PERMS)
    public R<PageResult<StockView>> stockPage(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              StockQuery query) {
        return R.ok(queryService.stockPage(current, size, query));
    }

    @GetMapping("/stock/summary")
    @PreAuthorize("@ss.hasPerm('wms:stock:list')")
    public R<PageResult<StockSummaryView>> summaryPage(@RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       StockQuery query) {
        return R.ok(queryService.summaryPage(current, size, query));
    }

    /** 收发存汇总 */
    @GetMapping("/stock/in-out")
    @PreAuthorize("@ss.hasPerm('wms:inout:list')")
    public R<PageResult<InOutRow>> inOutPage(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             InOutQuery query) {
        return R.ok(queryService.inOutPage(current, size, query));
    }

    /** 某个库存位置上的在库序列号 */
    @GetMapping("/stock/serials")
    @PreAuthorize(STOCK_READ_PERMS)
    public R<List<String>> availableSerials(@RequestParam Long warehouseId,
                                            @RequestParam(required = false) Long locationId,
                                            @RequestParam Long materialId,
                                            @RequestParam(required = false) String batchNo) {
        return R.ok(queryService.availableSerials(warehouseId, locationId, materialId, batchNo));
    }

    @GetMapping("/flow")
    @PreAuthorize("@ss.hasAnyPerm('wms:flow:list', 'wms:trace:list')")
    public R<PageResult<StockFlowView>> flowPage(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 FlowQuery query) {
        return R.ok(queryService.flowPage(current, size, query));
    }

    @GetMapping("/trace/batch")
    @PreAuthorize("@ss.hasPerm('wms:trace:list')")
    public R<PageResult<BatchView>> batchPage(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              BatchQuery query) {
        return R.ok(queryService.batchPage(current, size, query));
    }

    @GetMapping("/trace/batch/{id}")
    @PreAuthorize("@ss.hasPerm('wms:trace:list')")
    public R<BatchView> batch(@PathVariable Long id) {
        return R.ok(queryService.batch(id));
    }

    @GetMapping("/trace/serial")
    @PreAuthorize("@ss.hasPerm('wms:trace:list')")
    public R<PageResult<SerialView>> serialPage(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                SerialQuery query) {
        return R.ok(queryService.serialPage(current, size, query));
    }

    @GetMapping("/trace/serial/{id}/logs")
    @PreAuthorize("@ss.hasPerm('wms:trace:list')")
    public R<List<SerialLogView>> serialLogs(@PathVariable Long id) {
        return R.ok(queryService.serialLogs(id));
    }
}
