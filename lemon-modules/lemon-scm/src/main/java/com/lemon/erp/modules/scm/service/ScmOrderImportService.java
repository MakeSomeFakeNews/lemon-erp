package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.excel.ImportRow;
import com.lemon.erp.modules.scm.dto.ChannelRequests;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmPriceList;
import com.lemon.erp.modules.scm.mapper.ScmCustomerMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.mapper.ScmPriceListMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.scm.model.ScmConstants.DEFAULT_TAX_RATE;

/**
 * 外部订单导入（电商 / 门店）：统一格式按行提交（Excel 解析后或接口直接调用），同一外部单号的多行生成一张销售订单草稿（来源「外部导入」），
 * 单价为空时按价目表取价。外部单号已导入过的整单跳过（防重）；其他错误整批不导入
 */
@Service
@RequiredArgsConstructor
public class ScmOrderImportService {


    private final ScmOrderService orderService;
    private final ScmOrderMapper orderMapper;
    private final ScmCustomerMapper customerMapper;
    private final ScmPriceListMapper priceListMapper;
    private final WmsMaterialService materialService;
    private final Validator validator;
    private final TransactionTemplate transactionTemplate;

    /**
     * 导入结果
     *
     * @param orders  生成的订单数
     * @param skipped 已导入过而跳过的外部单号
     */
    public record Result(int total, int imported, List<ImportResult.RowError> errors, int orders, List<String> skipped) {
    }

    public Result importOrders(ImportRequest<ChannelRequests.ImportRow> req) {
        List<ImportResult.RowError> errors = new ArrayList<>();
        Map<String, List<ImportRow<ChannelRequests.ImportRow>>> groups = new LinkedHashMap<>();
        for (ImportRow<ChannelRequests.ImportRow> row : req.rows()) {
            if (row == null || row.data() == null) {
                continue;
            }
            String error = validate(row.data());
            if (error != null) {
                errors.add(new ImportResult.RowError(row.rowNum(), error));
            } else {
                groups.computeIfAbsent(row.data().externalNo().trim(), k -> new ArrayList<>()).add(row);
            }
        }
        List<String> skipped = new ArrayList<>();
        int[] orders = {0};
        if (errors.isEmpty() && !groups.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> {
                Set<String> existing = orderMapper.selectList(Wrappers.<ScmOrder>lambdaQuery().select(ScmOrder::getExternalNo)
                        .in(ScmOrder::getExternalNo, groups.keySet())).stream().map(ScmOrder::getExternalNo).collect(Collectors.toSet());
                Map<String, ScmCustomer> customers = customerMapper.selectList(Wrappers.<ScmCustomer>lambdaQuery().in(ScmCustomer::getCode,
                                groups.values().stream().flatMap(List::stream).map(r -> r.data().customerCode().trim()).distinct().toList()))
                        .stream().collect(Collectors.toMap(ScmCustomer::getCode, Function.identity()));
                Map<String, WmsMaterial> materials = materialService.mapByCodes(
                        groups.values().stream().flatMap(List::stream).map(r -> r.data().materialCode().trim()).distinct().toList());
                for (Map.Entry<String, List<ImportRow<ChannelRequests.ImportRow>>> g : groups.entrySet()) {
                    if (existing.contains(g.getKey())) {
                        skipped.add(g.getKey());
                        continue;
                    }
                    try {
                        createOrder(g.getKey(), g.getValue(), customers, materials);
                        orders[0]++;
                    } catch (BizException e) {
                        errors.add(new ImportResult.RowError(g.getValue().get(0).rowNum(), Messages.get("scm.text.externalOrderNo", g.getKey(), e.getLocalizedMessage())));
                    }
                }
                if (!errors.isEmpty()) {
                    status.setRollbackOnly();
                }
            });
        }
        errors.sort(Comparator.comparing(ImportResult.RowError::rowNum, Comparator.nullsLast(Comparator.naturalOrder())));
        int total = req.rows().size();
        return new Result(total, errors.isEmpty() ? total : 0, errors, errors.isEmpty() ? orders[0] : 0, errors.isEmpty() ? skipped : List.of());
    }

    private void createOrder(String externalNo, List<ImportRow<ChannelRequests.ImportRow>> rows, Map<String, ScmCustomer> customers,
                             Map<String, WmsMaterial> materials) {
        ChannelRequests.ImportRow first = rows.get(0).data();
        String customerCode = first.customerCode().trim();
        if (rows.stream().anyMatch(r -> !r.data().customerCode().trim().equals(customerCode) || !r.data().orderDate().equals(first.orderDate()))) {
            throw new BizException("scm.error.ordersSameExternalOrderNoMust");
        }
        ScmCustomer customer = customers.get(customerCode);
        if (customer == null) {
            throw new BizException("scm.error.customerCodeDoesNotExist", customerCode);
        }
        List<OrderItemRequest> items = new ArrayList<>();
        for (ImportRow<ChannelRequests.ImportRow> r : rows) {
            ChannelRequests.ImportRow d = r.data();
            WmsMaterial m = materials.get(d.materialCode().trim());
            if (m == null) {
                throw new BizException("scm.error.materialCodeDoesNotExist", d.materialCode());
            }
            BigDecimal price = d.price();
            BigDecimal taxRate = DEFAULT_TAX_RATE;
            if (price == null) {
                ScmPriceList p = priceListMapper.selectQuote(m.getId(), customer.getId(), customer.getLevel(), d.qty(), d.orderDate());
                if (p == null) {
                    throw new BizException("scm.error.materialHasNoPriceListPrice2", m.getCode());
                }
                price = p.getPrice();
                taxRate = p.getTaxRate();
            }
            items.add(new OrderItemRequest(m.getId(), d.qty(), price, taxRate, d.deliveryDate(), d.remark()));
        }
        Long id = orderService.createDraft(OrderKind.SALES, new OrderRequest(customer.getId(), first.orderDate(), first.deliveryDate(), null,
                "外部单号 " + externalNo, items), OrderSource.IMPORT); // i18n-ignore
        orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate().set(ScmOrder::getExternalNo, externalNo).eq(ScmOrder::getId, id));
    }

    private String validate(ChannelRequests.ImportRow data) {
        var violations = validator.validate(data);
        if (violations.isEmpty()) {
            return null;
        }
        return violations.stream().sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage).distinct().collect(Collectors.joining("；"));
    }
}
