package com.lemon.erp.modules.fin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.fin.model.BillKind;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 期末调汇结果：按币种、应收 / 应付汇总的调汇差额，生成的调汇凭证和次月初的冲回凭证（没有差额时不生成凭证，凭证 ID 为空）
 */
public record FxRevaluation(List<Line> lines, Long voucherId, @JsonInclude(JsonInclude.Include.NON_NULL) Long reverseVoucherId) {

    /**
     * @param remainingFc 未结算外币金额
     * @param bookBase    账面本位币金额
     * @param newBase     按期末汇率折算的本位币金额
     */
    public record Line(String currency, BillKind kind, BigDecimal rate, BigDecimal remainingFc, BigDecimal bookBase,
                       BigDecimal newBase, BigDecimal diff) {
    }

    /** 未结清的外币应收应付单（数据库查询结果） */
    @Data
    public static class OpenBill {
        private Long id;
        private BillKind kind;
        private Long partnerId;
        private String currency;
        private BigDecimal remainingFc;
        private BigDecimal remainingBase;
    }
}
