package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 工程变更、技术文档的请求
 */
public final class PlmRequests {

    private PlmRequests() {
    }

    /**
     * 新建工程变更：复制基准 BOM 为新版本（停用），在 BOM 中修改后提交
     *
     * @param fromBomId  基准 BOM（变更前）
     * @param newVersion 新版本号（修改变更时忽略）
     */
    public record Ecn(
            @NotBlank(message = "{mes.valid.enterChangeTitle}") @Size(max = 128, message = "{mes.valid.titleCannotExceedCharacters}") String title,
            Long fromBomId,
            @Size(max = 16, message = "{mes.valid.versionCannotExceedCharacters}") String newVersion,
            @NotBlank(message = "{mes.valid.selectChangeReason}") String reason,
            @Size(max = 1000, message = "{mes.valid.changeDescriptionCannotExceedCharacters}") String description,
            @NotNull(message = "{mes.valid.selectEffectiveDate}") LocalDate effectiveDate,
            Boolean syncDraftWo) {
    }

    public record Doc(
            @NotBlank(message = "{mes.valid.enterDocumentName}") @Size(max = 128, message = "{mes.valid.nameCannotExceedCharacters2}") String name,
            @NotBlank(message = "{mes.valid.selectDocumentType}") String docType,
            Long materialId,
            @NotBlank(message = "{mes.valid.enterVersion2}") @Size(max = 16, message = "{mes.valid.versionCannotExceedCharacters2}") String version,
            Long ecnId,
            @Size(max = 500, message = "{mes.valid.descriptionCannotExceedCharacters2}") String description) {
    }

    /** 升版：从已发布的版本复制出新的草稿版本 */
    public record Revise(
            @NotBlank(message = "{mes.valid.enterNewVersion}") @Size(max = 16, message = "{mes.valid.versionCannotExceedCharacters2}") String version,
            Long ecnId,
            @Size(max = 500, message = "{mes.valid.descriptionCannotExceedCharacters2}") String description) {
    }
}
