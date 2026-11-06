package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.modules.scm.entity.ScmCustomerPrice;
import com.lemon.erp.modules.scm.mapper.ScmCustomerPriceMapper;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 客户执行价：客户 × 物料的含税单价，开单默认价最优先
 */
@Service
@RequiredArgsConstructor
public class ScmCustomerPriceService {

    private static final int REMARK_MAX = 255;

    private final ScmCustomerPriceMapper priceMapper;
    private final ScmCustomerService customerService;
    private final WmsMaterialService materialService;

    public List<ScmCustomerPrice> list(Long customerId) {
        customerService.get(customerId);
        return priceMapper.selectByCustomer(customerId);
    }

    /** 客户对这些物料的执行价，物料 ID → 执行价 */
    public Map<Long, ScmCustomerPrice> ofMaterials(Long customerId, Collection<Long> materialIds) {
        if (customerId == null || materialIds.isEmpty()) {
            return Map.of();
        }
        return priceMapper.selectList(Wrappers.<ScmCustomerPrice>lambdaQuery().eq(ScmCustomerPrice::getCustomerId, customerId)
                        .in(ScmCustomerPrice::getMaterialId, materialIds)).stream()
                .collect(Collectors.toMap(ScmCustomerPrice::getMaterialId, Function.identity(), (a, b) -> a));
    }

    /** 整体替换客户的执行价 */
    @Transactional
    public void save(Long customerId, List<ScmCustomerPrice> rows) {
        customerService.get(customerId);
        Set<Long> seen = new HashSet<>();
        Map<Long, WmsMaterial> materials = materialService.mapByIds(rows.stream().map(ScmCustomerPrice::getMaterialId)
                .filter(java.util.Objects::nonNull).toList());
        for (int i = 0; i < rows.size(); i++) {
            ScmCustomerPrice r = rows.get(i);
            WmsMaterial m = r.getMaterialId() == null ? null : materials.get(r.getMaterialId());
            if (m == null) {
                throw new BizException("scm.error.customerPriceMaterialRequired", i + 1);
            }
            if (!seen.add(r.getMaterialId())) {
                throw new BizException("scm.error.customerPriceDuplicate", m.getCode());
            }
            if (r.getPrice() == null || r.getPrice().signum() < 0) {
                throw new BizException("scm.error.customerPriceInvalid", m.getCode());
            }
            if (r.getTaxRate() != null && (r.getTaxRate().signum() < 0 || r.getTaxRate().compareTo(BigDecimal.valueOf(100)) > 0)) {
                throw new BizException("scm.error.customerPriceTaxInvalid", m.getCode());
            }
            r.setId(null);
            r.setPrice(r.getPrice().setScale(4, RoundingMode.HALF_UP));
            r.setRemark(StringUtils.hasText(r.getRemark()) ? r.getRemark().trim() : null);
            if (r.getRemark() != null && r.getRemark().length() > REMARK_MAX) {
                r.setRemark(r.getRemark().substring(0, REMARK_MAX));
            }
        }
        DetailLines.replace(priceMapper, ScmCustomerPrice::getCustomerId, customerId, rows, ScmCustomerPrice::setCustomerId,
                priceMapper::insertBatch);
    }
}
