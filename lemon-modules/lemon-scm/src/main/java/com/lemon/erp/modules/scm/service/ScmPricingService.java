package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.modules.scm.dto.PricingViews;
import com.lemon.erp.modules.scm.dto.PricingViews.DefaultPrice;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmCustomerPrice;
import com.lemon.erp.modules.scm.entity.ScmPriceList;
import com.lemon.erp.modules.scm.entity.ScmPriceMemory;
import com.lemon.erp.modules.scm.entity.ScmPurchaseContract;
import com.lemon.erp.modules.scm.entity.ScmPurchaseContractItem;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.mapper.ScmPriceMemoryMapper;
import com.lemon.erp.modules.scm.mapper.ScmPricingMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.PriceSource;
import com.lemon.erp.modules.scm.model.ScmConstants;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.model.PriceLevels;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsPriceLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 开单默认价与价格参考。
 * <p>销售默认价：客户执行价 → 销售价目表（有效期、起订量）→ 客户价格等级的价格列 → 零售价 → 该客户最近售价（价格记忆）；
 * 停用的价格列不参与。采购默认价：供应商在单据日期有效的采购合同价 → 供货价格（阶梯价）→ 最近进价 → 参考成本价。
 * 价格均为含税、基本单位、本位币
 */
@Service
@RequiredArgsConstructor
public class ScmPricingService {

    /** 价格参考每个分区最多返回的行数 */
    public static final int REFERENCE_LIMIT = 200;

    private static final DataPermissionRule QUOTATION_RULE = new DataPermissionRule("scm_quotation", "dept_id", "create_by");

    private final ScmCustomerService customerService;
    private final ScmCustomerPriceService customerPriceService;
    private final ScmPriceListService priceListService;
    private final ScmPurchaseContractService contractService;
    private final ScmQueryService queryService;
    private final ScmPriceMemoryService memoryService;
    private final ScmPriceMemoryMapper memoryMapper;
    private final ScmPricingMapper pricingMapper;
    private final WmsMaterialService materialService;
    private final WmsPriceLevelService priceLevelService;
    private final PermissionService permissionService;

    /**
     * 开单默认价
     *
     * @param partnerId   客户 / 供应商，可为空（只按物料的价格列 / 参考成本）
     * @param materialIds 物料
     * @param qtys        与物料一一对应的数量（价目表起订量、阶梯价），可为空或比物料少（缺少的按 1）
     * @param date        单据日期，为空取今天
     */
    public List<DefaultPrice> defaults(OrderKind kind, Long partnerId, List<Long> materialIds, List<BigDecimal> qtys, LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<Long> ids = materialIds.stream().distinct().toList();
        Map<Long, WmsMaterial> materials = materialService.mapByIds(ids);
        Map<Long, BigDecimal> qtyOf = new HashMap<>();
        for (int i = 0; i < materialIds.size(); i++) {
            BigDecimal q = qtys != null && i < qtys.size() ? qtys.get(i) : null;
            qtyOf.putIfAbsent(materialIds.get(i), q == null || q.signum() <= 0 ? BigDecimal.ONE : q);
        }
        Map<Long, ScmPriceMemory> memory = memoryService.ofMaterials(kind, partnerId, ids);
        List<DefaultPrice> result = new ArrayList<>(ids.size());
        if (kind == OrderKind.SALES) {
            ScmCustomer customer = partnerId == null ? null : customerService.get(partnerId);
            Map<Long, ScmCustomerPrice> customerPrices = customerPriceService.ofMaterials(partnerId, ids);
            Set<Integer> enabledLevels = priceLevelService.enabledLevels();
            int level = customer == null || customer.getPriceLevel() == null ? PriceLevels.RETAIL : customer.getPriceLevel();
            for (Long id : ids) {
                result.add(salesPrice(id, materials.get(id), customerPrices.get(id), partnerId, qtyOf.get(id), day, level,
                        enabledLevels, memory.get(id)));
            }
        } else {
            Map<Long, ScmPurchaseContractItem> contractPrices = new HashMap<>();
            if (partnerId != null) {
                for (ScmPurchaseContract c : contractService.active(partnerId, day)) {
                    c.getItems().forEach(i -> contractPrices.putIfAbsent(i.getMaterialId(), i));
                }
            }
            for (Long id : ids) {
                result.add(purchasePrice(id, materials.get(id), contractPrices.get(id), partnerId, qtyOf.get(id), memory.get(id)));
            }
        }
        return result;
    }

