package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.AssetMoveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 资产领用、归还、调拨：领用、调拨填写新的部门 / 使用人 / 存放地点；归还清空使用人，可填写归还后的存放地点
 */
public record AssetMoveRequest(
        @NotNull(message = "请选择变动类型") AssetMoveType moveType,
        @NotNull(message = "请选择日期") LocalDate moveDate,
        Long deptId,
        Long userId,
        @Size(max = 128, message = "存放地点不能超过128个字符") String location,
        @Size(max = 255, message = "说明不能超过255个字符") String remark) {
}
