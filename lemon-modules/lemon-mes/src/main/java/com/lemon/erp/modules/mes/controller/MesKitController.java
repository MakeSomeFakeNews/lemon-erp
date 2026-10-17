package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.modules.mes.model.KitStatus;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.mes.dto.KitLineView;
import com.lemon.erp.modules.mes.dto.KitReleaseRequest;
import com.lemon.erp.modules.mes.dto.MaterialKitView;
import com.lemon.erp.modules.mes.dto.SubstituteSwitchRequest;
import com.lemon.erp.modules.mes.dto.WoKitView;
import com.lemon.erp.modules.mes.service.MesKitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 齐套分析：工单齐套、生产用料汇总、缺料下单、替代料切换
 */
@RestController
@RequestMapping("/mes/kit")
@RequiredArgsConstructor
public class MesKitController {

    private final MesKitService kitService;

    /** 未结束工单的齐套情况，kitStatus 见字典 mes_kit_status */
    @GetMapping("/orders")
    @PreAuthorize("@ss.hasPerm('mes:kit:list')")
    public R<List<WoKitView>> orders(@RequestParam(required = false) String woNo,
                                     @RequestParam(required = false) KitStatus kitStatus) {
        return R.ok(kitService.orders(woNo, kitStatus));
    }

    @GetMapping("/orders/{woId}")
    @PreAuthorize("@ss.hasPerm('mes:kit:list')")
    public R<List<KitLineView>> orderLines(@PathVariable Long woId) {
        return R.ok(kitService.orderLines(woId));
    }

    /** 生产用料汇总 */
    @GetMapping("/materials")
    @PreAuthorize("@ss.hasPerm('mes:kit:list')")
    public R<List<MaterialKitView>> materials(@RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "false") boolean onlyShort) {
        return R.ok(kitService.materials(keyword, onlyShort));
    }

    @GetMapping("/materials/{materialId}")
    @PreAuthorize("@ss.hasPerm('mes:kit:list')")
    public R<List<KitLineView>> materialLines(@PathVariable Long materialId) {
        return R.ok(kitService.materialLines(materialId));
    }

    /** @return 生成的采购订单号、工单号 */
    @PostMapping("/release")
    @PreAuthorize("@ss.hasPerm('mes:kit:release')")
    @OperLog(title = "齐套分析-缺料下单", type = OperType.INSERT) // i18n-ignore
    public R<List<String>> release(@Valid @RequestBody KitReleaseRequest req) {
        return R.ok(kitService.release(req));
    }

    @PostMapping("/substitute")
    @PreAuthorize("@ss.hasPerm('mes:kit:substitute')")
    @OperLog(title = "齐套分析-替代料", type = OperType.UPDATE) // i18n-ignore
    public R<Void> substitute(@Valid @RequestBody SubstituteSwitchRequest req) {
        kitService.switchSubstitute(req);
        return R.ok();
    }
}
