package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.scm.dto.OrderSummaryRow;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.scm.service.ScmOrderStatsService;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;

/**
 * 订单统计（只读）：按往来单位、业务员、物料、月份汇总订单金额
 */
public interface ScmOrderStatsMapper {

    /**
     * 订单明细按维度汇总，金额从高到低（按月时按月份先后）
     *
     * @param partner 往来单位名称或编码，模糊匹配
     * @param material 物料名称或编码，模糊匹配：只统计该物料的明细
     * @param salesman 业务员姓名，模糊匹配
     */
    Page<OrderSummaryRow> selectSummary(Page<OrderSummaryRow> page, @Param("kind") OrderKind kind,
                                        @Param("by") ScmOrderStatsService.GroupBy by,
                                        @Param("statuses") Collection<OrderStatus> statuses,
                                        @Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                        @Param("partner") String partner, @Param("material") String material,
                                        @Param("salesman") String salesman);

    /** 与 selectSummary 相同条件的合计（groupId 等为空） */
    OrderSummaryRow selectTotal(@Param("kind") OrderKind kind, @Param("statuses") Collection<OrderStatus> statuses,
                                @Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                @Param("partner") String partner, @Param("material") String material,
                                @Param("salesman") String salesman);
}
