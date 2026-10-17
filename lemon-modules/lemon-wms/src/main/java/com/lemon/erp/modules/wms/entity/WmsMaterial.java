package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.validation.Update;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.lemon.erp.modules.wms.model.CostMethod;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_material")
public class WmsMaterial extends BaseEntity {

    /** 新增时留空则按编码规则自动生成 */
    @NotBlank(groups = Update.class, message = "物料编码不能为空")
    @Pattern(regexp = "^([A-Za-z0-9_.-]{1,64})?$", message = "物料编码只能包含字母、数字、下划线、点和短横线，最多64个字符")
    private String code;

    @NotBlank(message = "物料名称不能为空")
    @Size(max = 128, message = "物料名称不能超过128个字符")
    private String name;

    @Size(max = 128, message = "规格型号不能超过128个字符")
    private String spec;

    @Size(max = 16, message = "计量单位不能超过16个字符")
    private String unit;

    /** 条码（基本单位），扫码录入物料 */
    @Size(max = 64, message = "条码不能超过64个字符")
    private String barcode;

    /** 分类，字典 wms_material_category */
    private String category;

    /** 是否启用批次管理：出入库必须填写批次号。产生库存业务后不能修改 */
    private Integer batchEnabled;

    /** 是否启用序列号管理：数量必须为整数，出入库逐个登记序列号。产生库存业务后不能修改 */
    private Integer serialEnabled;

    /** 是否来料检验：采购入库后冻结为待检，来料检验合格后才能出库 */
    private Integer iqcRequired;

    /** 安全库存，汇总库存低于该值时预警 */
    @DecimalMin(value = "0", message = "安全库存不能小于0")
    private BigDecimal safetyStock;

    /** 再订货点：预计库存不高于此值时建议补货，为空不参与补货建议 */
    @DecimalMin(value = "0", message = "再订货点不能小于0")
    private BigDecimal reorderPoint;

    /** 再订货量（经济批量） */
    @DecimalMin(value = "0", message = "再订货量不能小于0")
    private BigDecimal reorderQty;

    /** 默认仓库、库位：录入出入库单、订单时默认带出 */
    private Long defaultWarehouseId;

    private Long defaultLocationId;

    /** 参考成本价：没有核算单价时按此计价 */
    @DecimalMin(value = "0", message = "参考成本价不能小于0")
    private BigDecimal refCost;

    /** 存货计价方法，默认月末一次加权平均；修改后从下一次成本核算开始生效 */
    private CostMethod costMethod;

    /** 是否多规格款式（1 是）：按规格属性生成 SKU，款式本身不能出入库；由「多规格」功能设置 */
    private Integer isTemplate;

    /** SKU 所属的款式 */
    private Long templateId;

    /** SKU 的规格属性值，如 红色 / XL */
    private String variantText;

    /** 1 启用 0 停用 */
    private Integer status;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    public boolean isTemplateMaterial() {
        return Integer.valueOf(1).equals(isTemplate);
    }

    public boolean isBatchManaged() {
        return Integer.valueOf(1).equals(batchEnabled);
    }

    public boolean needsIqc() {
        return Integer.valueOf(1).equals(iqcRequired);
    }

    public boolean isSerialManaged() {
        return Integer.valueOf(1).equals(serialEnabled);
    }
}
