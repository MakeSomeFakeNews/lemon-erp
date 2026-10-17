package com.lemon.erp.modules.fin.mapper;

import com.lemon.erp.modules.fin.dto.FinAnalysis;
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

    /**
     * 期间内的明细，按日期排序
     *
     * @return [{bizDate, type（BILL / SETTLEMENT / ADVANCE）, docNo, sourceType, sourceNo, amount, remark}]
     */
    List<FinAnalysis.PartnerFlow> selectRows(@Param("billKind") BillKind billKind, @Param("settleKind") SettleKind settleKind,
                                         @Param("partnerId") Long partnerId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);
}
