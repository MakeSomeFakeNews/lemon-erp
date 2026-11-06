package com.lemon.erp.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ai.model.AiReportPeriodType;
import com.lemon.erp.modules.ai.model.AiReportStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 智能报告（周报 / 月报）：手动生成的只有生成人能看；定时任务自动生成的（userId 为空）包含全公司各板块数据，
 * 只有「智能报告」权限且数据权限为「全部数据」的用户能看。content、snapshot 是大字段，默认不查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report")
public class AiReport extends BaseEntity {

    private AiReportPeriodType periodType;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    /** 标题（默认语言，如「周报（2026-10-26 ~ 2026-11-01）」） */
    private String title;

    /** 报告正文（Markdown） */
    @TableField(select = false)
    private String content;

    /** 生成人，定时任务自动生成时为空 */
    private Long userId;

    /** 包含的数据来源标识，逗号分隔 */
    private String sources;

    /** 交给模型的数据快照（JSON） */
    @TableField(select = false)
    private String snapshot;

    private AiReportStatus status;

    /** 失败原因 */
    private String error;

    private Integer promptTokens;

    private Integer completionTokens;
}
