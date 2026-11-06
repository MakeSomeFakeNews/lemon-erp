package com.lemon.erp.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.ai.model.AiReportPeriodType;
import com.lemon.erp.modules.ai.model.AiReportStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能报告的列表项 / 详情：列表不含正文（content 为空），详情才有；快照另行读取
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiReportView {

    private Long id;
    private AiReportPeriodType periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String title;
    /** 生成人，自动生成时为空 */
    private Long userId;
    private String userName;
    /** 是否定时任务自动生成 */
    private boolean auto;
    /** 包含的数据来源标识（逗号分隔，存库的原值） */
    @JsonIgnore
    private String sources;
    /** 包含的数据来源名称（当前语言） */
    private List<String> sourceLabels;
    private AiReportStatus status;
    private String error;
    private Integer promptTokens;
    private Integer completionTokens;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    /** 正文（Markdown），只有详情返回 */
    private String content;
}
