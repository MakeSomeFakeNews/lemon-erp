package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.wms.entity.WmsPriceLevel;
import com.lemon.erp.modules.wms.service.WmsPriceLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 价格列设置（零售价、批发价1-8 的名称和是否启用）
 */
@RestController
@RequestMapping("/wms/price-level")
@RequiredArgsConstructor
public class WmsPriceLevelController {

    private final WmsPriceLevelService levelService;

    /** 登录即可访问：物料、客户、单据表单显示价格列名称 */
    @GetMapping
    public R<List<WmsPriceLevel>> list() {
        return R.ok(levelService.list());
    }

    @PutMapping
    @PreAuthorize("@ss.hasPerm('wms:material:priceLevel')")
    @OperLog(title = "价格列设置", type = OperType.UPDATE) // i18n-ignore
    public R<Void> save(@RequestBody List<WmsPriceLevel> levels) {
        levelService.save(levels);
        return R.ok();
    }
}
