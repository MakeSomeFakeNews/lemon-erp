package com.lemon.erp.modules.fin.model;

import com.lemon.erp.common.exception.BizException;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 财务模块的通用约定：单据种类与来源、物料分类、会计期间换算。自动生成凭证使用的科目见 {@link AccountRole}
 */
public final class FinConstants {

    /** 本位币 */
    public static final String BASE_CURRENCY = "CNY";
    /** 应收单 / 应付单种类（fin_bill.kind） */
    public static final String BILL_AR = "AR";
    public static final String BILL_AP = "AP";
    /** 应收应付单来源（字典 fin_bill_source）：供应商质量索赔，红字应付单 */
    public static final String BILL_SOURCE_QUALITY_CLAIM = "QUALITY_CLAIM";
    /** 销售返利（红字应收单） */
    public static final String BILL_SOURCE_REBATE = "REBATE";
    /** 项目开票（计入合同负债） */
    public static final String BILL_SOURCE_PROJECT = "PROJECT";
    /** 服务工单收费（计入其他业务收入） */
    public static final String BILL_SOURCE_TICKET = "TICKET";

    /** 库存现金、银行存款：收付款的结算科目必须在其下；现金流量表按此识别现金科目 */
    public static final String CASH = "1001";
    public static final String BANK = "1002";
    /** 进项税额、销项税额：现金流量表按此归类（自动凭证使用的科目见 AccountRole） */
    public static final String INPUT_VAT = "222101";
    public static final String OUTPUT_VAT = "222102";

    /** 物料分类：原材料、半成品、成品（字典 wms_material_category） */
    public static final String CATEGORY_RAW = "RAW";
    public static final String CATEGORY_SEMI = "SEMI";
    public static final String CATEGORY_FINISHED = "FINISHED";
    /** 物料分类：低值易耗品，存货科目为周转材料 */
    public static final String LOW_VALUE_CATEGORY = "LOW_VALUE";

    private FinConstants() {
    }

    /** 日期所在的会计期间 yyyyMM */
    public static int periodOf(LocalDate date) {
        return date.getYear() * 100 + date.getMonthValue();
    }

    public static YearMonth toYearMonth(int period) {
        int month = period % 100;
        if (month < 1 || month > 12 || period < 190001 || period > 299912) {
            throw new BizException("会计期间格式不正确：{0}", period);
        }
        return YearMonth.of(period / 100, month);
    }

    public static LocalDate firstDay(int period) {
        return toYearMonth(period).atDay(1);
    }

    public static LocalDate lastDay(int period) {
        return toYearMonth(period).atEndOfMonth();
    }

    public static int previous(int period) {
        YearMonth ym = toYearMonth(period).minusMonths(1);
        return ym.getYear() * 100 + ym.getMonthValue();
    }
}
