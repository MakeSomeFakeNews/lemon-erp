package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.scm.dto.ChannelRequests;
import com.lemon.erp.modules.scm.dto.ScmPriceViews;
import com.lemon.erp.modules.scm.entity.ScmChannelReport;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.service.ScmChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 经销商门户：只能访问当前账号关联的客户（经销商）的数据
 */
@RestController
@RequestMapping("/dealer")
@RequiredArgsConstructor
public class ScmDealerPortalController {

    private final ScmChannelService channelService;

    /** 可订货的物料和价格 */
    @GetMapping("/catalog")
    @PreAuthorize("@ss.hasPerm('dealer:order:list')")
    public R<List<ScmPriceViews.DealerCatalogItem>> catalog(@RequestParam(required = false) String keyword) {
        return R.ok(channelService.catalog(SecurityUtils.requireCustomerId(), keyword));
    }

    @GetMapping("/order")
    @PreAuthorize("@ss.hasPerm('dealer:order:list')")
    public R<PageResult<ScmOrder>> orders(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size) {
        return R.ok(channelService.orders(SecurityUtils.requireCustomerId(), current, size));
    }

    @GetMapping("/order/{id}")
    @PreAuthorize("@ss.hasPerm('dealer:order:list')")
    public R<ScmOrder> order(@PathVariable Long id) {
        return R.ok(channelService.order(SecurityUtils.requireCustomerId(), id));
    }

    /** 下单，返回订单 ID */
    @PostMapping("/order")
    @PreAuthorize("@ss.hasPerm('dealer:order:add')")
    @OperLog(title = "经销商门户-订货", type = OperType.INSERT) // i18n-ignore
    public R<Long> placeOrder(@Valid @RequestBody ChannelRequests.DealerOrder req) {
        return R.ok(channelService.placeOrder(SecurityUtils.requireCustomerId(), req));
    }

    /** 撤销还没有审核的订单 */
    @DeleteMapping("/order/{id}")
    @PreAuthorize("@ss.hasPerm('dealer:order:add')")
    @OperLog(title = "经销商门户-撤销订单", type = OperType.DELETE) // i18n-ignore
    public R<Void> cancelOrder(@PathVariable Long id) {
        channelService.cancelOrder(SecurityUtils.requireCustomerId(), id);
        return R.ok();
    }

    @GetMapping("/rebate/balance")
    @PreAuthorize("@ss.hasPerm('dealer:order:list')")
    public R<BigDecimal> rebateBalance() {
        return R.ok(channelService.rebateBalance(SecurityUtils.requireCustomerId()));
    }

    @GetMapping("/report")
    @PreAuthorize("@ss.hasPerm('dealer:report:list')")
    public R<PageResult<ScmChannelReport>> reports(@RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "10") long size) {
        return R.ok(channelService.reports(current, size, SecurityUtils.requireCustomerId(), null, null));
    }

    @GetMapping("/report/{id}")
    @PreAuthorize("@ss.hasPerm('dealer:report:list')")
    public R<ScmChannelReport> report(@PathVariable Long id) {
        return R.ok(channelService.report(id, SecurityUtils.requireCustomerId()));
    }

    /** 上报某月（yyyyMM）的库存和销量，重复上报覆盖 */
    @PutMapping("/report/{period}")
    @PreAuthorize("@ss.hasPerm('dealer:report:edit')")
    @OperLog(title = "经销商门户-渠道上报", type = OperType.UPDATE) // i18n-ignore
    public R<Long> saveReport(@PathVariable int period, @Valid @RequestBody ChannelRequests.Report req) {
        return R.ok(channelService.saveReport(SecurityUtils.requireCustomerId(), period, req));
    }
}
