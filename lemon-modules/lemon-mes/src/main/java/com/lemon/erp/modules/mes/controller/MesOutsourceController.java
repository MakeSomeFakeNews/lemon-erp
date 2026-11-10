package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.mes.dto.OutsourceCandidate;
import com.lemon.erp.modules.mes.dto.OutsourceLinesRequest;
import com.lemon.erp.modules.mes.dto.OutsourceStockRequest;
import com.lemon.erp.modules.mes.entity.MesOutsourceMaterial;
import com.lemon.erp.modules.mes.service.MesOutsourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 委外加工：委外订单（采购订单）的用料、发料、退料
 */
@RestController
@RequestMapping("/mes/outsource/{orderId}")
@RequiredArgsConstructor
public class MesOutsourceController {

    private final MesOutsourceService outsourceService;

    @GetMapping("/materials")
    @PreAuthorize("@ss.hasPerm('scm:purchase:list')")
    public R<List<MesOutsourceMaterial>> materials(@PathVariable Long orderId) {
        return R.ok(outsourceService.materials(orderId));
    }

    /** 可以按委外加工生成用料的明细（物料属性不是委外的自制件偶尔委外加工） */
    @GetMapping("/candidates")
    @PreAuthorize("@ss.hasPerm('scm:purchase:list')")
    public R<List<OutsourceCandidate>> candidates(@PathVariable Long orderId) {
        return R.ok(outsourceService.candidates(orderId));
    }

    /** 把选中的明细按委外加工生成用料 */
    @PostMapping("/lines")
    @PreAuthorize("@ss.hasPerm('scm:purchaseIn:add')")
    @OperLog(title = "委外用料-生成", type = OperType.INSERT) // i18n-ignore
    public R<Void> addLines(@PathVariable Long orderId, @Valid @RequestBody OutsourceLinesRequest req) {
        outsourceService.addLines(orderId, req.orderItemIds());
        return R.ok();
    }

    /** 委外发料：生成委外发料出库单 */
    @PostMapping("/issue")
    @PreAuthorize("@ss.hasPerm('scm:purchaseIn:add')")
    @OperLog(title = "委外发料", type = OperType.INSERT) // i18n-ignore
    public R<Void> issue(@PathVariable Long orderId, @Valid @RequestBody OutsourceStockRequest req) {
        outsourceService.issue(orderId, req);
        return R.ok();
    }

    /** 委外退料：加工商退回未用完的物料，生成委外收回入库单 */
    @PostMapping("/return")
    @PreAuthorize("@ss.hasPerm('scm:purchaseIn:add')")
    @OperLog(title = "委外退料", type = OperType.INSERT) // i18n-ignore
    public R<Void> returnMaterials(@PathVariable Long orderId, @Valid @RequestBody OutsourceStockRequest req) {
        outsourceService.returnMaterials(orderId, req);
        return R.ok();
    }
}
