package com.lemon.erp.modules.fin.model;

import com.lemon.erp.common.exception.BizException;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 财务模块的通用约定：本位币、物料分类、岗位类别、会计期间换算。
 * 科目编码不在这里写死：自动生成凭证、结算科目（库存现金、银行存款）等使用的科目见 {@link AccountRole}，可在「凭证科目设置」中修改
 */
public final class FinConstants {

    /** 本位币 */
    public static final String BASE_CURRENCY = "CNY";

    /** 物料分类：原材料、半成品、成品（字典 wms_material_category） */
    public static final String CATEGORY_RAW = "RAW";
    public static final String CATEGORY_SEMI = "SEMI";
    public static final String CATEGORY_FINISHED = "FINISHED";
    /** 物料分类：低值易耗品，存货科目为周转材料 */
    public static final String LOW_VALUE_CATEGORY = "LOW_VALUE";

    /** 岗位类别（业务字典 hr_position_category）的内置取值：生产岗位、销售岗位，工资计提按此取费用科目 */
    public static final String POSITION_PRODUCTION = "PRODUCTION";
    public static final String POSITION_SALES = "SALES";

    private FinConstants() {
    }

    /** 日期所在的会计期间 yyyyMM */
    public static int periodOf(LocalDate date) {
        return date.getYear() * 100 + date.getMonthValue();
    }

    public static YearMonth toYearMonth(int period) {
        int month = period % 100;
        if (month < 1 || month > 12 || period < 190001 || period > 299912) {
            throw new BizException("fin.error.invalidAccountingPeriodFormat", period);
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
