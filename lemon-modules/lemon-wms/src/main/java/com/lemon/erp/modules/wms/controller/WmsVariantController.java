package com.lemon.erp.modules.wms.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.wms.entity.WmsAttribute;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsVariantService;
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
import java.util.Map;

/**
 * 多规格物料：规格属性、款式的 SKU 生成与规格矩阵
 */
@RestController
@RequestMapping("/wms")
@RequiredArgsConstructor
public class WmsVariantController {

    private final WmsVariantService variantService;

    /** 规格属性及属性值（物料、订单页面使用，登录即可） */
    @GetMapping("/attribute")
    public R<List<WmsAttribute>> attributes() {
        return R.ok(variantService.attributes());
    }

    @PostMapping("/attribute")
    @PreAuthorize("@ss.hasPerm('wms:attribute:edit')")
    @OperLog(title = "规格属性", type = OperType.INSERT) // i18n-ignore
    public R<Long> createAttribute(@Valid @RequestBody WmsAttribute attr) {
        attr.setId(null);
        return R.ok(variantService.saveAttribute(attr));
    }

    @PutMapping("/attribute/{id}")
    @PreAuthorize("@ss.hasPerm('wms:attribute:edit')")
    @OperLog(title = "规格属性", type = OperType.UPDATE) // i18n-ignore
    public R<Long> updateAttribute(@PathVariable Long id, @Valid @RequestBody WmsAttribute attr) {
        attr.setId(id);
        return R.ok(variantService.saveAttribute(attr));
    }

    @DeleteMapping("/attribute/{id}")
    @PreAuthorize("@ss.hasPerm('wms:attribute:edit')")
    @OperLog(title = "规格属性", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteAttribute(@PathVariable Long id) {
        variantService.deleteAttribute(id);
        return R.ok();
    }

    /** 款式（订单按规格矩阵录入时选择，登录即可） */
    @GetMapping("/material/templates")
    public R<List<WmsMaterial>> templates(@RequestParam(required = false) String keyword) {
        return R.ok(variantService.templateOptions(keyword));
    }

    /** 款式的规格矩阵：属性、属性值和已生成的 SKU（登录即可，订单录入使用） */
    @GetMapping("/material/{id}/variants")
    public R<Map<String, Object>> variants(@PathVariable Long id) {
        return R.ok(variantService.matrix(id));
    }

    @GetMapping("/material/{id}/variant-stock")
    @PreAuthorize("@ss.hasPerm('wms:stock:list')")
    public R<Map<Long, BigDecimal>> variantStock(@PathVariable Long id) {
        return R.ok(variantService.stock(id));
    }

    /** @param valueIds 规格属性 ID → 选中的属性值 ID */
    public record GenerateRequest(Map<Long, List<Long>> valueIds) {
    }

    @PostMapping("/material/{id}/variants")
    @PreAuthorize("@ss.hasPerm('wms:material:add')")
    @OperLog(title = "生成 SKU", type = OperType.INSERT) // i18n-ignore
    public R<Integer> generate(@PathVariable Long id, @RequestBody GenerateRequest req) {
        return R.ok(variantService.generate(id, req.valueIds()));
    }
}
