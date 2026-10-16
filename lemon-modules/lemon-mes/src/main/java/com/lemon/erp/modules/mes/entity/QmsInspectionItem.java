package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.modules.mes.model.ItemMethod;
import com.lemon.erp.modules.mes.model.QcResult;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 检验单的逐项检验结果；定量项目的实测值用于 SPC
 */
@Data
@TableName("qms_inspection_item")
public class QmsInspectionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inspectionId;

    private Long planItemId;

    private Integer seq;

    private String name;

    private ItemMethod method;

    private String standard;

    private BigDecimal lowerLimit;

    private BigDecimal upperLimit;

    private String unit;

    private Integer critical;

    /** 定量项目的实测值，逗号分隔 */
    private String measured;

    /** 不合格样本数：定性项目为录入值，定量项目为超出上下限的实测值个数 */
    private Integer defectCount;

    /** PASS / FAIL */
    private QcResult result;
}
