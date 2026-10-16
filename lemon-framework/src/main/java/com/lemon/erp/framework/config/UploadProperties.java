package com.lemon.erp.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * @param path           公开文件（头像等）存储根目录，通过 /api/files/** 直接访问
 * @param attachmentPath 业务附件存储目录，不公开访问，只能通过附件接口按权限下载
 */
@ConfigurationProperties(prefix = "lemon.upload")
public record UploadProperties(String path, String attachmentPath) {

    public Path root() {
        return Path.of(path == null || path.isBlank() ? "./uploads" : path).toAbsolutePath().normalize();
    }

    public Path attachmentRoot() {
        return Path.of(attachmentPath == null || attachmentPath.isBlank() ? "./attachments" : attachmentPath)
                .toAbsolutePath().normalize();
    }
}
