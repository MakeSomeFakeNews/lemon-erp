package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 节假日（工作日 = 周一至周五 + 调休上班 - 法定节假日）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_holiday")
public class HrHoliday extends BaseEntity {

    @NotNull(message = "请选择日期")
    private LocalDate holidayDate;

    @NotBlank(message = "请填写名称")
    @Size(max = 64, message = "名称不能超过64个字符")
    private String name;

    /** HOLIDAY 法定节假日 / WORKDAY 调休上班 */
    @NotBlank(message = "请选择类型")
    @Pattern(regexp = "HOLIDAY|WORKDAY", message = "类型不正确")
    private String type;
}
