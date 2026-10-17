package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.mes.model.EcnStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 物料清单：产品的子件用量与工艺路线。只作为查询结果返回，新增 / 修改使用 BomRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_bom")
public class MesBom extends BaseEntity {

    /** 产品 */
    private Long materialId;

    private String version;

    /** 是否默认 BOM，每个产品只有一个 */
    private Integer isDefault;

    /** 生产提前期（天） */
    private Integer leadDays;

    /** 由工程变更创建的版本：变更生效前停用，只能在变更草稿时修改 */
    private Long ecnId;

    /** 1 启用 0 停用 */
    private EnableStatus status;

    private String remark;

    /** 创建该版本的工程变更 */
    @TableField(exist = false)
    private String ecnNo;
    @TableField(exist = false)
    private EcnStatus ecnStatus;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;

    /** 子件数，列表展示 */
    @TableField(exist = false)
    private Integer itemCount;

    /** 工序数，列表展示 */
    @TableField(exist = false)
    private Integer opCount;

    @TableField(exist = false)
    private List<MesBomItem> items;

    @TableField(exist = false)
    private List<MesBomOp> ops;
}
