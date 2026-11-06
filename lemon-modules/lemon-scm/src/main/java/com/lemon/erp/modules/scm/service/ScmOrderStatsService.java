package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.scm.dto.OrderSummaryRow;
import com.lemon.erp.modules.scm.mapper.ScmOrderStatsMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 订单统计（只读）：已审核及以后（含执行中、已完成、已关闭）的订单明细按往来单位、业务员、物料、月份汇总，
 * 金额为按汇率折算的本位币价税合计。按订单的数据权限（制单部门 / 制单人）过滤，与订单列表一致
 */
@Service
@RequiredArgsConstructor
public class ScmOrderStatsService {

    /** 汇总维度 */
    public enum GroupBy {
        /** 往来单位：销售为客户，采购为供应商 */
        PARTNER,
        /** 业务员（销售订单） */
        SALESMAN,
        MATERIAL,
        MONTH
    }

    /**
     * 汇总条件
     *
     * @param partner  往来单位名称或编码，模糊匹配
     * @param material 物料名称或编码，模糊匹配：只统计该物料的明细
     * @param salesman 业务员姓名，模糊匹配
     */
    public record SummaryQuery(OrderKind kind, GroupBy by, LocalDate begin, LocalDate end, String partner, String material,
                               String salesman) {
    }

    private final ScmOrderStatsMapper statsMapper;

    /** 前 size 个分组（total 为分组总数） */
    @DataPermission(table = "scm_order", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<OrderSummaryRow> summary(SummaryQuery q, long size) {
        Page<OrderSummaryRow> page = Page.of(1, size);
        statsMapper.selectSummary(page, q.kind(), q.by(), OrderStatus.AUDITED, q.begin(), q.end(), q.partner(), q.material(),
                q.salesman());
        return PageResult.of(page);
    }

    /** 相同条件的合计：订单数、数量、金额 */
    @DataPermission(table = "scm_order", deptColumn = "dept_id", userColumn = "create_by")
    public OrderSummaryRow total(SummaryQuery q) {
        return statsMapper.selectTotal(q.kind(), OrderStatus.AUDITED, q.begin(), q.end(), q.partner(), q.material(), q.salesman());
    }
}
