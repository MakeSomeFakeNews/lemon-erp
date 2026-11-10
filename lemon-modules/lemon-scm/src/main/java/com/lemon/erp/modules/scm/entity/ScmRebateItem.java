package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 返利项目（计算方式为「按项目填写金额」时）：项目名称、可选的品牌 / 物料分类、返利金额
 */
@Data
@TableName("scm_rebate_item")
public class ScmRebateItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long rebateId;

    private String itemName;

    /** 品牌（业务字典 wms_material_brand），可为空 */
    private String brand;

    /** 物料分类（业务字典 wms_material_category），可为空 */
    private String category;

    private BigDecimal amount;

    private String remark;
}
