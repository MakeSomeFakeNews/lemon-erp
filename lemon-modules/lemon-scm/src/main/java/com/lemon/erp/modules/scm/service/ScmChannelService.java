package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.scm.dto.ChannelRequests;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmChannelReport;
import com.lemon.erp.modules.scm.entity.ScmChannelReportItem;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmPriceList;
import com.lemon.erp.modules.scm.mapper.ScmChannelReportMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.mapper.ScmPriceListMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.SalesChannel;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 渠道（经销商）：经销商门户按价目表订货（销售订单草稿，自动应用促销、可抵扣返利，记到负责销售名下由内部审核），
 * 查看本经销商的订单，按月上报渠道库存和销量；内部查看渠道上报
 */
@Service
@RequiredArgsConstructor
public class ScmChannelService {

    /** 经销商订单的来源 */
    public static final String SOURCE_DEALER = "DEALER";

    private final ScmChannelReportMapper reportMapper;
    private final ScmPriceListMapper priceListMapper;
    private final ScmOrderMapper orderMapper;
    private final ScmOrderItemMapper itemMapper;
    private final ScmOrderService orderService;
    private final ScmCustomerService customerService;
    private final ScmRebateService rebateService;
    private final WmsMaterialService materialService;
    private final MessageSender messageSender;

