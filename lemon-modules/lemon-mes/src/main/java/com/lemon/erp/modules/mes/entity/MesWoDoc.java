package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 工单领料 / 退料 / 完工入库，保存即生成并确认仓库单据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_wo_doc")
public class MesWoDoc extends BaseEntity {

    private String docNo;

    /** WoDocKind：ISSUE / RETURN / FINISH / SUPPLEMENT */
    private String kind;

    private Long woId;

    private Long warehouseId;

    private LocalDate bizDate;

    private BigDecimal totalQty;

    private Long wmsDocId;

    private String wmsDocNo;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String woNo;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private List<MesWoDocItem> items;
}
