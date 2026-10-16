package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.wms.entity.WmsStockHold;
import com.lemon.erp.modules.wms.service.StockHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 库存冻结：待检、不合格的库存，可手工放行（让步接收）
 */
@RestController
@RequestMapping("/wms/hold")
@RequiredArgsConstructor
public class WmsStockHoldController {

    private final StockHoldService holdService;

    @GetMapping
    @PreAuthorize("@ss.hasAnyPerm('wms:hold:list', 'mes:qc:list')")
    public R<PageResult<WmsStockHold>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String sourceNo) {
        return R.ok(holdService.page(current, size, status, keyword, sourceNo));
    }

    /** 放行：body { remark } */
    @PostMapping("/{id}/release")
    @PreAuthorize("@ss.hasPerm('wms:hold:release')")
    @OperLog(title = "库存冻结-放行", type = OperType.UPDATE)
    public R<Void> release(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        holdService.release(id, body == null ? null : body.get("remark"));
        return R.ok();
    }
}
