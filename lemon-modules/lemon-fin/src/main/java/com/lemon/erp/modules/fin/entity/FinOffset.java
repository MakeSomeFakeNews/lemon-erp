package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.OffsetStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 冲账单：垫付运费、销售返利、三包赔偿等不付现金、用来抵扣往来款的项目。复核后生成红字应收 / 应付单，收付款时与欠款冲抵
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_offset")
public class FinOffset extends BaseEntity {

    private String offsetNo;

    /** AR 应收冲账（客户）/ AP 应付冲账（供应商） */
    private BillKind kind;

    private Long partnerId;

    private LocalDate offsetDate;

    /** 经办人（用户） */
    private Long handlerId;

    /** 冲账项目，业务字典 fin_offset_item */
    private String item;

    /** 冲账金额（正数） */
    private BigDecimal amount;

    /** 冲账项目的期间（如返利的统计期间），可为空 */
    private LocalDate beginDate;

    private LocalDate endDate;

    /** 票据编号 */
    private String ticketNo;

    /** 凭证的对方科目，为空时取凭证科目设置（应收冲账 / 应付冲账） */
    private Long accountId;

    private OffsetStatus status;

    /** 复核生成的红字应收 / 应付单 */
    private Long billId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long reviewBy;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String partnerCode;

    @TableField(exist = false)
    private String partnerName;

    @TableField(exist = false)
    private String handlerName;

    @TableField(exist = false)
    private String accountName;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private String reviewByName;

    @TableField(exist = false)
    private String billNo;

    /** 红字单据的已核销金额（负数）、凭证 */
    @TableField(exist = false)
    private BigDecimal billSettledAmount;

    @TableField(exist = false)
    private Long voucherId;

    @TableField(exist = false)
    private String voucherNo;

    /** 红字单据凭证的状态（已记账时不能取消复核） */
    @TableField(exist = false)
    private com.lemon.erp.modules.fin.model.VoucherStatus voucherStatus;
}
