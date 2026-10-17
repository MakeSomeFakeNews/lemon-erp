package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.print.PrintService;
import com.lemon.erp.modules.system.service.SysPrintTemplateService;
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
import java.util.Map;

/**
 * 打印模板（系统管理）
 */
@RestController
@RequestMapping("/system/print-template")
@RequiredArgsConstructor
public class SysPrintTemplateController {

    private final SysPrintTemplateService templateService;
    private final PrintService printService;

    /** 已修改的模板，所有登录用户打印单据时使用 */
    @GetMapping
    public R<Map<String, Object>> all() {
        return R.ok(templateService.all());
    }

    @PutMapping("/{key}")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板", type = OperType.UPDATE)
    public R<Void> save(@PathVariable String key, @RequestBody Map<String, Object> config) {
        templateService.save(key, config);
        return R.ok();
    }

    /** 恢复默认版式（同时删除上传的 JRXML） */
    @DeleteMapping("/{key}")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板-恢复默认", type = OperType.UPDATE)
    public R<Void> reset(@PathVariable String key) {
        templateService.reset(key);
        return R.ok();
    }

    /** 下载上传的 JRXML */
    @GetMapping("/{key}/jrxml")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    public ResponseEntity<byte[]> downloadJrxml(@PathVariable String key) {
        String jrxml = templateService.jrxml(key);
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
    @OperLog(title = "打印模板-上传 JRXML", type = OperType.UPDATE, saveParams = false)
    public R<Void> uploadJrxml(@PathVariable String key, @RequestParam("file") MultipartFile file) throws IOException {
        printService.checkUploadAllowed();
        if (file.getSize() > 1024 * 1024) {
            return R.fail("模板文件不能超过 1MB");
        }
        String jrxml = new String(file.getBytes(), StandardCharsets.UTF_8);
        printService.validateJrxml(jrxml);
        templateService.saveJrxml(key, jrxml);
        return R.ok();
    }

    /** 套打底图（data URL），没有时为空 */
    @GetMapping("/{key}/background")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:list')")
    public R<String> background(@PathVariable String key) {
        return R.ok(templateService.background(key));
    }

    public record BackgroundRequest(String dataUrl) {
    }

    /** 保存套打底图（为空时删除） */
    @PutMapping("/{key}/background")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板-套打底图", type = OperType.UPDATE, saveParams = false)
    public R<Void> saveBackground(@PathVariable String key, @RequestBody BackgroundRequest req) {
        templateService.saveBackground(key, req.dataUrl());
        return R.ok();
    }

    /** 删除上传的 JRXML，改回按配置打印 */
    @DeleteMapping("/{key}/jrxml")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板-删除 JRXML", type = OperType.UPDATE)
    public R<Void> removeJrxml(@PathVariable String key) {
        templateService.removeJrxml(key);
        return R.ok();
    }
}
