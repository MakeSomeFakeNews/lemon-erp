package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.entity.ScmPriceControl;
import com.lemon.erp.modules.scm.mapper.ScmPriceControlMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.PriceControlMode;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 价格控制：物料的最低销售价、最高采购价。手工录入的销售 / 采购订单保存时校验（按含税、基本单位、折算本位币的单价，
 * 促销调价按原价校验，赠品不校验）：控制方式为「禁止保存」时不能保存，「提示」时由订单页面提示。
 * 另提供往来单位的历史成交价，订单录入时参考
 */
@Service
@RequiredArgsConstructor
public class ScmPriceControlService {

    private static final int MAX_HISTORY = 20;

    private final ScmPriceControlMapper controlMapper;
    private final WmsMaterialService materialService;

    public PageResult<ScmPriceControl> page(long current, long size, String keyword) {
        Page<ScmPriceControl> page = Page.of(current, size);
        controlMapper.selectPageList(page, keyword);
        return PageResult.of(page);
    }

    /** 物料的价格控制（订单页面提示用） */
    public List<ScmPriceControl> ofMaterials(Collection<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }
        return controlMapper.selectList(Wrappers.<ScmPriceControl>lambdaQuery().in(ScmPriceControl::getMaterialId, materialIds));
    }

    public void create(ScmPriceControl c) {
        materialService.get(c.getMaterialId());
        c.setId(null);
        save(c);
    }

    @Transactional
    public void update(Long id, ScmPriceControl c) {
        materialService.get(c.getMaterialId());
        c.setId(id);
        save(c);
    }

    private void save(ScmPriceControl c) {
        if (c.getMinSalePrice() == null && c.getMaxPurchasePrice() == null) {
            throw new BizException("scm.error.enterLeastMinimumSalesPriceMaximum");
        }
        if (c.getControlMode() == null) {
            c.setControlMode(PriceControlMode.WARN);
        }
        try {
            if (c.getId() == null) {
                controlMapper.insert(c);
            } else {
                controlMapper.updateById(c);
                controlMapper.update(Wrappers.<ScmPriceControl>lambdaUpdate()
                        .set(ScmPriceControl::getMinSalePrice, c.getMinSalePrice())
                        .set(ScmPriceControl::getMaxPurchasePrice, c.getMaxPurchasePrice())
                        .set(ScmPriceControl::getRemark, c.getRemark())
                        .eq(ScmPriceControl::getId, c.getId()));
            }
        } catch (DuplicateKeyException e) {
            throw new BizException("scm.error.priceControlAlreadyExistsMaterial");
        }
    }

    public void delete(List<Long> ids) {
        controlMapper.deleteByIds(ids);
    }

    /** 往来单位某物料最近的成交价 */
    public List<Map<String, Object>> history(OrderKind kind, Long partnerId, Long materialId, int limit) {
        return controlMapper.selectHistory(kind, partnerId, materialId, Math.max(1, Math.min(limit, MAX_HISTORY)));
    }

    /**
     * 订单保存时校验：控制方式为禁止保存的物料，销售单价低于最低销售价或采购单价高于最高采购价时抛异常
     *
     * @param exchangeRate 订单汇率（本位币为 1）
     */
    public void check(OrderKind kind, List<ScmOrderItem> items, BigDecimal exchangeRate) {
        Map<Long, ScmPriceControl> controls = ofMaterials(items.stream().map(ScmOrderItem::getMaterialId).distinct().toList()).stream()
                .filter(c -> c.getControlMode() == PriceControlMode.BLOCK)
                .collect(Collectors.toMap(ScmPriceControl::getMaterialId, Function.identity()));
        if (controls.isEmpty()) {
            return;
        }
        BigDecimal rate = exchangeRate == null || exchangeRate.signum() <= 0 ? BigDecimal.ONE : exchangeRate;
        List<String> errors = new ArrayList<>();
        Map<Long, WmsMaterial> materials = materialService.mapByIds(controls.keySet());
        for (ScmOrderItem item : items) {
            ScmPriceControl c = controls.get(item.getMaterialId());
            if (c == null || Integer.valueOf(1).equals(item.getGift())) {
                continue;
            }
            BigDecimal price = (item.getOrigPrice() != null ? item.getOrigPrice() : item.getPrice()).multiply(rate).setScale(4, RoundingMode.HALF_UP);
            WmsMaterial m = materials.get(item.getMaterialId());
            String name = m == null ? String.valueOf(item.getMaterialId()) : m.getCode() + " " + m.getName();
            if (kind == OrderKind.SALES && c.getMinSalePrice() != null && price.compareTo(c.getMinSalePrice()) < 0) {
                errors.add(Messages.get("scm.text.unitPriceBelowMinimumSalesPrice", name, price.stripTrailingZeros().toPlainString(),
                        c.getMinSalePrice().stripTrailingZeros().toPlainString()));
            }
            if (kind == OrderKind.PURCHASE && c.getMaxPurchasePrice() != null && price.compareTo(c.getMaxPurchasePrice()) > 0) {
                errors.add(Messages.get("scm.text.unitPriceAboveMaximumPurchasePrice", name, price.stripTrailingZeros().toPlainString(),
                        c.getMaxPurchasePrice().stripTrailingZeros().toPlainString()));
            }
        }
        if (!errors.isEmpty()) {
            throw new BizException("{0}", String.join(Messages.get("scm.label.message"), errors));
        }
    }
}