    /** 可订货的物料：每个物料的基础价（不限起订量的最低一档）和起订数量阶梯 */
    public List<Map<String, Object>> catalog(Long customerId, String keyword) {
        ScmCustomer dealer = requireDealer(customerId);
        Map<Long, Map<String, Object>> byMaterial = new LinkedHashMap<>();
        Map<Long, List<Map<String, Object>>> tiers = new LinkedHashMap<>();
        Set<Long> levelSpecific = new HashSet<>();
        for (ScmPriceList p : reportMapper.selectCatalog(dealer.getLevel(), LocalDate.now(), keyword)) {
            // 有本等级价格的物料只用本等级的价格
            if (p.getCustomerLevel() != null) {
                levelSpecific.add(p.getMaterialId());
            } else if (levelSpecific.contains(p.getMaterialId())) {
                continue;
            }
            List<Map<String, Object>> materialTiers = tiers.computeIfAbsent(p.getMaterialId(), k -> new ArrayList<>());
            byMaterial.computeIfAbsent(p.getMaterialId(), k -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("materialId", p.getMaterialId());
                row.put("materialCode", p.getMaterialCode());
                row.put("materialName", p.getMaterialName());
                row.put("spec", p.getSpec());
                row.put("unit", p.getUnit());
                row.put("price", p.getPrice());
                row.put("taxRate", p.getTaxRate());
                row.put("tiers", materialTiers);
                return row;
            });
            if (p.getMinQty() != null && p.getMinQty().signum() > 0) {
                materialTiers.add(Map.of("minQty", p.getMinQty(), "price", p.getPrice()));
            }
        }
        return new ArrayList<>(byMaterial.values());
    }

    public PageResult<ScmOrder> orders(Long customerId, long current, long size) {
        Page<ScmOrder> page = orderMapper.selectPage(Page.of(current, size), Wrappers.<ScmOrder>lambdaQuery()
                .eq(ScmOrder::getKind, OrderKind.SALES.name()).eq(ScmOrder::getPartnerId, customerId).orderByDesc(ScmOrder::getId));
        return PageResult.of(page);
    }

    public ScmOrder order(Long customerId, Long id) {
        ScmOrder order = orderMapper.selectDetail(id);
        if (order == null || !OrderKind.SALES.name().equals(order.getKind()) || !customerId.equals(order.getPartnerId())) {
            throw new BizException("scm.error.orderDoesNotExist");
        }
        order.setItems(itemMapper.selectByOrderId(id));
        return order;
    }

    /** 经销商下单：单价按价目表（不在价目表中的物料不能订），生成销售订单草稿，通知销售审核 */
    @Transactional
    public Long placeOrder(Long customerId, ChannelRequests.DealerOrder req) {
        ScmCustomer dealer = requireDealer(customerId);
        LocalDate today = LocalDate.now();
        Map<Long, WmsMaterial> materials = materialService.getEnabled(req.items().stream().map(ChannelRequests.Line::materialId).toList());
        List<OrderItemRequest> items = req.items().stream().map(l -> {
            ScmPriceList p = priceListMapper.selectQuote(l.materialId(), dealer.getId(), dealer.getLevel(), l.qty(), today);
            if (p == null) {
                WmsMaterial m = materials.get(l.materialId());
                throw new BizException("scm.error.materialNotOrderableList", m.getCode(), m.getName());
            }
            return new OrderItemRequest(l.materialId(), l.qty(), p.getPrice(), p.getTaxRate(), null, null);
        }).toList();
        OrderRequest order = new OrderRequest(customerId, today, req.deliveryDate(), null, req.remark(), items, null, null, null,
                req.rebateAmount());
        Long id = orderService.createDraft(OrderKind.SALES, order, SOURCE_DEALER, true);
        // 记到负责销售名下，按其数据权限可见、审核
        if (dealer.getOwnerId() != null) {
            orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate().set(ScmOrder::getCreateBy, dealer.getOwnerId())
                    .set(ScmOrder::getDeptId, reportMapper.selectUserDept(dealer.getOwnerId())).eq(ScmOrder::getId, id));
        }
        ScmOrder created = orderMapper.selectById(id);
        messageSender.sendToPermission("scm:sales:approve", MessageSender.Message.of("REMIND",
                "经销商订货待审核：" + created.getOrderNo(), dealer.getName() + " 订货 ¥" + created.getTotalAmount().stripTrailingZeros().toPlainString(), // i18n-ignore
                "/scm/sales?id=" + id));
        return id;
    }

    public void cancelOrder(Long customerId, Long id) {
        orderService.deleteDealerDraft(customerId, id);
    }

    public BigDecimal rebateBalance(Long customerId) {
        return rebateService.balance(customerId, null);
    }

    // ---------------------------------------------------------------- 渠道库存销量上报

    public PageResult<ScmChannelReport> reports(long current, long size, Long customerId, Integer period, String keyword) {
        Page<ScmChannelReport> page = Page.of(current, size);
        reportMapper.selectPageList(page, customerId, period, keyword);
        return PageResult.of(page);
    }

    /** @param customerId 门户访问时校验所属经销商，内部访问为空 */
    public ScmChannelReport report(Long id, Long customerId) {
        ScmChannelReport r = id == null ? null : reportMapper.selectDetail(id);
        if (r == null || (customerId != null && !customerId.equals(r.getCustomerId()))) {
            throw new BizException("scm.error.reportRecordDoesNotExist");
        }
        r.setItems(reportMapper.selectItems(id));
        return r;
    }

    /** 上报：同一期间重复上报时覆盖明细 */
    @Transactional
    public Long saveReport(Long customerId, int period, ChannelRequests.Report req) {
        requireDealer(customerId);
        int month = period % 100;
        if (month < 1 || month > 12 || period > LocalDate.now().getYear() * 100 + LocalDate.now().getMonthValue()) {
            throw new BizException("scm.error.invalidReportingPeriodFutureMonthsCannot");
        }
        if (req.items().stream().map(ChannelRequests.ReportLine::materialId).distinct().count() != req.items().size()) {
            throw new BizException("scm.error.eachMaterialOnlyReportedOneLine");
        }
        materialService.mapByIds(req.items().stream().map(ChannelRequests.ReportLine::materialId).toList());
        ScmChannelReport r = reportMapper.selectOne(Wrappers.<ScmChannelReport>lambdaQuery()
                .eq(ScmChannelReport::getCustomerId, customerId).eq(ScmChannelReport::getPeriod, period));
        if (r == null) {
            r = new ScmChannelReport();
            r.setCustomerId(customerId);
            r.setPeriod(period);
        }
        r.setRemark(req.remark());
        r.setReportBy(SecurityUtils.getUserId());
        r.setReportTime(LocalDateTime.now());
        if (r.getId() == null) {
            reportMapper.insert(r);
        } else {
            reportMapper.updateById(r);
            reportMapper.deleteItems(r.getId());
        }
        reportMapper.insertItems(r.getId(), req.items().stream().map(l -> {
            ScmChannelReportItem i = new ScmChannelReportItem();
            i.setMaterialId(l.materialId());
            i.setSellQty(l.sellQty());
            i.setStockQty(l.stockQty());
            return i;
        }).toList());
        return r.getId();
    }

    private ScmCustomer requireDealer(Long customerId) {
        ScmCustomer c = customerService.get(customerId);
        if (c.getChannel() != SalesChannel.DEALER || !Integer.valueOf(1).equals(c.getStatus())) {
            throw new BizException("scm.error.customerNotEnabledDealer", c.getName());
        }
        return c;
    }
}
