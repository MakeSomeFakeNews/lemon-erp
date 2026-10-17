package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.ChannelRequests;
import com.lemon.erp.modules.scm.entity.ScmChannelReport;
import com.lemon.erp.modules.scm.service.ScmChannelService;
import com.lemon.erp.modules.scm.service.ScmOrderImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 渠道：渠道库存销量上报（内部查看）、外部订单导入
 */
@RestController
@RequiredArgsConstructor
public class ScmChannelController {

    private final ScmChannelService channelService;
    private final ScmOrderImportService importService;

    @GetMapping("/scm/channel/reports")
    @PreAuthorize("@ss.hasPerm('scm:channel:list')")
    public R<PageResult<ScmChannelReport>> reports(@RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) Long customerId,
                                                   @RequestParam(required = false) Integer period,
                                                   @RequestParam(required = false) String keyword) {
        return R.ok(channelService.reports(current, size, customerId, period, keyword));
    }

    @GetMapping("/scm/channel/reports/{id}")
    @PreAuthorize("@ss.hasPerm('scm:channel:list')")
    public R<ScmChannelReport> report(@PathVariable Long id) {
        return R.ok(channelService.report(id, null));
    }

    /**
     * 外部订单导入（电商 / 门店）：统一格式按行提交，同一外部单号为一张订单，已导入的外部单号跳过；也可供外部系统用接口调用
     */
    @PostMapping("/scm/sales/import")
    @PreAuthorize("@ss.hasPerm('scm:sales:add')")
    @OperLog(title = "销售订单-外部导入", type = OperType.INSERT) // i18n-ignore
    public R<ScmOrderImportService.Result> importOrders(@Valid @RequestBody ImportRequest<ChannelRequests.ImportRow> req) {
        return R.ok(importService.importOrders(req));
    }
}
