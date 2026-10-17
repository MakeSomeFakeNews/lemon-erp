package com.lemon.erp.modules.fin.mapper;

import com.lemon.erp.modules.fin.dto.FinAnalysis;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 经营分析查询：毛利、存货对账、现金流量
 */
public interface FinAnalysisMapper {

    /**
     * 销售毛利明细：销售出库 / 退货（不含税）按维度汇总，成本 = 数量 × 当期加权平均单价（没有核算时取参考成本价）
     *
     * @param groupBy MATERIAL / CUSTOMER / MONTH
     */
    List<FinAnalysis.MarginRow> selectGrossMargin(@Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                              @Param("groupBy") String groupBy, @Param("customerId") Long customerId);

    /**
     * 销售毛利明细：每条销售出库 / 退货明细的收入、成本（数量 × 当期加权平均单价，未核算时取参考成本价），退货为负
     *
     */
    List<FinAnalysis.MarginDetailRow> selectMarginDetail(@Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                                         @Param("customerId") Long customerId, @Param("keyword") String keyword,
                                                         @Param("limit") int limit);

    /** 成本核算的期末金额按物料分类汇总 */
    List<FinAnalysis.CategoryAmount> selectCostCloseByCategory(@Param("period") int period);

    /**
     * 物料数量核对：成本核算的期末数量与截至期末的库存流水合计（含在途调拨的调出）
     *
     * @return 只返回不一致的
     */
    List<FinAnalysis.QtyDiff> selectQtyDiff(@Param("period") int period, @Param("end") LocalDate end);

    /**
     * 涉及库存现金、银行存款的已记账凭证的全部分录
     *
     * @param cashCodes 货币资金科目编码（含下级），见 FinAccountService.cashCodes
     */
    List<FinAnalysis.CashEntry> selectCashVoucherEntries(@Param("begin") int begin, @Param("end") int end, @Param("cashCodes") List<String> cashCodes);
}
