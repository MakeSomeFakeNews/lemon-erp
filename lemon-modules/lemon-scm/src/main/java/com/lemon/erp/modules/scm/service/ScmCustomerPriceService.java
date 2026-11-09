package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.modules.scm.dto.CustomerPriceImportRow;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
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
import java.util.HashMap;
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

    /** 导出的最多行数 */
    public static final int EXPORT_LIMIT = 50000;

    private final ScmCustomerPriceMapper priceMapper;
    private final ScmCustomerService customerService;
    private final WmsMaterialService materialService;
    private final ImportExecutor importExecutor;

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

    /**
     * 导出执行价（含客户、物料编码名称），按客户编码、物料编码排序，最多 {@link #EXPORT_LIMIT} 行
     *
     * @param customerId 只导出该客户，为空导出全部客户
     * @param keyword    客户编码 / 名称，模糊匹配
     */
    public List<ScmCustomerPrice> export(Long customerId, String keyword) {
        if (customerId != null) {
            customerService.get(customerId);
        }
        return priceMapper.selectExport(customerId, StringUtils.hasText(keyword) ? keyword.trim() : null, EXPORT_LIMIT);
    }

    /**
     * Excel 导入：按 客户编码 × 物料编码 新增或更新执行价（文件中没有的执行价不变，删除在「执行价」中操作）。
     * 逐行校验，任何一行出错整批不导入；同一文件中同一客户的同一物料重复时报错
     */
    public ImportResult importRows(ImportRequest<CustomerPriceImportRow> req) {
        Map<String, ScmCustomer> customers = new HashMap<>();
        Set<String> materialCodes = new HashSet<>();
        req.rows().forEach(r -> {
            if (r != null && r.data() != null && StringUtils.hasText(r.data().materialCode())) {
                materialCodes.add(r.data().materialCode().trim());
            }
        });
        Map<String, WmsMaterial> materials = materialService.mapByCodes(materialCodes);
        Set<String> seen = new HashSet<>();
        return importExecutor.run(req, row -> {
            String customerCode = row.customerCode().trim();
            String materialCode = row.materialCode().trim();
            ScmCustomer customer = customers.computeIfAbsent(customerCode, customerService::findByCode);
            if (customer == null) {
                throw new BizException("scm.error.customerPriceCustomerNotFound", customerCode);
            }
            WmsMaterial m = materials.get(materialCode);
            if (m == null) {
                throw new BizException("scm.error.customerPriceMaterialNotFound", materialCode);
            }
            if (!seen.add(customer.getId() + "|" + m.getId())) {
                throw new BizException("scm.error.customerPriceImportDuplicate", customerCode, materialCode);
            }
            upsert(customer.getId(), m.getId(), row.price(), row.taxRate(), row.remark());
        });
    }

    /** 新增或更新一条执行价（税率、备注为空时清空） */
    private void upsert(Long customerId, Long materialId, BigDecimal price, BigDecimal taxRate, String remark) {
        String r = StringUtils.hasText(remark) ? remark.trim() : null;
        BigDecimal p = price.setScale(4, RoundingMode.HALF_UP);
        ScmCustomerPrice exist = priceMapper.selectOne(Wrappers.<ScmCustomerPrice>lambdaQuery()
                .eq(ScmCustomerPrice::getCustomerId, customerId).eq(ScmCustomerPrice::getMaterialId, materialId));
        if (exist == null) {
            ScmCustomerPrice row = new ScmCustomerPrice();
            row.setCustomerId(customerId);
            row.setMaterialId(materialId);
            row.setPrice(p);
            row.setTaxRate(taxRate);
            row.setRemark(r);
            priceMapper.insert(row);
        } else {
            priceMapper.update(Wrappers.<ScmCustomerPrice>lambdaUpdate()
                    .set(ScmCustomerPrice::getPrice, p)
                    .set(ScmCustomerPrice::getTaxRate, taxRate)
                    .set(ScmCustomerPrice::getRemark, r)
                    .set(ScmCustomerPrice::getUpdateTime, java.time.LocalDateTime.now())
                    .eq(ScmCustomerPrice::getId, exist.getId()));
        }
    }
}
