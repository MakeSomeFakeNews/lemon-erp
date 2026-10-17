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

    @NotBlank(message = "{mes.valid.equipmentCodeCannotEmpty}")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "{mes.valid.equipmentCodeOnlyContainLettersDigits}")
    private String code;

    @NotBlank(message = "{mes.valid.equipmentNameCannotEmpty}")
    @Size(max = 64, message = "{mes.valid.equipmentNameCannotExceedCharacters}")
    private String name;

    @Size(max = 64, message = "{mes.valid.modelCannotExceedCharacters}")
    private String model;

    private Long workCenterId;

    @Size(max = 64, message = "{mes.valid.installationLocationCannotExceedCharacters}")
    private String location;

    private LocalDate purchaseDate;

    /** 维修单开始处理时自动变为维修中，完成后恢复运行 */
    private EquipmentStatus status;

    /** 保养周期（天），为空不做定期保养 */
    @Min(value = 1, message = "{mes.valid.maintenanceCycleMustLeastDay}")
    @Max(value = 3650, message = "{mes.valid.maintenanceCycleCannotExceedDays}")
    private Integer maintainCycleDays;

    private LocalDate lastMaintainDate;

    /** 下次保养日期，保存时按上次保养日期 + 周期计算 */
    private LocalDate nextMaintainDate;

    @Size(max = 255, message = "{mes.valid.remarkCannotExceedCharacters}")
    private String remark;

    @TableField(exist = false)
    private String workCenterName;
}
