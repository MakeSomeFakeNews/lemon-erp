package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.IoRequests;
import com.lemon.erp.modules.fin.entity.FinIoCategory;
import com.lemon.erp.modules.fin.entity.FinIoDoc;
import com.lemon.erp.modules.fin.model.FundDirection;
import com.lemon.erp.modules.fin.model.IoStatus;
import com.lemon.erp.modules.fin.service.FinIoCategoryService;
import com.lemon.erp.modules.fin.service.FinIoService;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 其他收支 /fin/io：其他收入单、费用支出单、收支类别、按类别汇总与明细。
 * 权限 fin:io:list/add/edit/remove/confirm，收支类别维护 fin:io:category
 */
@RestController
@RequestMapping("/fin/io")
@RequiredArgsConstructor
public class FinIoController {

    private final FinIoService ioService;
    private final FinIoCategoryService categoryService;

    // ---------------------------------------------------------------- 收支类别

    /** 收支类别：录入单据时按方向取启用的（status=1），维护页面取全部 */
    @GetMapping("/category")
    @PreAuthorize("@ss.hasPerm('fin:io:list')")
    public R<List<FinIoCategory>> categories(@RequestParam(required = false) FundDirection direction,
                                             @RequestParam(required = false) EnableStatus status) {
        return R.ok(categoryService.list(direction, status));
    }

    @PostMapping("/category")
    @PreAuthorize("@ss.hasPerm('fin:io:category')")
    @OperLog(title = "收支类别", type = OperType.INSERT) // i18n-ignore
    public R<Long> createCategory(@Valid @RequestBody IoRequests.CategorySave req) {
        return R.ok(categoryService.create(req));
    }

    @PutMapping("/category/{id}")
    @PreAuthorize("@ss.hasPerm('fin:io:category')")
    @OperLog(title = "收支类别", type = OperType.UPDATE) // i18n-ignore
    public R<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody IoRequests.CategorySave req) {
        categoryService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/category/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:io:category')")
    @OperLog(title = "收支类别", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteCategories(@PathVariable List<Long> ids) {
        categoryService.delete(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 汇总、明细

    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('fin:io:list')")
    public R<List<IoRequests.SummaryRow>> summary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                  @RequestParam(required = false) FundDirection direction) {
        return R.ok(ioService.summary(begin, end, direction));
    }

    @GetMapping("/lines")
    @PreAuthorize("@ss.hasPerm('fin:io:list')")
    public R<PageResult<IoRequests.LineRow>> lines(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                   @RequestParam(required = false) FundDirection direction,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "10") long size) {
        return R.ok(ioService.lines(current, size, begin, end, direction, categoryId));
    }

    // ---------------------------------------------------------------- 收支单

    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:io:list')")
    public R<PageResult<FinIoDoc>> page(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) FundDirection direction,
                                        @RequestParam(required = false) IoStatus status,
                                        @RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) Long accountId,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(ioService.page(current, size, new IoRequests.Query(direction, status, categoryId, accountId, keyword, beginDate, endDate)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:io:list')")
    public R<FinIoDoc> detail(@PathVariable Long id) {
        return R.ok(ioService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:io:add')")
    @OperLog(title = "其他收支单", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody IoRequests.Save req) {
        return R.ok(ioService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:io:edit')")
    @OperLog(title = "其他收支单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody IoRequests.Save req) {
        ioService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:io:remove')")
    @OperLog(title = "其他收支单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        ioService.delete(ids);
        return R.ok();
    }

    /** 确认：生成凭证 */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('fin:io:confirm')")
    @OperLog(title = "其他收支单-确认", type = OperType.UPDATE) // i18n-ignore
    public R<Void> confirm(@PathVariable Long id) {
        ioService.confirm(id);
        return R.ok();
    }

    /** 取消确认：删除未审核的凭证 */
    @PostMapping("/{id}/cancel-confirm")
    @PreAuthorize("@ss.hasPerm('fin:io:confirm')")
    @OperLog(title = "其他收支单-取消确认", type = OperType.UPDATE) // i18n-ignore
    public R<Void> cancelConfirm(@PathVariable Long id) {
        ioService.cancelConfirm(id);
        return R.ok();
    }
}
