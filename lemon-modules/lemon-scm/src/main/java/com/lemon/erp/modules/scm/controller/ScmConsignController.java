package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.ConsignSettleRequest;
import com.lemon.erp.modules.scm.service.ScmConsignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 寄售结算（寄售仓的库存在「库存查询」中按仓库查看）
 */
@RestController
@RequestMapping("/scm/consign")
@RequiredArgsConstructor
public class ScmConsignController {

    private final ScmConsignService consignService;

    /** 客户寄售结算：生成销售订单并从客户寄售仓出库 */
    @PostMapping("/customer")
    @PreAuthorize("@ss.hasPerm('scm:consign:settle')")
    @OperLog(title = "客户寄售结算", type = OperType.INSERT) // i18n-ignore
    public R<Map<String, Long>> settleCustomer(@Valid @RequestBody ConsignSettleRequest req) {
        return R.ok(consignService.settleCustomer(req));
    }

    /** 供应商寄售（VMI）结算：生成采购订单、采购入库到自有仓库，寄售仓消耗出库 */
    @PostMapping("/supplier")
    @PreAuthorize("@ss.hasPerm('scm:consign:settle')")
    @OperLog(title = "供应商寄售结算", type = OperType.INSERT) // i18n-ignore
    public R<Map<String, Long>> settleSupplier(@Valid @RequestBody ConsignSettleRequest req) {
        return R.ok(consignService.settleSupplier(req));
    }
}
