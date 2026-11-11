package com.lemon.erp.modules.system.dto;

import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 修改打印格式。新增时 templateKey 必填，sourceId 为复制来源的格式（同一模板标识），为空时按默认版式；
 * 修改时 templateKey、sourceId 不使用。isDefault 为 true 时设为该单据的默认格式（原默认格式取消），false 不改变
 */
public record PrintFormatRequest(
        String templateKey,
        @NotBlank(message = "{system.valid.printFormatNameRequired}")
        @Size(max = 32, message = "{system.valid.printFormatNameTooLong}") String name,
        Long sourceId,
        Boolean isDefault,
        @Min(value = 0, message = "{system.valid.printFormatSortInvalid}")
        @Max(value = 9999, message = "{system.valid.printFormatSortInvalid}") Integer sort,
        EnableStatus status) {
}
