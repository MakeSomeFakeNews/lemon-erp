package com.lemon.erp.modules.mes.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.mes.entity.MesForecast;
import com.lemon.erp.modules.mes.mapper.MesForecastMapper;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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

    private final MesForecastMapper forecastMapper;
    private final WmsMaterialService materialService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('mes:mrp:list')")
    public R<PageResult<MesForecast>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Integer beginPeriod,
                                           @RequestParam(required = false) Integer endPeriod,
                                           @RequestParam(required = false) String keyword) {
        Page<MesForecast> page = Page.of(current, size);
        forecastMapper.selectPageList(page, beginPeriod, endPeriod, keyword);
        return R.ok(PageResult.of(page));
    }

    /** 保存：同一物料、月份已有预测时覆盖数量 */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('mes:mrp:run')")
    @OperLog(title = "销售预测", type = OperType.INSERT) // i18n-ignore
    @Transactional
    public R<Long> save(@Valid @RequestBody MesForecast req) {
        if (req.getPeriod() % 100 < 1 || req.getPeriod() % 100 > 12) {
            throw new BizException("月份不正确");
        }
        materialService.get(req.getMaterialId());
        MesForecast old = forecastMapper.selectOne(Wrappers.<MesForecast>lambdaQuery()
                .eq(MesForecast::getMaterialId, req.getMaterialId()).eq(MesForecast::getPeriod, req.getPeriod()));
        if (old != null) {
            forecastMapper.update(Wrappers.<MesForecast>lambdaUpdate().set(MesForecast::getQty, req.getQty())
                    .set(MesForecast::getRemark, req.getRemark()).eq(MesForecast::getId, old.getId()));
            return R.ok(old.getId());
        }
        req.setId(null);
        req.setCreateBy(SecurityUtils.getUserId());
        forecastMapper.insert(req);
        return R.ok(req.getId());
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('mes:mrp:run')")
    @OperLog(title = "销售预测", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        forecastMapper.deleteByIds(ids);
        return R.ok();
    }
}
