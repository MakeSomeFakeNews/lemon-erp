package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
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

    @NotBlank(message = "职位编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "职位编码只能包含字母、数字、下划线和短横线，最多32个字符")
    private String code;

    @NotBlank(message = "职位名称不能为空")
    @Size(max = 64, message = "职位名称不能超过64个字符")
    private String name;

    /** 职位类别(字典 hr_position_category) */
    private String category;

    /** 职级，如 P5、M2 */
    @Size(max = 16, message = "职级不能超过16个字符")
    private String grade;

    private Integer sort;

    /** 编制人数，为空表示不限；招聘需求审核时校验 在职 + 招聘中 不超过编制 */
    @Min(value = 0, message = "编制人数不能小于0")
    private Integer headcount;

    /** 1 启用 0 停用 */
    private Integer status;

    /** 职责说明 */
    @Size(max = 255, message = "职责说明不能超过255个字符")
    private String remark;

    /** 在职人数 */
    @TableField(exist = false)
    private Integer employeeCount;
}