    private DefaultPrice salesPrice(Long id, WmsMaterial material, ScmCustomerPrice customerPrice, Long customerId, BigDecimal qty,
                                    LocalDate date, int level, Set<Integer> enabledLevels, ScmPriceMemory memory) {
        if (customerPrice != null) {
            return new DefaultPrice(id, customerPrice.getPrice(), customerPrice.getTaxRate(), PriceSource.CUSTOMER_PRICE);
        }
        ScmPriceList listPrice = priceListService.quote(id, customerId, qty, date);
        if (listPrice != null) {
            return new DefaultPrice(id, listPrice.getPrice(), listPrice.getTaxRate(), PriceSource.PRICE_LIST);
        }
        if (material != null) {
            if (level != PriceLevels.RETAIL && enabledLevels.contains(level) && PriceLevels.price(material, level) != null) {
                return new DefaultPrice(id, PriceLevels.price(material, level), null, PriceSource.PRICE_LEVEL);
            }
            if (enabledLevels.contains(PriceLevels.RETAIL) && material.getRetailPrice() != null) {
                return new DefaultPrice(id, material.getRetailPrice(), null, PriceSource.RETAIL);
            }
        }
        if (memory != null) {
            return new DefaultPrice(id, memory.getLastPrice(), null, PriceSource.LAST_PRICE);
        }
        return new DefaultPrice(id, null, null, null);
    }

    private DefaultPrice purchasePrice(Long id, WmsMaterial material, ScmPurchaseContractItem contractItem, Long supplierId, BigDecimal qty,
                                       ScmPriceMemory memory) {
        if (contractItem != null) {
            return new DefaultPrice(id, contractItem.getPrice(), contractItem.getTaxRate(), PriceSource.CONTRACT_PRICE);
        }
        ScmSupplierPrice supplierPrice = supplierId == null ? null : queryService.supplierPrice(supplierId, id);
        if (supplierPrice != null) {
            return new DefaultPrice(id, queryService.effectivePrice(supplierPrice, qty), supplierPrice.getTaxRate(), PriceSource.SUPPLIER_PRICE);
        }
        if (memory != null) {
            return new DefaultPrice(id, memory.getLastPrice(), null, PriceSource.LAST_PRICE);
        }
        if (material != null && material.getRefCost() != null) {
            return new DefaultPrice(id, material.getRefCost(), null, PriceSource.REF_COST);
        }
        return new DefaultPrice(id, null, null, null);
    }

    // ---------------------------------------------------------------- 价格参考

    /** 是否可以查看销售方面的价格（报价、销售历史、客户对比） */
    public boolean canViewSales() {
        return permissionService.hasAnyPerm("scm:sales:list", "scm:salesOut:list", "scm:quotation:list");
    }

    /** 是否可以查看采购方面的价格（进货历史、进货数量、供商对比） */
    public boolean canViewPurchase() {
        return permissionService.hasAnyPerm("scm:purchase:list", "scm:purchaseIn:list");
    }

    /**
     * 价格参考：没有销售查看权限时不返回报价、销售历史、客户对比，没有采购查看权限时不返回进货历史、进货数量、供商对比。
     * 报价、出入库历史和进货数量按制单人部门 + 制单人做数据权限；价格记忆是汇总结果，按权限整体裁剪
     */
    public PricingViews.Reference reference(PricingViews.ReferenceQuery q) {
        return reference(q, null);
    }

    /**
     * @param partnerKind 往来单位的种类（客户 SALES / 供应商 PURCHASE）：指定时 partnerId 只用于该方向的分区，
     *                    另一方向的分区不按往来单位筛选（客户与供应商的 ID 不在同一张表）；为空时都按 partnerId 筛选
     */
    public PricingViews.Reference reference(PricingViews.ReferenceQuery q, OrderKind partnerKind) {
        boolean sales = canViewSales();
        boolean purchase = canViewPurchase();
        PricingViews.ReferenceQuery sq = partnerKind == OrderKind.PURCHASE ? withoutPartner(q) : q;
        PricingViews.ReferenceQuery pq = partnerKind == OrderKind.SALES ? withoutPartner(q) : q;
        return new PricingViews.Reference(
                sales ? DataPermissionContext.execute(QUOTATION_RULE, () -> pricingMapper.selectQuotationHistory(sq)) : null,
                sales ? DataPermissionContext.execute(ScmConstants.TRADE_RULE, () -> pricingMapper.selectTradeHistory(TradeKind.SALES_OUT, true, sq)) : null,
                purchase ? DataPermissionContext.execute(ScmConstants.TRADE_RULE, () -> pricingMapper.selectTradeHistory(TradeKind.PURCHASE_IN, false, pq)) : null,
                purchase ? DataPermissionContext.execute(ScmConstants.TRADE_RULE, () -> pricingMapper.selectPurchaseQty(pq)) : null,
                purchase ? memoryMapper.selectCompare(OrderKind.PURCHASE, pq) : null,
                sales ? memoryMapper.selectCompare(OrderKind.SALES, sq) : null);
    }

    private static PricingViews.ReferenceQuery withoutPartner(PricingViews.ReferenceQuery q) {
        return new PricingViews.ReferenceQuery(q.materialId(), null, q.keyword(), q.start(), q.end(), q.limit());
    }
}
