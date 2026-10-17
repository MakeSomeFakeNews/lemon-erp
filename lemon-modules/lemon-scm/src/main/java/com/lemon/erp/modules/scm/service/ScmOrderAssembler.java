package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsMaterialUnit;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.scm.model.ScmConstants.DEFAULT_TAX_RATE;
import static com.lemon.erp.modules.scm.model.ScmConstants.amount;
import static com.lemon.erp.modules.scm.model.ScmConstants.tax;

/**
 * 订单请求转为实体：校验往来单位、仓库、物料，计算明细金额，填写表头的币种汇率和合计
 */
@Component
@RequiredArgsConstructor
public class ScmOrderAssembler {

    private final ScmSupplierService supplierService;
    private final ScmCustomerService customerService;
    private final WmsMaterialService materialService;
    private final WmsWarehouseService warehouseService;
    private final ExchangeRateProvider rateProvider;

    /** 销售订单的业务员：客户的负责人，没有时为制单人 */
    public Long salesmanOf(Long customerId, Long userId) {
        Long owner = customerService.get(customerId).getOwnerId();
        return owner != null ? owner : userId;
    }

    /** 校验往来单位、仓库、物料，计算金额 */
    public List<ScmOrderItem> toItems(OrderKind kind, OrderRequest req) {
        if (kind == OrderKind.PURCHASE) {
            ScmSupplier supplier = supplierService.get(req.partnerId());
            if (supplier.getStatus() != EnableStatus.ENABLED) {
                throw new BizException("scm.error.supplierDisabled", supplier.getName());
            }
        } else {
            ScmCustomer customer = customerService.get(req.partnerId());
            if (customer.getStatus() != EnableStatus.ENABLED) {
                throw new BizException("scm.error.customerDisabled", customer.getName());
            }
        }
        if (req.warehouseId() != null) {
            WmsWarehouse warehouse = warehouseService.get(req.warehouseId());
            if (warehouse.getStatus() != EnableStatus.ENABLED) {
                throw new BizException("scm.error.warehouseDisabled", warehouse.getName());
            }
        }
        Map<Long, WmsMaterial> materials = materialService.getEnabled(req.items().stream().map(OrderItemRequest::materialId).toList());
        return req.items().stream().map(r -> {
            WmsMaterial material = materials.get(r.materialId());
            if (material.isSerialManaged() && r.qty().stripTrailingZeros().scale() > 0) {
                throw new BizException("scm.error.materialSerialManagedQuantityMustInteger", material.getCode(), material.getName());
            }
            ScmOrderItem item = new ScmOrderItem();
            item.setMaterialId(r.materialId());
            item.setQty(r.qty());
            item.setPrice(r.price());
            item.setTaxRate(r.taxRate() == null ? DEFAULT_TAX_RATE : r.taxRate());
            item.setAmount(amount(r.qty(), r.price()));
            item.setTaxAmount(tax(item.getAmount(), item.getTaxRate()));
            item.setDeliveryDate(r.deliveryDate() != null ? r.deliveryDate() : req.deliveryDate());
            item.setRemark(r.remark());
            if (r.unitName() != null && !r.unitName().isBlank() && !r.unitName().equals(material.getUnit())) {
                WmsMaterialUnit unit = materialService.units(material.getId()).stream().filter(u -> u.getUnit().equals(r.unitName()))
                        .findFirst().orElseThrow(() -> new BizException("scm.error.materialHasNoAlternateUnit", material.getCode(),
                                material.getName(), r.unitName()));
                item.setUnitName(unit.getUnit());
                item.setUnitRatio(unit.getRatio());
            }
            return item;
        }).toList();
    }

    /** 币种、汇率（外币没有填写时取汇率表），往来单位等表头字段和合计 */
    public void fillHeader(ScmOrder order, OrderRequest req, List<ScmOrderItem> items) {
        String currency = req.currency() == null || req.currency().isBlank() ? ExchangeRateProvider.BASE : req.currency().trim();
        BigDecimal rate = BigDecimal.ONE;
        if (!ExchangeRateProvider.BASE.equals(currency)) {
            rate = req.exchangeRate() != null ? req.exchangeRate() : rateProvider.rate(currency, req.orderDate());
            if (rate == null) {
                throw new BizException("scm.error.noExchangeRateMaintainCurrencyRates", currency);
            }
        }
        order.setCurrency(currency);
        order.setExchangeRate(rate);
        order.setPartnerId(req.partnerId());
        order.setOrderDate(req.orderDate());
        order.setDeliveryDate(req.deliveryDate());
        order.setWarehouseId(req.warehouseId());
        order.setContractId(req.contractId());
        order.setRemark(req.remark());
        order.setTotalQty(Decimals.sum(items, ScmOrderItem::getQty));
        order.setTotalAmount(Decimals.sum(items, ScmOrderItem::getAmount));
        order.setTaxAmount(Decimals.sum(items, ScmOrderItem::getTaxAmount));
    }
}
