package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 生产统计报表的查询条件（GET 参数），日期范围必填。报工明细的 operatorId / opName / workCenterId / day / productId 由汇总行穿透传入
 */
@Data
public class MesStatQuery {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate begin;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate end;

    /** 操作员（用户 ID） */
    private Long operatorId;
    private Long workCenterId;
    /** 穿透：工作中心为空的工序 */
    private Boolean noWorkCenter;
    /** 工序名称 */
    private String opName;
    /** 产品 */
    private Long productId;
    /** 产品分类（业务字典） */
    private String category;
    /** 车间（工单的部门） */
    private Long deptId;
    /** 报工日期（穿透某一天） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate day;
    /** 物料（用料差异） */
    private Long materialId;
    /** 工单状态（工单完成情况） */
    private WorkOrderStatus status;
    /** 产品编码 / 名称、工单号 */
    private String keyword;
}
