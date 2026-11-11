package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.dto.FinAnalysis;
import com.lemon.erp.modules.fin.dto.PartnerSummaryRow;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.modules.fin.model.BillKind;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 往来对账单：往来单位的应收应付单、收付款、预收预付
 */
public interface FinPartnerLedgerMapper {

    /** 日期之前的应收 / 应付单合计 */
    BigDecimal sumBillsBefore(@Param("billKind") BillKind billKind, @Param("partnerId") Long partnerId, @Param("date") LocalDate date);

    /** 日期之前已生效的收 / 付款合计 */
    BigDecimal sumSettlementsBefore(@Param("settleKind") SettleKind settleKind, @Param("partnerId") Long partnerId, @Param("date") LocalDate date);

    /** 日期之前的预收 / 预付款合计 */
    BigDecimal sumAdvancesBefore(@Param("settleKind") SettleKind settleKind, @Param("partnerId") Long partnerId, @Param("date") LocalDate date);

    /** 日期之前的应收应付对冲合计（客户按 customer_id，供应商按 supplier_id） */
    BigDecimal sumNettingsBefore(@Param("billKind") BillKind billKind, @Param("partnerId") Long partnerId, @Param("date") LocalDate date);

    /**
     * 期间内的明细，按日期排序
     *
     * @return [{bizDate, type（BILL / SETTLEMENT / ADVANCE / NETTING）, docNo, sourceType, sourceNo, amount, remark}]
     */
    List<FinAnalysis.PartnerFlow> selectRows(@Param("billKind") BillKind billKind, @Param("settleKind") SettleKind settleKind,
                                         @Param("partnerId") Long partnerId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 往来汇总：一个往来单位一行（当前余额），按净欠款降序；withAp 挂接关联供应商的应付（仅应收） */
    IPage<PartnerSummaryRow> selectSummary(IPage<PartnerSummaryRow> page, @Param("q") PartnerSummaryRow.Query query,
                                           @Param("settleKind") SettleKind settleKind, @Param("withAp") boolean withAp);

    /** 往来汇总的合计（同样的筛选条件） */
    PartnerSummaryRow selectSummaryTotal(@Param("q") PartnerSummaryRow.Query query, @Param("settleKind") SettleKind settleKind,
                                         @Param("withAp") boolean withAp);
}
