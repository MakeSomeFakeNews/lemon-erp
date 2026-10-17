package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.entity.SysCustomField;
import com.lemon.erp.modules.system.service.SysCustomFieldService;
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
import java.util.Map;

/**
 * 自定义字段：定义维护、业务记录的值
 */
@RestController
@RequestMapping("/system/custom-field")
@RequiredArgsConstructor
public class SysCustomFieldController {

    private final SysCustomFieldService fieldService;

    /** 可扩展的业务类型 */
    @GetMapping("/types")
    @PreAuthorize("@ss.hasPerm('sys:customField:list')")
    public R<List<Map<String, String>>> types() {
        return R.ok(fieldService.types());
    }

    @GetMapping("/all")
    @PreAuthorize("@ss.hasPerm('sys:customField:list')")
    public R<List<SysCustomField>> all(@RequestParam(required = false) String bizType) {
        return R.ok(fieldService.all(bizType));
    }

    /** 业务类型启用的字段（表单、列表使用，登录即可） */
    @GetMapping
    public R<List<SysCustomField>> enabled(@RequestParam String bizType) {
        return R.ok(fieldService.enabled(bizType));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('sys:customField:edit')")
    @OperLog(title = "自定义字段", type = OperType.INSERT) // i18n-ignore 操作日志存库
    public R<Long> create(@Valid @RequestBody SysCustomField field) {
        field.setId(null);
        return R.ok(fieldService.save(field));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:customField:edit')")
    @OperLog(title = "自定义字段", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Long> update(@PathVariable Long id, @Valid @RequestBody SysCustomField field) {
        field.setId(id);
        return R.ok(fieldService.save(field));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('sys:customField:edit')")
    @OperLog(title = "自定义字段", type = OperType.DELETE) // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable Long id) {
        fieldService.delete(id);
        return R.ok();
    }

    /** 一条记录的值（按业务类型的查看权限和数据权限） */
    @GetMapping("/values")
    public R<Map<String, String>> values(@RequestParam String bizType, @RequestParam Long bizId) {
        return R.ok(fieldService.values(bizType, bizId));
    }

    /** 列表显示：多条记录的值 */
    @PostMapping("/values/batch")
    public R<Map<Long, Map<String, String>>> batchValues(@RequestParam String bizType, @RequestBody List<Long> bizIds) {
        return R.ok(fieldService.batchValues(bizType, bizIds));
    }

    /** 保存一条记录的值（按业务类型的修改权限和数据权限） */
    @PutMapping("/values")
    @OperLog(title = "自定义字段值", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> saveValues(@RequestParam String bizType, @RequestParam Long bizId, @RequestBody Map<String, String> values) {
        fieldService.saveValues(bizType, bizId, values);
        return R.ok();
    }
}
