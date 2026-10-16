package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工厂日历：按日期调整可用工时（放假为 0、加班），工作中心的设置优先于全厂（workCenterId = 0）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_calendar")
public class MesCalendar extends BaseEntity {

    private Long workCenterId;

    private LocalDate calDate;

    /** 当天每个资源的可用工时 */
    private BigDecimal hours;

    private String remark;
}
