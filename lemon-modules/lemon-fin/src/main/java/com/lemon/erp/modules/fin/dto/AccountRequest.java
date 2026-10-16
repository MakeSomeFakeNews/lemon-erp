package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 修改会计科目：下级科目的类别、方向默认继承上级
 *
 * @param parentId  上级科目，不填为一级科目
 * @param category  类别，一级科目必填
 * @param direction 余额方向 DEBIT / CREDIT，一级科目必填
 */
public record AccountRequest(
        @NotBlank(message = "请填写科目编码") @Pattern(regexp = "^\\d{4,20}$", message = "科目编码为 4-20 位数字") String code,
        @NotBlank(message = "请填写科目名称") @Size(max = 64, message = "科目名称不能超过64个字符") String name,
        Long parentId,
        String category,
        String direction,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
