package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.DocType;
import com.lemon.erp.modules.mes.model.EcnReason;
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
            @NotBlank(message = "请填写变更标题") @Size(max = 128, message = "标题不能超过128个字符") String title,
            Long fromBomId,
            @Size(max = 16, message = "版本号不能超过16个字符") String newVersion,
            @NotNull(message = "请选择变更原因") EcnReason reason,
            @Size(max = 1000, message = "变更说明不能超过1000个字符") String description,
            @NotNull(message = "请选择生效日期") LocalDate effectiveDate,
            Boolean syncDraftWo) {
    }

    public record Doc(
            @NotBlank(message = "请填写文档名称") @Size(max = 128, message = "名称不能超过128个字符") String name,
            @NotNull(message = "请选择文档类型") DocType docType,
            Long materialId,
            @NotBlank(message = "请填写版本") @Size(max = 16, message = "版本不能超过16个字符") String version,
            Long ecnId,
            @Size(max = 500, message = "说明不能超过500个字符") String description) {
    }

    /** 升版：从已发布的版本复制出新的草稿版本 */
    public record Revise(
            @NotBlank(message = "请填写新版本") @Size(max = 16, message = "版本不能超过16个字符") String version,
            Long ecnId,
            @Size(max = 500, message = "说明不能超过500个字符") String description) {
    }
}
