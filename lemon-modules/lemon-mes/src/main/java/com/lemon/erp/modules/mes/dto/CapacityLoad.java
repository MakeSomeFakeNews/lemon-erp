package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.WorkOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 工作中心产能负荷：期间合计、每日产能与负荷、在制工序（按计划开工日期）
 *
 * @param capacity 期间可用工时
 * @param load     期间负荷工时（在制工序剩余工时按计划日期均摊到工作日）
 */
public record CapacityLoad(Long workCenterId, String code, String name, BigDecimal capacity, BigDecimal load,
                           List<Day> days, List<Op> ops) {

    public record Day(LocalDate date, BigDecimal capacity, BigDecimal load) {
    }

    /**
     * @param hours 剩余工时
     */
    public record Op(Long woId, String woNo, WorkOrderStatus woStatus, Long opId, Integer seq, String opName,
                     LocalDate planStart, LocalDate planEnd, BigDecimal hours) {
    }
}
