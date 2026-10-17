package com.lemon.erp.modules.hr.entity;

import com.lemon.erp.modules.hr.model.InterviewMethod;
import com.lemon.erp.modules.hr.model.InterviewResult;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 面试记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_interview")
public class HrInterview extends BaseEntity {

    /** 应聘者 */
    private Long candidateId;

    /** 轮次 */
    private Integer round;

    /** 面试时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime interviewTime;

    /** 面试官 */
    private Long interviewerId;

    /** 方式 ONSITE现场 VIDEO视频 PHONE电话 */
    private InterviewMethod method;

    /** 评分（0-100） */
    private Integer score;

    /** 结果(字典 hr_interview_result) PENDING待面试 PASS通过 FAIL未通过 */
    private InterviewResult result;

    /** 面试评价 */
    private String feedback;

    @TableField(exist = false)
    private String interviewerName;
}
