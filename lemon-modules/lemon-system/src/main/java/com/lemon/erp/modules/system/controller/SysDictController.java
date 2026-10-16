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

@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService dictService;

    // ---------------- 字典类型 ----------------

    @GetMapping("/type")
    @PreAuthorize("@ss.hasPerm('sys:dict:list')")
    public R<PageResult<SysDictType>> pageType(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(required = false) String type,
                                               @RequestParam(required = false) Integer status) {
        return R.ok(dictService.pageType(current, size, name, type, status));
    }

    @GetMapping("/type/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dict:list')")
    public R<SysDictType> getType(@PathVariable Long id) {
        return R.ok(dictService.getType(id));
    }

    @PostMapping("/type")
    @PreAuthorize("@ss.hasPerm('sys:dict:add')")
    @OperLog(title = "字典类型", type = OperType.INSERT)
    public R<Void> createType(@Validated(Create.class) @RequestBody SysDictType type) {
        dictService.createType(type);
        return R.ok();
    }

    @PutMapping("/type/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dict:edit')")
    @OperLog(title = "字典类型", type = OperType.UPDATE)
    public R<Void> updateType(@PathVariable Long id, @Validated(Update.class) @RequestBody SysDictType type) {
        type.setId(id);
        dictService.updateType(type);
        return R.ok();
    }

    @DeleteMapping("/type/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:dict:remove')")
    @OperLog(title = "字典类型", type = OperType.DELETE)
    public R<Void> deleteTypes(@PathVariable List<Long> ids) {
        dictService.deleteTypes(ids);
        return R.ok();
    }

    // ---------------- 字典数据 ----------------

    @GetMapping("/data")
    @PreAuthorize("@ss.hasPerm('sys:dict:list')")
    public R<PageResult<SysDictData>> pageData(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam String dictType,
                                               @RequestParam(required = false) String label) {
        return R.ok(dictService.pageData(current, size, dictType, label));
    }

    /** 登录即可访问，前端渲染字典标签/下拉框使用 */
    @GetMapping("/data/type/{dictType}")
    public R<List<SysDictData>> listDataByType(@PathVariable String dictType) {
        return R.ok(dictService.listDataByType(dictType));
    }

    @PostMapping("/data")
    @PreAuthorize("@ss.hasPerm('sys:dict:add')")
    @OperLog(title = "字典数据", type = OperType.INSERT)
    public R<Void> createData(@Validated(Create.class) @RequestBody SysDictData data) {
        dictService.createData(data);
        return R.ok();
    }

    @PutMapping("/data/{id}")
    @PreAuthorize("@ss.hasPerm('sys:dict:edit')")
    @OperLog(title = "字典数据", type = OperType.UPDATE)
    public R<Void> updateData(@PathVariable Long id, @Validated(Update.class) @RequestBody SysDictData data) {
        data.setId(id);
        dictService.updateData(data);
        return R.ok();
    }

    @DeleteMapping("/data/{ids}")
    @PreAuthorize("@ss.hasPerm('sys:dict:remove')")
    @OperLog(title = "字典数据", type = OperType.DELETE)
    public R<Void> deleteData(@PathVariable List<Long> ids) {
        dictService.deleteData(ids);
        return R.ok();
    }
}
