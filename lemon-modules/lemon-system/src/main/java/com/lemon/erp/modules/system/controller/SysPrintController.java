package com.lemon.erp.modules.system.controller;

import com.lemon.erp.modules.system.print.PrintRequest;
import com.lemon.erp.modules.system.print.PrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 单据打印：前端组装单据（已在页面上，有查看权限），这里只负责按模板生成 PDF，所有登录用户可用
 */
@RestController
@RequestMapping("/system/print")
@RequiredArgsConstructor
public class SysPrintController {

    private final PrintService printService;

    /** 生成 PDF（在浏览器中预览、打印或下载） */
    @PostMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@RequestBody PrintRequest request) {
        byte[] pdf = printService.pdf(request);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(request.getTitle() + ".pdf", StandardCharsets.UTF_8).build().toString())
                .body(pdf);
    }

    /** 当前配置下的默认版式 JRXML（用示例单据生成），用 Jaspersoft Studio 修改后上传 */
    @PostMapping("/jrxml")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    public ResponseEntity<byte[]> defaultJrxml(@RequestBody PrintRequest request) {
        String jrxml = printService.defaultJrxml(request);
        String name = (request.getType() == null ? "template" : request.getType()) + ".jrxml";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(name, StandardCharsets.UTF_8).build().toString())
                .body(jrxml.getBytes(StandardCharsets.UTF_8));
    }
}
