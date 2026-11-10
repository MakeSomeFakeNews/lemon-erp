package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.BillKind;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 往来汇总（应收 / 应付）：一个往来单位一行，当前余额（本位币）。
 * 净欠款 = 期初余额 + 单据余额 + 预收 / 预付余额（负数）；坏账核销已计入单据的已核销，单列只作参考。
 * 与往来对账单的关系：对账单期末余额（截至今天）= 净欠款 + 坏账核销（对账单不扣坏账；外币单据另有收付汇率与记账汇率的差额）
 */
@Data
public class PartnerSummaryRow {

    private Long partnerId;
    private String partnerCode;
    private String partnerName;
    /** 负责人（客户的负责销售；供应商没有负责人） */
    private String ownerName;

    /** 期初应收 / 应付余额：期初往来（手工录入、导入的单据）的未核销余额 */
    private BigDecimal opening;
    /** 应收 / 应付单据余额：业务单据（出入库、退货、运费、返利、冲账单等，红字为负数）的未核销余额 */
    private BigDecimal billBalance;
    /** 预收 / 预付未冲抵余额，负数（冲减欠款） */
    private BigDecimal advanceBalance;
    /** 坏账核销累计（应付为无法支付转销），已从单据余额中扣除 */
    private BigDecimal badDebt;
    /** 净欠款合计 = 期初 + 单据余额 + 预收预付（负数） */
    private BigDecimal net;

    /** 汇总查询条件 */
    public record Query(BillKind kind, Long partnerId, String keyword, Long ownerId, boolean onlyBalance) {
    }
}
