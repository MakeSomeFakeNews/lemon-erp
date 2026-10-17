package com.lemon.erp.modules.fin.mapper;

import com.lemon.erp.modules.fin.dto.UninvoicedRow;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.InvoiceStatus;
import com.lemon.erp.modules.fin.model.InvoiceKind;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.fin.entity.FinInvoice;
import com.lemon.erp.modules.fin.entity.FinInvoiceItem;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface FinInvoiceMapper extends BaseMapper<FinInvoice> {

    Page<FinInvoice> selectPageList(Page<FinInvoice> page, @Param("kind") InvoiceKind kind, @Param("keyword") String keyword,
                                    @Param("partnerId") Long partnerId, @Param("status") InvoiceStatus status,
                                    @Param("unmatched") boolean unmatched);

    /** 增加勾稽金额，超过发票金额时返回 0 */
    int addMatched(@Param("id") Long id, @Param("amount") java.math.BigDecimal amount);

    FinInvoice selectDetail(@Param("id") Long id);

    List<FinInvoiceItem> selectItems(@Param("invoiceId") Long invoiceId);

    /** 往来单位未完全开票 / 收票的应收应付单（期初单据除外） */
    List<com.lemon.erp.modules.fin.entity.FinBill> selectOpenBills(@Param("billKind") BillKind billKind, @Param("partnerId") Long partnerId);

    /**
     * 未开票 / 未收票（暂估）：截至日期的应收应付单中尚未勾稽发票的金额，按往来单位汇总
     *
     * @return [{partnerId, partnerName, billCount, amount, invoiced, uninvoiced}]
     */
    List<UninvoicedRow> selectUninvoiced(@Param("billKind") BillKind billKind, @Param("asOf") LocalDate asOf);
}
