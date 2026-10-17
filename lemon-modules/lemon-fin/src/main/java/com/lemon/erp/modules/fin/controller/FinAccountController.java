package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
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

import java.util.List;
import com.lemon.erp.modules.fin.dto.AccountRequest;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.service.FinAccountService;

/**
 * 会计科目
 */
@RestController
@RequestMapping("/fin/account")
@RequiredArgsConstructor
public class FinAccountController {

    private final FinAccountService accountService;

    /** 科目树；有关键字时平铺返回匹配的科目 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:account:list')")
    public R<List<FinAccount>> tree(@RequestParam(required = false) String keyword) {
        return R.ok(accountService.tree(keyword));
    }

    /** 登录即可访问：全部科目（含上级），账簿、预算选择科目 */
    @GetMapping("/all")
    public R<List<FinAccount>> all() {
        return R.ok(accountService.listAll());
    }

    /** 登录即可访问：启用的末级科目，凭证录入、结算账户、折旧科目选择 */
    @GetMapping("/options")
    public R<List<FinAccount>> options() {
        return R.ok(accountService.leafOptions());
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:account:add')")
    @OperLog(title = "会计科目", type = OperType.INSERT) // i18n-ignore
    public R<Void> create(@Valid @RequestBody AccountRequest req) {
        accountService.create(req);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:account:edit')")
    @OperLog(title = "会计科目", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AccountRequest req) {
        accountService.update(id, req);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('fin:account:edit')")
    @OperLog(title = "会计科目-修改状态", type = OperType.UPDATE) // i18n-ignore
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        accountService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:account:remove')")
    @OperLog(title = "会计科目", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        accountService.delete(ids);
        return R.ok();
    }
}
