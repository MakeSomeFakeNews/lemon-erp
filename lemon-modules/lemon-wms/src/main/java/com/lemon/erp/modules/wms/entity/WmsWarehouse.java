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

    @NotBlank(message = "仓库编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "仓库编码只能包含字母、数字、下划线和短横线，最多32个字符")
    private String code;

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 64, message = "仓库名称不能超过64个字符")
    private String name;

    /** 仓库类型，字典 wms_warehouse_type：NORMAL / RAW / FINISHED / DEFECT（不良品仓不计入可用库存） */
    private String type;

    /** 寄售仓的客户 / 供应商 */
    private Long partnerId;

    @Size(max = 255, message = "地址不能超过255个字符")
    private String address;

    @Size(max = 64, message = "联系人不能超过64个字符")
    private String contact;

    @Size(max = 32, message = "联系电话不能超过32个字符")
    private String phone;

    private Integer sort;

    /** 1 启用 0 停用；停用后不能在新单据中使用 */
    private Integer status;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
