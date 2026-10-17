package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.file.FileStorageService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.setting.Setting;
import com.lemon.erp.framework.setting.SettingService;
import com.lemon.erp.modules.system.entity.SysAttachment;
import com.lemon.erp.modules.system.mapper.SysAttachmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 业务附件：上传、列表、下载、删除。权限 = 业务类型登记的功能权限 + 业务记录的数据权限
 */
@Service
@RequiredArgsConstructor
public class SysAttachmentService {

    /** 每张单据的附件个数上限 */
    private static final int MAX_PER_BIZ = 50;

    private final SysAttachmentMapper attachmentMapper;
    private final FileStorageService fileStorageService;
    private final SysRecordAccessService recordAccessService;
    private final SettingService settings;

    public List<SysAttachment> list(String bizType, Long bizId) {
        checkAccess(bizType, bizId, false);
        return attachmentMapper.selectByBiz(bizType, bizId);
    }

    @Transactional
    public SysAttachment upload(String bizType, Long bizId, MultipartFile file) {
        checkAccess(bizType, bizId, true);
        if (attachmentMapper.selectCount(Wrappers.<SysAttachment>lambdaQuery()
                .eq(SysAttachment::getBizType, bizType).eq(SysAttachment::getBizId, bizId)) >= MAX_PER_BIZ) {
            throw new BizException("system.error.upAttachmentsPerDocument", MAX_PER_BIZ);
        }
        String name = file.getOriginalFilename();
        name = name == null || name.isBlank() ? "未命名" : Path.of(name.replace('\\', '/')).getFileName().toString(); // i18n-ignore 附件名称存库
        if (name.length() > 200) {
            name = name.substring(name.length() - 200);
        }
        // 单个附件上限见系统参数
        String path = fileStorageService.storeAttachment(file, settings.getInt(Setting.ATTACHMENT_MAX_MB) * 1024L * 1024);
        // 事务回滚时删除已保存的文件
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    fileStorageService.deleteAttachment(path);
                }
            }
        });
        SysAttachment a = new SysAttachment();
        a.setBizType(bizType);
        a.setBizId(bizId);
        a.setFileName(name);
        a.setFileSize(file.getSize());
        a.setContentType(file.getContentType());
        a.setStoragePath(path);
        a.setCreateBy(SecurityUtils.getUserId());
        attachmentMapper.insert(a);
        return a;
    }

    /** 下载：返回附件记录和文件路径 */
    public Map.Entry<SysAttachment, Path> download(Long id) {
        SysAttachment a = get(id);
        checkAccess(a.getBizType(), a.getBizId(), false);
        Path file = fileStorageService.resolveAttachment(a.getStoragePath());
        if (!Files.isRegularFile(file)) {
            throw new BizException("system.error.attachmentFileDoesNotExist");
        }
        return Map.entry(a, file);
    }

    @Transactional
    public void delete(Long id) {
        SysAttachment a = get(id);
        checkAccess(a.getBizType(), a.getBizId(), true);
        attachmentMapper.deleteById(id);
        String path = a.getStoragePath();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.deleteAttachment(path);
            }
        });
    }

    private SysAttachment get(Long id) {
        SysAttachment a = attachmentMapper.selectById(id);
        if (a == null) {
            throw new BizException("system.error.attachmentDoesNotExist");
        }
        return a;
    }

    private void checkAccess(String bizType, Long bizId, boolean write) {
        recordAccessService.check(bizType, bizId, write, "system.label.attachments");
    }
}
