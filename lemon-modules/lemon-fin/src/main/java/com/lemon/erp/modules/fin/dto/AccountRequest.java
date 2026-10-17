package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.AuxItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增 / 修改会计科目：下级科目的类别、方向默认继承上级
 *
 * @param parentId  上级科目，不填为一级科目
 * @param category  类别，一级科目必填
 * @param direction 余额方向 DEBIT / CREDIT，一级科目必填
 * @param auxItems  辅助核算项目，可多选；科目已使用时也可以修改（只影响之后录入的凭证）
 */
public record AccountRequest(
        @NotBlank(message = "{fin.valid.enterAccountCode}") @Pattern(regexp = "^\\d{4,20}$", message = "{fin.valid.accountCodeMustDigits}") String code,
        @NotBlank(message = "{fin.valid.enterAccountName}") @Size(max = 64, message = "{fin.valid.accountNameCannotExceedCharacters}") String name,
        Long parentId,
        String category,
        String direction,
        @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark,
        List<AuxItem> auxItems) {
}
