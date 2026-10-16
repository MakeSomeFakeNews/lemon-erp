package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 委外收货核销用料记录：成本核算按期间把核销的子件成本从委托加工物资转入委外件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_outsource_consume")
public class MesOutsourceConsume extends BaseEntity {

    private Long orderId;

    /** 采购入库 / 退货单 */
    private Long tradeDocId;

    private Long productId;

    private Long materialId;

    /** 核销数量，采购退货为负数 */
    private BigDecimal qty;

    private LocalDate bizDate;
}
