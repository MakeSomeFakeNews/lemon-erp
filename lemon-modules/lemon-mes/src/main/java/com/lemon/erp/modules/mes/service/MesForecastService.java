package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.mes.entity.MesForecast;
import com.lemon.erp.modules.mes.mapper.MesForecastMapper;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 销售预测：按物料、月份维护，MRP 运算时可选择计入
 */
@Service
@RequiredArgsConstructor
public class MesForecastService {

    private final MesForecastMapper forecastMapper;
    private final WmsMaterialService materialService;

    public PageResult<MesForecast> page(long current, long size, Integer beginPeriod, Integer endPeriod, String keyword) {
        Page<MesForecast> page = Page.of(current, size);
        forecastMapper.selectPageList(page, beginPeriod, endPeriod, keyword);
        return PageResult.of(page);
    }

    /** 保存：同一物料、月份已有预测时覆盖数量，返回预测 ID */
    @Transactional
    public Long save(MesForecast req) {
        if (req.getPeriod() % 100 < 1 || req.getPeriod() % 100 > 12) {
            throw new BizException("mes.error.invalidMonth");
        }
        materialService.get(req.getMaterialId());
        MesForecast old = forecastMapper.selectOne(Wrappers.<MesForecast>lambdaQuery()
                .eq(MesForecast::getMaterialId, req.getMaterialId()).eq(MesForecast::getPeriod, req.getPeriod()));
        if (old != null) {
            forecastMapper.update(Wrappers.<MesForecast>lambdaUpdate().set(MesForecast::getQty, req.getQty())
                    .set(MesForecast::getRemark, req.getRemark()).eq(MesForecast::getId, old.getId()));
            return old.getId();
        }
        req.setId(null);
        req.setCreateBy(SecurityUtils.getUserId());
        forecastMapper.insert(req);
        return req.getId();
    }

    public void delete(List<Long> ids) {
        forecastMapper.deleteByIds(ids);
    }
}
