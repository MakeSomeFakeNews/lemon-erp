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
     * @param groupBy MATERIAL / CUSTOMER / MONTH；按月份时按月份先后排列，其他按收入从高到低
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

    /**
     * 期间内已记账的付款凭证中，核销固定资产购置应付单（来源 ASSET）的部分：现金流量表把这部分付款归入「购建固定资产支付的现金」
     *
     * @return 只返回核销了固定资产应付单的付款凭证：[{voucherId, assetAmount（核销固定资产应付单的金额）, totalAmount（核销合计）}]
     */
    List<FinAnalysis.AssetPayment> selectAssetPayments(@Param("begin") int begin, @Param("end") int end);
}
