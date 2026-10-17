package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.mes.entity.MesForecast;
import com.lemon.erp.modules.mes.service.MesForecastService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 销售预测：按物料、月份维护，MRP 运算时可选择计入
 */
@RestController
@RequestMapping("/mes/forecast")
@RequiredArgsConstructor
public class MesForecastController {

    private final MesForecastService forecastService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('mes:mrp:list')")
    public R<PageResult<MesForecast>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Integer beginPeriod,
                                           @RequestParam(required = false) Integer endPeriod,
                                           @RequestParam(required = false) String keyword) {
        return R.ok(forecastService.page(current, size, beginPeriod, endPeriod, keyword));
    }

    /** 保存：同一物料、月份已有预测时覆盖数量 */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('mes:mrp:run')")
    @OperLog(title = "销售预测", type = OperType.INSERT) // i18n-ignore
    public R<Long> save(@Valid @RequestBody MesForecast req) {
        return R.ok(forecastService.save(req));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('mes:mrp:run')")
    @OperLog(title = "销售预测", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        forecastService.delete(ids);
        return R.ok();
    }
}
