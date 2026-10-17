package com.lemon.erp.framework.file;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.config.UploadProperties;
import com.lemon.erp.framework.config.WebMvcConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件存储，按 {业务目录}/{yyyy}/{MM}/{uuid}.{ext} 存放，返回可直接访问的 URL
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    public static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");

    private final UploadProperties uploadProperties;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * 保存图片
     *
     * @param dir     业务目录，如 avatar
     * @param maxSize 最大字节数
     * @return 访问 URL，如 /api/files/avatar/2026/10/xxx.png
     */
    public String storeImage(MultipartFile file, String dir, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择文件");
        }
        if (file.getSize() > maxSize) {
            throw new BizException("文件大小不能超过 {0}MB", maxSize / 1024 / 1024);
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        ext = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        if (!IMAGE_EXTENSIONS.contains(ext) || contentType == null || !contentType.startsWith("image/")) {
            throw new BizException("只支持 {0} 格式的图片", String.join("/", IMAGE_EXTENSIONS));
        }
        String relative = dir + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM")) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = uploadProperties.root().resolve(relative).normalize();
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException("文件保存失败：{0}", e.getMessage());
        }
        return contextPath + WebMvcConfig.FILE_URL_PREFIX + relative;
    }

    /** 附件禁止上传的扩展名（可执行文件、脚本） */
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of("exe", "bat", "cmd", "com", "msi", "sh", "ps1", "vbs",
            "js", "jar", "dll", "scr", "html", "htm", "svg", "php", "jsp");

    /**
     * 保存业务附件到附件目录（不公开访问）
     *
     * @return 相对附件目录的存储路径
     */
    public String storeAttachment(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择文件");
        }
        if (file.getSize() > maxSize) {
            throw new BizException("文件大小不能超过 {0}MB", maxSize / 1024 / 1024);
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        ext = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        if (BLOCKED_EXTENSIONS.contains(ext) || !ext.matches("[a-z0-9]{0,10}")) {
            throw new BizException("不支持上传该类型的文件");
        }
        String relative = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM")) + "/"
                + UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
        Path target = resolveAttachment(relative);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException("文件保存失败：{0}", e.getMessage());
        }
        return relative;
    }

    /** 附件的文件路径；存储路径不能跳出附件目录 */
    public Path resolveAttachment(String relative) {
        Path root = uploadProperties.attachmentRoot();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new BizException("文件路径不正确");
        }
        return target;
    }

    /** 删除附件文件，失败忽略（记录已删除即可） */
    public void deleteAttachment(String relative) {
        try {
            Files.deleteIfExists(resolveAttachment(relative));
        } catch (IOException | BizException ignored) {
            // 文件可能已被手工清理
        }
    }
}
