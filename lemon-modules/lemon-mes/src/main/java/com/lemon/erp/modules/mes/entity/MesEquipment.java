package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.mes.model.EquipmentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_equipment")
public class MesEquipment extends BaseEntity {

    @NotBlank(message = "设备编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "设备编码只能包含字母、数字、下划线和短横线，最多32个字符")
    private String code;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 64, message = "设备名称不能超过64个字符")
    private String name;

    @Size(max = 64, message = "规格型号不能超过64个字符")
    private String model;

    private Long workCenterId;

    @Size(max = 64, message = "安装位置不能超过64个字符")
    private String location;

    private LocalDate purchaseDate;

    /** 维修单开始处理时自动变为维修中，完成后恢复运行 */
    private EquipmentStatus status;

    /** 保养周期（天），为空不做定期保养 */
    @Min(value = 1, message = "保养周期至少1天")
    @Max(value = 3650, message = "保养周期不能超过3650天")
    private Integer maintainCycleDays;

    private LocalDate lastMaintainDate;

    /** 下次保养日期，保存时按上次保养日期 + 周期计算 */
    private LocalDate nextMaintainDate;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    @TableField(exist = false)
    private String workCenterName;
}
