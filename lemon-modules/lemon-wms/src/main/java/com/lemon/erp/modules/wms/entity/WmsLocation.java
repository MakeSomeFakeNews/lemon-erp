package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.wms.model.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_location")
public class WmsLocation extends BaseEntity {

    @NotNull(message = "请选择所属仓库")
    private Long warehouseId;

    @NotBlank(message = "库位编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "库位编码只能包含字母、数字、下划线和短横线，最多32个字符")
    private String code;

    @Size(max = 64, message = "库位名称不能超过64个字符")
    private String name;

    /** 区域：拣货路径按 区域 → 排序 → 编码 */
    @Size(max = 32, message = "区域不能超过32个字符")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String zone;

    /** 类型，为空按存储位 */
    private LocationType locType;

    private Integer sort;

    /** 1 启用 0 停用 */
    private Integer status;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    /** 所属仓库名称，仅列表展示 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String warehouseName;
}
