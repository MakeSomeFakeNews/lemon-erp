package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.entity.SysDictData;
import com.lemon.erp.modules.system.entity.SysDictType;
import com.lemon.erp.modules.system.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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

/**
 * 业务字典 /system/biz-dict：各租户维护自己的业务字典取值（如客户等级、出库类别、不良类型），只能访问业务字典；
 * 系统字典由平台在「字典管理」中维护
 */
@RestController
@RequestMapping("/system/biz-dict")
@RequiredArgsConstructor
public class SysBizDictController {

    private final SysDictService dictService;

    @GetMapping("/types")
    @PreAuthorize("@ss.hasPerm('sys:bizdict:list')")
    public R<List<SysDictType>> types() {
        return R.ok(dictService.businessTypes());
    }

    @GetMapping("/data")
    @PreAuthorize("@ss.hasPerm('sys:bizdict:list')")
    public R<PageResult<SysDictData>> pageData(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam String dictType,
                                               @RequestParam(required = false) String label) {
        return R.ok(dictService.pageBusinessData(current, size, dictType, label));
    }

    @PostMapping("/data")
    @PreAuthorize("@ss.hasPerm('sys:bizdict:edit')")
    @OperLog(title = "业务字典", type = OperType.INSERT) // i18n-ignore 操作日志存库
    public R<Void> createData(@Validated(Create.class) @RequestBody SysDictData data) {
        dictService.createBusinessData(data);
        return R.ok();
    }

    @PutMapping("/data/{id}")
    @PreAuthorize("@ss.hasPerm('sys:bizdict:edit')")
    @OperLog(title = "业务字典", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> updateData(@PathVariable Long id, @Validated(Update.class) @RequestBody SysDictData data) {
        data.setId(id);
        dictService.updateBusinessData(data);
        return R.ok();
    }

    @DeleteMapping("/data/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:bizdict:edit')")
    @OperLog(title = "业务字典", type = OperType.DELETE) // i18n-ignore 操作日志存库
    public R<Void> deleteData(@PathVariable List<Long> ids) {
        dictService.deleteBusinessData(ids);
        return R.ok();
    }
}
