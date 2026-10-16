package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 记账凭证：未审核 → 已审核 → 已记账，只有已记账的凭证进入账簿和报表。只作为查询结果返回，新增 / 修改使用 VoucherRequest
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_voucher")
public class FinVoucher extends BaseEntity {

    private Integer period;

    private Integer voucherSeq;

    /** 凭证字号，如 记-0001 */
    private String voucherNo;

    private LocalDate voucherDate;

    private String summary;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;

    private Integer attachmentCount;

    /** VoucherStatus */
    private String status;

    /** VoucherSource */
    private String sourceType;

    private String sourceNo;

    /** 审批流程实例ID */
    private String flowInstanceId;

    /** 红字冲销凭证：被冲销的原凭证 */
    private Long reversalOf;

    /** 已被红字冲销 1 */
    private Integer reversed;

    private Long createBy;

    private Long auditBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    private Long postBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime postTime;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private String auditByName;

    @TableField(exist = false)
    private String postByName;

    /** 分录，仅详情返回 */
    @TableField(exist = false)
    private List<FinVoucherEntry> entries;
}
