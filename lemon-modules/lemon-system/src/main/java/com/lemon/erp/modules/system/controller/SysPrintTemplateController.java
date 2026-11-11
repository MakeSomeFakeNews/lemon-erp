package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.dto.PrintBackgroundRequest;
import com.lemon.erp.modules.system.dto.PrintFormatRequest;
import com.lemon.erp.modules.system.dto.PrintFormatVO;
import com.lemon.erp.modules.system.print.PrintService;
import com.lemon.erp.modules.system.service.SysPrintTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 打印模板（系统管理）。同一单据可以有多份打印格式：按模板标识的接口带 formatId 时操作该格式，不带时操作默认格式
 * （标签、通用设置只有默认格式）；维护格式沿用打印模板的权限
 */
@RestController
@RequestMapping("/system/print-template")
@RequiredArgsConstructor
public class SysPrintTemplateController {

    private final SysPrintTemplateService templateService;
    private final PrintService printService;

    /** 已修改的模板（各单据的默认格式），所有登录用户打印单据时使用 */
    @GetMapping
    public R<Map<String, Object>> all() {
        return R.ok(templateService.all());
    }

    /** 各单据的打印格式（含配置），所有登录用户打印单据时选择格式使用 */
    @GetMapping("/formats")
    public R<List<PrintFormatVO>> formats() {
        return R.ok(templateService.formats());
    }

    /** 新增打印格式（可从已有格式复制） */
    @PostMapping("/formats")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板-新增格式", type = OperType.INSERT) // i18n-ignore 操作日志存库
    public R<Long> createFormat(@Valid @RequestBody PrintFormatRequest req) {
        return R.ok(templateService.createFormat(req));
    }

    /** 修改打印格式的名称、排序、状态，或设为默认 */
    @PutMapping("/formats/{id}")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板-修改格式", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> updateFormat(@PathVariable Long id, @Valid @RequestBody PrintFormatRequest req) {
        templateService.updateFormat(id, req);
        return R.ok();
    }

    @DeleteMapping("/formats/{id}")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板-删除格式", type = OperType.DELETE) // i18n-ignore 操作日志存库
    public R<Void> deleteFormat(@PathVariable Long id) {
        templateService.deleteFormat(id);
        return R.ok();
    }

    @PutMapping("/{key}")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> save(@PathVariable String key, @RequestParam(required = false) Long formatId,
                        @RequestBody Map<String, Object> config) {
        templateService.save(key, formatId, config);
        return R.ok();
    }

    /** 恢复默认版式（同时删除上传的 JRXML）：带 formatId 时只清空该格式，不带时删除该单据的全部格式 */
    @DeleteMapping("/{key}")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板-恢复默认", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> reset(@PathVariable String key, @RequestParam(required = false) Long formatId) {
        templateService.reset(key, formatId);
        return R.ok();
    }

    /** 下载上传的 JRXML */
    @GetMapping("/{key}/jrxml")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    public ResponseEntity<byte[]> downloadJrxml(@PathVariable String key, @RequestParam(required = false) Long formatId) {
        String jrxml = templateService.jrxml(key, formatId);
        if (jrxml == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(key + ".jrxml", StandardCharsets.UTF_8).build().toString())
                .body(jrxml.getBytes(StandardCharsets.UTF_8));
    }

    /** 上传 JRXML：之后该单据按 JRXML 打印（SaaS 模式下只有平台租户可以上传） */
    @PostMapping("/{key}/jrxml")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板-上传 JRXML", type = OperType.UPDATE, saveParams = false) // i18n-ignore 操作日志存库
    public R<Void> uploadJrxml(@PathVariable String key, @RequestParam(required = false) Long formatId,
                               @RequestParam("file") MultipartFile file) throws IOException {
        printService.uploadJrxml(key, formatId, file);
        return R.ok();
    }

    /** 套打底图（data URL），没有时为空 */
    @GetMapping("/{key}/background")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:list')")
    public R<String> background(@PathVariable String key, @RequestParam(required = false) Long formatId) {
        return R.ok(templateService.background(key, formatId));
    }

    /** 保存套打底图（为空时删除） */
    @PutMapping("/{key}/background")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板-套打底图", type = OperType.UPDATE, saveParams = false) // i18n-ignore 操作日志存库
    public R<Void> saveBackground(@PathVariable String key, @RequestParam(required = false) Long formatId,
                                  @RequestBody PrintBackgroundRequest req) {
        templateService.saveBackground(key, formatId, req.dataUrl());
        return R.ok();
    }

    /** 删除上传的 JRXML，改回按配置打印 */
    @DeleteMapping("/{key}/jrxml")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板-删除 JRXML", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> removeJrxml(@PathVariable String key, @RequestParam(required = false) Long formatId) {
        templateService.removeJrxml(key, formatId);
        return R.ok();
    }
}
