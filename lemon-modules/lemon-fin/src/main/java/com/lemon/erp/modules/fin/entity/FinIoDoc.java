package com.lemon.erp.modules.fin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.fin.model.BillSource;
import com.lemon.erp.modules.fin.model.FundDirection;
import com.lemon.erp.modules.fin.model.IoPartnerType;
import com.lemon.erp.modules.fin.model.IoStatus;
import com.lemon.erp.modules.fin.model.VoucherStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 其他收支单：其他收入单（废料销售、佣金收入等，IN）、费用支出单（房租、水电、差旅等，OUT）。
 * 确认后生成凭证（收入 借 结算账户 / 贷 类别科目；支出 借 类别科目 / 贷 结算账户），进入出纳日记账和资金日报
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_io_doc")
public class FinIoDoc extends BaseEntity {

    private String docNo;

    /** IN 其他收入 / OUT 费用支出 */
    private FundDirection direction;

    private LocalDate docDate;

    /** 票据编号（发票号、收据号等） */
    private String ticketNo;

    /** 关联单位类型，可为空 */
    private IoPartnerType partnerType;

    /** 客户 / 供应商（关联单位类型为客户、供应商时） */
    private Long partnerId;

    /** 其他单位的名称（关联单位类型为其他时） */
    private String partnerName;

    /** 结算方式（业务字典 fin_settle_method） */
    private String settleMethod;

    /** 结算账户：现金 / 银行末级科目 */
    private Long accountId;

    /** 费用归属部门（支出凭证的分录带部门，预算按部门统计），可为空 */
    private Long deptId;

    /** 金额合计（明细之和） */
    private BigDecimal amount;

    private IoStatus status;

    private Long voucherId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long confirmBy;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    private String remark;

    /** 来源单据类型：由业务单据生成时填写（销售出库、采购退货现付的运费），手工录入为空；有来源的单据不能修改、删除 */
    private BillSource sourceType;

    private Long sourceId;

    private String sourceNo;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 关联单位的显示名称：客户 / 供应商取资料名称，其他取填写的名称 */
    @TableField(exist = false)
    private String partnerLabel;

    @TableField(exist = false)
    private String accountName;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private String voucherNo;

    @TableField(exist = false)
    private VoucherStatus voucherStatus;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private String confirmByName;

    /** 列表：明细的类别名称（顿号分隔） */
    @TableField(exist = false)
    private String categoryNames;

    @TableField(exist = false)
    private List<FinIoDocItem> items;
}
