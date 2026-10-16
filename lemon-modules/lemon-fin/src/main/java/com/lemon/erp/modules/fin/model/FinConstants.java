package com.lemon.erp.modules.fin.model;

import com.lemon.erp.common.exception.BizException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

/**
 * 财务模块的通用约定：自动生成凭证使用的科目编码（init.sql 预置），会计期间换算
 */
public final class FinConstants {

    /** 库存现金、银行存款：收付款的结算科目必须在其下 */
    /** 本位币 */
    public static final String BASE_CURRENCY = "CNY";
    /** 应收单 / 应付单种类（fin_bill.kind） */
    public static final String BILL_AR = "AR";
    public static final String BILL_AP = "AP";
    /** 应收应付单来源（字典 fin_bill_source）：供应商质量索赔，红字应付单 */
    public static final String BILL_SOURCE_QUALITY_CLAIM = "QUALITY_CLAIM";
    /** 销售返利（红字应收单） */
    public static final String BILL_SOURCE_REBATE = "REBATE";

    public static final String CASH = "1001";
    public static final String BANK = "1002";
    public static final String RECEIVABLE = "1122";
    /** 应收票据 */
    public static final String NOTES_RECEIVABLE = "1121";
    /** 应付票据 */
    public static final String NOTES_PAYABLE = "2201";
    /** 预付账款：预付供应商 */
    public static final String PREPAYMENT = "1123";
    /** 其他应收款：员工借款 */
    public static final String OTHER_RECEIVABLE = "1221";
    /** 原材料：原材料、包装材料等 */
    public static final String RAW_MATERIAL = "1403";
    /** 库存商品：半成品、成品 */
    public static final String GOODS = "1405";
    /** 委托加工物资：委外发出的材料 */
    public static final String OUTSOURCE_MATERIAL = "1408";
    public static final String FIXED_ASSET = "1601";
    public static final String DEPRECIATION = "1602";
    public static final String ASSET_DISPOSAL = "1606";
    /** 待处理财产损溢：盘盈盘亏等其他出入库 */
    public static final String PENDING_LOSS = "1901";
    public static final String PAYABLE = "2202";
    /** 预收账款：预收客户 */
    public static final String ADVANCE_RECEIVED = "2203";
    /** 合同负债：项目开票先计入，按完工百分比确认收入时转入主营业务收入 */
    public static final String CONTRACT_LIABILITY = "2205";
    public static final String OTHER_PAYABLE = "2241";
    public static final String SALARY = "2211";
    public static final String INPUT_VAT = "222101";
    public static final String OUTPUT_VAT = "222102";
    /** 未交增值税、城市维护建设税、教育费附加、地方教育附加 */
    public static final String UNPAID_VAT = "222104";
    public static final String CITY_TAX = "222105";
    public static final String EDU_SURCHARGE = "222106";
    public static final String LOCAL_EDU_SURCHARGE = "222107";
    /** 税金及附加 */
    public static final String TAX_SURCHARGE = "6403";
    public static final String PROFIT_THIS_YEAR = "4103";
    /** 利润分配：年末本年利润转入 */
    public static final String PROFIT_DISTRIBUTION = "4104";
    /** 营业外收入 / 支出：固定资产处置净损益 */
    public static final String NON_OPERATING_INCOME = "6301";
    public static final String NON_OPERATING_EXPENSE = "6711";
    public static final String PRODUCTION_COST = "5001";
    public static final String MANUFACTURING_OVERHEAD = "5101";
    /** 合同履约成本：项目领料、项目人工和费用 */
    public static final String CONTRACT_COST = "5401";
    public static final String REVENUE = "6001";
    public static final String COST_OF_SALES = "6401";
    public static final String SELLING_EXPENSE = "6601";
    /** 财务费用：汇兑损益 */
    public static final String FINANCE_EXPENSE = "6603";
    public static final String OTHER_REVENUE = "6051";
    public static final String TRAINING_EXPENSE = "660205";

    /** 计入库存商品的物料分类，其余计入原材料 */
    private static final Set<String> GOODS_CATEGORIES = Set.of("SEMI", "FINISHED");
    /** 物料分类：低值易耗品，存货科目为周转材料 */
    public static final String LOW_VALUE_CATEGORY = "LOW_VALUE";
    /** 周转材料（低值易耗品） */
    public static final String TURNOVER_MATERIAL = "1411";

    private FinConstants() {
    }

    /** 物料分类对应的存货科目 */
    public static String inventoryAccount(String materialCategory) {
        if (LOW_VALUE_CATEGORY.equals(materialCategory)) {
            return TURNOVER_MATERIAL;
        }
        return GOODS_CATEGORIES.contains(materialCategory) ? GOODS : RAW_MATERIAL;
    }

    /** 日期所在的会计期间 yyyyMM */
    public static int periodOf(LocalDate date) {
        return date.getYear() * 100 + date.getMonthValue();
    }

    public static YearMonth toYearMonth(int period) {
        int month = period % 100;
        if (month < 1 || month > 12 || period < 190001 || period > 299912) {
            throw new BizException("会计期间格式不正确：" + period);
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
