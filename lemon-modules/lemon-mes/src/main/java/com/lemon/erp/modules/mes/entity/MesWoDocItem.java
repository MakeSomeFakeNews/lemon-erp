package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_wo_doc_item")
public class MesWoDocItem extends BaseEntity {

    private Long docId;

    private Long materialId;

    private Long locationId;

    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expiryDate;

    private BigDecimal qty;

    private String serialNos;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String unit;

    @TableField(exist = false)
    private String locationCode;
}
