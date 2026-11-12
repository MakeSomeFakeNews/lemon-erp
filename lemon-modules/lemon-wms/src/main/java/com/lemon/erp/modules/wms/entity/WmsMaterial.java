package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.data.SearchKeys;
import com.lemon.erp.framework.data.Searchable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.lemon.erp.modules.wms.model.CostMethod;
import com.lemon.erp.modules.wms.model.StockAlertMode;
import com.baomidou.mybatisplus.annotation.TableField;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_material")
public class WmsMaterial extends BaseEntity implements Searchable {

    /** 新增时留空则按编码规则自动生成 */
    @NotBlank(groups = Update.class, message = "{wms.valid.materialCodeRequired}")
    @Pattern(regexp = "^([A-Za-z0-9_.-]{1,64})?$", message = "{wms.valid.materialCodeMayOnlyContainLetters}")
    private String code;

    @NotBlank(message = "{wms.valid.materialNameRequired}")
    @Size(max = 128, message = "{wms.valid.materialNameCannotExceedCharacters}")
    private String name;

    @Size(max = 128, message = "{wms.valid.specificationCannotExceedCharacters}")
    private String spec;

    @Size(max = 16, message = "{wms.valid.unitMeasureCannotExceedCharacters}")
    private String unit;

    /** 条码（基本单位），扫码录入物料 */
    @Size(max = 64, message = "{wms.valid.barcodeCannotExceedCharacters}")
    private String barcode;

    /** 分类编码（物料分类 wms_material_category.code，多级，见 WmsMaterialCategoryService） */
    private String category;

    /** 品牌，业务字典 wms_material_brand */
    @Size(max = 64, message = "{wms.valid.brandTooLong}")
    private String brand;

    /** 是否启用批次管理：出入库必须填写批次号。产生库存业务后不能修改 */
    private Integer batchEnabled;

    /** 是否启用序列号管理：数量必须为整数，出入库逐个登记序列号。产生库存业务后不能修改 */
    private Integer serialEnabled;

    /** 是否来料检验：采购入库后冻结为待检，来料检验合格后才能出库 */
    private Integer iqcRequired;

    /**
     * 安全库存（库存下限）：按总库存预警时所有仓库的合计低于该值时预警；
     * 分仓库预警时为各仓库下限之和（保存时计算，供补货建议、MRP 使用）
     */
    @DecimalMin(value = "0", message = "{wms.valid.safetyStockCannotLess}")
    private BigDecimal safetyStock;

    /** 库存上限：按总库存预警时所有仓库的合计高于该值时预警；分仓库预警时为各仓库上限之和（保存时计算） */
    @DecimalMin(value = "0", message = "{wms.valid.maxStockCannotLess}")
    private BigDecimal maxStock;

    /** 库存预警方式，默认按总库存 */
    private StockAlertMode alertMode;

    /**
     * 分仓库的上下限（预警方式为分仓库时）：查看物料详情时返回；新增、修改时传入则整体替换，
     * 修改时不传（null）保留原设置
     */
    @Valid
    @TableField(exist = false)
    private List<WmsMaterialWarehouseLimit> warehouseLimits;

    /** 再订货点：预计库存不高于此值时建议补货，为空不参与补货建议 */
    @DecimalMin(value = "0", message = "{wms.valid.reorderPointCannotLess}")
    private BigDecimal reorderPoint;

    /** 再订货量（经济批量） */
    @DecimalMin(value = "0", message = "{wms.valid.reorderQuantityCannotLess}")
    private BigDecimal reorderQty;

    /** 默认仓库、库位：录入出入库单、订单时默认带出 */
    private Long defaultWarehouseId;

    private Long defaultLocationId;

    /** 参考成本价：没有核算单价时按此计价 */
    @DecimalMin(value = "0", message = "{wms.valid.referenceCostCannotLess}")
    private BigDecimal refCost;

    /** 零售价、批发价1-8（含税、基本单位、本位币）：价格列，名称和是否启用见价格列设置 */
    @DecimalMin(value = "0", message = "{wms.valid.priceCannotLess}")
    private BigDecimal retailPrice;

    @DecimalMin(value = "0", message = "{wms.valid.priceCannotLess}")
    private BigDecimal wholesalePrice1;

    @DecimalMin(value = "0", message = "{wms.valid.priceCannotLess}")
    private BigDecimal wholesalePrice2;

    @DecimalMin(value = "0", message = "{wms.valid.priceCannotLess}")
    private BigDecimal wholesalePrice3;

    @DecimalMin(value = "0", message = "{wms.valid.priceCannotLess}")
    private BigDecimal wholesalePrice4;

    @DecimalMin(value = "0", message = "{wms.valid.priceCannotLess}")
    private BigDecimal wholesalePrice5;

    @DecimalMin(value = "0", message = "{wms.valid.priceCannotLess}")
    private BigDecimal wholesalePrice6;

    @DecimalMin(value = "0", message = "{wms.valid.priceCannotLess}")
    private BigDecimal wholesalePrice7;

    @DecimalMin(value = "0", message = "{wms.valid.priceCannotLess}")
    private BigDecimal wholesalePrice8;

    /** 存货计价方法，默认月末一次加权平均；修改后从下一次成本核算开始生效 */
    private CostMethod costMethod;

    /** 是否多规格款式（1 是）：按规格属性生成 SKU，款式本身不能出入库；由「多规格」功能设置 */
    private Integer isTemplate;

    /** SKU 所属的款式 */
    private Long templateId;

    /** SKU 的规格属性值，如 红色 / XL */
    private String variantText;

    /** 主图 URL（图片中排在第一张的），由 {@link com.lemon.erp.modules.wms.service.WmsMaterialImageService} 维护，新增、修改物料时忽略 */
    private String imageUrl;

    /** 1 启用 0 停用 */
    private EnableStatus status;

    @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}")
    private String remark;

    /** 检索列：由编码、名称、规格、条码计算（{@link SearchKeys}），下拉搜索、列表关键字按它匹配拼音首字母和去掉分隔符的缩写 */
    @JsonIgnore
    private String searchKey;

    @Override
    public String[] searchTexts() {
        return new String[]{code, name, spec, barcode};
    }

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
