package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.scm.entity.ScmPriceList;
import com.lemon.erp.modules.scm.mapper.ScmPriceListMapper;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售价目表：按物料、客户等级或客户专属、数量阶梯、有效期维护销售单价，订单 / 报价选物料后取价
 */
@Service
@RequiredArgsConstructor
public class ScmPriceListService {

    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(13);

    private final ScmPriceListMapper priceListMapper;
    private final WmsMaterialService materialService;
    private final ScmCustomerService customerService;

    public PageResult<ScmPriceList> page(long current, long size, String keyword, String level) {
        Page<ScmPriceList> page = Page.of(current, size);
        priceListMapper.selectPageList(page, keyword, level);
        return PageResult.of(page);
    }

    /** 取价：客户专属价优先，其次客户等级价；没有匹配的价格时返回 null */
    public ScmPriceList quote(Long materialId, Long customerId, BigDecimal qty, LocalDate date) {
        String level = customerId == null ? null : customerService.get(customerId).getLevel();
        return priceListMapper.selectQuote(materialId, customerId, level,
                qty == null ? BigDecimal.ONE : qty, date == null ? LocalDate.now() : date);
    }

    public void create(ScmPriceList p) {
        priceListMapper.insert(normalize(p, null));
    }

    @Transactional
    public void update(Long id, ScmPriceList req) {
        if (priceListMapper.selectById(id) == null) {
            throw new BizException("scm.error.priceEntryDoesNotExist");
        }
        ScmPriceList p = normalize(req, id);
        priceListMapper.updateById(p);
        // 可为空的字段要能清空
        priceListMapper.update(Wrappers.<ScmPriceList>lambdaUpdate()
                .set(ScmPriceList::getCustomerLevel, p.getCustomerLevel())
                .set(ScmPriceList::getCustomerId, p.getCustomerId())
                .set(ScmPriceList::getValidFrom, p.getValidFrom())
                .set(ScmPriceList::getValidTo, p.getValidTo())
                .set(ScmPriceList::getRemark, p.getRemark())
                .eq(ScmPriceList::getId, id));
    }

    public void delete(List<Long> ids) {
        priceListMapper.deleteByIds(ids);
    }

    private ScmPriceList normalize(ScmPriceList p, Long id) {
        materialService.get(p.getMaterialId());
        if (p.getValidFrom() != null && p.getValidTo() != null && p.getValidTo().isBefore(p.getValidFrom())) {
            throw new BizException("scm.error.validDateCannotEarlierValidDate");
        }
        p.setId(id);
        p.setCustomerLevel(p.getCustomerLevel() == null || p.getCustomerLevel().isBlank() ? null : p.getCustomerLevel());
        // 客户专属价只看客户，不再区分等级
        if (p.getCustomerId() != null) {
            customerService.get(p.getCustomerId());
            p.setCustomerLevel(null);
        }
        p.setMinQty(Decimals.nz(p.getMinQty()));
        p.setTaxRate(p.getTaxRate() == null ? DEFAULT_TAX_RATE : p.getTaxRate());
        return p;
    }
}
