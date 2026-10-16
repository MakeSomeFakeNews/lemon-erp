package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.entity.SysAttachment;
import com.lemon.erp.modules.system.service.SysAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 业务附件：权限按业务类型和业务记录在服务中校验
 */
@RestController
@RequestMapping("/system/attachment")
@RequiredArgsConstructor
public class SysAttachmentController {

    private final SysAttachmentService attachmentService;

    @GetMapping
    public R<List<SysAttachment>> list(@RequestParam String bizType, @RequestParam Long bizId) {
        return R.ok(attachmentService.list(bizType, bizId));
    }

    @PostMapping
    @OperLog(title = "附件-上传", type = OperType.INSERT, saveParams = false)
    public R<SysAttachment> upload(@RequestParam String bizType, @RequestParam Long bizId,
                                   @RequestParam("file") MultipartFile file) {
        return R.ok(attachmentService.upload(bizType, bizId, file));
    }

    /** 下载：以附件形式返回，禁止浏览器按内容猜测类型 */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        var entry = attachmentService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(entry.getKey().getFileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(entry.getValue()));
    }

    @DeleteMapping("/{id}")
    @OperLog(title = "附件-删除", type = OperType.DELETE)
    public R<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return R.ok();
    }
}
