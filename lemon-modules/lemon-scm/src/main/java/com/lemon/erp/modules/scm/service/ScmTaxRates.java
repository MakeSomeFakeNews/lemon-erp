package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.dict.DictExtension;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.rule.BizRules;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 明细税率的默认值与发票类型（issue #82）。
 * <ul>
 *   <li>默认税率：业务参数 {@link ScmBizRules#DEFAULT_TAX_RATE}（默认 13%），明细、价格资料没有填写税率且没有其他来源时使用</li>
 *   <li>发票类型：业务字典 {@link #INVOICE_KIND_DICT}（开票类型，每个租户可自行增加），每个取值的扩展值是税率。
 *       订单、报价单、直接开单的出入库及其草稿的表头可选发票类型，明细未填写税率时取发票类型的税率</li>
 * </ul>
 * 明细未填写税率时的兜底顺序：发票类型的税率 → 默认税率
 */
@Component
@RequiredArgsConstructor
public class ScmTaxRates {

    /** 发票类型（开票类型）业务字典：取值的扩展值为税率（%） */
    public static final String INVOICE_KIND_DICT = "scm_invoice_kind";

    private final BizRules bizRules;
    private final DictLookup dictLookup;

    /** 默认税率（%） */
    public BigDecimal defaultRate() {
        return bizRules.decimal(ScmBizRules.DEFAULT_TAX_RATE);
    }

    /** 发票类型的税率；为空、已停用或没有设置税率时为 null */
    public BigDecimal rateOf(String invoiceKind) {
        if (invoiceKind == null || invoiceKind.isBlank()) {
            return null;
        }
        return dictLookup.extValues(INVOICE_KIND_DICT).get(invoiceKind);
    }

    /** 明细未填写税率时使用的税率：发票类型的税率，没有时为默认税率 */
    public BigDecimal fallback(String invoiceKind) {
        BigDecimal rate = rateOf(invoiceKind);
        return rate != null ? rate : defaultRate();
    }

    /** 明细税率：填写了用填写的，否则按 {@link #fallback} */
    public BigDecimal resolve(BigDecimal taxRate, String invoiceKind) {
        return taxRate != null ? taxRate : fallback(invoiceKind);
    }

    /**
     * 发票类型须为业务字典中启用的取值；与 unchanged（修改前的值、来源单据的值）相同时不校验，已停用的旧值可以保留
     *
     * @return 规范后的值（空白为 null）
     */
    public String checkInvoiceKind(String invoiceKind, String unchanged) {
        String kind = invoiceKind == null || invoiceKind.isBlank() ? null : invoiceKind.trim();
        if (kind != null && !Objects.equals(kind, unchanged)) {
            dictLookup.check(INVOICE_KIND_DICT, kind, "scm.label.invoiceKind");
        }
        return kind;
    }

    /** 开票类型字典的扩展值是税率：0 ~ 100，最多 2 位小数（与明细税率一致） */
    @Configuration(proxyBeanMethods = false)
    static class Dicts {
        @Bean
        DictExtension scmInvoiceKindExtension() {
            return new DictExtension(INVOICE_KIND_DICT, "scm.label.taxRatePercent", 2, BigDecimal.ZERO, Decimals.HUNDRED);
        }
    }
}
