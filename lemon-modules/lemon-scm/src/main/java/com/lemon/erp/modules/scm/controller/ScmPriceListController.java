package com.lemon.erp.modules.scm.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmPriceControl;
import com.lemon.erp.modules.scm.entity.ScmPriceList;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmPriceControlService;
import java.util.Map;
import com.lemon.erp.modules.scm.mapper.ScmPriceListMapper;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;

/**
 * 销售价目表
 */
@RestController
@RequestMapping("/scm/price-list")
@RequiredArgsConstructor
public class ScmPriceListController {

    private final ScmPriceListMapper priceListMapper;
    private final WmsMaterialService materialService;
    private final ScmCustomerService customerService;
    private final ScmPriceControlService controlService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:price:list')")
    public R<PageResult<ScmPriceList>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String level) {
        Page<ScmPriceList> page = Page.of(current, size);
        priceListMapper.selectPageList(page, keyword, level);
        return R.ok(PageResult.of(page));
    }

    /** 取价：销售订单、报价单选择物料后带出单价；没有匹配的价格时返回空 */
    @GetMapping("/quote")
    @PreAuthorize("@ss.hasAnyPerm('scm:price:list', 'scm:sales:add', 'scm:sales:edit', 'scm:quotation:add', 'scm:quotation:edit')")
    public R<ScmPriceList> quote(@RequestParam Long materialId, @RequestParam(required = false) Long customerId,
                                 @RequestParam(required = false) BigDecimal qty,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String level = null;
        if (customerId != null) {
            ScmCustomer c = customerService.get(customerId);
            level = c.getLevel();
        }
        return R.ok(priceListMapper.selectQuote(materialId, customerId, level, qty == null ? BigDecimal.ONE : qty, date == null ? LocalDate.now() : date));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:price:edit')")
    @OperLog(title = "销售价目表", type = OperType.INSERT) // i18n-ignore
    public R<Void> create(@Valid @RequestBody ScmPriceList req) {
        priceListMapper.insert(normalize(req, null));
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:price:edit')")
    @OperLog(title = "销售价目表", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ScmPriceList req) {
        if (priceListMapper.selectById(id) == null) {
            throw new BizException("价目不存在");
        }
        ScmPriceList p = normalize(req, id);
        priceListMapper.updateById(p);
        // 可为空的字段要能清空
        priceListMapper.update(Wrappers.<ScmPriceList>lambdaUpdate()
                .set(ScmPriceList::getCustomerLevel, p.getCustomerLevel())
                .set(ScmPriceList::getCustomerId, p.getCustomerId())
                .set(ScmPriceList::getValidFrom, p.getValidFrom())
                .set(ScmPriceList::getValidTo, p.getValidTo())
                .set(ScmPriceList::getRemark, p.getRemark())
                .eq(ScmPriceList::getId, id));
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:price:edit')")
    @OperLog(title = "销售价目表", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        priceListMapper.deleteByIds(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 价格控制、历史成交价

    @GetMapping("/control")
    @PreAuthorize("@ss.hasPerm('scm:price:list')")
    public R<PageResult<ScmPriceControl>> controls(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String keyword) {
        return R.ok(controlService.page(current, size, keyword));
    }

    /** 物料的价格控制：订单页面提示（登录即可） */
    @GetMapping("/control/of")
    public R<List<ScmPriceControl>> controlsOf(@RequestParam List<Long> materialIds) {
        return R.ok(controlService.ofMaterials(materialIds));
    }

    @PostMapping("/control")
    @PreAuthorize("@ss.hasPerm('scm:price:edit')")
    @OperLog(title = "价格控制", type = OperType.INSERT) // i18n-ignore
    public R<Void> createControl(@Valid @RequestBody ScmPriceControl req) {
        materialService.get(req.getMaterialId());
        req.setId(null);
        controlService.save(req);
        return R.ok();
    }

    @PutMapping("/control/{id}")
    @PreAuthorize("@ss.hasPerm('scm:price:edit')")
    @OperLog(title = "价格控制", type = OperType.UPDATE) // i18n-ignore
    public R<Void> updateControl(@PathVariable Long id, @Valid @RequestBody ScmPriceControl req) {
        materialService.get(req.getMaterialId());
        req.setId(id);
        controlService.save(req);
        return R.ok();
    }

    @DeleteMapping("/control/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:price:edit')")
    @OperLog(title = "价格控制", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteControl(@PathVariable List<Long> ids) {
        controlService.delete(ids);
        return R.ok();
    }

    /** 往来单位某物料的历史成交价（kind 为 sales / purchase），订单录入时参考 */
    @GetMapping("/history")
    @PreAuthorize("@ss.hasAnyPerm('scm:price:list', 'scm:sales:list', 'scm:purchase:list')")
    public R<List<Map<String, Object>>> history(@RequestParam String kind, @RequestParam Long partnerId, @RequestParam Long materialId,
                                                @RequestParam(defaultValue = "5") int limit) {
        return R.ok(controlService.history(OrderKind.ofPath(kind), partnerId, materialId, limit));
    }

    private ScmPriceList normalize(ScmPriceList p, Long id) {
        materialService.get(p.getMaterialId());
        if (p.getValidFrom() != null && p.getValidTo() != null && p.getValidTo().isBefore(p.getValidFrom())) {
            throw new BizException("有效期止不能早于有效期起");
        }
        p.setId(id);
        p.setCustomerLevel(p.getCustomerLevel() == null || p.getCustomerLevel().isBlank() ? null : p.getCustomerLevel());
        // 客户专属价只看客户，不再区分等级
        if (p.getCustomerId() != null) {
            customerService.get(p.getCustomerId());
            p.setCustomerLevel(null);
        }
        p.setMinQty(p.getMinQty() == null ? BigDecimal.ZERO : p.getMinQty());
        p.setTaxRate(p.getTaxRate() == null ? BigDecimal.valueOf(13) : p.getTaxRate());
        return p;
    }
}
