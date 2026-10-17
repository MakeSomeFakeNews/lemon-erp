package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 职位
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_position")
public class HrPosition extends BaseEntity {

    @NotBlank(message = "{hr.valid.positionCodeCannotEmpty}")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "{hr.valid.positionCodeMayOnlyContainLetters}")
    private String code;

    @NotBlank(message = "{hr.valid.positionNameCannotEmpty}")
    @Size(max = 64, message = "{hr.valid.positionNameCannotExceedCharacters}")
    private String name;

    /** 职位类别(字典 hr_position_category) */
    private String category;

    /** 职级，如 P5、M2 */
    @Size(max = 16, message = "{hr.valid.jobGradeCannotExceedCharacters}")
    private String grade;

    private Integer sort;

    /** 编制人数，为空表示不限；招聘需求审核时校验 在职 + 招聘中 不超过编制 */
    @Min(value = 0, message = "{hr.valid.headcountCannotLess}")
    private Integer headcount;

    /** 1 启用 0 停用 */
    private EnableStatus status;

    /** 职责说明 */
    @Size(max = 255, message = "{hr.valid.responsibilitiesCannotExceedCharacters}")
    private String remark;

    /** 在职人数 */
    @TableField(exist = false)
    private Integer employeeCount;
}
