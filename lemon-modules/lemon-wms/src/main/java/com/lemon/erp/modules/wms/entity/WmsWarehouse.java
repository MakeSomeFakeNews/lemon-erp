package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_warehouse")
public class WmsWarehouse extends BaseEntity {

    @NotBlank(message = "{wms.valid.warehouseCodeRequired}")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "{wms.valid.warehouseCodeMayOnlyContainLetters}")
    private String code;

    @NotBlank(message = "{wms.valid.warehouseNameRequired}")
    @Size(max = 64, message = "{wms.valid.warehouseNameCannotExceedCharacters}")
    private String name;

    /** 仓库类型，字典 wms_warehouse_type：NORMAL / RAW / FINISHED / DEFECT（不良品仓不计入可用库存） */
    private String type;

    /** 寄售仓的客户 / 供应商 */
    private Long partnerId;

    @Size(max = 255, message = "{wms.valid.addressCannotExceedCharacters}")
    private String address;

    @Size(max = 64, message = "{wms.valid.contactCannotExceedCharacters}")
    private String contact;

    @Size(max = 32, message = "{wms.valid.contactPhoneCannotExceedCharacters}")
    private String phone;

    private Integer sort;

    /** 1 启用 0 停用；停用后不能在新单据中使用 */
    private Integer status;

    @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}")
    private String remark;
}
