package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.scm.dto.SrmViews;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.entity.ScmPurchaseContract;
import com.lemon.erp.modules.scm.entity.ScmPurchaseContractItem;
import com.lemon.erp.modules.scm.mapper.ScmPurchaseContractItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmPurchaseContractMapper;
import com.lemon.erp.modules.scm.model.ContractStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.scm.model.ScmConstants.amount;
import static com.lemon.erp.modules.scm.model.ScmConstants.tax;

/**
 * 采购订单引用采购合同：按合同价下单，校验合同状态、供应商、期限，以及物料数量上限和合同金额上限。
 * 已下单量按非草稿的引用订单计算（关闭的订单按已入库数量），保存订单和提交审核时各校验一次
 */
@Service
@RequiredArgsConstructor
public class ScmContractUsageService {

    private final ScmPurchaseContractMapper contractMapper;
    private final ScmPurchaseContractItemMapper itemMapper;

    /**
     * 按合同价覆盖订单明细的单价、税率并重算金额，校验合同
     *
     * @param orderId 修改订单时为订单 ID（已下单量排除自身），新建为空
     */
    public void apply(Long contractId, Long supplierId, LocalDate orderDate, String currency, List<ScmOrderItem> items, Long orderId) {
        ScmPurchaseContract contract = check(contractId, supplierId, orderDate);
        if (currency != null && !ExchangeRateProvider.BASE.equals(currency)) {
            throw new BizException("引用采购合同的订单只能使用本位币");
        }
        Map<Long, ScmPurchaseContractItem> contractItems = contractItems(contractId);
        for (ScmOrderItem item : items) {
            ScmPurchaseContractItem ci = contractItems.get(item.getMaterialId());
            if (ci == null) {
                throw new BizException("物料不在采购合同「{0}」中", contract.getContractNo());
            }
            item.setPrice(ci.getPrice());
            item.setTaxRate(ci.getTaxRate());
            item.setAmount(amount(item.getQty(), ci.getPrice()));
            item.setTaxAmount(tax(item.getAmount(), ci.getTaxRate()));
        }
        checkLimits(contract, contractItems, items, orderId);
    }

    /** 提交审核时再次校验：同一合同可能同时有多张草稿订单 */
    public void checkOnApprove(Long contractId, Long supplierId, LocalDate orderDate, List<ScmOrderItem> items, Long orderId) {
        ScmPurchaseContract contract = check(contractId, supplierId, orderDate);
        checkLimits(contract, contractItems(contractId), items, orderId);
    }

    private ScmPurchaseContract check(Long contractId, Long supplierId, LocalDate orderDate) {
        ScmPurchaseContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BizException("采购合同不存在");
        }
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BizException("采购合同「{0}」未生效或已关闭", contract.getContractNo());
        }
        if (!contract.getSupplierId().equals(supplierId)) {
            throw new BizException("订单供应商与采购合同「{0}」的供应商不一致", contract.getContractNo());
        }
        if (orderDate.isBefore(contract.getStartDate()) || orderDate.isAfter(contract.getEndDate())) {
            throw new BizException("订单日期不在采购合同「{0}」的期限内（{1} ~ {2}）", contract.getContractNo(),
                    contract.getStartDate(), contract.getEndDate());
        }
        return contract;
    }

    private Map<Long, ScmPurchaseContractItem> contractItems(Long contractId) {
        return itemMapper.selectByContract(contractId).stream()
                .collect(Collectors.toMap(ScmPurchaseContractItem::getMaterialId, Function.identity()));
    }

    private void checkLimits(ScmPurchaseContract contract, Map<Long, ScmPurchaseContractItem> contractItems, List<ScmOrderItem> items,
                             Long orderId) {
        Map<Long, SrmViews.ContractUsage> usage = contractMapper.selectUsage(contract.getId(), orderId).stream()
                .collect(Collectors.toMap(SrmViews.ContractUsage::materialId, Function.identity()));
        Map<Long, BigDecimal> qty = items.stream()
                .collect(Collectors.groupingBy(ScmOrderItem::getMaterialId, Collectors.reducing(BigDecimal.ZERO, ScmOrderItem::getQty, BigDecimal::add)));
        for (Map.Entry<Long, BigDecimal> e : qty.entrySet()) {
            ScmPurchaseContractItem ci = contractItems.get(e.getKey());
            if (ci.getQtyLimit() == null) {
                continue;
            }
            SrmViews.ContractUsage u = usage.get(e.getKey());
            BigDecimal used = u == null ? BigDecimal.ZERO : u.orderedQty();
            if (used.add(e.getValue()).compareTo(ci.getQtyLimit()) > 0) {
                throw new BizException("物料「{0} {1}」超出合同数量上限 {2}（其他订单已下单 {3}）", ci.getMaterialCode(),
                        ci.getMaterialName(), plain(ci.getQtyLimit()), plain(used));
            }
        }
        if (contract.getAmountLimit() != null) {
            BigDecimal used = usage.values().stream().map(SrmViews.ContractUsage::orderedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal amount = items.stream().map(ScmOrderItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (used.add(amount).compareTo(contract.getAmountLimit()) > 0) {
                throw new BizException("超出采购合同「{0}」的金额上限 {1}（其他订单已下单 {2}）", contract.getContractNo(),
                        plain(contract.getAmountLimit()), plain(used));
            }
        }
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
